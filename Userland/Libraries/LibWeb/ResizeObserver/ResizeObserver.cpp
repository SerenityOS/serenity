/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Element.h>
#include <LibWeb/ResizeObserver/ResizeObserver.h>

namespace Web::ResizeObserver {

// https://drafts.csswg.org/resize-observer/#dom-resizeobserver-resizeobserver
NonnullRefPtr<ResizeObserver> ResizeObserver::create_with_global_object(JS::GlobalObject& global_object, JS::Value callback)
{
    // FIXME: Implement
    (void)global_object;
    (void)callback;
    return adopt_ref(*new ResizeObserver);
}

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
