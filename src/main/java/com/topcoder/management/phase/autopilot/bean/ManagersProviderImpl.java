package com.topcoder.management.phase.autopilot.bean;

import com.topcoder.onlinereview.component.deliverable.UploadManager;
import com.topcoder.onlinereview.component.project.management.ProjectManager;
import com.topcoder.onlinereview.component.project.phase.PhaseManager;
import com.topcoder.onlinereview.component.resource.ResourceManager;
import com.topcoder.onlinereview.component.reviewupload.ManagersProvider;
import org.springframework.beans.factory.annotation.Autowired;

public class ManagersProviderImpl implements ManagersProvider {

    @Autowired
    private ResourceManager resourceManager;
    @Autowired
    private ProjectManager projectManager;
    @Autowired
    private PhaseManager phaseManager;
    @Autowired
    private UploadManager uploadManager;

    @Override
    public ResourceManager getResourceManager() {
        return resourceManager;
    }

    @Override
    public ProjectManager getProjectManager() {
        return projectManager;
    }

    @Override
    public PhaseManager getPhaseManager() {
        return phaseManager;
    }

    @Override
    public UploadManager getUploadManager() {
        return uploadManager;
    }
}
