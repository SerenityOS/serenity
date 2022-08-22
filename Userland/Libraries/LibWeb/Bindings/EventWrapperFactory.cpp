/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/CloseEventWrapper.h>
#include <LibWeb/Bindings/CustomEventWrapper.h>
#include <LibWeb/Bindings/EventWrapper.h>
#include <LibWeb/Bindings/EventWrapperFactory.h>
#include <LibWeb/Bindings/KeyboardEventWrapper.h>
#include <LibWeb/Bindings/MediaQueryListEventWrapper.h>
#include <LibWeb/Bindings/MessageEventWrapper.h>
#include <LibWeb/Bindings/MouseEventWrapper.h>
#include <LibWeb/Bindings/PageTransitionEventWrapper.h>
#include <LibWeb/Bindings/ProgressEventWrapper.h>
#include <LibWeb/Bindings/PromiseRejectionEventWrapper.h>
#include <LibWeb/Bindings/SubmitEventWrapper.h>
#include <LibWeb/Bindings/WebGLContextEventWrapper.h>

namespace Web::Bindings {

EventWrapper* wrap(JS::Realm& realm, DOM::Event& event)
{
    if (event.wrapper())
        return static_cast<EventWrapper*>(event.wrapper());

    if (is<DOM::CustomEvent>(event))
        return static_cast<CustomEventWrapper*>(wrap_impl(realm, static_cast<DOM::CustomEvent&>(event)));
    if (is<CSS::MediaQueryListEvent>(event))
        return static_cast<MediaQueryListEventWrapper*>(wrap_impl(realm, static_cast<CSS::MediaQueryListEvent&>(event)));
    if (is<HTML::CloseEvent>(event))
        return static_cast<CloseEventWrapper*>(wrap_impl(realm, static_cast<HTML::CloseEvent&>(event)));
    if (is<HTML::MessageEvent>(event))
        return static_cast<MessageEventWrapper*>(wrap_impl(realm, static_cast<HTML::MessageEvent&>(event)));
    if (is<HTML::PageTransitionEvent>(event))
        return static_cast<PageTransitionEventWrapper*>(wrap_impl(realm, static_cast<HTML::PageTransitionEvent&>(event)));
    if (is<HTML::PromiseRejectionEvent>(event))
        return static_cast<PromiseRejectionEventWrapper*>(wrap_impl(realm, static_cast<HTML::PromiseRejectionEvent&>(event)));
    if (is<HTML::SubmitEvent>(event))
        return static_cast<SubmitEventWrapper*>(wrap_impl(realm, static_cast<HTML::SubmitEvent&>(event)));
    if (is<UIEvents::KeyboardEvent>(event))
        return static_cast<KeyboardEventWrapper*>(wrap_impl(realm, static_cast<UIEvents::KeyboardEvent&>(event)));
    if (is<UIEvents::MouseEvent>(event))
        return static_cast<MouseEventWrapper*>(wrap_impl(realm, static_cast<UIEvents::MouseEvent&>(event)));
    if (is<XHR::ProgressEvent>(event))
        return static_cast<ProgressEventWrapper*>(wrap_impl(realm, static_cast<XHR::ProgressEvent&>(event)));
    if (is<UIEvents::UIEvent>(event))
        return static_cast<UIEventWrapper*>(wrap_impl(realm, static_cast<UIEvents::UIEvent&>(event)));
    if (is<WebGL::WebGLContextEvent>(event))
        return static_cast<WebGLContextEventWrapper*>(wrap_impl(realm, static_cast<WebGL::WebGLContextEvent&>(event)));
    return static_cast<EventWrapper*>(wrap_impl(realm, event));
}

}
