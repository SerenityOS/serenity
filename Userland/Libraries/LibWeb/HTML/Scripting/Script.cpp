/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/Scripting/Script.h>

namespace Web::HTML {

Script::Script(AK::URL base_url, String filename, EnvironmentSettingsObject& environment_settings_object)
    : m_base_url(move(base_url))
    , m_filename(move(filename))
    , m_settings_object(environment_settings_object)
{
}

Script::~Script() = default;

void Script::visit_host_defined_self(JS::Cell::Visitor& visitor)
{
    visitor.visit(this);
}

}
