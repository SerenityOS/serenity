/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/IntersectionObserver/IntersectionObserver.h>

namespace Web::IntersectionObserver {

// https://w3c.github.io/IntersectionObserver/#dom-intersectionobserver-intersectionobserver
WebIDL::ExceptionOr<JS::NonnullGCPtr<IntersectionObserver>> IntersectionObserver::construct_impl(JS::Realm& realm, WebIDL::CallbackType* callback, IntersectionObserverInit const& options)
{
    // FIXME: Implement
    (void)callback;
    (void)options;

    return MUST_OR_THROW_OOM(realm.heap().allocate<IntersectionObserver>(realm, realm));
}

IntersectionObserver::IntersectionObserver(JS::Realm& realm)
    : PlatformObject(realm)
{
}

IntersectionObserver::~IntersectionObserver() = default;

JS::ThrowCompletionOr<void> IntersectionObserver::initialize(JS::Realm& realm)
{
    MUST_OR_THROW_OOM(Base::initialize(realm));
    set_prototype(&Bindings::ensure_web_prototype<Bindings::IntersectionObserverPrototype>(realm, "IntersectionObserver"));

    return {};
}

// https://w3c.github.io/IntersectionObserver/#dom-intersectionobserver-observe
void IntersectionObserver::observe(DOM::Element& target)
{
    // FIXME: Implement
    (void)target;
}

// https://w3c.github.io/IntersectionObserver/#dom-intersectionobserver-unobserve
void IntersectionObserver::unobserve(DOM::Element& target)
{
    // FIXME: Implement
    (void)target;
}

// https://w3c.github.io/IntersectionObserver/#dom-intersectionobserver-disconnect
void IntersectionObserver::disconnect()
{
    // FIXME: Implement
}

}
