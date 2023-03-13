/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/URL.h>
#include <LibJS/Heap/Cell.h>
#include <LibJS/Script.h>
#include <LibWeb/Forward.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/webappapis.html#concept-script
class Script
    : public JS::Cell
    , public JS::Script::HostDefined {
    JS_CELL(Script, JS::Cell);

public:
    virtual ~Script() override;

    AK::URL const& base_url() const { return m_base_url; }
    DeprecatedString const& filename() const { return m_filename; }

    EnvironmentSettingsObject& settings_object() { return m_settings_object; }

protected:
    Script(AK::URL base_url, DeprecatedString filename, EnvironmentSettingsObject& environment_settings_object);

private:
    virtual void visit_host_defined_self(JS::Cell::Visitor&) override;

    AK::URL m_base_url;
    DeprecatedString m_filename;
    JS::NonnullGCPtr<EnvironmentSettingsObject> m_settings_object;
};

}
