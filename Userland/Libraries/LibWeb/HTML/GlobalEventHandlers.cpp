/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/EventTarget.h>
#include <LibWeb/HTML/EventNames.h>
#include <LibWeb/HTML/GlobalEventHandlers.h>
#include <LibWeb/UIEvents/EventNames.h>

namespace Web::HTML {

#undef __ENUMERATE
#define __ENUMERATE(attribute_name, event_name)                                                           \
    void GlobalEventHandlers::set_##attribute_name(WebIDL::CallbackType* value)                           \
    {                                                                                                     \
        global_event_handlers_to_event_target(event_name).set_event_handler_attribute(event_name, value); \
    }                                                                                                     \
    WebIDL::CallbackType* GlobalEventHandlers::attribute_name()                                           \
    {                                                                                                     \
        return global_event_handlers_to_event_target(event_name).event_handler_attribute(event_name);     \
    }
ENUMERATE_GLOBAL_EVENT_HANDLERS(__ENUMERATE)
#undef __ENUMERATE

GlobalEventHandlers::~GlobalEventHandlers() = default;
}
