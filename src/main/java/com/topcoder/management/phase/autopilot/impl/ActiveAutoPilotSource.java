/*
 * Copyright (C) 2006-2012 TopCoder Inc., All Rights Reserved.
 */

package com.topcoder.management.phase.autopilot.impl;

import com.topcoder.management.phase.autopilot.AutoPilotSource;
import com.topcoder.management.phase.autopilot.AutoPilotSourceException;
import com.topcoder.management.phase.autopilot.logging.LogMessage;
import com.topcoder.onlinereview.component.project.management.Project;
import com.topcoder.onlinereview.component.project.management.ProjectFilterUtility;
import com.topcoder.onlinereview.component.project.management.ProjectManager;
import com.topcoder.onlinereview.component.search.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * An implementation of AutoPilotSource that retrieves all currently active projects that have auto
 * pilot switch on in its extended property. It uses Project Management component to search all
 * projects which are active and have auto pilot switch on in its extended property.
 * buildFilter/processProject are protected so that it can be extended/customized easily in the
 * future.
 * </p>
 * <p>
 * This class is immutable, however it's not guaranteed to be thread-safe because the underlying
 * ProjectManager instance that is used by this class may not be thread-safe. In a multiple thread
 * situation, application is advised to synchronize on the AutoPilot instance to ensure that only a
 * single thread is retrieving/advancing project phases at one time.
 * </p>
 * @author sindu, abelli
 * @version 1.0.2
 */
public class ActiveAutoPilotSource implements AutoPilotSource {

    /**
     * <p>
     * Represents the default project status name for an active status. Referenced in ctor().
     * </p>
     */
    public static final String DEFAULT_ACTIVE_STATUS_NAME = "Active";

    /**
     * <p>
     * Represents the default extended property name for the auto pilot switch. Referenced in
     * ctor().
     * </p>
     */
    public static final String DEFAULT_EXTPROP_AUTOPILOTSWITCH = "Autopilot Option";

    /**
     * <p>
     * Represents the default extended property value for the auto pilot switch. Referenced in
     * ctor().
     * </p>
     */
    public static final String DEFAULT_EXTPROP_AUTOPILOTSWITCH_VALUE = "On";

    /**
     * Zero length <code>long</code> array, which can be returned by {@link #processProject(Project[])}.
     */
    private static final long[] ZERO_LONG_ARRAY = new long[0];

    /**
     * <p>The log used by this class for logging errors and debug information.</p>
     */
    private static final Logger log = LoggerFactory.getLogger(ActiveAutoPilotSource.class);
    
    /**
     * <p>
     * Represents the ProjectManager instance that is used to search project based on its status and
     * extended property. This variable is initially null, initialized in constructor using object
     * factory and immutable afterwards. It is referenced by getProjectId. It can be retrieved with
     * the getter.
     * </p>
     */
    private ProjectManager projectManager;

    /**
     * <p>
     * Represents the project status name of an active status. This will be used to compare a
     * project status name. This variable is initially null, set in the constructor and immutable
     * afterwards. It is referenced by getProjectId. It can be retrieved with the getter.
     * </p>
     */
    private String activeStatusName;

    /**
     * <p>
     * Represents the extended property name for auto pilot switch. This will be used to search
     * projects. This variable is initially null, set in the constructor and immutable afterwards.
     * It is referenced by getProjectId. It can be retrieved with the getter.
     * </p>
     */
    private String extPropAutoPilotSwitch;

    /**
     * <p>
     * Represents the extended property value for auto pilot switch. This will be used to search
     * projects. This variable is initially null, set in the constructor and immutable afterwards.
     * It is referenced by getProjectId. It can be retrieved with the getter.
     * </p>
     */
    private String extPropAutoPilotSwitchValue;

    /**
     * <p>
     * Return the project manager instance used by this class.
     * </p>
     * @return the project manager instance used by this class.
     */
    protected ProjectManager getProjectManager() {
        return this.projectManager;
    }

    /**
     * <p>
     * Returns the name of a project status that represents an active status.
     * </p>
     * @return A non-null string representing the active project status name
     */
    public String getActiveStatusName() {
        return this.activeStatusName;
    }

    /**
     * <p>
     * Returns the extended property name for auto pilot switch.
     * </p>
     * @return A non-null string representing the extended property name for auto pilot switch
     */
    public String getExtPropAutoPilotSwitch() {
        return this.extPropAutoPilotSwitch;
    }

    /**
     * <p>
     * Returns the extended property value for auto pilot switch.
     * </p>
     * @return A non-null string representing the extended property value for auto pilot switch
     */
    public String getExtPropAutoPilotSwitchValue() {
        return this.extPropAutoPilotSwitchValue;
    }

    public void setProjectManager(ProjectManager projectManager) {
        this.projectManager = projectManager;
    }

    public void setActiveStatusName(String activeStatusName) {
        this.activeStatusName = activeStatusName;
    }

    public void setExtPropAutoPilotSwitch(String extPropAutoPilotSwitch) {
        this.extPropAutoPilotSwitch = extPropAutoPilotSwitch;
    }

    public void setExtPropAutoPilotSwitchValue(String extPropAutoPilotSwitchValue) {
        this.extPropAutoPilotSwitchValue = extPropAutoPilotSwitchValue;
    }

    /**
     * <p>
     * Retrieves all project ids that are active and have Autopilot option switch on in its extended
     * property. This will use {@link ProjectManager#searchProjects(Filter)}. buildFilter is used to build
     * the filter. The return Project[] is then processed by processProject() to extract its id,
     * it's also possible to do more programmatically filtering here.
     * </p>
     * @return a long[] representing project ids to auto pilot (could be empty, but never null)
     * @throws AutoPilotSourceException if an error occurs retrieving the project ids
     */
    public long[] getProjectIds() throws AutoPilotSourceException {
        Filter f = buildFilter();
        try {
            Project[] proj = projectManager.searchProjects(f);

            return processProject(proj);
        } catch (Exception e) {
        	log.error("Fail to get projects from projectManager.\n" + LogMessage.getExceptionStackTrace(e));
            return ZERO_LONG_ARRAY;
        }
    }

    /**
     * <p>
     * Builds the filter that is to be passed to {@link ProjectManager#searchProjects(Filter)}. The
     * filters must filter based on the following search criteria:<br>
     * <ul>
     * <li> - Project status = Active</li>
     * <li> - Extended property name: AutopilotOption = On</li>
     * </ul>
     * <br>
     * </p>
     * @return A non-null filter representing the search criteria
     */
    protected Filter buildFilter() {
        Filter stat = ProjectFilterUtility.buildStatusNameEqualFilter(getActiveStatusName());
        Filter extProp = ProjectFilterUtility.buildProjectPropertyEqualFilter(
            getExtPropAutoPilotSwitch(), getExtPropAutoPilotSwitchValue());
        return ProjectFilterUtility.buildAndFilter(stat, extProp);
    }

    /**
     * <p>
     * Extracts project ids into array. Future implementation may want to override this to do
     * additional filtering programmatically.
     * </p>
     * @param project an array of Project[] whose id to return (could be null)
     * @return a long[] representing project id (never null, but could be empty)
     */
    protected long[] processProject(Project[] project) {
        if (null == project || project.length < 1) {
            return ZERO_LONG_ARRAY;
        }

        long[] ids = new long[project.length];
        for (int i = 0; i < project.length; i++) {
            ids[i] = project[i].getId();
        }

        return ids;
    }
}
