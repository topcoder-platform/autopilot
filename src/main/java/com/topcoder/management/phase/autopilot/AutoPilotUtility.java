/*
 * Copyright (C) 2012 TopCoder Inc., All Rights Reserved.
 */

package com.topcoder.management.phase.autopilot;

import com.topcoder.management.phase.autopilot.logging.LogMessage;
import com.topcoder.onlinereview.component.commandline.ArgumentValidationException;
import com.topcoder.onlinereview.component.commandline.CommandLineUtility;
import com.topcoder.onlinereview.component.commandline.IllegalSwitchException;
import com.topcoder.onlinereview.component.commandline.IntegerValidator;
import com.topcoder.onlinereview.component.commandline.Switch;
import com.topcoder.onlinereview.component.commandline.UsageException;
import com.topcoder.onlinereview.component.project.phase.PhaseHandler;
import com.topcoder.onlinereview.component.project.phase.PhaseManager;
import com.topcoder.onlinereview.component.project.phase.PhaseOperationEnum;
import com.topcoder.onlinereview.component.project.phase.PhaseType;
import com.topcoder.onlinereview.component.project.phase.handler.AppealsPhaseHandler;
import com.topcoder.onlinereview.component.project.phase.handler.CheckpointSubmissionPhaseHandler;
import com.topcoder.onlinereview.component.project.phase.handler.SpecificationReviewPhaseHandler;
import com.topcoder.onlinereview.component.project.phase.handler.SpecificationSubmissionPhaseHandler;
import com.topcoder.onlinereview.component.project.phase.handler.or.PRAggregationPhaseHandler;
import com.topcoder.onlinereview.component.project.phase.handler.or.PRAppealResponsePhaseHandler;
import com.topcoder.onlinereview.component.project.phase.handler.or.PRApprovalPhaseHandler;
import com.topcoder.onlinereview.component.project.phase.handler.or.PRCheckpointReviewPhaseHandler;
import com.topcoder.onlinereview.component.project.phase.handler.or.PRCheckpointScreeningPhaseHandler;
import com.topcoder.onlinereview.component.project.phase.handler.or.PRFinalFixPhaseHandler;
import com.topcoder.onlinereview.component.project.phase.handler.or.PRFinalReviewPhaseHandler;
import com.topcoder.onlinereview.component.project.phase.handler.or.PRIterativeReviewPhaseHandler;
import com.topcoder.onlinereview.component.project.phase.handler.or.PRPostMortemPhaseHandler;
import com.topcoder.onlinereview.component.project.phase.handler.or.PRRegistrationPhaseHandler;
import com.topcoder.onlinereview.component.project.phase.handler.or.PRReviewPhaseHandler;
import com.topcoder.onlinereview.component.project.phase.handler.or.PRScreeningPhaseHandler;
import com.topcoder.onlinereview.component.project.phase.handler.or.PRSubmissionPhaseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * This class  provides the command line entry for the AutoPilot tool. See {@link #main(String[])}
 * for more detailed doc.
 * </p>
 *
 * <p>
 * Version 1.0.1 (Online Review Build and Deploy Scripts 2 Assembly 1.0) Change notes:
 *   <ol>
 *     <li>Added support for <code>guardFile</code> configuration property.</li>
 *   </ol>
 * </p>
 *
 * @author sindu, abelli, VolodymyrK
 * @version 1.0.2
 */
public class AutoPilotUtility {

    private static final Logger log = LoggerFactory.getLogger(AutoPilotUtility.class);

    /**
     * <p>A <code>Thread</code> running the <code>AutoPilotJobStopper</code>.</p>
     *
     * @since 1.0.1
     */
    private static Thread stopper;

    /**
     * <p>This is a Command Line switch that is used to retrieve the poll interval in seconds.</p>
     *
     * @since 1.0.2
     */
    private static Switch pollSwitch;

    /**
     * <p>This is a Command Line switch that is used to retrieve the list of project IDs to be processed.</p>
     *
     * @since 1.0.2
     */
    private static Switch projectSwitch;

    /**
     * <p>
     * A main static method to provide command line functionality to the component. Basically, the
     * command line provides 2 functionalities:<br>
     * <br> - run once (given a list of project ids or retrieve ids from AutoPilotSource)<br>
     * <br> - poll mode (scheduled in background to run Auto Pilot every some intervals)<br>
     * <br>
     * The command line syntax is:<br>
     * java AutoPilotJob [-config configFile] [-namespace ns [-autopilot apKey]] (-poll [interval]
     * [-jobname jobname] | -project [Id[, ...]])<br>
     * <br> - configFile specifies the path to config files to be loaded into configuration manager.
     * if not specified it's assumed the config file is preloaded<br>
     * <br> - ns and apKey are optional, it's used to instantiate AutoPilotJob for project mode. ns
     * is also used to instantiate the Scheduler. The default values are AutoPilotJob's full name &
     * AutoPilot's full name respectively.<br>
     * <br> - poll or project These next options are mutually exclusive (to indicate two kinds of
     * run-mode): Both poll/projects are specified, or none are specified is not allowed.<br>
     * <br>
     * A) Poll-mode<br>
     * <br> - poll is used to define the interval in seconds, if interval is not specified, a
     * default of 60 seconds is used. The autopilot job will be executed every this interval starting
     * from midnight.<br>
     * <br> - jobname is the job name, Job Scheduling will use this job name. The default value is
     * 'AutoPilotJob'. It is optional and can only be specified if poll is specified.<br>
     * <br> - ns is used to instantiate the Scheduler. Optional. The default value is AutoPilotJob's full
     * name.<br>
     * <br> - apKey is ignored.<br>
     * <br>
     * <br>
     * B) Project mode<br>
     * <br> - project can be specified to process projects with the given ids. The project ids will
     * be processed once and the application terminates, it doesn't go into poll mode. If no ids are
     * given, AutoPilotSource is used instead.<br>
     * <br> - ns and apKey, must be specified or not at the same time, default to AutoPilotJob's
     * full name and AutoPilot's full name respectively.<br>
     * <br>
     * </p>
     *
     * @param args the command line arguments
     * @throws IllegalArgumentException if argument is invalid, i.e.
     *                                  <ul>
     *                                  <li>specifying namespace without apKey (and vice versa) for project mode,</li>
     *                                  <li>specifying both poll/project, or no poll/project is given,</li>
     *                                  <li>specifying jobname without poll,</li>
     *                                  <li>poll interval cannot be converted to long</li>
     *                                  <li>poll interval &lt;= 0,</li>
     *                                  <li>project id cannot be converted to long</li>
     *                                  <li><code>IllegalSwitchException</code> occurs</li>
     *                                  <li><code>ArgumentValidationException</code> occurs</li>
     *                                  <li><code>UsageException</code> occurs</li>
     *                                  </ul>
     * @throws ConfigurationException   if any error occurs loading config file or instantiating the
     *                                  AutoPilotJob or while configuring the job scheduler
     * @throws AutoPilotSourceException if any error occurs retrieving project ids from
     *                                  AutoPilotSource
     * @throws PhaseOperationException  if any error occurs while ending/starting a phase
     * @throws RuntimeException         if runtime exceptions occurs while executing the command line.
     */
    public static void main(String[] args) throws ConfigurationException, AutoPilotSourceException,
            PhaseOperationException {
        if (null == args) {
            throw new IllegalArgumentException("args cannot be null");
        }

        // Build command line parser.
        CommandLineUtility clu = new CommandLineUtility();
        if (args.length < 1 || !parseArgs(clu, args)) {
            showUsage();
            return;
        }
        ApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml");

        addPhaseHandlers(context);

        List validSwitches = clu.getValidSwitches();

        try {
            // One of project and poll can exist.
            if (validSwitches.contains(pollSwitch)
                    && validSwitches.contains(projectSwitch)) {
                // Both poll and project exists.
                showUsage();
                throw new IllegalArgumentException("either project or poll can exist");

            } else if (!validSwitches.contains(pollSwitch)
                    && !validSwitches.contains(projectSwitch)) {
                // Neither poll nor project exists.
                showUsage();
                throw new IllegalArgumentException("either project or poll must exist");

            } else if (validSwitches.contains(projectSwitch)) {
                // Deal with project mode.
                dealProject(clu, context.getBean(AutoPilotJob.class));

            } else if (validSwitches.contains(pollSwitch)) {
                // Deal with poll mode.
                ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);
                dealPoll(clu, context, ses);
            }
        } catch (RuntimeException t) {
            log.error("fail to build command line cause of illegal switch:"
                    + "\n" + LogMessage.getExceptionStackTrace(t));
            showUsage();
            throw t;
        }
    }

    private static void addPhaseHandlers(ApplicationContext context) {
        PhaseManager phaseManager = context.getBean(PhaseManager.class);
        PhaseType[] entities = phaseManager.getAllPhaseTypes();
        registerPhaseHandlerForOperation(phaseManager, context.getBean(PRRegistrationPhaseHandler.class),
                entities, "Registration");
        registerPhaseHandlerForOperation(phaseManager, context.getBean(PRSubmissionPhaseHandler.class),
                entities, "Submission");
        registerPhaseHandlerForOperation(phaseManager, context.getBean(PRScreeningPhaseHandler.class),
                entities, "Screening");
        registerPhaseHandlerForOperation(phaseManager, context.getBean(PRReviewPhaseHandler.class),
                entities, "Review");
        registerPhaseHandlerForOperation(phaseManager, context.getBean(AppealsPhaseHandler.class),
                entities, "Appeals");
        registerPhaseHandlerForOperation(phaseManager, context.getBean(PRAppealResponsePhaseHandler.class),
                entities, "Appeals Response");
        registerPhaseHandlerForOperation(phaseManager, context.getBean(PRAggregationPhaseHandler.class),
                entities, "Aggregation");
        registerPhaseHandlerForOperation(phaseManager, context.getBean(PRFinalFixPhaseHandler.class),
                entities, "Final Fix");
        registerPhaseHandlerForOperation(phaseManager, context.getBean(PRFinalReviewPhaseHandler.class),
                entities, "Final Review");
        registerPhaseHandlerForOperation(phaseManager, context.getBean(PRApprovalPhaseHandler.class),
                entities, "Approval");
        registerPhaseHandlerForOperation(phaseManager, context.getBean(PRPostMortemPhaseHandler.class),
                entities, "Post-Mortem");
        registerPhaseHandlerForOperation(phaseManager, context.getBean(SpecificationSubmissionPhaseHandler.class),
                entities, "Specification Submission");
        registerPhaseHandlerForOperation(phaseManager, context.getBean(SpecificationReviewPhaseHandler.class),
                entities, "Specification Review");
        registerPhaseHandlerForOperation(phaseManager, context.getBean(CheckpointSubmissionPhaseHandler.class),
                entities, "Checkpoint Submission");
        registerPhaseHandlerForOperation(phaseManager, context.getBean(PRCheckpointScreeningPhaseHandler.class),
                entities, "Checkpoint Screening");
        registerPhaseHandlerForOperation(phaseManager, context.getBean(PRCheckpointReviewPhaseHandler.class),
                entities, "Checkpoint Review");
        registerPhaseHandlerForOperation(phaseManager, context.getBean(PRIterativeReviewPhaseHandler.class),
                entities, "Iterative Review");
    }

    private static void registerPhaseHandlerForOperation(PhaseManager manager,
                                                         PhaseHandler handler,
                                                         PhaseType[] entities,
                                                         String phaseName) {
        for (PhaseType entity : entities) {
            if (phaseName.equals(entity.getName())) {
                manager.registerHandler(handler, entity, PhaseOperationEnum.START);
                manager.registerHandler(handler, entity, PhaseOperationEnum.END);
                return;
            }
        }
    }

    /**
     * <p>
     * Parse the arguments.
     * </p>
     *
     * @param commandLineUtility the command line utility.
     * @param args               the arguments.
     * @return <code>false</code> if any error occurs; <code>true</code> otherwise.
     */
    private static boolean parseArgs(CommandLineUtility commandLineUtility, String[] args) {
        try {
            // create switches
            pollSwitch = new Switch("poll", false, 0, 1, new IntegerValidator(), "Poll interval in seconds");
            projectSwitch = new Switch("project", false, 0, -1, new IntegerValidator(), "Project id");
            commandLineUtility.addSwitch(pollSwitch);
            commandLineUtility.addSwitch(projectSwitch);
        } catch (IllegalSwitchException e) {
            // never happens
        }

        try {
            commandLineUtility.parse(args);
            return true;
        } catch (ArgumentValidationException e) {
            System.out.println("Argument validation fails.");
            return false;
        } catch (UsageException e) {
            System.out.println("Fails to parse the arguments.");
            return false;
        }
    }

    /**
     * <p>
     * Deal with poll mode.
     * </p>
     *
     * @param clu the parsed command line utility.
     * @throws ConfigurationException - if there is configuration exceptions.
     */
    private static void dealPoll(CommandLineUtility clu, ApplicationContext context, ScheduledExecutorService ses)
            throws ConfigurationException {
        // Use 60 seconds if interval not specified.
        int interval = 60;
        String poll = clu.getSwitch("poll").getValue();
        if (null != poll) {
            interval = Integer.parseInt(poll);
        }
        ses.scheduleAtFixedRate(() -> {
            AutoPilotResult[] result = context.getBean(AutoPilotJob.class).execute();
            printResult(result);
        }, 0, interval, TimeUnit.SECONDS);
    }

    /**
     * <p>
     * Deal with project mode.
     * </p>
     *
     * @throws IllegalArgumentException - if jobname is specified, or namespace specified without
     *                                  apKey (and vice versa)
     * @throws ConfigurationException   - if there is configuration exceptions.
     * @throws AutoPilotSourceException - if there is auto pilot source exceptions.
     * @throws PhaseOperationException  - if there is phase operation exceptions.
     */
    private static void dealProject(CommandLineUtility clu, AutoPilotJob autoPilotJob)
            throws ConfigurationException, AutoPilotSourceException, PhaseOperationException {
        // Parse project Ids.
        List ids = clu.getSwitch("project").getValues();
        long[] projectId = null;
        if (null != ids && !ids.isEmpty()) {
            projectId = new long[ids.size()];
            int i = -1;
            for (Iterator it = ids.iterator(); it.hasNext(); ) {
                projectId[++i] = Long.parseLong((String) it.next());
            }
        }

        // run and print the result.
        AutoPilotResult[] result = (null == projectId) ? autoPilotJob.execute() : autoPilotJob.run(projectId);
        printResult(result);
    }

    /**
     * <p>
     * Show usage of this command line tool.
     * </p>
     */
    private static void showUsage() {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    AutoPilotJob.class.getClassLoader().getResourceAsStream("usage")));
            String ln = br.readLine();
            while (ln != null) {
                System.err.println(ln);
                ln = br.readLine();
            }
            br.close();
        } catch (IOException e) {
            System.err.println("Cannot find usage file!");
        }
    }

    /**
     * <p>
     * Print the auto pilot result.
     * </p>
     *
     * @param result result array.
     */
    private static void printResult(AutoPilotResult[] result) {
        System.out.println("|      ProjectId      |    Ended    |    Started    |");
        final char[] space1 = "                                                    ".toCharArray();
        for (int i = 0; i < result.length; i++) {
            StringBuffer buf = new StringBuffer();

            // Print project Id.
            System.out.print('|');
            String strId = String.valueOf(result[i].getProjectId());
            buf.append(space1, 0, (21 - strId.length()) / 2);
            buf.append(strId);
            buf.append(space1, buf.length(), 21 - buf.length());
            System.out.print(buf);

            // Print ended.
            System.out.print('|');
            buf.delete(0, buf.length());
            String strEnd = String.valueOf(result[i].getPhaseEndedCount());
            buf.append(space1, 0, (13 - strEnd.length()) / 2);
            buf.append(strEnd);
            buf.append(space1, buf.length(), 13 - buf.length());
            System.out.print(buf);

            // Print started.
            System.out.print('|');
            buf.delete(0, buf.length());
            String strStart = String.valueOf(result[i].getPhaseStartedCount());
            buf.append(space1, 0, (15 - strEnd.length()) / 2);
            buf.append(strStart);
            buf.append(space1, buf.length(), 15 - buf.length());
            System.out.print(buf);

            System.out.println('|');
        }
    }
}
