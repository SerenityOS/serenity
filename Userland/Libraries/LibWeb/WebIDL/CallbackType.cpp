/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Object.h>
#include <LibWeb/WebIDL/CallbackType.h>

namespace Web::WebIDL {

CallbackType::CallbackType(JS::Object& callback, HTML::EnvironmentSettingsObject& callback_context)
    : callback(callback)
    , callback_context(callback_context)
{
}

StringView CallbackType::class_name() const { return "CallbackType"sv; }
void CallbackType::visit_edges(Cell::Visitor& visitor)
{
    Cell::visit_edges(visitor);
    visitor.visit(&callback);
}

}
