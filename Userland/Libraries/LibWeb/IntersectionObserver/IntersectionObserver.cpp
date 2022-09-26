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
JS::NonnullGCPtr<IntersectionObserver> IntersectionObserver::construct_impl(JS::Realm& realm, WebIDL::CallbackType* callback, IntersectionObserverInit const& options)
{
    // FIXME: Implement
    (void)callback;
    (void)options;

    return *realm.heap().allocate<IntersectionObserver>(realm, realm);
}

IntersectionObserver::IntersectionObserver(JS::Realm& realm)
    : PlatformObject(realm)
{
    set_prototype(&Bindings::cached_web_prototype(realm, "IntersectionObserver"));
}

IntersectionObserver::~IntersectionObserver() = default;

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
