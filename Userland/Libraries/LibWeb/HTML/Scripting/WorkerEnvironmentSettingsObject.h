/*
 * Copyright (c) 2022, Ben Abraham <ben.d.abraham@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/URL.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/Forward.h>

namespace Web::HTML {

class WorkerEnvironmentSettingsObject final
    : public EnvironmentSettingsObject
    , public Weakable<WorkerEnvironmentSettingsObject> {
public:
    WorkerEnvironmentSettingsObject(NonnullOwnPtr<JS::ExecutionContext> execution_context)
        : EnvironmentSettingsObject(move(execution_context))
    {
    }

    static WeakPtr<WorkerEnvironmentSettingsObject> setup(NonnullOwnPtr<JS::ExecutionContext> execution_context /* FIXME: null or an environment reservedEnvironment, a URL topLevelCreationURL, and an origin topLevelOrigin */)
    {
        auto* realm = execution_context->realm;
        VERIFY(realm);
        auto settings_object = adopt_own(*new WorkerEnvironmentSettingsObject(move(execution_context)));
        settings_object->target_browsing_context = nullptr;
        realm->set_host_defined(move(settings_object));

        return static_cast<WorkerEnvironmentSettingsObject*>(realm->host_defined());
    }

    virtual ~WorkerEnvironmentSettingsObject() override = default;

    JS::GCPtr<DOM::Document> responsible_document() override { return nullptr; }
    String api_url_character_encoding() override { return m_api_url_character_encoding; }
    AK::URL api_base_url() override { return m_url; }
    Origin origin() override { return m_origin; }
    CanUseCrossOriginIsolatedAPIs cross_origin_isolated_capability() override { TODO(); }

private:
    String m_api_url_character_encoding;
    AK::URL m_url;
    HTML::Origin m_origin;
};

}
