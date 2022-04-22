/*
 * Copyright (C) 2006 TopCoder Inc., All Rights Reserved.
 */
package com.topcoder.management.phase.autopilot.accuracytests.mock;

import com.topcoder.util.format.ObjectFormatter;
import com.topcoder.util.log.Level;

/**
 * <p></p>
 *
 * @author TCSDEVELOPER
 * @version 1.0
 */
public class MockLogAlternate extends MockLog {

    public MockLogAlternate(String config) {
        super(config);
    }

    /**
     * <p>Checks the equality of this object to specified one.</p>
     *
     * @param obj the reference object with which to compare.
     * @return <code>true</code> if this object is the same as the obj argument; <code>false</code> otherwise.
     */
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        return this.getClass().getName().equals(obj.getClass().getName());
    }

    @Override
    public void log(Level level, Object message, ObjectFormatter objectFormatter) {
        super.log(level, message);
    }

    @Override
    public void log(Level level, String messageFormat, Object arg1) {
        super.log(level, messageFormat);
    }

    @Override
    public void log(Level level, String messageFormat, Object arg1, Object arg2) {
        super.log(level, messageFormat);
    }

    @Override
    public void log(Level level, String messageFormat, Object arg1, Object arg2, Object arg3) {
        super.log(level, messageFormat);
    }

    @Override
    public void log(Level level, String messageFormat, Object[] args) {
        super.log(level, messageFormat);
    }

    @Override
    public void log(Level level, Throwable cause, Object message) {
        super.log(level, message);
    }

    @Override
    public void log(Level level, Throwable cause, Object message, ObjectFormatter objectFormatter) {
        super.log(level, message);
    }

    @Override
    public void log(Level level, Throwable cause, String messageFormat, Object arg1) {
        super.log(level, messageFormat);
    }

    @Override
    public void log(Level level, Throwable cause, String messageFormat, Object arg1, Object arg2) {
        super.log(level, messageFormat);
    }

    @Override
    public void log(Level level, Throwable cause, String messageFormat, Object arg1, Object arg2, Object arg3) {
        super.log(level, messageFormat);
    }

    @Override
    public void log(Level level, Throwable cause, String messageFormat, Object[] args) {
        super.log(level, messageFormat);
    }
}
