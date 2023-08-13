/*
 * Copyright (c) 2022, Ben Abraham <ben.d.abraham@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/URL.h>
#include <LibWeb/Bindings/DedicatedWorkerExposedInterfaces.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/Forward.h>

namespace Web::HTML {

class WorkerEnvironmentSettingsObject final
    : public EnvironmentSettingsObject {
    JS_CELL(WindowEnvironmentSettingsObject, EnvironmentSettingsObject);

public:
    WorkerEnvironmentSettingsObject(NonnullOwnPtr<JS::ExecutionContext> execution_context)
        : EnvironmentSettingsObject(move(execution_context))
    {
    }

    static JS::NonnullGCPtr<WorkerEnvironmentSettingsObject> setup(NonnullOwnPtr<JS::ExecutionContext> execution_context /* FIXME: null or an environment reservedEnvironment, a URL topLevelCreationURL, and an origin topLevelOrigin */)
    {
        auto realm = execution_context->realm;
        VERIFY(realm);
        auto settings_object = realm->heap().allocate<WorkerEnvironmentSettingsObject>(*realm, move(execution_context));
        settings_object->target_browsing_context = nullptr;

        auto intrinsics = realm->heap().allocate<Bindings::Intrinsics>(*realm, *realm);
        auto host_defined = make<Bindings::HostDefined>(settings_object, intrinsics);
        realm->set_host_defined(move(host_defined));

        // FIXME: Shared workers should use the shared worker method
        Bindings::add_dedicated_worker_exposed_interfaces(realm->global_object());

        return settings_object;
    }

    virtual ~WorkerEnvironmentSettingsObject() override = default;

    JS::GCPtr<DOM::Document> responsible_document() override { return nullptr; }
    DeprecatedString api_url_character_encoding() override { return m_api_url_character_encoding; }
    AK::URL api_base_url() override { return m_url; }
    Origin origin() override { return m_origin; }
    PolicyContainer policy_container() override { return m_policy_container; }
    CanUseCrossOriginIsolatedAPIs cross_origin_isolated_capability() override { TODO(); }

private:
    DeprecatedString m_api_url_character_encoding;
    AK::URL m_url;
    HTML::Origin m_origin;
    HTML::PolicyContainer m_policy_container;
};

}
