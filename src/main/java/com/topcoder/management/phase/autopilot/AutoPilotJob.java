/*
 * Copyright (C) 2006-2012 TopCoder Inc., All Rights Reserved.
 */

package com.topcoder.management.phase.autopilot;

import com.topcoder.management.phase.autopilot.logging.LogMessage;
import com.topcoder.onlinereview.component.scheduler.Schedulable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Represents an auto pilot job that is to be executed using Job Scheduling component. A new
 * instance of this class will be created and executed (in a separate thread) by the Scheduler at a
 * certain interval.
 * </p>
 * <p>
 * This class may not be thread-safe, the variable done is mutable by the 'run' method. However this
 * class will be run in its own thread by the Scheduler, and only a single thread will execute the
 * run method, so in the context of the scheduler, it's thread safe. The internal scheduler is
 * instantiated in schedule() in a synchronized block. It's also thread safe in the context of the
 * command-line because only one thread is active.
 * </p>
 * @author sindu, abelli, TCSDEVELOPER
 * @version 1.0.2
 */
public class AutoPilotJob implements Runnable, Schedulable {

    /**
     * <p>The log used by this class for logging errors and debug information.</p>
     */
    private static final Logger log = LoggerFactory.getLogger(AutoPilotJob.class);

    /**
     * <p>
     * Represents the default operator name that is used to do auditing if none is specified in the
     * configuration files.
     * </p>
     */
    public static final String DEFAULT_OPERATOR = "AutoPilotJob";

    /**
     * <p>
     * Represents the status to return when a job is running.
     * </p>
     */
    public static final String STATUS_RUNNING = "RUNNING";

    /**
     * <p>
     * Represents the status to return when the job is completed.
     * </p>
     */
    public static final String STATUS_DONE = "DONE";

    /**
     * <p>
     * Represents the AutoPilot instance that is used to do the job. This variable is initially
     * null, initialized in constructor using object factory and immutable afterwards. It can be
     * retrieved with the getter. It is cached by the class so it only gets constructed once.
     * </p>
     */
    private AutoPilot autoPilot;

    /**
     * <p>
     * Represents the operator name that is used to do auditing. This variable is initially null,
     * initialized in constructor using values from configuration and immutable afterwards. It can
     * be retrieved with the getter. It is cached by the class so it only gets constructed once.
     * </p>
     */
    private String operator;

    /**
     * <p>
     * Represents a flag that indicates whether the job is completed. This variable is initially
     * false, after run() is complete, it'll be set to true. It is referenced by the isDone() method
     * to inform the scheduler that the job is completed.
     * </p>
     */
    private boolean done = false;

    /**
     * <p>
     * Return the auto pilot instance used by this class.
     * </p>
     * @return the auto pilot instance used by this class
     */
    public AutoPilot getAutoPilot() {
        return this.autoPilot;
    }

    /**
     * <p>
     * Return the operator name used to do auditing.
     * </p>
     * @return the operator name used to do auditing
     */
    public String getOperator() {
        return this.operator;
    }

    public void setAutoPilot(AutoPilot autoPilot) {
        this.autoPilot = autoPilot;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    /**
     * <p>
     * This method implements 'run' in the Runnable interface. It's invoked to start the job.
     * </p>
     * @throws RuntimeException - if fail to execute the auto pilot job. AutoPilotSourceException
     *             and PhaseOperationException will be wrapped in RuntimeException.
     */
    public void run() {
        // to prevent the potential if the same job instance will be executed more than once.
        done = false;

        try {
            execute();
        } catch (AutoPilotSourceException e) {
        	log.error( "fail to advance projects with " + getOperator()
                    + " cause of auto pilot source exception \n" + LogMessage.getExceptionStackTrace(e));
            e.printStackTrace(System.err);
            throw new RuntimeException("fail to advance projects with " + getOperator()
                + " cause of auto pilot source exception", e);
        } catch (PhaseOperationException e) {
        	log.error( "fail to advance project " + e.getProjectId() + " phase "
                    + e.getPhase() + " with " + getOperator()
                    + " cause of phase operation exception \n" + LogMessage.getExceptionStackTrace(e));
            e.printStackTrace(System.err);
            throw new RuntimeException("fail to advance project " + e.getProjectId() + " phase "
                + e.getPhase() + " with " + getOperator() + " cause of phase operation exception",
                e);
        }

        done = true;
    }

    /**
     * <p>
     * This method is invoked by the scheduled job to process all active projects.
     * </p>
     * @return an array of AutoPilotResult representing result of auto-pilot (never null, but can be
     *         empty)
     * @throws AutoPilotSourceException if any error occurs retrieving project ids from
     *             AutoPilotSource
     * @throws PhaseOperationException if any error occurs while ending/starting a phase
     */
    public AutoPilotResult[] execute() throws AutoPilotSourceException, PhaseOperationException {
        log.info(new LogMessage(null, getOperator(), "AutoPilot job iteration.").toString());
        AutoPilotResult[] ret = autoPilot.advanceProjects(getOperator());
        log.info(new LogMessage(null, getOperator(), "AutoPilot job iteration - end.").toString());
        return ret;
    }

    /**
     * <p>
     * This method is invoked by command line interface to process a given list of project ids.
     * </p>
     * @param projectId a list of project id to process
     * @return an array of AutoPilotResult representing result of auto-pilot (never null, but can be
     *         empty)
     * @throws IllegalArgumentException if projectId is null
     * @throws AutoPilotSourceException if any error occurs retrieving project ids from
     *             AutoPilotSource
     * @throws PhaseOperationException if any error occurs while ending/starting a phase
     */
    public AutoPilotResult[] run(long[] projectId) throws AutoPilotSourceException,
        PhaseOperationException {
        log.info(new LogMessage(null, getOperator(), "AutoPilot iteration.").toString());
        AutoPilotResult[] ret = autoPilot.advanceProjects(projectId, getOperator());
        log.debug( new LogMessage(null, getOperator(), "AutoPilot iteration - end.").toString());
        return ret;
    }

    /**
     * <p>
     * This method implements 'isDone' in the Schedulable interface. It's invoked by scheduler to
     * check whether the job is completed.
     * </p>
     * @return true if job is completed, false otherwise
     */
    public boolean isDone() {
        return done;
    }

    /**
     * <p>
     * This is invoked by the Scheduler when the Scheduler is stopped. We simply do nothing here. If
     * the job is still running, we'll let it run until it's finished. Since it's running in its own
     * thread, we can just do nothing here so that we don't block the scheduler's thread.
     * </p>
     */
    public void close() {
        // your code here
    }

    /**
     * <p>
     * This should return the job status. Application can poll this status using Scheduler's Job.
     * </p>
     * @return STATUS_DONE or STATUS_RUNNING depending whether the job has completed
     */
    public String getStatus() {
        return isDone() ? STATUS_DONE : STATUS_RUNNING;
    }
}
