/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/URL.h>
#include <LibJS/Heap/Cell.h>
#include <LibWeb/Forward.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/webappapis.html#concept-script
class Script : public JS::Cell {
    JS_CELL(Script, JS::Cell);

public:
    virtual ~Script() override;

    AK::URL const& base_url() const { return m_base_url; }
    String const& filename() const { return m_filename; }

    EnvironmentSettingsObject& settings_object() { return m_settings_object; }

protected:
    Script(AK::URL base_url, String filename, EnvironmentSettingsObject& environment_settings_object);

private:
    AK::URL m_base_url;
    String m_filename;
    EnvironmentSettingsObject& m_settings_object;
};

}
