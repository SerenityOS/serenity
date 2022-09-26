/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/ResizeObserver/ResizeObserver.h>

namespace Web::ResizeObserver {

// https://drafts.csswg.org/resize-observer/#dom-resizeobserver-resizeobserver
JS::NonnullGCPtr<ResizeObserver> ResizeObserver::construct_impl(JS::Realm& realm, WebIDL::CallbackType* callback)
{
    // FIXME: Implement
    (void)callback;
    return *realm.heap().allocate<ResizeObserver>(realm, realm);
}

ResizeObserver::ResizeObserver(JS::Realm& realm)
    : PlatformObject(realm)
{
    set_prototype(&Bindings::cached_web_prototype(realm, "ResizeObserver"));
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
