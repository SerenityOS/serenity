/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/DOMEventListener.h>
#include <LibWeb/HTML/EventHandler.h>

namespace Web::HTML {

EventHandler::EventHandler(String s)
    : value(move(s))
{
}

EventHandler::EventHandler(WebIDL::CallbackType& c)
    : value(&c)
{
}

void EventHandler::visit_edges(Cell::Visitor& visitor)
{
    Cell::visit_edges(visitor);
    visitor.visit(listener);

    if (auto* callback = value.get_pointer<WebIDL::CallbackType*>())
        visitor.visit(*callback);
}

}
