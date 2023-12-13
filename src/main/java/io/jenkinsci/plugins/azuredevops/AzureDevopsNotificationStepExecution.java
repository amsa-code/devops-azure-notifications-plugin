package io.jenkinsci.plugins.azuredevops;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.security.ACL;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.azd.enums.GitStatusState;
import org.azd.git.GitApi;
import org.azd.git.types.GitPullRequest;
import org.azd.git.types.GitRepository;
import org.azd.git.types.GitStatus;
import org.azd.git.types.GitStatusContext;
import org.azd.git.types.PullRequests;
import org.azd.utils.AzDClientApi;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;

public class AzureDevopsNotificationStepExecution extends SynchronousNonBlockingStepExecution<Void>
        implements Serializable {

    private AzureDevopsNotificationStep azureDevopsNotificationStep;

    public AzureDevopsNotificationStepExecution(
            StepContext context, AzureDevopsNotificationStep azureDevopsNotificationStep) {
        super(context);
        this.azureDevopsNotificationStep = azureDevopsNotificationStep;
    }

    @Override
    protected Void run() throws Exception {

        Run run = getContext().get(Run.class);
        TaskListener taskListener = getContext().get(TaskListener.class);

        String credentialsId = azureDevopsNotificationStep.getCredentialsId();
        if (credentialsId == null) {
            taskListener.getLogger().println("credentialsId cannot be null");
            return null;
        }

        StringCredentials stringCredentials = CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentials(StringCredentials.class, Jenkins.get(), ACL.SYSTEM),
                CredentialsMatchers.withId(credentialsId));

        if (stringCredentials == null) {
            taskListener.getLogger().println("Cannot find any credentials for id:" + credentialsId);
            return null;
        }

        String personalAccessToken = stringCredentials.getSecret().getPlainText();

        var webApi = new AzDClientApi(
                azureDevopsNotificationStep.getOrganizationName(),
                azureDevopsNotificationStep.getProjectName(),
                personalAccessToken);

        String branchName =
                run.getEnvironment(getContext().get(TaskListener.class)).get("BRANCH_NAME");

        if (StringUtils.isBlank(branchName)) {
            taskListener.getLogger().println("Cannot find any branchName");
            return null;
        }

        taskListener.getLogger().println("Check branch " + branchName);

        GitApi gitApi = webApi.getGitApi();
        PullRequests pullRequestsResult = gitApi.getPullRequests(azureDevopsNotificationStep.getRepositoryName());
        List<GitPullRequest> pullRequests = pullRequestsResult.getPullRequests();

        Optional<GitPullRequest> optPr = pullRequests.stream()
                .filter(pr -> {
                    String sourceRefName = pr.getSourceRefName();
                    taskListener
                            .getLogger()
                            .println("checking branch name " + branchName + " with PR sourceRefName " + sourceRefName);
                    return StringUtils.endsWith(sourceRefName, "/" + branchName);
                })
                .findFirst();

        if (optPr.isEmpty()) {
            taskListener.getLogger().println("Cannot find any PR for branch " + branchName);
            return null;
        }

        GitRepository gitRepository = gitApi.getRepository(azureDevopsNotificationStep.getRepositoryName());

        // String buildUrl = run.getEnvironment(getContext().get(TaskListener.class)).get("BUILD_URL");

        Result result = run.getResult();

        // Create a new status
        var gitPullRequestStatus = new GitStatus();
        gitPullRequestStatus.setContext(new GitStatusContext() {
            {
                setGenre("Build");
                setName("Build Result");
            }
        });

        if (result != null) {
            if (result.isBetterOrEqualTo(Result.SUCCESS)) {
                gitPullRequestStatus.setState(GitStatusState.SUCCEEDED);
            } else {
                gitPullRequestStatus.setState(GitStatusState.FAILED);
            }
            taskListener
                    .getLogger()
                    .println("will mark PR " + optPr.get().getPullRequestId() + " with status "
                            + gitPullRequestStatus.getState());
            gitApi.createPullRequestStatus(optPr.get().getPullRequestId(), gitRepository.getId(), gitPullRequestStatus);
            // TODO add a comment with build only if result changes or first build
            //            if (run.getPreviousBuild() == null || !result.equals(run.getPreviousBuild().getResult())) {
            //
            //            }
        }

        return null;
    }
}
