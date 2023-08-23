/*
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Heap/Heap.h>
#include <LibJS/Runtime/Promise.h>
#include <LibJS/Runtime/Realm.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Bindings/NavigationTransitionPrototype.h>
#include <LibWeb/HTML/NavigationHistoryEntry.h>
#include <LibWeb/HTML/NavigationTransition.h>

namespace Web::HTML {

JS::NonnullGCPtr<NavigationTransition> NavigationTransition::create(JS::Realm& realm, Bindings::NavigationType navigation_type, JS::NonnullGCPtr<NavigationHistoryEntry> from_entry, JS::GCPtr<JS::Promise> finished_promise)
{
    return realm.heap().allocate<NavigationTransition>(realm, realm, navigation_type, from_entry, finished_promise);
}

NavigationTransition::NavigationTransition(JS::Realm& realm, Bindings::NavigationType navigation_type, JS::NonnullGCPtr<NavigationHistoryEntry> from_entry, JS::GCPtr<JS::Promise> finished_promise)
    : Bindings::PlatformObject(realm)
    , m_navigation_type(navigation_type)
    , m_from_entry(from_entry)
    , m_finished_promise(finished_promise)
{
}

NavigationTransition::~NavigationTransition() = default;

void NavigationTransition::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::NavigationTransitionPrototype>(realm, "NavigationTransition"));
}

void NavigationTransition::visit_edges(JS::Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_from_entry);
    visitor.visit(m_finished_promise);
}

}
