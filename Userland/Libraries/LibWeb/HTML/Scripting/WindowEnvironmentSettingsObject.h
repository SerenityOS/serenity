/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/Window.h>
#include <LibWeb/HTML/Scripting/Environments.h>

namespace Web::HTML {

class WindowEnvironmentSettingsObject final : public EnvironmentSettingsObject {
public:
    static void setup(AK::URL& creation_url, JS::ExecutionContext& execution_context /* FIXME: null or an environment reservedEnvironment, a URL topLevelCreationURL, and an origin topLevelOrigin */);

    virtual ~WindowEnvironmentSettingsObject() override = default;

    virtual RefPtr<DOM::Document> responsible_document() override;
    virtual String api_url_character_encoding() override;
    virtual AK::URL api_base_url() override;
    virtual Origin origin() override;
    virtual CanUseCrossOriginIsolatedAPIs cross_origin_isolated_capability() override;

private:
    WindowEnvironmentSettingsObject(DOM::Window&, JS::ExecutionContext& execution_context);

    NonnullRefPtr<DOM::Window> m_window;
};

}
