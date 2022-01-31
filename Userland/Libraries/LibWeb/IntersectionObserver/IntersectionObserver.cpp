/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Element.h>
#include <LibWeb/IntersectionObserver/IntersectionObserver.h>

namespace Web::IntersectionObserver {

// https://w3c.github.io/IntersectionObserver/#dom-intersectionobserver-intersectionobserver
NonnullRefPtr<IntersectionObserver> IntersectionObserver::create_with_global_object(JS::GlobalObject& global_object, JS::Value callback, IntersectionObserverInit const& options)
{
    // FIXME: Implement
    (void)global_object;
    (void)callback;
    (void)options;

    return adopt_ref(*new IntersectionObserver);
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
