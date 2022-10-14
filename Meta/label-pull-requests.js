/*
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

const Label = {
    CommunityApproved: "✅ pr-community-approved",
    HasConflicts: "⚠️ pr-has-conflicts",
    IsBlocked: "⛔️ pr-is-blocked",
    MaintainerApprovedButAwaitingCi: "✅ pr-maintainer-approved-but-awaiting-ci",
    NeedsReview: "👀 pr-needs-review",
    Unclear: "🤔 pr-unclear",
    WaitingForAuthor: "⏳ pr-waiting-for-author",
};

const subjectiveLabels = [Label.IsBlocked, Label.Unclear];

function removeExistingPrLabels(currentLabels, keepSubjectiveLabels) {
    return currentLabels.filter(
        label =>
            !label.includes("pr-") ||
            label === Label.HasConflicts ||
            (keepSubjectiveLabels && subjectiveLabels.includes(label))
    );
}

async function labelsForGenericPullRequestChange(currentLabels) {
    const filteredLabels = removeExistingPrLabels(currentLabels, true);
    filteredLabels.push(Label.NeedsReview);
    return filteredLabels;
}

async function labelsForPullRequestEffectivelyClosed(currentLabels) {
    return removeExistingPrLabels(currentLabels, false);
}

function apiErrorHandler(error) {
    console.log(
        "::warning::Encountered error during event handling, not updating labels. Error:",
        error
    );
}

module.exports = ({ github, context }) => {
    async function labelsForPullRequestReviewSubmitted(currentLabels, { pull_request, review }) {
        let newLabels = currentLabels;
        const isBlocked = currentLabels.some(label => label === Label.IsBlocked);

        if (review.state.toLowerCase() === "approved") {
            const maintainers = (
                await github.rest.teams.listMembersInOrg({
                    org: "SerenityOS",
                    team_slug: "maintainers",
                })
            ).data;

            const approvedByMaintainer = maintainers.some(
                maintainerInArray => maintainerInArray.login === review.user.login
            );

            if (approvedByMaintainer) {
                newLabels = newLabels.filter(
                    label => !(label === Label.NeedsReview || label === Label.WaitingForAuthor)
                );

                if (!newLabels.includes(Label.MaintainerApprovedButAwaitingCi))
                    newLabels.push(Label.MaintainerApprovedButAwaitingCi);
            } else {
                if (!newLabels.includes(Label.CommunityApproved))
                    newLabels.push(Label.CommunityApproved);
            }
        } else if (!isBlocked) {
            // Remove approval labels.
            newLabels = newLabels.filter(
                label =>
                    !(
                        label === Label.CommunityApproved ||
                        label === Label.MaintainerApprovedButAwaitingCi
                    )
            );

            if (review.user.login === pull_request.user.login) newLabels.push(Label.NeedsReview);
            else newLabels.push(Label.WaitingForAuthor);
        }

        return newLabels;
    }

    const eventHandlers = {
        opened: labelsForGenericPullRequestChange,
        reopened: labelsForGenericPullRequestChange,
        submitted: labelsForPullRequestReviewSubmitted,
        dismissed: labelsForGenericPullRequestChange,
        converted_to_draft: labelsForPullRequestEffectivelyClosed,
        ready_for_review: labelsForGenericPullRequestChange,
        synchronize: labelsForGenericPullRequestChange, // synchronize is triggered when the branch is changed
        edited: labelsForGenericPullRequestChange,
        review_requested: labelsForGenericPullRequestChange,
        closed: labelsForPullRequestEffectivelyClosed,
    };

    const eventName = context.payload.action;
    const handlerForCurrentEvent = eventHandlers[eventName];

    async function updateLabels(currentLabelsAsObjects) {
        const currentLabels = [];
        currentLabelsAsObjects.forEach(labelObject => currentLabels.push(labelObject.name));

        const isEffectivelyClosed =
            context.payload.pull_request.draft ||
            context.payload.pull_request.state.toLowerCase() === "closed";

        const newLabels = await (isEffectivelyClosed
            ? labelsForPullRequestEffectivelyClosed(currentLabels, context.payload)
            : handlerForCurrentEvent(currentLabels, context.payload));

        console.log(
            `Received '${eventName}' event for ${
                isEffectivelyClosed ? "draft/closed" : "open"
            } pull request, changing labels from '${currentLabels}' to '${newLabels}'`
        );

        return github.rest.issues.setLabels({
            issue_number: context.payload.pull_request.number,
            owner: context.repo.owner,
            repo: context.repo.repo,
            labels: newLabels,
        });
    }

    if (handlerForCurrentEvent) {
        github.rest.issues
            .listLabelsOnIssue({
                issue_number: context.payload.pull_request.number,
                owner: context.repo.owner,
                repo: context.repo.repo,
            })
            .then(result => updateLabels(result.data))
            .catch(apiErrorHandler);
    } else {
        console.log(`::warning::No handler for the '${eventName}' event, not updating labels.`);
    }
};
