/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Interpreter.h>
#include <LibJS/Parser.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/EventListener.h>
#include <LibWeb/HTML/EventHandler.h>
#include <LibWeb/HTML/EventNames.h>
#include <LibWeb/HTML/GlobalEventHandlers.h>
#include <LibWeb/UIEvents/EventNames.h>

namespace Web::HTML {

#undef __ENUMERATE
#define __ENUMERATE(attribute_name, event_name)                                                       \
    void GlobalEventHandlers::set_##attribute_name(HTML::EventHandler value)                          \
    {                                                                                                 \
        global_event_handlers_to_event_target().set_event_handler_attribute(event_name, move(value)); \
    }                                                                                                 \
    HTML::EventHandler GlobalEventHandlers::attribute_name()                                          \
    {                                                                                                 \
        return global_event_handlers_to_event_target().event_handler_attribute(event_name);           \
    }
ENUMERATE_GLOBAL_EVENT_HANDLERS(__ENUMERATE)
#undef __ENUMERATE

GlobalEventHandlers::~GlobalEventHandlers()
{
}

}
