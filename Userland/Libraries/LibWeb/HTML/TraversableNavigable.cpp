/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/QuickSort.h>
#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/BrowsingContextGroup.h>
#include <LibWeb/HTML/DocumentState.h>
#include <LibWeb/HTML/NavigationParams.h>
#include <LibWeb/HTML/SessionHistoryEntry.h>
#include <LibWeb/HTML/TraversableNavigable.h>
#include <LibWeb/Platform/EventLoopPlugin.h>

namespace Web::HTML {

TraversableNavigable::TraversableNavigable(Page& page)
    : m_page(page)
{
}

TraversableNavigable::~TraversableNavigable() = default;

void TraversableNavigable::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    for (auto& entry : m_session_history_entries)
        visitor.visit(entry);
}

static OrderedHashTable<TraversableNavigable*>& user_agent_top_level_traversable_set()
{
    static OrderedHashTable<TraversableNavigable*> set;
    return set;
}

struct BrowsingContextAndDocument {
    JS::NonnullGCPtr<HTML::BrowsingContext> browsing_context;
    JS::NonnullGCPtr<DOM::Document> document;
};

// https://html.spec.whatwg.org/multipage/document-sequences.html#creating-a-new-top-level-browsing-context
static WebIDL::ExceptionOr<BrowsingContextAndDocument> create_a_new_top_level_browsing_context_and_document(Page& page)
{
    // 1. Let group and document be the result of creating a new browsing context group and document.
    auto [group, document] = TRY(BrowsingContextGroup::create_a_new_browsing_context_group_and_document(page));

    // 2. Return group's browsing context set[0] and document.
    return BrowsingContextAndDocument { **group->browsing_context_set().begin(), document };
}

// https://html.spec.whatwg.org/multipage/document-sequences.html#creating-a-new-top-level-traversable
WebIDL::ExceptionOr<JS::NonnullGCPtr<TraversableNavigable>> TraversableNavigable::create_a_new_top_level_traversable(Page& page, JS::GCPtr<HTML::BrowsingContext> opener, String target_name)
{
    auto& vm = Bindings::main_thread_vm();

    // 1. Let document be null.
    JS::GCPtr<DOM::Document> document = nullptr;

    // 2. If opener is null, then set document to the second return value of creating a new top-level browsing context and document.
    if (!opener) {
        document = TRY(create_a_new_top_level_browsing_context_and_document(page)).document;
    }

    // 3. Otherwise, set document to the second return value of creating a new auxiliary browsing context and document given opener.
    else {
        document = TRY(BrowsingContext::create_a_new_auxiliary_browsing_context_and_document(page, *opener)).document;
    }

    // 4. Let documentState be a new document state, with
    auto document_state = vm.heap().allocate_without_realm<DocumentState>();

    // document: document
    document_state->set_document(document);

    // navigable target name: targetName
    document_state->set_navigable_target_name(target_name);

    // 5. Let traversable be a new traversable navigable.
    auto traversable = vm.heap().allocate_without_realm<TraversableNavigable>(page);

    // 6. Initialize the navigable traversable given documentState.
    TRY_OR_THROW_OOM(vm, traversable->initialize_navigable(document_state, nullptr));

    // 7. Let initialHistoryEntry be traversable's active session history entry.
    auto initial_history_entry = traversable->active_session_history_entry();
    VERIFY(initial_history_entry);

    // 8. Set initialHistoryEntry's step to 0.
    initial_history_entry->step = 0;

    // 9. Append initialHistoryEntry to traversable's session history entries.
    traversable->m_session_history_entries.append(*initial_history_entry);

    // FIXME: 10. If opener is non-null, then legacy-clone a traversable storage shed given opener's top-level traversable and traversable. [STORAGE]

    // 11. Append traversable to the user agent's top-level traversable set.
    user_agent_top_level_traversable_set().set(traversable);

    // 12. Return traversable.
    return traversable;
}

// https://html.spec.whatwg.org/multipage/document-sequences.html#create-a-fresh-top-level-traversable
WebIDL::ExceptionOr<JS::NonnullGCPtr<TraversableNavigable>> TraversableNavigable::create_a_fresh_top_level_traversable(Page& page, AK::URL const& initial_navigation_url, Variant<Empty, String, POSTResource> initial_navigation_post_resource)
{
    // 1. Let traversable be the result of creating a new top-level traversable given null and the empty string.
    auto traversable = TRY(create_a_new_top_level_traversable(page, nullptr, {}));

    // 2. Navigate traversable to initialNavigationURL using traversable's active document, with documentResource set to initialNavigationPostResource.
    TRY(traversable->navigate(initial_navigation_url, *traversable->active_document(), initial_navigation_post_resource));

    // 3. Return traversable.
    return traversable;
}

// https://html.spec.whatwg.org/multipage/document-sequences.html#top-level-traversable
bool TraversableNavigable::is_top_level_traversable() const
{
    // A top-level traversable is a traversable navigable with a null parent.
    return parent() == nullptr;
}

// https://html.spec.whatwg.org/multipage/browsing-the-web.html#getting-all-used-history-steps
Vector<int> TraversableNavigable::get_all_used_history_steps() const
{
    // FIXME: 1. Assert: this is running within traversable's session history traversal queue.

    // 2. Let steps be an empty ordered set of non-negative integers.
    OrderedHashTable<int> steps;

    // 3. Let entryLists be the ordered set « traversable's session history entries ».
    Vector<Vector<JS::NonnullGCPtr<SessionHistoryEntry>>> entry_lists { session_history_entries() };

    // 4. For each entryList of entryLists:
    while (!entry_lists.is_empty()) {
        auto entry_list = entry_lists.take_first();

        // 1. For each entry of entryList:
        for (auto& entry : entry_list) {
            // 1. Append entry's step to steps.
            steps.set(entry->step.get<int>());

            // 2. For each nestedHistory of entry's document state's nested histories, append nestedHistory's entries list to entryLists.
            for (auto& nested_history : entry->document_state->nested_histories())
                entry_lists.append(nested_history.entries);
        }
    }

    // 5. Return steps, sorted.
    auto sorted_steps = steps.values();
    quick_sort(sorted_steps);
    return sorted_steps;
}

// https://html.spec.whatwg.org/multipage/browsing-the-web.html#getting-the-history-object-length-and-index
TraversableNavigable::HistoryObjectLengthAndIndex TraversableNavigable::get_the_history_object_length_and_index(int step) const
{
    // 1. Let steps be the result of getting all used history steps within traversable.
    auto steps = get_all_used_history_steps();

    // 2. Let scriptHistoryLength be the size of steps.
    auto script_history_length = steps.size();

    // 3. Assert: steps contains step.
    VERIFY(steps.contains_slow(step));

    // 4. Let scriptHistoryIndex be the index of step in steps.
    auto script_history_index = *steps.find_first_index(step);

    // 5. Return (scriptHistoryLength, scriptHistoryIndex).
    return HistoryObjectLengthAndIndex {
        .script_history_length = script_history_length,
        .script_history_index = script_history_index
    };
}

// https://html.spec.whatwg.org/multipage/browsing-the-web.html#getting-the-used-step
int TraversableNavigable::get_the_used_step(int step) const
{
    // 1. Let steps be the result of getting all used history steps within traversable.
    auto steps = get_all_used_history_steps();

    // 2. Return the greatest item in steps that is less than or equal to step.
    VERIFY(!steps.is_empty());
    Optional<int> result;
    for (size_t i = 0; i < steps.size(); i++) {
        if (steps[i] <= step) {
            if (!result.has_value() || (result.value() < steps[i])) {
                result = steps[i];
            }
        }
    }
    return result.value();
}

// https://html.spec.whatwg.org/multipage/browsing-the-web.html#get-all-navigables-whose-current-session-history-entry-will-change-or-reload
Vector<JS::Handle<Navigable>> TraversableNavigable::get_all_navigables_whose_current_session_history_entry_will_change_or_reload(int target_step) const
{
    // 1. Let results be an empty list.
    Vector<JS::Handle<Navigable>> results;

    // 2. Let navigablesToCheck be « traversable ».
    Vector<JS::Handle<Navigable>> navigables_to_check;
    navigables_to_check.append(const_cast<TraversableNavigable&>(*this));

    // 3. For each navigable of navigablesToCheck:
    while (!navigables_to_check.is_empty()) {
        auto navigable = navigables_to_check.take_first();

        // 1. Let targetEntry be the result of getting the target history entry given navigable and targetStep.
        auto target_entry = navigable->get_the_target_history_entry(target_step);

        // 2. If targetEntry is not navigable's current session history entry or targetEntry's document state's reload pending is true, then append navigable to results.
        if (target_entry != navigable->current_session_history_entry() || target_entry->document_state->reload_pending()) {
            results.append(*navigable);
        }

        // 3. If targetEntry's document is navigable's document, and targetEntry's document state's reload pending is false, then extend navigablesToCheck with the child navigables of navigable.
        if (target_entry->document_state->document() == navigable->active_document() && !target_entry->document_state->reload_pending()) {
            navigables_to_check.extend(navigable->child_navigables());
        }
    }

    // 4. Return results.
    return results;
}

// https://html.spec.whatwg.org/multipage/browsing-the-web.html#apply-the-history-step
void TraversableNavigable::apply_the_history_step(int step, Optional<SourceSnapshotParams> source_snapshot_params)
{
    // FIXME: 1. Assert: This is running within traversable's session history traversal queue.

    // 2. Let targetStep be the result of getting the used step given traversable and step.
    auto target_step = get_the_used_step(step);

    // FIXME: 3. If initiatorToCheck is given, then:

    // FIXME: 4. Let navigablesCrossingDocuments be the result of getting all navigables that might experience a cross-document traversal given traversable and targetStep.

    // FIXME: 5. If checkForUserCancelation is true, and the result of checking if unloading is user-canceled given navigablesCrossingDocuments given traversable and targetStep is true, then return.

    // 6. Let changingNavigables be the result of get all navigables whose current session history entry will change or reload given traversable and targetStep.
    auto changing_navigables = get_all_navigables_whose_current_session_history_entry_will_change_or_reload(target_step);

    // FIXME: 7. Let nonchangingNavigablesThatStillNeedUpdates be the result of getting all navigables that only need history object length/index update given traversable and targetStep.

    // 8. For each navigable of changingNavigables:
    for (auto& navigable : changing_navigables) {
        // 1. Let targetEntry be the result of getting the target history entry given navigable and targetStep.
        auto target_entry = navigable->get_the_target_history_entry(target_step);

        // 2. Set navigable's current session history entry to targetEntry.
        navigable->set_current_session_history_entry(target_entry);

        // 3. Set navigable's ongoing navigation to "traversal".
        navigable->set_ongoing_navigation(Traversal::Tag);
    }

    // 9. Let totalChangeJobs be the size of changingNavigables.
    auto total_change_jobs = changing_navigables.size();

    // 10. Let completedChangeJobs be 0.
    size_t completed_change_jobs = 0;

    struct ChangingNavigableContinuationState {
        JS::Handle<DOM::Document> displayed_document;
        JS::Handle<SessionHistoryEntry> target_entry;
        JS::Handle<Navigable> navigable;
        bool update_only;
    };

    // 11. Let changingNavigableContinuations be an empty queue of changing navigable continuation states.
    Queue<ChangingNavigableContinuationState> changing_navigable_continuations;

    // 12. For each navigable of changingNavigables, queue a global task on the navigation and traversal task source of navigable's active window to run the steps:
    for (auto& navigable : changing_navigables) {
        queue_global_task(Task::Source::NavigationAndTraversal, *navigable->active_window(), [&] {
            // 1. Let displayedEntry be navigable's active session history entry.
            auto displayed_entry = navigable->active_session_history_entry();

            // 2. Let targetEntry be navigable's current session history entry.
            auto target_entry = navigable->current_session_history_entry();

            // 3. Let changingNavigableContinuation be a changing navigable continuation state with:
            auto changing_navigable_continuation = ChangingNavigableContinuationState {
                .displayed_document = displayed_entry->document_state->document(),
                .target_entry = target_entry,
                .navigable = navigable,
                .update_only = false
            };

            // 4. If displayedEntry is targetEntry and targetEntry's document state's reload pending is false, then:
            if (displayed_entry == target_entry && !target_entry->document_state->reload_pending()) {
                // 1. Set changingNavigableContinuation's update-only to true.
                changing_navigable_continuation.update_only = true;

                // 2. Enqueue changingNavigableContinuation on changingNavigableContinuations.
                changing_navigable_continuations.enqueue(move(changing_navigable_continuation));

                // 3. Abort these steps.
                return;
            }

            // 5. Let oldOrigin be targetEntry's document state's origin.
            [[maybe_unused]] auto old_origin = target_entry->document_state->origin();

            auto after_document_populated = [target_entry, changing_navigable_continuation, &changing_navigable_continuations]() mutable {
                // 1. If targetEntry's document is null, then set changingNavigableContinuation's update-only to true.
                if (!target_entry->document_state->document()) {
                    changing_navigable_continuation.update_only = true;
                }

                // FIXME: 2. If targetEntry's document's origin is not oldOrigin, then set targetEntry's serialized state to StructuredSerializeForStorage(null).

                // FIXME: 3. If all of the following are true:

                // 4. Enqueue changingNavigableContinuation on changingNavigableContinuations.
                changing_navigable_continuations.enqueue(move(changing_navigable_continuation));
            };

            // 6. If targetEntry's document is null, or targetEntry's document state's reload pending is true, then:
            if (!target_entry->document_state->document() || target_entry->document_state->reload_pending()) {
                // FIXME: 1. Let navTimingType be "back_forward" if targetEntry's document is null; otherwise "reload".

                // FIXME: 2. Let targetSnapshotParams be the result of snapshotting target snapshot params given navigable.

                // 3. Let potentiallyTargetSpecificSourceSnapshotParams be sourceSnapshotParams.
                Optional<SourceSnapshotParams> potentially_target_specific_source_snapshot_params = source_snapshot_params;

                // 4. If potentiallyTargetSpecificSourceSnapshotParams is null, then set it to the result of snapshotting source snapshot params given navigable's active document.
                if (!potentially_target_specific_source_snapshot_params.has_value()) {
                    potentially_target_specific_source_snapshot_params = navigable->active_document()->snapshot_source_snapshot_params();
                }

                // 5. Set targetEntry's document state's reload pending to false.
                target_entry->document_state->set_reload_pending(false);

                // 6. Let allowPOST be targetEntry's document state's reload pending.
                auto allow_POST = target_entry->document_state->reload_pending();

                // 7. In parallel, attempt to populate the history entry's document for targetEntry, given navigable, potentiallyTargetSpecificSourceSnapshotParams,
                //    targetSnapshotParams, with allowPOST set to allowPOST and completionSteps set to queue a global task on the navigation and traversal task source given
                //    navigable's active window to run afterDocumentPopulated.
                navigable->populate_session_history_entry_document(target_entry, {}, {}, *potentially_target_specific_source_snapshot_params, allow_POST, [this, after_document_populated]() mutable {
                             queue_global_task(Task::Source::NavigationAndTraversal, *active_window(), [after_document_populated]() mutable {
                                 after_document_populated();
                             });
                         })
                    .release_value_but_fixme_should_propagate_errors();
            }
            // Otherwise, run afterDocumentPopulated immediately.
            else {
                after_document_populated();
            }
        });
    }

    // FIXME: 13. Let navigablesThatMustWaitBeforeHandlingSyncNavigation be an empty set.

    // FIXME: 14. While completedChangeJobs does not equal totalChangeJobs:
    while (completed_change_jobs != total_change_jobs) {
        // FIXME: 1. If traversable's running nested apply history step is false, then:

        // AD-HOC: Since currently populate_session_history_entry_document does not run in parallel
        //         we call spin_until to interrupt execution of this function and let document population
        //         to complete.
        Platform::EventLoopPlugin::the().spin_until([&] {
            return !changing_navigable_continuations.is_empty() || completed_change_jobs == total_change_jobs;
        });

        if (changing_navigable_continuations.is_empty()) {
            continue;
        }

        // 2. Let changingNavigableContinuation be the result of dequeuing from changingNavigableContinuations.
        auto changing_navigable_continuation = changing_navigable_continuations.dequeue();

        // 3. If changingNavigableContinuation is nothing, then continue.

        // 4. Let displayedDocument be changingNavigableContinuation's displayed document.
        auto displayed_document = changing_navigable_continuation.displayed_document;

        // 5. Let targetEntry be changingNavigableContinuation's target entry.
        auto target_entry = changing_navigable_continuation.target_entry;

        // 6. Let navigable be changingNavigableContinuation's navigable.
        auto navigable = changing_navigable_continuation.navigable;

        // 7. Set navigable's ongoing navigation to null.
        navigable->set_ongoing_navigation({});

        // 8. Let (scriptHistoryLength, scriptHistoryIndex) be the result of getting the history object length and index given traversable and targetStep.
        auto [script_history_length, script_history_index] = get_the_history_object_length_and_index(target_step);
        (void)script_history_length;
        (void)script_history_index;

        // FIXME: 9. Append navigable to navigablesThatMustWaitBeforeHandlingSyncNavigation.

        // 10. Queue a global task on the navigation and traversal task source given navigable's active window to run the steps:
        queue_global_task(Task::Source::NavigationAndTraversal, *navigable->active_window(), [&, target_entry, navigable, displayed_document, update_only = changing_navigable_continuation.update_only] {
            // 1. If changingNavigableContinuation's update-only is false, then:
            if (!update_only) {
                // 1. Unload displayedDocument given targetEntry's document.
                displayed_document->unload(target_entry->document_state->document());

                // 2. For each childNavigable of displayedDocument's descendant navigables, queue a global task on the navigation and traversal task source given
                //    childNavigable's active window to unload childNavigable's active document.
                for (auto child_navigable : displayed_document->descendant_navigables()) {
                    queue_global_task(Task::Source::NavigationAndTraversal, *navigable->active_window(), [child_navigable] {
                        child_navigable->active_document()->unload();
                    });
                }

                // 3. Activate history entry targetEntry for navigable.
                navigable->activate_history_entry(*target_entry);
            }

            // FIXME: 2. If targetEntry's document is not equal to displayedDocument, then queue a global task on the navigation and traversal task source given targetEntry's document's
            //           relevant global object to perform the following step. Otherwise, continue onward to perform the following step within the currently-queued task.

            // FIXME: 3. Update document for history step application given targetEntry's document, targetEntry, changingNavigableContinuation's update-only, scriptHistoryLength, and
            //           scriptHistoryIndex.

            // 4. Increment completedChangeJobs.
            completed_change_jobs++;
        });
    }

    // FIXME: 15. Let totalNonchangingJobs be the size of nonchangingNavigablesThatStillNeedUpdates.

    // FIXME: 16. Let completedNonchangingJobs be 0.

    // FIXME: 17. Let (scriptHistoryLength, scriptHistoryIndex) be the result of getting the history object length and index given traversable and targetStep.

    // FIXME: 18. For each navigable of nonchangingNavigablesThatStillNeedUpdates, queue a global task on the navigation and traversal task source given navigable's active window to run the steps:

    // FIXME: 19. Wait for completedNonchangingJobs to equal totalNonchangingJobs.

    // 20. Set traversable's current session history step to targetStep.
    m_current_session_history_step = target_step;
}

// https://html.spec.whatwg.org/multipage/browsing-the-web.html#clear-the-forward-session-history
void TraversableNavigable::clear_the_forward_session_history()
{
    // FIXME: 1. Assert: this is running within navigable's session history traversal queue.

    // 2. Let step be the navigable's current session history step.
    auto step = current_session_history_step();

    // 3. Let entryLists be the ordered set « navigable's session history entries ».
    Vector<Vector<JS::NonnullGCPtr<SessionHistoryEntry>>&> entry_lists;
    entry_lists.append(session_history_entries());

    // 4. For each entryList of entryLists:
    while (!entry_lists.is_empty()) {
        auto& entry_list = entry_lists.take_first();

        // 1. Remove every session history entry from entryList that has a step greater than step.
        entry_list.remove_all_matching([step](auto& entry) {
            return entry->step.template get<int>() > step;
        });

        // 2. For each entry of entryList:
        for (auto& entry : entry_list) {
            // 1. For each nestedHistory of entry's document state's nested histories, append nestedHistory's entries list to entryLists.
            for (auto& nested_history : entry->document_state->nested_histories()) {
                entry_lists.append(nested_history.entries);
            }
        }
    }
}

// https://html.spec.whatwg.org/multipage/browsing-the-web.html#traverse-the-history-by-a-delta
void TraversableNavigable::traverse_the_history_by_delta(int delta)
{
    // FIXME: 1. Let sourceSnapshotParams and initiatorToCheck be null.

    // FIXME: 2. If sourceDocument is given, then:

    // 3. Append the following session history traversal steps to traversable:
    append_session_history_traversal_steps([this, delta] {
        // 1. Let allSteps be the result of getting all used history steps for traversable.
        auto all_steps = get_all_used_history_steps();

        // 2. Let currentStepIndex be the index of traversable's current session history step within allSteps.
        auto current_step_index = *all_steps.find_first_index(current_session_history_step());

        // 3. Let targetStepIndex be currentStepIndex plus delta
        auto target_step_index = current_step_index + delta;

        // 4. If allSteps[targetStepIndex] does not exist, then abort these steps.
        if (target_step_index >= all_steps.size()) {
            return;
        }

        // 5. Apply the history step allSteps[targetStepIndex] to traversable, with checkForUserCancelation set to true,
        //    sourceSnapshotParams set to sourceSnapshotParams, and initiatorToCheck set to initiatorToCheck.
        apply_the_history_step(all_steps[target_step_index]);
    });
}

// https://html.spec.whatwg.org/multipage/browsing-the-web.html#update-for-navigable-creation/destruction
void TraversableNavigable::update_for_navigable_creation_or_destruction()
{
    // 1. Let step be traversable's current session history step.
    auto step = current_session_history_step();

    // 2. Return the result of applying the history step step to traversable given false, false, null, null, and null.
    // FIXME: Pass false, false, null, null, and null as arguments.
    apply_the_history_step(step);
}

// https://html.spec.whatwg.org/multipage/browsing-the-web.html#apply-the-reload-history-step
void TraversableNavigable::apply_the_reload_history_step()
{
    // 1. Let step be traversable's current session history step.
    auto step = current_session_history_step();

    // 2. Return the result of applying the history step step to traversable given true, false, null, null, and null.
    // FIXME: Pass true, false, null, null, and null as arguments.
    apply_the_history_step(step);
}

void TraversableNavigable::apply_the_push_or_replace_history_step(int step)
{
    // 1. Return the result of applying the history step step to traversable given false, false, null, null, and null.
    // FIXME: Pass false, false, null, null, and null as arguments.
    // FIXME: Return result of history application.
    apply_the_history_step(step);
}

// https://html.spec.whatwg.org/multipage/document-sequences.html#close-a-top-level-traversable
void TraversableNavigable::close_top_level_traversable()
{
    VERIFY(is_top_level_traversable());

    // 1. Let toUnload be traversable's active document's inclusive descendant navigables.
    auto to_unload = active_document()->inclusive_descendant_navigables();

    // FIXME: 2. If the result of checking if unloading is user-canceled for toUnload is true, then return.

    // 3. Unload the active documents of each of toUnload.
    for (auto navigable : to_unload) {
        navigable->active_document()->unload();
    }

    // 4. Destroy traversable.
    destroy_top_level_traversable();
}

// https://html.spec.whatwg.org/multipage/document-sequences.html#destroy-a-top-level-traversable
void TraversableNavigable::destroy_top_level_traversable()
{
    VERIFY(is_top_level_traversable());

    // 1. Let browsingContext be traversable's active browsing context.
    auto browsing_context = active_browsing_context();

    // 2. For each historyEntry in traversable's session history entries:
    for (auto& history_entry : m_session_history_entries) {
        // 1. Let document be historyEntry's document.
        auto document = history_entry->document_state->document();

        // 2. If document is not null, then destroy document.
        if (document)
            document->destroy();
    }

    // 3. Remove browsingContext.
    browsing_context->remove();

    // FIXME: 4. Remove traversable from the user interface (e.g., close or hide its tab in a tabbed browser).

    // 5. Remove traversable from the user agent's top-level traversable set.
    user_agent_top_level_traversable_set().remove(this);
}

// https://html.spec.whatwg.org/multipage/browsing-the-web.html#finalize-a-same-document-navigation
void finalize_a_same_document_navigation(JS::NonnullGCPtr<TraversableNavigable> traversable, JS::NonnullGCPtr<Navigable> target_navigable, JS::NonnullGCPtr<SessionHistoryEntry> target_entry, JS::GCPtr<SessionHistoryEntry> entry_to_replace)
{
    // FIXME: 1. Assert: this is running on traversable's session history traversal queue.

    // 2. If targetNavigable's active session history entry is not targetEntry, then return.
    if (target_navigable->active_session_history_entry() != target_entry) {
        return;
    }

    // 3. Let targetStep be null.
    Optional<int> target_step;

    // 4. Let targetEntries be the result of getting session history entries for targetNavigable.
    auto& target_entries = target_navigable->get_session_history_entries();

    // 5. If entryToReplace is null, then:
    if (!entry_to_replace) {
        // 1. Clear the forward session history of traversable.
        traversable->clear_the_forward_session_history();

        // 2. Set targetStep to traversable's current session history step + 1.
        target_step = traversable->current_session_history_step() + 1;

        // 3. Set targetEntry's step to targetStep.
        target_entry->step = *target_step;

        // 4. Append targetEntry to targetEntries.
        target_entries.append(target_entry);
    } else {
        // 1. Replace entryToReplace with targetEntry in targetEntries.
        *(target_entries.find(*entry_to_replace)) = target_entry;

        // 2. Set targetEntry's step to entryToReplace's step.
        target_entry->step = entry_to_replace->step;

        // 3. Set targetStep to traversable's current session history step.
        target_step = traversable->current_session_history_step();
    }

    // 6. Apply the push/replace history step targetStep to traversable.
    traversable->apply_the_push_or_replace_history_step(*target_step);
}

}
