/*
 * Copyright (c) 2022, Ben Abraham <ben.d.abraham@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/URL.h>
#include <LibWeb/Bindings/DedicatedWorkerExposedInterfaces.h>
#include <LibWeb/Bindings/WorkerGlobalScopeGlobalMixin.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/Forward.h>
#include <LibWeb/HTML/WorkerGlobalScope.h>

namespace Web::HTML {

class WorkerEnvironmentSettingsObject final
    : public EnvironmentSettingsObject {
    JS_CELL(WindowEnvironmentSettingsObject, EnvironmentSettingsObject);

public:
    WorkerEnvironmentSettingsObject(NonnullOwnPtr<JS::ExecutionContext> execution_context)
        : EnvironmentSettingsObject(move(execution_context))
    {
    }

    // https://html.spec.whatwg.org/multipage/workers.html#set-up-a-worker-environment-settings-object
    static JS::NonnullGCPtr<WorkerEnvironmentSettingsObject> setup(NonnullOwnPtr<JS::ExecutionContext> execution_context, EnvironmentSettingsObject& outside_settings /* FIXME: and a number unsafeWorkerCreationTime */)
    {
        // 1. Let inherited origin be outside settings's origin.
        auto inherited_origin = outside_settings.origin();

        // 2. Let realm be the value of execution context's Realm component.
        JS::GCPtr<JS::Realm> realm = execution_context->realm;

        // 3. Let worker global scope be realm's global object.
        VERIFY(is<WorkerGlobalScope>(realm->global_object()));
        auto& worker_global_scope = static_cast<WorkerGlobalScope&>(realm->global_object());

        // 4. Let settings object be a new environment settings object whose algorithms are defined as follows (...)
        auto settings_object = realm->heap().allocate<WorkerEnvironmentSettingsObject>(*realm, move(execution_context)).release_allocated_value_but_fixme_should_propagate_errors();

        // FIXME: This isn't entirely correct, as cross origin isolated capability may
        //        be changed later, e.g in steps 7 & 8 of "run a worker".
        settings_object->m_cross_origin_isolated_capability = worker_global_scope.cross_origin_isolated_capability() ? CanUseCrossOriginIsolatedAPIs::Yes : CanUseCrossOriginIsolatedAPIs::No;

        // 5. Set settings object's id to a new unique opaque string, creation URL to worker global scope's url, top-level creation URL to null, target browsing context to null, and active service worker to null.

        // 6. If worker global scope is a DedicatedWorkerGlobalScope object, then set settings object's top-level origin to outside settings's top-level origin.
        // 7. Otherwise, set settings object's top-level origin to an implementation-defined value.
        // FIXME: Differentiate between DedicatedWorkerGlobalScope and SharedWorkerGlobalScope
        settings_object->top_level_origin = outside_settings.top_level_origin;

        // 8. Set realm's [[HostDefined]] field to settings object.
        // Non-Standard: We store the ESO next to the web intrinsics in a custom HostDefined object
        auto intrinsics = realm->heap().allocate<Bindings::Intrinsics>(*realm, *realm).release_allocated_value_but_fixme_should_propagate_errors();
        auto host_defined = make<Bindings::HostDefined>(settings_object, intrinsics);
        realm->set_host_defined(move(host_defined));

        // Non-Standard: We cannot fully initialize window object until *after* the we set up
        //    the realm's [[HostDefined]] internal slot as the internal slot contains the web platform intrinsics
        worker_global_scope.initialize_web_interfaces({}, *realm).release_value_but_fixme_should_propagate_errors();

        // 9. Return settings object.
        return settings_object;
    }

    virtual ~WorkerEnvironmentSettingsObject() override = default;

    JS::GCPtr<DOM::Document> responsible_document() override { return nullptr; }
    DeprecatedString api_url_character_encoding() override { return m_api_url_character_encoding; }
    AK::URL api_base_url() override { return m_url; }
    Origin origin() override { return m_origin; }
    PolicyContainer policy_container() override { return m_policy_container; }
    CanUseCrossOriginIsolatedAPIs cross_origin_isolated_capability() override { return m_cross_origin_isolated_capability; }

private:
    DeprecatedString m_api_url_character_encoding;
    AK::URL m_url;
    HTML::Origin m_origin;
    HTML::PolicyContainer m_policy_container;
    CanUseCrossOriginIsolatedAPIs m_cross_origin_isolated_capability;
};

}
