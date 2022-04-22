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
import com.topcoder.util.scheduler.Job;
import com.topcoder.util.scheduler.Scheduler;

/**
 * <p>
 * Unit tests for class <code>AutoPilotJob</code>.
 * </p>
 * @author abelli
 * @version 1.0
 */
public class AutoPilotJobTest extends TestCase {
    /** AutoPilotJob instance used in test cases.*/
    private AutoPilotJob job;

    /**
     * <p>
     * Setup the test fixture. Load namespaces used to create AutoPilotJob instance,
     * then create AutoPilotJob instance for test cases.
     * </p>
     * @throws Exception - to JUnit.
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        // Release AutoPilotJob.scheduler.
        TestHelper.releaseSingletonInstance(AutoPilotJob.class, "operator");

        ConfigManager cfg = ConfigManager.getInstance();
        cfg.add(ActiveAutoPilotSource.class.getName(), "active_auto_source_pilot.xml",
            ConfigManager.CONFIG_XML_FORMAT);
        cfg.add(DefaultProjectPilot.class.getName(), "project_pilot.xml",
            ConfigManager.CONFIG_XML_FORMAT);
        cfg.add("logging.xml");

        cfg.add(AutoPilot.class.getName(), "auto_pilot.xml", ConfigManager.CONFIG_XML_FORMAT);

        // Operator is omitted in config files.
        cfg.add(AutoPilotJob.class.getName(), "auto_pilot_job.xml", ConfigManager.CONFIG_XML_FORMAT);
        cfg.add(AutoPilotJob.class.getName() + AutoPilotJob.OBJECT_FACTORY_POSTFIX,
            "auto_pilot_job_factory.xml", ConfigManager.CONFIG_XML_FORMAT);

        // Operator is specified in config files.
        cfg.add("AutoPilotJob_Operator", "auto_pilot_job_operator.xml", ConfigManager.CONFIG_XML_FORMAT);
        cfg.add("AutoPilotJob_Operator.factory", "auto_pilot_job_operator_factory.xml",
            ConfigManager.CONFIG_XML_FORMAT);

        cfg.add("scheduler", "scheduler.xml", ConfigManager.CONFIG_XML_FORMAT);
        job = new AutoPilotJob();
    }

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
     * Test method for {@link AutoPilotJob#AutoPilotJob()}.
     * Verifies the instance created successfully.
     */
    public void testAutoPilotJob() {
        assertTrue(job instanceof AutoPilotJob);
    }

    /**
     * Test method for {@link AutoPilotJob#AutoPilotJob(java.lang.String, java.lang.String)}.
     * Verifies constructs with specified namespace and auto pilot key successfully.
     * Operator is omitted in config files.
     * @throws Exception - to JUnit.
     */
    public void testAutoPilotJobStringString() throws Exception {
        job = new AutoPilotJob(AutoPilotJob.class.getName(), AutoPilot.class.getName());
        assertTrue(job instanceof AutoPilotJob);
        assertEquals(AutoPilotJob.DEFAULT_OPERATOR, job.getOperator());
    }

    /**
     * Test method for {@link AutoPilotJob#AutoPilotJob(java.lang.String, java.lang.String)}.
     * Verifies constructs with specified namespace and auto pilot key successfully.
     * Operator is specified in config files.
     * @throws Exception - to JUnit.
     */
    public void testAutoPilotJobStringStringOperator() throws Exception {
        job = new AutoPilotJob("AutoPilotJob_Operator", AutoPilot.class.getName());
        assertTrue(job instanceof AutoPilotJob);
        assertEquals("AutoPilotJob", job.getOperator());
    }

    /**
     * Test method for {@link AutoPilotJob#AutoPilotJob(AutoPilot, java.lang.String)}.
     * Verifies constructs with auto pilot and operator successfully.
     * @throws Exception - to JUnit.
     */
    public void testAutoPilotJobAutoPilotString() throws Exception {
        job = new AutoPilotJob(new AutoPilot(), "Operator", LogManager.getLog());
        assertTrue(job instanceof AutoPilotJob);
        assertEquals("Operator", job.getOperator());
    }

    /**
     * Test method for {@link AutoPilotJob#getAutoPilot()}.
     * Verifies return the correct auto pilot.
     */
    public void testGetAutoPilot() {
        assertTrue(job.getAutoPilot() instanceof AutoPilot);
    }

    /**
     * Test method for {@link AutoPilotJob#getOperator()}.
     * Verifies return the default operator.
     */
    public void testGetOperator() throws ConfigurationException {
        job = new AutoPilotJob("AutoPilotJob_Operator", AutoPilot.class.getName());
        assertEquals(AutoPilotJob.DEFAULT_OPERATOR, job.getOperator());
    }

    /**
     * Test method for {@link AutoPilotJob#run()}.
     * Verifies the job body can execute properly.
     */
    public void testRun() {
        job.run();
    }

    /**
     * Test method for {@link AutoPilotJob#run(long[])}.
     * Verifies return correct auto pilot result.
     * @throws Exception - to JUnit.
     */
    public void testRunLongArray() throws Exception {
        AutoPilotResult[] results = job.run(new long[] {1, 2});
        assertEquals(2, results.length);
    }

    /**
     * Test method for {@link AutoPilotJob#isDone()}.
     * Verifies false before run(); true after run().
     */
    public void testIsDone() {
        assertFalse(job.isDone());

        job.run();
        assertTrue(job.isDone());
    }

    /**
     * Test method for {@link AutoPilotJob#close()}.
     * Verifies close() can execute properly.
     */
    public void testClose() {
        job.close();
    }

    /**
     * Test method for {@link AutoPilotJob#getStatus()}.
     * Verifies return 'RUNNING' before run(); 'DONE' after that.
     */
    public void testGetStatus() {
        assertEquals(AutoPilotJob.STATUS_RUNNING, job.getStatus());
        job.run();
        assertEquals(AutoPilotJob.STATUS_DONE, job.getStatus());
    }

    /**
     * Aggregates the test suite.
     * @return the test suite.
     */
    public static Test suite() {
        final TestSuite suite = new TestSuite();
        suite.addTestSuite(AutoPilotJobTest.class);

        return suite;
    }

}
