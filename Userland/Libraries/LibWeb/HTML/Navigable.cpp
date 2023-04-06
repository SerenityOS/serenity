/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Crypto/Crypto.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/DocumentState.h>
#include <LibWeb/HTML/Navigable.h>
#include <LibWeb/HTML/SessionHistoryEntry.h>
#include <LibWeb/HTML/TraversableNavigable.h>
#include <LibWeb/Platform/EventLoopPlugin.h>

namespace Web::HTML {

static HashTable<Navigable*>& all_navigables()
{
    static HashTable<Navigable*> set;
    return set;
}

Navigable::Navigable()
{
    all_navigables().set(this);
}

Navigable::~Navigable()
{
    all_navigables().remove(this);
}

void Navigable::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_parent);
    visitor.visit(m_current_session_history_entry);
    visitor.visit(m_active_session_history_entry);
    visitor.visit(m_container);
}

JS::GCPtr<Navigable> Navigable::navigable_with_active_document(JS::NonnullGCPtr<DOM::Document> document)
{
    for (auto* navigable : all_navigables()) {
        if (navigable->active_document() == document)
            return navigable;
    }
    return nullptr;
}

// https://html.spec.whatwg.org/multipage/document-sequences.html#initialize-the-navigable
ErrorOr<void> Navigable::initialize_navigable(JS::NonnullGCPtr<DocumentState> document_state, JS::GCPtr<Navigable> parent)
{
    static int next_id = 0;
    m_id = TRY(String::number(next_id++));

    // 1. Let entry be a new session history entry, with
    JS::NonnullGCPtr<SessionHistoryEntry> entry = *heap().allocate_without_realm<SessionHistoryEntry>();

    // URL: document's URL
    entry->url = document_state->document()->url();

    // document state: documentState
    entry->document_state = document_state;

    // 2. Set navigable's current session history entry to entry.
    m_current_session_history_entry = entry;

    // 3. Set navigable's active session history entry to entry.
    m_active_session_history_entry = entry;

    // 4. Set navigable's parent to parent.
    m_parent = parent;

    return {};
}

// https://html.spec.whatwg.org/multipage/document-sequences.html#nav-document
JS::GCPtr<DOM::Document> Navigable::active_document()
{
    // A navigable's active document is its active session history entry's document.
    return m_active_session_history_entry->document_state->document();
}

// https://html.spec.whatwg.org/multipage/document-sequences.html#nav-bc
JS::GCPtr<BrowsingContext> Navigable::active_browsing_context()
{
    // A navigable's active browsing context is its active document's browsing context.
    // If this navigable is a traversable navigable, then its active browsing context will be a top-level browsing context.
    if (auto document = active_document())
        return document->browsing_context();
    return nullptr;
}

// https://html.spec.whatwg.org/multipage/document-sequences.html#nav-wp
JS::GCPtr<HTML::WindowProxy> Navigable::active_window_proxy()
{
    // A navigable's active WindowProxy is its active browsing context's associated WindowProxy.
    if (auto browsing_context = active_browsing_context())
        return browsing_context->window_proxy();
    return nullptr;
}

// https://html.spec.whatwg.org/multipage/document-sequences.html#nav-window
JS::GCPtr<HTML::Window> Navigable::active_window()
{
    // A navigable's active window is its active WindowProxy's [[Window]].
    if (auto window_proxy = active_window_proxy())
        return window_proxy->window();
    return nullptr;
}

// https://html.spec.whatwg.org/multipage/document-sequences.html#nav-target
String Navigable::target_name() const
{
    // FIXME: A navigable's target name is its active session history entry's document state's navigable target name.
    dbgln("FIXME: Implement Navigable::target_name()");
    return {};
}

// https://html.spec.whatwg.org/multipage/document-sequences.html#nav-container
JS::GCPtr<NavigableContainer> Navigable::container() const
{
    // The container of a navigable navigable is the navigable container whose nested navigable is navigable, or null if there is no such element.
    return m_container;
}

void Navigable::set_container(JS::GCPtr<NavigableContainer> container)
{
    m_container = container;
}

// https://html.spec.whatwg.org/multipage/document-sequences.html#nav-traversable
JS::GCPtr<TraversableNavigable> Navigable::traversable_navigable() const
{
    // 1. Let navigable be inputNavigable.
    auto navigable = const_cast<Navigable*>(this);

    // 2. While navigable is not a traversable navigable, set navigable to navigable's parent.
    while (navigable && !is<TraversableNavigable>(*navigable))
        navigable = navigable->parent();

    // 3. Return navigable.
    return static_cast<TraversableNavigable*>(navigable);
}

// https://html.spec.whatwg.org/multipage/document-sequences.html#nav-top
JS::GCPtr<TraversableNavigable> Navigable::top_level_traversable()
{
    // 1. Let navigable be inputNavigable.
    auto navigable = this;

    // 2. While navigable's parent is not null, set navigable to navigable's parent.
    while (navigable->parent())
        navigable = navigable->parent();

    // 3. Return navigable.
    return verify_cast<TraversableNavigable>(navigable);
}

// https://html.spec.whatwg.org/multipage/browsing-the-web.html#getting-session-history-entries
Vector<JS::NonnullGCPtr<SessionHistoryEntry>>& Navigable::get_session_history_entries() const
{
    // 1. Let traversable be navigable's traversable navigable.
    auto traversable = traversable_navigable();

    // FIXME 2. Assert: this is running within traversable's session history traversal queue.

    // 3. If navigable is traversable, return traversable's session history entries.
    if (this == traversable)
        return traversable->session_history_entries();

    // 4. Let docStates be an empty ordered set of document states.
    Vector<JS::GCPtr<DocumentState>> doc_states;

    // 5. For each entry of traversable's session history entries, append entry's document state to docStates.
    for (auto& entry : traversable->session_history_entries())
        doc_states.append(entry->document_state);

    // 6. For each docState of docStates:
    while (!doc_states.is_empty()) {
        auto doc_state = doc_states.take_first();

        // 1. For each nestedHistory of docState's nested histories:
        for (auto& nested_history : doc_state->nested_histories()) {
            // 1. If nestedHistory's id equals navigable's id, return nestedHistory's entries.
            if (nested_history.id == id())
                return nested_history.entries;

            // 2. For each entry of nestedHistory's entries, append entry's document state to docStates.
            for (auto& entry : nested_history.entries)
                doc_states.append(entry->document_state);
        }
    }

    VERIFY_NOT_REACHED();
}

// To navigate a navigable navigable to a URL url using a Document sourceDocument,
// with an optional POST resource, string, or null documentResource (default null),
// an optional response-or-null response (default null), an optional boolean exceptionsEnabled (default false),
// an optional history handling behavior historyHandling (default "push"),
// an optional string cspNavigationType (default "other"),
// and an optional referrer policy referrerPolicy (default the empty string):

// https://html.spec.whatwg.org/multipage/browsing-the-web.html#navigate
WebIDL::ExceptionOr<void> Navigable::navigate(
    AK::URL const& url,
    JS::NonnullGCPtr<DOM::Document> source_document,
    Variant<Empty, String, POSTResource> document_resource,
    JS::GCPtr<Fetch::Infrastructure::Response> response,
    bool exceptions_enabled,
    HistoryHandlingBehavior history_handling,
    String csp_navigation_type,
    ReferrerPolicy::ReferrerPolicy referrer_policy)
{
    // FIXME: 1. Let sourceSnapshotParams be the result of snapshotting source snapshot params given sourceDocument.

    // 2. Let initiatorOriginSnapshot be sourceDocument's origin.
    auto initiator_origin_snapshot = source_document->origin();

    // FIXME: 3. If sourceDocument's node navigable is not allowed by sandboxing to navigate navigable given and sourceSnapshotParams, then:
    if constexpr (false) {
        // 1. If exceptionsEnabled is true, then throw a "SecurityError" DOMException.
        if (exceptions_enabled) {
            return WebIDL::SecurityError::create(*vm().current_realm(), "Source document's node navigable is not allowed to navigate"sv);
        }

        // 2 Return.
        return {};
    }

    // 4. Let navigationId be the result of generating a random UUID.
    String navigation_id = TRY_OR_THROW_OOM(vm(), Crypto::generate_random_uuid());

    // FIXME: 5. If the surrounding agent is equal to navigable's active document's relevant agent, then continue these steps.
    //           Otherwise, queue a global task on the navigation and traversal task source given navigable's active window to continue these steps.

    // FIXME: 6. If navigable's active document's unload counter is greater than 0,
    //           then invoke WebDriver BiDi navigation failed with a WebDriver BiDi navigation status whose id is navigationId,
    //           status is "canceled", and url is url, and return.

    // 7. If any of the following are true:
    //    - url equals navigable's active document's URL;
    //    - url's scheme is "javascript"; or
    //    - navigable's active document's is initial about:blank is true
    if (url.equals(active_document()->url())
        || url.scheme() == "javascript"sv
        || active_document()->is_initial_about_blank()) {
        // then set historyHandling to "replace".
        history_handling = HistoryHandlingBehavior::Replace;
    }

    // 8. If all of the following are true:
    //    - documentResource is null;
    //    - response is null;
    //    - url equals navigable's active session history entry's URL with exclude fragments set to true; and
    //    - url's fragment is non-null
    if (document_resource.has<Empty>()
        && !response
        && url.equals(active_session_history_entry()->url, AK::URL::ExcludeFragment::Yes)
        && !url.fragment().is_null()) {
        // 1. Navigate to a fragment given navigable, url, historyHandling, and navigationId.
        TRY(navigate_to_a_fragment(url, history_handling, navigation_id));

        // 2. Return.
        return {};
    }

    // 9. If navigable's parent is non-null, then set navigable's is delaying load events to true.
    if (parent() != nullptr) {
        set_delaying_load_events(true);
    }

    // 10. Let targetBrowsingContext be navigable's active browsing context.
    [[maybe_unused]] auto target_browsing_context = active_browsing_context();

    // FIXME: 11. Let targetSnapshotParams be the result of snapshotting target snapshot params given navigable.

    // FIXME: 12. Invoke WebDriver BiDi navigation started with targetBrowsingContext, and a new WebDriver BiDi navigation status whose id is navigationId, url is url, and status is "pending".

    // 13. If navigable's ongoing navigation is "traversal", then:
    if (ongoing_navigation().has<Traversal>()) {
        // FIXME: 1. Invoke WebDriver BiDi navigation failed with targetBrowsingContext and a new WebDriver BiDi navigation status whose id is navigationId, status is "canceled", and url is url.

        // 2. Return.
        return {};
    }

    // 14. Set navigable's ongoing navigation to navigationId.
    m_ongoing_navigation = navigation_id;

    // 15. If url's scheme is "javascript", then:
    if (url.scheme() == "javascript"sv) {
        // 1. Queue a global task on the navigation and traversal task source given navigable's active window to navigate to a javascript: URL given navigable, url, historyHandling, initiatorOriginSnapshot, and cspNavigationType.
        queue_global_task(Task::Source::NavigationAndTraversal, *active_window(), [this, url, history_handling, initiator_origin_snapshot, csp_navigation_type] {
            (void)navigate_to_a_javascript_url(url, history_handling, initiator_origin_snapshot, csp_navigation_type);
        });

        // 2. Return.
        return {};
    }

    // 16. In parallel, run these steps:
    Platform::EventLoopPlugin::the().deferred_invoke([this, document_resource, url, navigation_id, referrer_policy, initiator_origin_snapshot, response] {
        // FIXME: 1. Let unloadPromptCanceled be the result of checking if unloading is user-canceled for navigable's active document's inclusive descendant navigables.

        // FIXME: 2. If unloadPromptCanceled is true, or navigable's ongoing navigation is no longer navigationId, then:

        // 3. Queue a global task on the navigation and traversal task source given navigable's active window to abort navigable's active document.
        queue_global_task(Task::Source::NavigationAndTraversal, *active_window(), [this] {
            VERIFY(active_document());
            active_document()->abort();
        });

        // 4. Let documentState be a new document state with
        //    request referrer policy: referrerPolicy
        //    initiator origin: initiatorOriginSnapshot
        //    FIXME: resource: documentResource
        //    navigable target name: navigable's target name
        JS::NonnullGCPtr<DocumentState> document_state = *heap().allocate_without_realm<DocumentState>();
        document_state->set_request_referrer_policy(referrer_policy);
        document_state->set_initiator_origin(initiator_origin_snapshot);
        document_state->set_navigable_target_name(target_name());

        // 5. If url is about:blank, then set documentState's origin to documentState's initiator origin.
        if (url == "about:blank"sv) {
            document_state->set_origin(document_state->initiator_origin());
        }

        // 6. Otherwise, if url is about:srcdoc, then set documentState's origin to navigable's parent's active document's origin.
        else if (url == "about:srcdoc"sv) {
            document_state->set_origin(parent()->active_document()->origin());
        }

        // 7. Let historyEntry be a new session history entry, with its URL set to url and its document state set to documentState.
        JS::NonnullGCPtr<SessionHistoryEntry> history_entry = *heap().allocate_without_realm<SessionHistoryEntry>();
        history_entry->url = url;
        history_entry->document_state = document_state;

        // FIXME: 8. Let navigationParams be null.

        // FIXME: 9. If response is non-null:
        if (response) {
        }
    });

    return {};
}

WebIDL::ExceptionOr<void> Navigable::navigate_to_a_fragment(AK::URL const&, HistoryHandlingBehavior, String navigation_id)
{
    (void)navigation_id;
    TODO();
}

WebIDL::ExceptionOr<void> Navigable::navigate_to_a_javascript_url(AK::URL const&, HistoryHandlingBehavior, Origin const& initiator_origin, String csp_navigation_type)
{
    (void)initiator_origin;
    (void)csp_navigation_type;
    TODO();
}

}
