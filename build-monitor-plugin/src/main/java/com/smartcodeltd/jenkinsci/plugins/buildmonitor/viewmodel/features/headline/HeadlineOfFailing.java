package com.smartcodeltd.jenkinsci.plugins.buildmonitor.viewmodel.features.headline;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.smartcodeltd.jenkinsci.plugins.buildmonitor.readability.Lister;
import com.smartcodeltd.jenkinsci.plugins.buildmonitor.readability.Pluraliser;
import com.smartcodeltd.jenkinsci.plugins.buildmonitor.viewmodel.BuildViewModel;
import com.smartcodeltd.jenkinsci.plugins.buildmonitor.viewmodel.JobView;
import com.smartcodeltd.jenkinsci.plugins.buildmonitor.viewmodel.features.SerialisableAsJsonObjectCalled;

import java.util.List;
import java.util.Set;

import static com.google.common.collect.Iterables.getLast;
import static com.google.common.collect.Lists.newLinkedList;
import static hudson.model.Result.SUCCESS;

public class HeadlineOfFailing implements SerialisableAsJsonObjectCalled<Headline> {

    private final static Set<String> Empty_Set = ImmutableSet.of();

    private final JobView job;
    private final HeadlineConfig config;

    public HeadlineOfFailing(JobView job, HeadlineConfig config) {
        this.job = job;
        this.config = config;
    }

    @Override
    public Headline asJson() {
        return new Headline(text(job.lastCompletedBuild()));
    }

    private String text(BuildViewModel lastBuild) {
        List<BuildViewModel> failedBuildsNewestToOldest = failedBuildsSince(lastBuild);

        String buildsFailedSoFar = Pluraliser.pluralise(
                "%s build has failed",
                "%s builds have failed",
                failedBuildsNewestToOldest.size()
        );

        BuildViewModel firstFailedBuild = failedBuildsNewestToOldest.isEmpty()
                ? lastBuild
                : getLast(failedBuildsNewestToOldest);

        return Lister.describe(
                buildsFailedSoFar,
                buildsFailedSoFar + " since %s committed their changes",
                newLinkedList(responsibleFor(firstFailedBuild))
        );
    }

    private List<BuildViewModel> failedBuildsSince(BuildViewModel build) {
        BuildViewModel currentBuild = build;

        List<BuildViewModel> failedBuilds = Lists.newArrayList();

        while (! SUCCESS.equals(currentBuild.result())) {

            if (! currentBuild.isRunning()) {
                failedBuilds.add(currentBuild);
            }

            if (! currentBuild.hasPreviousBuild()) {
                break;
            }

            currentBuild = currentBuild.previousBuild();
        }

        return failedBuilds;
    }

    private Set<String> responsibleFor(BuildViewModel build) {
        return config.displayCommitters
                ? build.culprits()
                : Empty_Set;
    }
}
