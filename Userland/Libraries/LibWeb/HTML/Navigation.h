/*
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Promise.h>
#include <LibWeb/Bindings/NavigationPrototype.h>
#include <LibWeb/DOM/EventTarget.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#navigationupdatecurrententryoptions
struct NavigationUpdateCurrentEntryOptions {
    JS::Value state;
};

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#navigationoptions
struct NavigationOptions {
    JS::Value info;
};

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#navigationnavigateoptions
struct NavigationNavigateOptions : public NavigationOptions {
    JS::Value state;
    Bindings::NavigationHistoryBehavior history = Bindings::NavigationHistoryBehavior::Auto;
};

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#navigationreloadoptions
struct NavigationReloadOptions : public NavigationOptions {
    JS::Value state;
};

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#navigationresult
struct NavigationResult {
    JS::NonnullGCPtr<JS::Promise> committed;
    JS::NonnullGCPtr<JS::Promise> finished;
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
    bool can_go_back() const;
    bool can_go_forward() const;

    // https://html.spec.whatwg.org/multipage/nav-history-apis.html#dom-navigation-transition
    JS::GCPtr<NavigationTransition> transition() const { return m_transition; }

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

    virtual ~Navigation() override;

private:
    explicit Navigation(JS::Realm&);

    virtual void initialize(JS::Realm&) override;
    virtual void visit_edges(Visitor&) override;

    // https://html.spec.whatwg.org/multipage/nav-history-apis.html#navigation-entry-list
    // Each Navigation has an associated entry list, a list of NavigationHistoryEntry objects, initially empty.
    Vector<JS::NonnullGCPtr<NavigationHistoryEntry>> m_entry_list;

    // https://html.spec.whatwg.org/multipage/nav-history-apis.html#navigation-current-entry-index
    // Each Navigation has an associated current entry index, an integer, initially −1.
    i64 m_current_entry_index { -1 };

    // https://html.spec.whatwg.org/multipage/nav-history-apis.html#concept-navigation-transition
    // Each Navigation has a transition, which is a NavigationTransition or null, initially null.
    JS::GCPtr<NavigationTransition> m_transition { nullptr };
};

}
