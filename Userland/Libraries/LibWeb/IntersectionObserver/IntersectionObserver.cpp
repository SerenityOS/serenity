/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Element.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/IntersectionObserver/IntersectionObserver.h>

namespace Web::IntersectionObserver {

// https://w3c.github.io/IntersectionObserver/#dom-intersectionobserver-intersectionobserver
JS::NonnullGCPtr<IntersectionObserver> IntersectionObserver::create_with_global_object(HTML::Window& window, WebIDL::CallbackType* callback, IntersectionObserverInit const& options)
{
    // FIXME: Implement
    (void)callback;
    (void)options;

    return *window.heap().allocate<IntersectionObserver>(window.realm(), window);
}

IntersectionObserver::IntersectionObserver(HTML::Window& window)
    : PlatformObject(window.realm())
{
    set_prototype(&window.cached_web_prototype("IntersectionObserver"));
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
