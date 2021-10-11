/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

namespace Web::Bindings {

EventWrapper* wrap(JS::GlobalObject& global_object, DOM::Event& event)
{
    if (is<DOM::CustomEvent>(event))
        return static_cast<CustomEventWrapper*>(wrap_impl(global_object, static_cast<DOM::CustomEvent&>(event)));
    if (is<CSS::MediaQueryListEvent>(event))
        return static_cast<MediaQueryListEventWrapper*>(wrap_impl(global_object, static_cast<CSS::MediaQueryListEvent&>(event)));
    if (is<HTML::CloseEvent>(event))
        return static_cast<CloseEventWrapper*>(wrap_impl(global_object, static_cast<HTML::CloseEvent&>(event)));
    if (is<HTML::MessageEvent>(event))
        return static_cast<MessageEventWrapper*>(wrap_impl(global_object, static_cast<HTML::MessageEvent&>(event)));
    if (is<HTML::PageTransitionEvent>(event))
        return static_cast<PageTransitionEventWrapper*>(wrap_impl(global_object, static_cast<HTML::PageTransitionEvent&>(event)));
    if (is<HTML::PromiseRejectionEvent>(event))
        return static_cast<PromiseRejectionEventWrapper*>(wrap_impl(global_object, static_cast<HTML::PromiseRejectionEvent&>(event)));
    if (is<HTML::SubmitEvent>(event))
        return static_cast<SubmitEventWrapper*>(wrap_impl(global_object, static_cast<HTML::SubmitEvent&>(event)));
    if (is<UIEvents::KeyboardEvent>(event))
        return static_cast<KeyboardEventWrapper*>(wrap_impl(global_object, static_cast<UIEvents::KeyboardEvent&>(event)));
    if (is<UIEvents::MouseEvent>(event))
        return static_cast<MouseEventWrapper*>(wrap_impl(global_object, static_cast<UIEvents::MouseEvent&>(event)));
    if (is<XHR::ProgressEvent>(event))
        return static_cast<ProgressEventWrapper*>(wrap_impl(global_object, static_cast<XHR::ProgressEvent&>(event)));
    return static_cast<EventWrapper*>(wrap_impl(global_object, event));
}

}
