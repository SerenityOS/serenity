/*
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/Scripting/WorkerEnvironmentSettingsObject.h>
#include <LibWeb/HTML/WorkerGlobalScope.h>

namespace Web::HTML {

JS_DEFINE_ALLOCATOR(WorkerEnvironmentSettingsObject);

JS::NonnullGCPtr<WorkerEnvironmentSettingsObject> WorkerEnvironmentSettingsObject::setup(JS::NonnullGCPtr<Page> page, NonnullOwnPtr<JS::ExecutionContext> execution_context /* FIXME: null or an environment reservedEnvironment, a URL topLevelCreationURL, and an origin topLevelOrigin */)
{
    auto realm = execution_context->realm;
    VERIFY(realm);

    auto& worker = verify_cast<HTML::WorkerGlobalScope>(realm->global_object());

    auto settings_object = realm->heap().allocate<WorkerEnvironmentSettingsObject>(*realm, move(execution_context), worker);
    settings_object->target_browsing_context = nullptr;

    auto intrinsics = realm->heap().allocate<Bindings::Intrinsics>(*realm, *realm);
    auto host_defined = make<Bindings::HostDefined>(settings_object, intrinsics, page);
    realm->set_host_defined(move(host_defined));

    // Non-Standard: We cannot fully initialize worker object until *after* the we set up
    //    the realm's [[HostDefined]] internal slot as the internal slot contains the web platform intrinsics
    worker.initialize_web_interfaces({});

    return settings_object;
}

void WorkerEnvironmentSettingsObject::visit_edges(JS::Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_global_scope);
}

}
