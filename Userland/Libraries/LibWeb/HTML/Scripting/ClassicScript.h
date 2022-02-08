/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Script.h>
#include <LibWeb/Forward.h>
#include <LibWeb/HTML/Scripting/Script.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/webappapis.html#classic-script
class ClassicScript final
    : public Script
    , public JS::Script::HostDefined {
public:
    virtual ~ClassicScript() override;

    enum class MutedErrors {
        No,
        Yes,
    };
    static NonnullRefPtr<ClassicScript> create(String filename, StringView source, EnvironmentSettingsObject&, AK::URL base_url, MutedErrors = MutedErrors::No);

    JS::Script* script_record() { return m_script_record; }
    JS::Script const* script_record() const { return m_script_record; }

    EnvironmentSettingsObject& settings_object() { return m_settings_object; }

    enum class RethrowErrors {
        No,
        Yes,
    };
    JS::Completion run(RethrowErrors = RethrowErrors::No);

    MutedErrors muted_errors() const { return m_muted_errors; }

private:
    ClassicScript(AK::URL base_url, String filename, EnvironmentSettingsObject& environment_settings_object);

    EnvironmentSettingsObject& m_settings_object;
    RefPtr<JS::Script> m_script_record;
    MutedErrors m_muted_errors { MutedErrors::No };
    Optional<JS::Parser::Error> m_error_to_rethrow;
};

}
