/*
 * Copyright (C) 2006-2012 TopCoder Inc., All Rights Reserved.
 */

package com.topcoder.management.phase.autopilot.impl;

import com.topcoder.kafka.messaging.KafkaMessageProducer;
import com.topcoder.management.phase.autopilot.AutoPilotResult;
import com.topcoder.management.phase.autopilot.PhaseOperationException;
import com.topcoder.management.phase.autopilot.ProjectPilot;
import com.topcoder.onlinereview.component.grpcclient.GrpcHelper;
import com.topcoder.onlinereview.component.project.management.PersistenceException;
import com.topcoder.onlinereview.component.project.management.ProjectManager;
import com.topcoder.onlinereview.component.project.phase.Dependency;
import com.topcoder.onlinereview.component.project.phase.Phase;
import com.topcoder.onlinereview.component.project.phase.PhaseManagementException;
import com.topcoder.onlinereview.component.project.phase.PhaseManager;
import com.topcoder.onlinereview.component.project.phase.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

/**
 * <p>
 * A default implementation of ProjectPilot which will advance project phases. It delegates the
 * start/end operations to the Phase Management component. PhaseManager instance is created in ctor
 * using the object factory component. advancePhase() attempts to minimize canStart/canEnd queries
 * by processing project phases in order from the earliest phase (having no dependencies) up to the
 * latest phase. Logging Wrapper will be used to audit phase start/end. Some methods are made
 * protected so that it can be extended/customized easily in the future.
 * </p>
 * <p>
 * This class is immutable, however it's not guaranteed to be thread-safe because the underlying
 * PhaseManager instance that is used by this class may not be thread-safe. In a multiple thread
 * situation, application is advised to synchronize on the AutoPilot instance to ensure that only a
 * single thread is retrieving/advancing project phases at one time.
 * </p>
 * @author sindu, abelli
 * @version 1.0.2
 */
public class DefaultProjectPilot implements ProjectPilot {

    /**
     * <p>
     * Represents the default phase status name for a scheduled phase. Referenced in ctor().
     * </p>
     */
    public static final String DEFAULT_SCHEDULED_STATUS_NAME = "Scheduled";

    /**
     * <p>
     * Represents the default phase status name for an open phase. Referenced in ctor().
     * </p>
     */
    public static final String DEFAULT_OPEN_STATUS_NAME = "Open";

    /**
     * <p>
     * Represents the PhaseManager instance that is used to retrieve project phases. This variable
     * is initially null, initialized in constructor using object factory and immutable afterwards.
     * It is referenced by advancePhase to getPhases, query whether phase can be ended/started, and
     * end/start the phase itself. It can be retrieved with the getter.
     * </p>
     */
    private PhaseManager phaseManager;

    /**
     * <p>
     * Represents the ProjectManager instance that is used to retrieve project. This variable
     * is initially null, initialized in constructor using object factory and immutable afterwards.
     * It is referenced by advancePhase to getPhases, query whether phase can be ended/started, and
     * end/start the phase itself. It can be retrieved with the getter.
     * </p>
     */
    private ProjectManager projectManager;

    /**
     * <p>
     * Represents the log used to do auditing whenever a phase is started/ended. The audit log
     * should include timestamp, project, phase, operation, and the operator This variable is
     * initially null, initialized in constructor and immutable afterwards. It can be retrieved with
     * the getter.
     * </p>
     */
    private static final Logger log = LoggerFactory.getLogger(DefaultProjectPilot.class);

    /**
     * <p>
     * Represents the phase status name of a Scheduled phase. This will be used to compare a phase
     * status name. Scheduled phases will be started if possible. This variable is initially null,
     * set in the constructor and immutable afterwards. It is referenced by advancePhase. It can be
     * retrieved with the getter.
     * </p>
     */
    private String scheduledStatusName;

    /**
     * <p>
     * Represents the phase status name of an Open phase. This will be used to compare a phase
     * status name. Open phases will be ended if possible. This variable is initially null, set in
     * the constructor and immutable afterwards. It is referenced by advancePhase. It can be
     * retrieved with the getter.
     * </p>
     */
    private String openStatusName;

    private List<Long> lastPhases = Arrays.asList(4L, 6L, 7L, 8L, 9L, 10L, 11L, 18L);

    /**
     * <p>
     * Represents kafka message producer.
     * </p>
     */
    private final KafkaMessageProducer producer = new KafkaMessageProducer();

    public void setPhaseManager(PhaseManager phaseManager) {
        this.phaseManager = phaseManager;
    }

    public void setProjectManager(ProjectManager projectManager) {
        this.projectManager = projectManager;
    }

    public void setScheduledStatusName(String scheduledStatusName) {
        this.scheduledStatusName = scheduledStatusName;
    }

    public void setOpenStatusName(String openStatusName) {
        this.openStatusName = openStatusName;
    }

    /**
     * <p>
     * Return the phase manager instance used by this class.
     * </p>
     * @return The phase manager instance used by this class
     */
    protected PhaseManager getPhaseManager() {
        return this.phaseManager;
    }

    protected ProjectManager getProjectManager() { return this.projectManager; }

    /**
     * <p>
     * Returns the name of a phase status that represents a scheduled phase.
     * </p>
     * @return A non-null string representing the scheduled phase status name
     */
    public String getScheduledStatusName() {
        return this.scheduledStatusName;
    }

    /**
     * <p>
     * Returns the name of a phase status that represents an open phase.
     * </p>
     * @return A non-null string representing the open phase status name
     */
    public String getOpenStatusName() {
        return this.openStatusName;
    }

    /**
     * <p>
     * Advances project's phases. This will end all opened phases that can be ended, and start all
     * scheduled phases that can be started. The returned value is an int[] of 2 elements:<br>
     * 1st element is the number of ended phases 2nd element is the number of started phases As
     * specified in Project Phases, it's guaranteed that there'll be no cyclic dependency in the
     * phases.<br>
     * In order to minimize phase query operations, we should process the project phases starting
     * from the first phase up to the last phase. The first phase is the phase with no dependency.
     * To do this, we'll simply walk all the phases and do post-order walk of the dependency tree.
     * This means all phase dependencies will be processed first before the current phase. To
     * prevent a phase from being processed twice, we'll store processed phase id in a HashSet. The
     * above tree-walking is repeated until no more phase change can be made.<br>
     * <br>
     * </p>
     * @param projectId the project id whose phases to end/start
     * @param operator the operator (used for auditing)
     * @return AutoPilotResult representing number of ended/started phases
     * @throws IllegalArgumentException if operator is null or an empty (trimmed) string
     */
    public AutoPilotResult advancePhases(long projectId, String operator) {
        // Check arguments.
        if (null == operator) {
            throw new IllegalArgumentException("operator cannot be null");
        }
        if (operator.trim().length() < 1) {
            throw new IllegalArgumentException("operator cannot be empty");
        }

        // Get phases.
        int countEnd = 0, countStart = 0, countLasPhaseEnd = 0;
        Set set = new HashSet();
        Project proj;
        try {
            proj = phaseManager.getPhases(projectId);
            if (null == proj) {
                log.error("project with " + projectId + " does not exist");
                throw new PhaseOperationException(projectId, null, "project with " + projectId + " does not exist");
            }

            set.clear();
            Phase[] phases = proj.getAllPhases();
            if (null != phases && phases.length > 0) {
                for (int i = 0; i < phases.length; i++) {
                    int[] cc = processPhase(phases[i], set, operator);
                    countEnd += cc[0];
                    countStart += cc[1];
                    countLasPhaseEnd += cc[2];
                }
            }
            boolean statusHasChanged = countLasPhaseEnd > 0;
            boolean phaseUpdated = countEnd > 0 || countStart > 0;
            boolean submissionUpdated = countLasPhaseEnd > 0;
            GrpcHelper.getSyncServiceRpc().autopilotSync(projectId, statusHasChanged, phaseUpdated, submissionUpdated);
        } catch (Exception e) {
            ByteArrayOutputStream s = new ByteArrayOutputStream();
            e.printStackTrace(new PrintStream(s));
            log.error(new Date()
                    + " - Error occurs while trying to advance phases of project with id "
                    + projectId
                    + ", stack trace : "
                    + s.toString());
        } finally {
            return new AutoPilotResult(projectId, countEnd, countStart);
        }
    }

    /**
     * <p>
     * Recursively processes all dependencies of this phase, then the phase itself. This method
     * handles the recursive part, the actual process is handled by doPhaseOperation(). This
     * implements the post-order walk of the dependencies tree starting from the given phase. A
     * post-order walk will cause all dependencies to be processed first before processing the
     * current phase.
     * </p>
     * @param phase the current phase to process
     * @param processedPhase a set of Long representing phases id that have been processed
     * @param operator the operator name for auditing
     * @return [0] is number of ended phases, [1] is number of started phases
     * @throws PhaseOperationException if any error occurs processing the phase
     */
    protected int[] processPhase(Phase phase, Set processedPhase, String operator)
        throws PhaseOperationException {
        if (null == phase || processedPhase.contains(new Long(phase.getId()))) {
            // Should return new array cause we do not known if it is used to aggregate.
            return new int[] {0, 0, 0};
        }

        int[] count = {0, 0, 0};

        // Process its dependencies firstly.
        Dependency[] dd = phase.getAllDependencies();
        if (dd != null && dd.length > 0) {
            for (int i = 0; i < dd.length; i++) {
                int[] tmp = processPhase(dd[i].getDependency(), processedPhase, operator);
                count[0] += tmp[0];
                count[1] += tmp[1];
                count[2] += tmp[2];
            }
        }

        // process this phase.
        int[] tmp = doPhaseOperation(phase, operator);
        count[0] += tmp[0];
        count[1] += tmp[1];
        count[2] += tmp[2];

        processedPhase.add(new Long(phase.getId()));

        return count;
    }

    /**
     * <p>
     * Ends or starts a phase if possible. This is called by processPhase to do the actual phase
     * operation. End the phase if it's open and can be ended. Start the phase if it's scheduled and
     * can be started. Phase changes should be audited using the log and operator name.
     * </p>
     * @param phase the phase to end/start
     * @param operator the operator name for auditing
     * @return {1,0} if phase is ended; {0,1} if phase is started; {0,0} otherwise
     * @throws PhaseOperationException if any error occurs ending/starting the phase
     */
    protected int[] doPhaseOperation(Phase phase, String operator) throws PhaseOperationException {
        if (phase.getPhaseStatus() == null) {
            // Should return new array cause we do not known if it is used to aggregate.
            return new int[] {0, 0, 0};
        }

        int[] count = {0, 0, 0};

        // End if the phase is open and can end.
        try {
            if (phase.getPhaseStatus().getName().equals(getOpenStatusName())
                && phaseManager.canEnd(phase).isSuccess()) {

        	// FIX: made by Badal on 17.JAN.18
        	// canStart() & canEnd() method returns boolean itself
        	// and so isSuccess can't be invoked on it
//        	if (phase.getPhaseStatus().getName().equals(getOpenStatusName())
//                    && phaseManager.canEnd(phase)) {

                phaseManager.end(phase, operator);
                count[0]++;
                if (lastPhases.contains(phase.getPhaseType().getId())) {
                    count[2]++;
                }

                com.topcoder.onlinereview.component.project.management.Project project;
                try {
                    project = projectManager.getProject(phase.getProject().getId());
                } catch (PersistenceException e) {
                    throw new PhaseOperationException(phase.getProject().getId(), phase, "Failed to get project status");
                }

                doAudit(phase, true, operator, project.getProjectStatus().getName());
            }
        } catch (PhaseManagementException e) {
            log.error("fail to end the phase cause of phase management exception");
            throw new PhaseOperationException(phase.getProject().getId(), phase,
                "fail to end the phase cause of phase management exception", e);
        }

        // Start if the phase is scheduled and can start.
        try {
            if (phase.getPhaseStatus().getName().equals(getScheduledStatusName())
                && phaseManager.canStart(phase).isSuccess()) {
//            if (phase.getPhaseStatus().getName().equals(getScheduledStatusName())
//                    && phaseManager.canStart(phase)) {
                phaseManager.start(phase, operator);
                count[1]++;
                doAudit(phase, false, operator);
            }
        } catch (PhaseManagementException e) {
            log.error("fail to start the phase cause of phase management exception");
            throw new PhaseOperationException(phase.getProject().getId(), phase,
                "fail to start the phase cause of phase management exception", e);
        }

        return count;
    }

    /**
     * <p>
     * This method audits a phase change. It is called by doPhaseOperation whenever a phase is
     * successfully started/ended. It should log the timestamp, project, phase, operation, and the
     * operator name.
     * </p>
     * @param phase the phase to audit
     * @param isEnd true if the phase was ended; false if the phase was started
     * @param operator the operator name to audit
     * @throws PhaseOperationException if any error occurs auditing the entry
     */
    protected void doAudit(Phase phase, boolean isEnd, String operator)
            throws PhaseOperationException {
        doAudit(phase, isEnd, operator, null);
    }
    /**
     * <p>
     * This method audits a phase change. It is called by doPhaseOperation whenever a phase is
     * successfully started/ended. It should log the timestamp, project, phase, operation, and the
     * operator name.
     * </p>
     * @param phase the phase to audit
     * @param isEnd true if the phase was ended; false if the phase was started
     * @param operator the operator name to audit
     * @param projectStatus project status
     * @throws PhaseOperationException if any error occurs auditing the entry
     */
    /**
     * Badal : added sample json message for testing on dev
     */
    protected void doAudit(Phase phase, boolean isEnd, String operator, String projectStatus)
        throws PhaseOperationException {
        log.info(new Date().toString()
                + " - project "
                + phase.getProject().getId()
                + " - phase "
                + phase.getId()
                + " - phase type "
                + ((null == phase.getPhaseType()) ? "Null Phase Type" : phase.getPhaseType()
                    .getName()) + " - " + (isEnd ? "END" : "START") + " - operator " + operator
                + " - projectStatus " + projectStatus);

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        MessageFormat message = new MessageFormat(dateFormat.format(new Date()),
                    phase.getProject().getId(), phase.getId(),
                    (null == phase.getPhaseType()) ? "Null Phase Type" : phase.getPhaseType().getName(),
                    isEnd ? "END" : "START", operator, projectStatus);

        log.info("JSON_MESSAGE ::: WILL SEND");

        producer.postRequestUsingGson(message);
    }
}
