/*
 * Copyright (C) 2006-2012 TopCoder Inc., All Rights Reserved.
 */

package com.topcoder.management.phase.autopilot;

import com.topcoder.management.phase.autopilot.logging.LogMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <p>
 * This is the main class which performs auto-pilot for projects. Auto-piloting a project means
 * automating project phases execution. There are two kinds of project phases execution:<br>
 * <br> - ending a project phase (if it's open and certain conditions are met)<br>
 * <br> - starting a project phase (if it's scheduled and certain conditions are met)<br>
 * This class delegates the project phase execution to ProjectPilot interface. The projects' ids to
 * auto-pilot can be supplied programmatically or automatically searched from all projects who meet
 * certain criteria. The task of searching projects is delegated to AutoPilotSource implementation.
 * Note, this class doesn't poll/execute phase change at certain intervals. See AutoPilotJob for
 * that.<br>
 * This class is immutable, however it's not guaranteed to be thread-safe. Thread-safety will depend
 * on the instance of AutoPilotSource & ProjectPilot. Calling advanceProjects from multiple thread
 * may confuse ProjectPilot because phase status may be changed from other thread. Multi-threaded
 * applications are advised to lock on the AutoPilot instance to ensure thread-safety.
 * </p>
 * @author sindu, abelli
 * @version 1.0.2
 */
public class AutoPilot {

    /**
     * <p>The log used by this class for logging errors and debug information.</p>
     */
    private static final Logger log = LoggerFactory.getLogger(AutoPilot.class);

    /**
     * The collection which hold ids of processing project.
     */
    private static final Set processingProjectIds = new HashSet();

    /**
     * Zero length <code>AutoPilotResult</code> array, which can be returned by
     * {@link #advanceProjects(long[], String)}.
     */
    private static final AutoPilotResult[] ZERO_AUTO_PILOT_RESULT_ARRAY = new AutoPilotResult[0];

    private static final ExecutorService THREAD_POOL = Executors.newFixedThreadPool(10);

    /**
     * <p>
     * Represents the AutoPilotSource instance that is used to retrieve a list of project ids to
     * auto-pilot. This variable is initially null, initialized in constructor using object factory
     * and immutable afterwards. It can be retrieved with the getter. It's used in
     * advanceProjects(String) to retrieve project ids which are to advance.
     * </p>
     */
    private AutoPilotSource autoPilotSource;

    /**
     * <p>
     * Represents the ProjectPilot instance that is used to pilot a project (start/end project
     * phases). This variable is initially null, initialized in constructor using object factory and
     * immutable afterwards. It can be retrieved with the getter. It's used in advanceProject(long,
     * String) to advance the given project id phases.
     * </p>
     */
    private ProjectPilot projectPilot;

    /**
     * <p>
     * Return the auto pilot source instance used by this class.
     * </p>
     * @return the auto pilot source instance used by this class
     */
    protected AutoPilotSource getAutoPilotSource() {
        return autoPilotSource;
    }

    /**
     * <p>
     * Return the project pilot instance used by this class.
     * </p>
     * @return the project pilot instance used by this class
     */
    protected ProjectPilot getProjectPilot() {
        return projectPilot;
    }

    public void setAutoPilotSource(AutoPilotSource autoPilotSource) {
        this.autoPilotSource = autoPilotSource;
    }

    public void setProjectPilot(ProjectPilot projectPilot) {
        this.projectPilot = projectPilot;
    }

    /**
     * <p>
     * The main method which will retrieve all projects to auto-pilot using AutoPilotSource. Each
     * project is then advanced using ProjectPilot. This is repeated until no more phase changes can
     * be made for all projects.
     * </p>
     * @param operator the operator (used for auditing)
     * @return an array of AutoPilotResult representing result of auto-pilot (never null, but can be
     *         empty)
     * @throws IllegalArgumentException if operator is null or empty (trimmed) string
     * @throws AutoPilotSourceException if any error occurs retrieving project ids from
     *             AutoPilotSource
     * @throws PhaseOperationException if any error occurs while ending/starting a phase
     */
    public AutoPilotResult[] advanceProjects(String operator) throws AutoPilotSourceException,
        PhaseOperationException {
        // Check arguments.
        if (null == operator) {
            throw new IllegalArgumentException("operator cannot be null");
        }
        if (operator.trim().length() < 1) {
            throw new IllegalArgumentException("operator cannot be empty");
        }

        long[] projIds = autoPilotSource.getProjectIds();
        log.info(String.format("Found %d active projects", projIds.length));
        return advanceProjects(projIds, operator);
    }

    /**
     * <p>
     * Another convenient method to auto-pilot given project id list. Each project is advanced using
     * ProjectPilot. This is repeated until no more phase changes can be made for all projects.
     * </p>
     * @param projectId a list of project id to auto-pilot
     * @param operator the operator (used for auditing)
     * @return an array of AutoPilotResult representing result of auto-pilot (never null, but can be
     *         empty)
     * @throws IllegalArgumentException if operator is null or empty (trimmed) string or project id
     *             is null
     * @throws PhaseOperationException if any error occurs while ending/starting a phase
     */
    public AutoPilotResult[] advanceProjects(long[] projectId, String operator) throws PhaseOperationException {
        // Check arguments.
        if (null == operator) {
            throw new IllegalArgumentException("operator cannot be null");
        }
        if (operator.trim().length() < 1) {
            throw new IllegalArgumentException("operator cannot be empty");
        }
        if (null == projectId) {
            throw new IllegalArgumentException("projectId cannot be null");
        }

        if (projectId.length < 1) {
            return ZERO_AUTO_PILOT_RESULT_ARRAY;
        }
        log.debug(new LogMessage(null, operator, "Checking active projects: " + getIdString(projectId)).toString());

        // Map key is Long (project id). Map value is AutoPilotResult instance.
        Map resMap = new HashMap();
        CountDownLatch latch = new CountDownLatch(projectId.length);
        for (long id : projectId) {
            THREAD_POOL.execute(() -> {
                AutoPilotResult result = null;
                Long longProjectId = new Long(id);

                // Check if the project is processing by another thread
                synchronized (processingProjectIds) {
                    if (processingProjectIds.contains(longProjectId)) {
                        log.info(
                                new LogMessage(null, operator, "Stopped in synchronized for projectId=" + longProjectId)
                                        .toString());
                        return;
                    } else {
                        processingProjectIds.add(longProjectId);
                    }
                }

                try {
                    result = advanceProject(longProjectId, operator);
                    // store/aggregate into Map
                    if (resMap.containsKey(longProjectId)) {
                        // Aggregate the result only if at least one of counters > 0.
                        if (result.getPhaseEndedCount() > 0 || result.getPhaseStartedCount() > 0) {
                            ((AutoPilotResult) resMap.get(longProjectId)).aggregate(result);
                        }
                    } else {
                        resMap.put(longProjectId, result);
                    }
                } finally {
                    // Make sure this project can be processed by next thread
                    synchronized (processingProjectIds) {
                        processingProjectIds.remove(longProjectId);
                        latch.countDown();
                    }
                }
            });
        }
        try {
            latch.await();
        } catch (InterruptedException E) {

        }

        return (AutoPilotResult[]) resMap.values().toArray(new AutoPilotResult[resMap.size()]);
    }

    /**
     * <p>
     * Another convenient method to auto-pilot a given project id. The project is advanced using
     * ProjectPilot.
     * </p>
     * @param projectId the project id to auto-pilot
     * @param operator the operator (used for auditing)
     * @return AutoPilotResult representing result of auto-pilot (never null)
     * @throws IllegalArgumentException if operator is null or empty (trimmed) string
     * @throws PhaseOperationException if any error occurs while ending/starting a phase
     */
    public AutoPilotResult advanceProject(long projectId, String operator) throws PhaseOperationException {
    	log.info(new LogMessage(new Long(projectId), operator, "Checking project phases.").toString());
        return projectPilot.advancePhases(projectId, operator);
    }
    /**
     * Get id string spereated by comma for the ids.
     * @param ids the id array
     * @return string seperated by comma
     */
	private String getIdString(long[] ids) {
		if (ids == null || ids.length == 0) {
			return "";
		}
		StringBuffer idString = new StringBuffer();
        for(int i = 0 ; i < ids.length; i++) {
        	idString.append(',').append(ids[i]);
        }
		return idString.substring(1);
	}
}

