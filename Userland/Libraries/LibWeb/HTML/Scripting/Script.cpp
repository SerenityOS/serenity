/*
 * Copyright (c) 2021-2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/Scripting/Environments.h>
#include <LibWeb/HTML/Scripting/Script.h>

namespace Web::HTML {

JS_DEFINE_ALLOCATOR(Script);

Script::Script(URL base_url, ByteString filename, EnvironmentSettingsObject& environment_settings_object)
    : m_base_url(move(base_url))
    , m_filename(move(filename))
    , m_settings_object(environment_settings_object)
{
}

Script::~Script() = default;

void Script::visit_host_defined_self(JS::Cell::Visitor& visitor)
{
    visitor.visit(*this);
}

void Script::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_settings_object);
    visitor.visit(m_parse_error);
    visitor.visit(m_error_to_rethrow);
}

}
