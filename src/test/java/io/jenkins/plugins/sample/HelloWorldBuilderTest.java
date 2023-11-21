package io.jenkins.plugins.sample;

import java.util.List;
import java.util.Optional;
import org.azd.enums.PullRequestAsyncStatus;
import org.azd.enums.PullRequestStatus;
import org.azd.git.types.GitPullRequest;
import org.azd.git.types.GitRef;
import org.azd.git.types.GitRepository;
import org.azd.git.types.GitStatus;
import org.azd.git.types.GitStatusContext;
import org.azd.git.types.PullRequests;
import org.azd.utils.AzDClientApi;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class HelloWorldBuilderTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    final String name = "Bobby";

    @Test
    @Ignore
    public void manualTest() throws Exception {
        String organizationName = "amsadevelopment";
        String projectName = "amsa-java";
        String personalAccessToken = "";

        var webApi = new AzDClientApi(organizationName, projectName, personalAccessToken);

        GitRef gitRef = webApi.getGitApi().getBranch("amsa-java", "ignore-plexus-interpolation-sec");

        GitRepository gitRepository = webApi.getGitApi().getRepository("amsa-java");

        PullRequests pullRequestsResult = webApi.getGitApi().getPullRequests("amsa-java");
        List<GitPullRequest> pullRequests = pullRequestsResult.getPullRequests();
        System.out.println("pullRequests:" + pullRequests);

        Optional<GitPullRequest> optPr = pullRequests.stream()
                .filter(pr -> pr.getSourceRefName().contains("release-jobs-as-pipeline"))
                .findFirst();

        if (optPr.isPresent()) {
            GitPullRequest pr = optPr.get();
            PullRequestStatus prStatus = pr.getStatus();
            // webApi.getGitApi().createPullRequestStatus(optPr.get().getPullRequestId(), gitRepository.getId(),
            // GitStatus
            pr.setMergeStatus(PullRequestAsyncStatus.FAILURE);
            // Create a new status
            var gitPullRequestStatus = new GitStatus();
            gitPullRequestStatus.setContext(new GitStatusContext() {
                {
                    setGenre("Build");
                    setName("Failure");
                }
            });
            // gitPullRequestStatus.
            /// gitPullRequestStatus.setState(GitStatusState.FAILED);
            var newStatus = webApi.getGitApi()
                    .createPullRequestStatus(pr.getPullRequestId(), gitRepository.getId(), gitPullRequestStatus);
            System.out.println("newStatus:" + newStatus);
            // webApi.getGitApi().getPullRequestStatus(pr.getPullRequestId(), gitRepository.getId())
            // webApi.getGitApi().createPullRequestLabel(gitRepository.getName(), pr.getPullRequestId(), "BUILD
            // FAILURE");
        }
    }
}
