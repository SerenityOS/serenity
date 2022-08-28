/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/Scripting/Environments.h>
#include <LibWeb/HTML/Window.h>

namespace Web::HTML {

class WindowEnvironmentSettingsObject final : public EnvironmentSettingsObject {
public:
    static void setup(AK::URL const& creation_url, NonnullOwnPtr<JS::ExecutionContext>, Optional<Environment>, AK::URL top_level_creation_url, Origin top_level_origin);

    virtual ~WindowEnvironmentSettingsObject() override = default;

    virtual JS::GCPtr<DOM::Document> responsible_document() override;
    virtual String api_url_character_encoding() override;
    virtual AK::URL api_base_url() override;
    virtual Origin origin() override;
    virtual CanUseCrossOriginIsolatedAPIs cross_origin_isolated_capability() override;

private:
    WindowEnvironmentSettingsObject(Window&, NonnullOwnPtr<JS::ExecutionContext>);

    JS::Handle<Window> m_window;
};

}
