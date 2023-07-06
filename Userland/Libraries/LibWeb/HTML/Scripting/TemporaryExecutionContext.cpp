/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/Scripting/Environments.h>
#include <LibWeb/HTML/Scripting/TemporaryExecutionContext.h>

namespace Web::HTML {

TemporaryExecutionContext::TemporaryExecutionContext(EnvironmentSettingsObject& environment_settings)
    : m_environment_settings(environment_settings)
{
    m_environment_settings.prepare_to_run_script();
}

TemporaryExecutionContext::~TemporaryExecutionContext()
{
    m_environment_settings.clean_up_after_running_script();
}

}
