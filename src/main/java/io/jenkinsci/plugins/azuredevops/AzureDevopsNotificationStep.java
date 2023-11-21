package io.jenkinsci.plugins.azuredevops;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;

public class AzureDevopsNotificationStep extends Step {

    private String organizationName;

    private String projectName;

    private String credentialsId;

    private String repositoryName;

    @DataBoundConstructor
    public AzureDevopsNotificationStep(
            String organizationName, String projectName, String credentialsId, String repositoryName) {
        this.organizationName = organizationName;
        this.projectName = projectName;
        this.credentialsId = credentialsId;
        this.repositoryName = repositoryName;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new AzureDevopsNotificationStepExecution(context, this);
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {
        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return new HashSet<>(Arrays.asList(TaskListener.class, EnvVars.class, Run.class));
        }

        @Override
        public String getFunctionName() {
            return "azureDevopsNotificationStep";
        }
    }
}
