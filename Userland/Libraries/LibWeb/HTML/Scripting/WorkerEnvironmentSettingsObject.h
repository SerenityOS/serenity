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

// FIXME: This is a bit ugly, this implementation is basically a 1:1 copy of what is in ESO
//        just modified to use DOM::Document instead of DOM::Window since workers have no window
class WorkerEnvironmentSettingsObject final
    : public EnvironmentSettingsObject
    , public Weakable<WorkerEnvironmentSettingsObject> {
public:
    WorkerEnvironmentSettingsObject(DOM::Document& document, JS::ExecutionContext& execution_context)
        : EnvironmentSettingsObject(execution_context)
        , m_document(document)
    {
    }

    static WeakPtr<WorkerEnvironmentSettingsObject> setup(DOM::Document& document, JS::ExecutionContext& execution_context /* FIXME: null or an environment reservedEnvironment, a URL topLevelCreationURL, and an origin topLevelOrigin */)
    {
        auto* realm = execution_context.realm;
        VERIFY(realm);
        auto settings_object = adopt_own(*new WorkerEnvironmentSettingsObject(document, execution_context));
        settings_object->target_browsing_context = nullptr;
        realm->set_host_defined(move(settings_object));

        return static_cast<WorkerEnvironmentSettingsObject*>(realm->host_defined());
    }

    virtual ~WorkerEnvironmentSettingsObject() override = default;

    RefPtr<DOM::Document> responsible_document() override { return m_document; }
    String api_url_character_encoding() override { return m_document->encoding_or_default(); }
    AK::URL api_base_url() override { return m_document->url(); }
    Origin origin() override { return m_document->origin(); }
    CanUseCrossOriginIsolatedAPIs cross_origin_isolated_capability() override { TODO(); }

private:
    NonnullRefPtr<DOM::Document> m_document;
};

}
