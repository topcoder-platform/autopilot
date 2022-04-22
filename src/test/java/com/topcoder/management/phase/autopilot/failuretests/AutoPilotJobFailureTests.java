/*
 * Copyright (C) 2006 TopCoder Inc., All Rights Reserved.
 */
package com.topcoder.management.phase.autopilot.failuretests;

import com.topcoder.management.phase.autopilot.*;
import com.topcoder.management.phase.autopilot.accuracytests.TestDataFactory;
import com.topcoder.util.objectfactory.InvalidClassSpecificationException;
import com.topcoder.util.objectfactory.impl.IllegalReferenceException;
import com.topcoder.util.objectfactory.impl.SpecificationConfigurationException;


/**
 * <p>
 * Failure test cases for <code>AutoPilotJob</code>.
 * </p>
 *
 * @author skatou
 * @version 1.0
 */
public class AutoPilotJobFailureTests extends FailureTestsHelper {
    /**
     * Sets up the test environment. Configurations are loaded.
     *
     * @throws Exception pass to JUnit.
     */
    protected void setUp() throws Exception {
        TestHelper.releaseSingletonInstance(AutoPilotJob.class, "operator");
        TestHelper.releaseSingletonInstance(AutoPilotJob.class, "autoPilot");
        super.setUp();
        loadConfig();
    }

    /**
     * Tests constructor without the configuration loaded. ConfigurationException should be thrown.
     *
     * @throws Exception pass to JUnit.
     */
    public void testConstructor1NoConfig() throws Exception {
        try {
            unloadConfig();
            new AutoPilotJob();
            fail("ConfigurationException should be thrown");
        } catch (ConfigurationException e) {
            assertTrue("Cause not as expected", e.getCause() instanceof SpecificationConfigurationException);
        }
    }

    /**
     * Tests constructor with a null argument. IllegalArgumentException should be thrown.
     *
     * @throws Exception pass to JUnit.
     */
    public void testConstructor2Null1() throws Exception {
        try {
            new AutoPilotJob((String) null, "b");
            fail("IllegalArgumentException should be thrown");
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    /**
     * Tests constructor with a null argument. IllegalArgumentException should be thrown.
     *
     * @throws Exception pass to JUnit.
     */
    public void testConstructor2Null2() throws Exception {
        try {
            new AutoPilotJob("a", null);
            fail("IllegalArgumentException should be thrown");
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    /**
     * Tests constructor with a empty string. IllegalArgumentException should be thrown.
     *
     * @throws Exception pass to JUnit.
     */
    public void testConstructor2EmptyString1() throws Exception {
        try {
            new AutoPilotJob("", "b");
            fail("IllegalArgumentException should be thrown");
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    /**
     * Tests constructor with a empty string. IllegalArgumentException should be thrown.
     *
     * @throws Exception pass to JUnit.
     */
    public void testConstructor2EmptyString2() throws Exception {
        try {
            new AutoPilotJob("       ", "b");
            fail("IllegalArgumentException should be thrown");
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    /**
     * Tests constructor with a empty string. IllegalArgumentException should be thrown.
     *
     * @throws Exception pass to JUnit.
     */
    public void testConstructor2EmptyString3() throws Exception {
        try {
            new AutoPilotJob("a", "");
            fail("IllegalArgumentException should be thrown");
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    /**
     * Tests constructor with a empty string. IllegalArgumentException should be thrown.
     *
     * @throws Exception pass to JUnit.
     */
    public void testConstructor2EmptyString4() throws Exception {
        try {
            new AutoPilotJob("a", "      ");
            fail("IllegalArgumentException should be thrown");
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    /**
     * Tests constructor with namespace that does not exist. ConfigurationException should be thrown.
     */
    public void testConstructor2ConfigNoNamespace() {
        try {
            new AutoPilotJob("BadNamespace", AutoPilot.class.getName());
            fail("ConfigurationException should be thrown");
        } catch (ConfigurationException e) {
            assertTrue("Cause not as expected", e.getCause() instanceof SpecificationConfigurationException);
        }
    }

    /**
     * Tests constructor with AutoPilot key that does not exist. ConfigurationException should be thrown.
     *
     * @throws Exception pass to JUnit.
     */
    public void testConstructor2ConfigNoAutoPilot() throws Exception {
        try {
            new AutoPilotJob(AutoPilotJob.class.getName(), "no");
            fail("ConfigurationException should be thrown");
        } catch (ConfigurationException e) {
            assertTrue("Cause not as expected", e.getCause() instanceof InvalidClassSpecificationException);
        }
    }

    /**
     * Tests constructor with a namespace that contains loop. ConfigurationException should be thrown.
     *
     * @throws Exception pass to JUnit.
     */
    public void testConstructor2ConfigLoop() throws Exception {
        try {
            new AutoPilotJob(AutoPilotJob.class.getName() + ".Loop", AutoPilot.class.getName());
            fail("ConfigurationException should be thrown");
        } catch (ConfigurationException e) {
            assertTrue("Cause not as expected", e.getCause() instanceof IllegalReferenceException);
        }
    }

    /**
     * Tests constructor with a namespace that contains wrong type. ConfigurationException should be thrown.
     *
     * @throws Exception pass to JUnit.
     */
    public void testConstructor2ConfigWrongType() throws Exception {
        try {
            new AutoPilotJob(AutoPilotJob.class.getName() + ".WrongType", AutoPilot.class.getName());
            fail("ConfigurationException should be thrown");
        } catch (ConfigurationException e) {
            // ok
        }
    }

    /**
     * Tests constructor with a null argument. IllegalArgumentException should be thrown.
     *
     * @throws Exception pass to JUnit.
     */
    public void testConstructor3Null1() throws Exception {
        try {
            new AutoPilotJob(null, "a");
            fail("IllegalArgumentException should be thrown");
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    /**
     * Tests constructor with a null argument. IllegalArgumentException should be thrown.
     *
     * @throws Exception pass to JUnit.
     */
    public void testConstructor3Null2() throws Exception {
        try {
            new AutoPilotJob(new AutoPilot(), null, TestDataFactory.getLog());
            fail("IllegalArgumentException should be thrown");
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    /**
     * Tests constructor with a empty string. IllegalArgumentException should be thrown.
     *
     * @throws Exception pass to JUnit.
     */
    public void testConstructor3EmptyString1() throws Exception {
        try {
            new AutoPilotJob(new AutoPilot(), "", TestDataFactory.getLog());
            fail("IllegalArgumentException should be thrown");
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    /**
     * Tests constructor with a empty string. IllegalArgumentException should be thrown.
     *
     * @throws Exception pass to JUnit.
     */
    public void testConstructor3EmptyString2() throws Exception {
        try {
            new AutoPilotJob(new AutoPilot(), "            ", TestDataFactory.getLog());
            fail("IllegalArgumentException should be thrown");
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    /**
     * Tests run method. AutoPilotSource fails, RuntimeException will be thrown, with cause an instance of
     * AutoPilotSourceException.
     *
     * @throws Exception pass to JUnit.
     */
    public void testRun1AutoPilotSourceFail() throws Exception {
        try {
            MockProjectManager.setSearchProjectsException(true);
            new AutoPilotJob().run();
            fail("RuntimeException should be thrown");
        } catch (RuntimeException e) {
            assertTrue("cause not as expected", e.getCause() instanceof AutoPilotSourceException);
        }
    }

    /**
     * Tests run method. ProjectPilot fails.
     * PhaseOperationException ignored in DefaultProjectPilot#advancePhases, so no exception thrown.
     *
     * @throws Exception pass to JUnit.
     */
    public void testRun1ProjectPilotFail() throws Exception {
        MockProjectManager.setSearchProjectsException(false);
        MockPhaseManager.setGetPhasesException(true);
        new AutoPilotJob().run();
        // PhaseOperationException ignored in DefaultProjectPilot#advancePhases, so no exception thrown.
    }

    /**
     * Tests run(long[]) method with a null argument. IllegalArgumentException will be thrown.
     *
     * @throws Exception pass to JUnit.
     */
    public void testRun2Null() throws Exception {
        try {
            new AutoPilotJob().run(null);
            fail("IllegalArgumentException should be thrown");
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    /**
     * Tests run(long[]) method. ProjectPilot fails.
     * PhaseOperationException ignored in DefaultProjectPilot#advancePhases, so no exception thrown.
     * @throws Exception pass to JUnit.
     */
    public void testRun2ProjectPilotFail() throws Exception {
        MockProjectManager.setSearchProjectsException(false);
        MockPhaseManager.setGetPhasesException(true);
        new AutoPilotJob().run(new long[] {1 });
        // PhaseOperationException ignored in DefaultProjectPilot#advancePhases, so no exception thrown.
    }
}
