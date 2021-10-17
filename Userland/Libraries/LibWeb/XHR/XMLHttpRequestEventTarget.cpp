/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/EventHandler.h>
#include <LibWeb/XHR/EventNames.h>
#include <LibWeb/XHR/XMLHttpRequestEventTarget.h>

namespace Web::XHR {

#undef __ENUMERATE
#define __ENUMERATE(attribute_name, event_name)                                                  \
    void XMLHttpRequestEventTarget::set_##attribute_name(Optional<Bindings::CallbackType> value) \
    {                                                                                            \
        set_event_handler_attribute(event_name, move(value));                                    \
    }                                                                                            \
    Bindings::CallbackType* XMLHttpRequestEventTarget::attribute_name()                          \
    {                                                                                            \
        return event_handler_attribute(event_name);                                              \
    }
ENUMERATE_XML_HTTP_REQUEST_EVENT_TARGET_EVENT_HANDLERS(__ENUMERATE)
#undef __ENUMERATE

}
