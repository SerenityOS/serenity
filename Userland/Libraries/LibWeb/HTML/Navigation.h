/*
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Promise.h>
#include <LibWeb/Bindings/NavigationPrototype.h>
#include <LibWeb/DOM/EventTarget.h>
#include <LibWeb/HTML/HistoryHandlingBehavior.h>
#include <LibWeb/HTML/StructuredSerialize.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#navigationupdatecurrententryoptions
struct NavigationUpdateCurrentEntryOptions {
    JS::Value state;
};

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#navigationoptions
struct NavigationOptions {
    Optional<JS::Value> info;
};

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#navigationnavigateoptions
struct NavigationNavigateOptions : public NavigationOptions {
    Optional<JS::Value> state;
    Bindings::NavigationHistoryBehavior history = Bindings::NavigationHistoryBehavior::Auto;
};

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#navigationreloadoptions
struct NavigationReloadOptions : public NavigationOptions {
    Optional<JS::Value> state;
};

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#navigationresult
struct NavigationResult {
    // FIXME: Are we supposed to return a PromiseCapability (WebIDL::Promise) here?
    JS::NonnullGCPtr<JS::Object> committed;
    JS::NonnullGCPtr<JS::Object> finished;
};

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#navigation-api-method-tracker
struct NavigationAPIMethodTracker final : public JS::Cell {
    JS_CELL(NavigationAPIMethodTracker, JS::Cell);

    NavigationAPIMethodTracker(JS::NonnullGCPtr<Navigation> navigation,
        Optional<String> key,
        JS::Value info,
        Optional<SerializationRecord> serialized_state,
        JS::GCPtr<NavigationHistoryEntry> commited_to_entry,
        JS::NonnullGCPtr<WebIDL::Promise> committed_promise,
        JS::NonnullGCPtr<WebIDL::Promise> finished_promise);

    virtual void visit_edges(Cell::Visitor&) override;

    JS::NonnullGCPtr<Navigation> navigation;
    Optional<String> key;
    JS::Value info;
    Optional<SerializationRecord> serialized_state;
    JS::GCPtr<NavigationHistoryEntry> commited_to_entry;
    JS::NonnullGCPtr<WebIDL::Promise> committed_promise;
    JS::NonnullGCPtr<WebIDL::Promise> finished_promise;
};

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#navigation-interface
class Navigation : public DOM::EventTarget {
    WEB_PLATFORM_OBJECT(Navigation, DOM::EventTarget);

public:
    [[nodiscard]] static JS::NonnullGCPtr<Navigation> create(JS::Realm&);

    // IDL properties and methods
    Vector<JS::NonnullGCPtr<NavigationHistoryEntry>> entries() const;
    JS::GCPtr<NavigationHistoryEntry> current_entry() const;
    WebIDL::ExceptionOr<void> update_current_entry(NavigationUpdateCurrentEntryOptions);

    // https://html.spec.whatwg.org/multipage/nav-history-apis.html#dom-navigation-transition
    JS::GCPtr<NavigationTransition> transition() const { return m_transition; }

    bool can_go_back() const;
    bool can_go_forward() const;

    WebIDL::ExceptionOr<NavigationResult> navigate(String url, NavigationNavigateOptions const&);
    WebIDL::ExceptionOr<NavigationResult> reload(NavigationReloadOptions const&);

    WebIDL::ExceptionOr<NavigationResult> traverse_to(String key, NavigationOptions const&);
    WebIDL::ExceptionOr<NavigationResult> back(NavigationOptions const&);
    WebIDL::ExceptionOr<NavigationResult> forward(NavigationOptions const&);

    // Event Handlers
    void set_onnavigate(WebIDL::CallbackType*);
    WebIDL::CallbackType* onnavigate();

    void set_onnavigatesuccess(WebIDL::CallbackType*);
    WebIDL::CallbackType* onnavigatesuccess();

    void set_onnavigateerror(WebIDL::CallbackType*);
    WebIDL::CallbackType* onnavigateerror();

    void set_oncurrententrychange(WebIDL::CallbackType*);
    WebIDL::CallbackType* oncurrententrychange();

    // Abstract Operations
    bool has_entries_and_events_disabled() const;
    i64 get_the_navigation_api_entry_index(SessionHistoryEntry const&) const;
    void abort_the_ongoing_navigation(Optional<JS::NonnullGCPtr<WebIDL::DOMException>> error = {});

    virtual ~Navigation() override;

    // Internal Getters
    JS::GCPtr<NavigateEvent> ongoing_navigate_event() const { return m_ongoing_navigate_event; }

private:
    explicit Navigation(JS::Realm&);

    virtual void initialize(JS::Realm&) override;
    virtual void visit_edges(Visitor&) override;

    using AnyException = decltype(declval<WebIDL::ExceptionOr<void>>().exception());
    NavigationResult early_error_result(AnyException);

    JS::NonnullGCPtr<NavigationAPIMethodTracker> maybe_set_the_upcoming_non_traverse_api_method_tracker(JS::Value info, Optional<SerializationRecord>);
    JS::NonnullGCPtr<NavigationAPIMethodTracker> add_an_upcoming_traverse_api_method_tracker(String destination_key, JS::Value info);
    WebIDL::ExceptionOr<NavigationResult> perform_a_navigation_api_traversal(String key, NavigationOptions const&);

    // https://html.spec.whatwg.org/multipage/nav-history-apis.html#navigation-entry-list
    // Each Navigation has an associated entry list, a list of NavigationHistoryEntry objects, initially empty.
    Vector<JS::NonnullGCPtr<NavigationHistoryEntry>> m_entry_list;

    // https://html.spec.whatwg.org/multipage/nav-history-apis.html#navigation-current-entry-index
    // Each Navigation has an associated current entry index, an integer, initially −1.
    i64 m_current_entry_index { -1 };

    // https://html.spec.whatwg.org/multipage/nav-history-apis.html#concept-navigation-transition
    // Each Navigation has a transition, which is a NavigationTransition or null, initially null.
    JS::GCPtr<NavigationTransition> m_transition { nullptr };

    // https://html.spec.whatwg.org/multipage/nav-history-apis.html#ongoing-navigate-event
    JS::GCPtr<NavigateEvent> m_ongoing_navigate_event { nullptr };

    // https://html.spec.whatwg.org/multipage/nav-history-apis.html#focus-changed-during-ongoing-navigation
    bool m_focus_changed_during_ongoing_navigation { false };

    // https://html.spec.whatwg.org/multipage/nav-history-apis.html#suppress-normal-scroll-restoration-during-ongoing-navigation
    bool m_suppress_scroll_restoration_during_ongoing_navigation { false };

    // https://html.spec.whatwg.org/multipage/nav-history-apis.html#ongoing-api-method-tracker
    JS::GCPtr<NavigationAPIMethodTracker> m_ongoing_api_method_tracker = nullptr;

    // https://html.spec.whatwg.org/multipage/nav-history-apis.html#upcoming-non-traverse-api-method-tracker
    JS::GCPtr<NavigationAPIMethodTracker> m_upcoming_non_traverse_api_method_tracker = nullptr;

    // https://html.spec.whatwg.org/multipage/nav-history-apis.html#upcoming-non-traverse-api-method-tracker
    HashMap<String, JS::NonnullGCPtr<NavigationAPIMethodTracker>> m_upcoming_traverse_api_method_trackers;
};

HistoryHandlingBehavior to_history_handling_behavior(Bindings::NavigationHistoryBehavior);

}
