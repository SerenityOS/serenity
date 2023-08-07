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
WebIDL::ExceptionOr<JS::NonnullGCPtr<ResizeObserver>> ResizeObserver::construct_impl(JS::Realm& realm, WebIDL::CallbackType* callback)
{
    // FIXME: Implement
    (void)callback;
    return MUST_OR_THROW_OOM(realm.heap().allocate<ResizeObserver>(realm, realm));
}

ResizeObserver::ResizeObserver(JS::Realm& realm)
    : PlatformObject(realm)
{
}

ResizeObserver::~ResizeObserver() = default;

void ResizeObserver::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::ResizeObserverPrototype>(realm, "ResizeObserver"));
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
