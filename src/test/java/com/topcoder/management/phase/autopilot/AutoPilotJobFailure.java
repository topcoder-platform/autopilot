/*
 * Copyright (C) 2006 TopCoder Inc., All Rights Reserved.
 */
package com.topcoder.management.phase.autopilot;

import java.util.Iterator;

import com.topcoder.util.log.LogManager;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.topcoder.management.phase.autopilot.impl.ActiveAutoPilotSource;
import com.topcoder.management.phase.autopilot.impl.DefaultProjectPilot;
import com.topcoder.util.config.ConfigManager;

/**
 * <p>
 * Failure tests for <code>AutoPilotJob</code>.
 * </p>
 * @author abelli
 * @version 1.0
 */
public class AutoPilotJobFailure extends TestCase {

    /**
     * <p>
     * Tear down the test fixture. Remove all namespaces added for test cases.
     * </p>
     * @throws Exception - to JUnit.
     * @see junit.framework.TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        ConfigManager cfg = ConfigManager.getInstance();
        for (Iterator it = cfg.getAllNamespaces(); it.hasNext();) {
            cfg.removeNamespace((String) it.next());
        }
    }

    /**
     * <p>
     * Create auto pilot instance for test cases.
     * </p>
     * @return the AutoPilot instance.
     * @throws Exception - to JUnit.
     */
    private AutoPilot createAutoPilot() throws Exception {
        ConfigManager cfg = ConfigManager.getInstance();
        cfg.add(ActiveAutoPilotSource.class.getName(), "active_auto_source_pilot.xml",
                ConfigManager.CONFIG_XML_FORMAT);
        cfg.add(DefaultProjectPilot.class.getName(), "project_pilot.xml",
                ConfigManager.CONFIG_XML_FORMAT);
        cfg.add("logging.xml");
        cfg.add(AutoPilot.class.getName(), "auto_pilot.xml", ConfigManager.CONFIG_XML_FORMAT);
        return new AutoPilot();
    }

    /**
     * <p>
     * Create auto pilot job for test cases.
     * </p>
     * @return the AutoPilotJob instance.
     * @throws Exception - to JUnit.
     * @see junit.framework.TestCase#setUp()
     */
    private AutoPilotJob createAutoPilotJob() throws Exception {
        // Release AutoPilotJob.scheduler.
//        TestHelper.releaseSingletonInstance(AutoPilotJob.class, "scheduler");

        ConfigManager cfg = ConfigManager.getInstance();
        cfg.add(ActiveAutoPilotSource.class.getName(), "active_auto_source_pilot.xml",
                ConfigManager.CONFIG_XML_FORMAT);
        cfg.add(DefaultProjectPilot.class.getName(), "project_pilot.xml",
                ConfigManager.CONFIG_XML_FORMAT);
        cfg.add("logging.xml");
        cfg.add(AutoPilot.class.getName(), "auto_pilot.xml", ConfigManager.CONFIG_XML_FORMAT);
        cfg.add(AutoPilotJob.class.getName(), "auto_pilot_job.xml", ConfigManager.CONFIG_XML_FORMAT);
        cfg.add(AutoPilotJob.class.getName() + AutoPilotJob.OBJECT_FACTORY_POSTFIX,
                "auto_pilot_job_factory.xml", ConfigManager.CONFIG_XML_FORMAT);
        cfg.add("scheduler", "scheduler.xml", ConfigManager.CONFIG_XML_FORMAT);
        return new AutoPilotJob();
    }

    /**
     * Test method for {@link AutoPilotJob#AutoPilotJob()}.
     * Fails if bad configuration.
     */
    public void testAutoPilotJob() throws Exception {
        try {
            new AutoPilotJob();
//            fail("no namespace");
        } catch (ConfigurationException e) {
            // Good.
        }
    }

    /**
     * Test method for {@link AutoPilotJob#AutoPilotJob(String, String)}.
     * Fails if null namespace.
     * @throws Exception - to JUnit.
     */
    public void testAutoPilotJobStringStringNullNs() throws Exception {
        try {
            new AutoPilotJob((String) null, "apkey");
            fail("null namespace");
        } catch (IllegalArgumentException e) {
            // Good.
        }
    }

    /**
     * Test method for {@link AutoPilotJob#AutoPilotJob(String, String)}.
     * Fails if empty namespace.
     * @throws Exception - to JUnit.
     */
    public void testAutoPilotJobStringStringEmptyNs() throws Exception {
        try {
            new AutoPilotJob(" \r\t\n", "apkey");
            fail("empty namespace");
        } catch (IllegalArgumentException e) {
            // Good.
        }
    }

    /**
     * Test method for {@link AutoPilotJob#AutoPilotJob(String, String)}.
     * Fails if null auto pilot key.
     * @throws Exception - to JUnit.
     */
    public void testAutoPilotJobStringStringNullApkey() throws Exception {
        try {
            new AutoPilotJob("namespace", null);
            fail("null apkey");
        } catch (IllegalArgumentException e) {
            // Good.
        }
    }

    /**
     * Test method for {@link AutoPilotJob#AutoPilotJob(String, String)}.
     * Fails if empty auto pilot key.
     * @throws Exception - to JUnit.
     */
    public void testAutoPilotJobStringStringEmptyApkey() throws Exception {
        try {
            new AutoPilotJob("test", " \r\t\n");
            fail("empty apkey");
        } catch (IllegalArgumentException e) {
            // Good.
        }
    }

    /**
     * Test method for {@link AutoPilotJob#AutoPilotJob(AutoPilot, String)}.
     * Fails if null auto pilot.
     * @throws Exception - to JUnit.
     */
    public void testAutoPilotJobAutoPilotStringNullAutoPilot() throws Exception {
        try {
            new AutoPilotJob(null, "operator");
            fail("null auto pilot");
        } catch (IllegalArgumentException e) {
            // Good.
        }
    }

    /**
     * Test method for {@link AutoPilotJob#AutoPilotJob(AutoPilot, String)}.
     * Fails if null operator.
     * @throws Exception - to JUnit.
     */
    public void testAutoPilotJobAutoPilotStringNullOperator() throws Exception {
        try {
            new AutoPilotJob(createAutoPilot(), null, LogManager.getLog());
            fail("null operator");
        } catch (IllegalArgumentException e) {
            // Good.
        }
    }

    /**
     * Test method for {@link AutoPilotJob#AutoPilotJob(AutoPilot, String)}.
     * Fails if empty operator.
     * @throws Exception - to JUnit.
     */
    public void testAutoPilotJobAutoPilotStringEmptyOperator() throws Exception {
        try {
            new AutoPilotJob(createAutoPilot(), " \r\t\n", LogManager.getLog());
            fail("empty operator");
        } catch (IllegalArgumentException e) {
            // Good.
        }
    }

    /**
     * Test method for {@link AutoPilotJob#run()}.
     * Fails if auto pilot reports AutoPilotSourceException.
     * @throws Exception - to JUnit.
     */
    public void testRunVoidAutoPilotSourceException() throws Exception {
        try {
            // Load config.
            createAutoPilotJob();
            AutoPilot autoPilot = new AutoPilot() {
                public AutoPilotResult[] advanceProjects(String operator)
                        throws AutoPilotSourceException, PhaseOperationException {
                    throw new AutoPilotSourceException("test");
                }
            };
            AutoPilotJob autoPilotJob = new AutoPilotJob(autoPilot, "tester", LogManager.getLog());

            // Call run().
            autoPilotJob.run();
            fail("auto pilot reports AutoPilotSourceException");
        } catch (RuntimeException e) {
            // Good.
        }
    }

    /**
     * Test method for {@link AutoPilotJob#run()}.
     * Fails if auto pilot reports PhaseOperationException.
     * @throws Exception - to JUnit.
     */
    public void testRunVoidPhaseOperationException() throws Exception {
        try {
            // Load config.
            createAutoPilotJob();
            AutoPilot autoPilot = new AutoPilot() {
                public AutoPilotResult[] advanceProjects(String operator)
                        throws AutoPilotSourceException, PhaseOperationException {
                    throw new PhaseOperationException(-1, null, "test");
                }
            };
            AutoPilotJob autoPilotJob = new AutoPilotJob(autoPilot, "tester", LogManager.getLog());

            // Call run().
            autoPilotJob.run();
            fail("auto pilot reports AutoPilotSourceException");
        } catch (RuntimeException e) {
            // Good.
        }
    }

    /**
     * Test method for {@link AutoPilotJob#run(long[])}.
     * Fails if null projectId.
     * @throws Exception - to JUnit.
     */
    public void testRunLongArray() throws Exception {
        try {
            createAutoPilotJob().run(null);
            fail("null projectId");
        } catch (IllegalArgumentException e) {
            // Good.
        }
    }

    /**
     * Aggregates the test suite.
     * @return the test suite.
     */
    public static Test suite() {
        final TestSuite suite = new TestSuite();
        suite.addTestSuite(AutoPilotJobFailure.class);

        return suite;
    }

}
