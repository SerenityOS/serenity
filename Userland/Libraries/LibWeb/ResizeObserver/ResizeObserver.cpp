/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Element.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/ResizeObserver/ResizeObserver.h>

namespace Web::ResizeObserver {

// https://drafts.csswg.org/resize-observer/#dom-resizeobserver-resizeobserver
JS::NonnullGCPtr<ResizeObserver> ResizeObserver::create_with_global_object(HTML::Window& window, WebIDL::CallbackType* callback)
{
    // FIXME: Implement
    (void)callback;
    return *window.heap().allocate<ResizeObserver>(window.realm(), window);
}

ResizeObserver::ResizeObserver(HTML::Window& window)
    : PlatformObject(window.realm())
{
    set_prototype(&window.cached_web_prototype("ResizeObserver"));
}

ResizeObserver::~ResizeObserver() = default;

// https://drafts.csswg.org/resize-observer/#dom-resizeobserver-observe
void ResizeObserver::observe(DOM::Element& target, ResizeObserverOptions options)
{
    // FIXME: Implement
    (void)target;
    (void)options;
}

// https://drafts.csswg.org/resize-observer/#dom-resizeobserver-unobserve
void ResizeObserver::unobserve(DOM::Element& target)
{
    // FIXME: Implement
    (void)target;
}

// https://drafts.csswg.org/resize-observer/#dom-resizeobserver-disconnect
void ResizeObserver::disconnect()
{
    // FIXME: Implement
}

}
