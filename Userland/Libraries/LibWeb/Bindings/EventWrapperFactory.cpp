/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/CloseEventWrapper.h>
#include <LibWeb/Bindings/EventWrapper.h>
#include <LibWeb/Bindings/EventWrapperFactory.h>
#include <LibWeb/Bindings/MessageEventWrapper.h>
#include <LibWeb/Bindings/MouseEventWrapper.h>

namespace Web {
namespace Bindings {

EventWrapper* wrap(JS::GlobalObject& global_object, DOM::Event& event)
{
    if (is<HTML::MessageEvent>(event))
        return static_cast<MessageEventWrapper*>(wrap_impl(global_object, static_cast<HTML::MessageEvent&>(event)));
    if (is<HTML::CloseEvent>(event))
        return static_cast<CloseEventWrapper*>(wrap_impl(global_object, static_cast<HTML::CloseEvent&>(event)));
    if (is<UIEvents::MouseEvent>(event))
        return static_cast<MouseEventWrapper*>(wrap_impl(global_object, static_cast<UIEvents::MouseEvent&>(event)));
    return static_cast<EventWrapper*>(wrap_impl(global_object, event));
}

}
}
