/*
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Bindings/PlatformObject.h>
#include <LibWeb/HTML/NavigationType.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#navigationtransition
class NavigationTransition : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(NavigationTransition, Bindings::PlatformObject);
    JS_DECLARE_ALLOCATOR(NavigationTransition);

public:
    [[nodiscard]] static JS::NonnullGCPtr<NavigationTransition> create(JS::Realm&, Bindings::NavigationType, JS::NonnullGCPtr<NavigationHistoryEntry>, JS::GCPtr<JS::Promise>);

    // https://html.spec.whatwg.org/multipage/nav-history-apis.html#dom-navigationtransition-navigationtype
    Bindings::NavigationType navigation_type() const { return m_navigation_type; }

    // https://html.spec.whatwg.org/multipage/nav-history-apis.html#dom-navigationtransition-from
    JS::NonnullGCPtr<NavigationHistoryEntry> from() const { return m_from_entry; }

    // https://html.spec.whatwg.org/multipage/nav-history-apis.html#dom-navigationtransition-finished
    JS::GCPtr<JS::Promise> finished() const { return m_finished_promise; }

    virtual ~NavigationTransition() override;

private:
    NavigationTransition(JS::Realm&, Bindings::NavigationType, JS::NonnullGCPtr<NavigationHistoryEntry>, JS::GCPtr<JS::Promise>);

    virtual void initialize(JS::Realm&) override;
    virtual void visit_edges(JS::Cell::Visitor&) override;

    // https://html.spec.whatwg.org/multipage/nav-history-apis.html#concept-navigationtransition-navigationtype
    Bindings::NavigationType m_navigation_type;

    // https://html.spec.whatwg.org/multipage/nav-history-apis.html#concept-navigationtransition-from
    JS::NonnullGCPtr<NavigationHistoryEntry> m_from_entry;

    // https://html.spec.whatwg.org/multipage/nav-history-apis.html#concept-navigationtransition-finished
    JS::GCPtr<JS::Promise> m_finished_promise;
};

}
