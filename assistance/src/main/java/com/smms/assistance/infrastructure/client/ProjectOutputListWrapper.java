package com.smms.assistance.infrastructure.client;

import java.util.List;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class ProjectOutputListWrapper {
    public List<ProjectOutput> projects;

    public ProjectOutputListWrapper() {}

    public ProjectOutputListWrapper(List<ProjectOutput> projects) {
        this.projects = projects;
    }
}
