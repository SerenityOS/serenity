/*
 * Copyright (c) 2022, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Heap/Cell.h>
#include <LibJS/Runtime/Realm.h>
#include <LibWeb/Bindings/HostDefined.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/Scripting/Environments.h>

namespace Web::Bindings {

void HostDefined::visit_edges(JS::Cell::Visitor& visitor)
{
    JS::Realm::HostDefined::visit_edges(visitor);
    visitor.visit(environment_settings_object);
    visitor.visit(*intrinsics);
}

void HostDefined::debugger_hook()
{
    if (!environment_settings_object)
        return;

    if (!environment_settings_object->responsible_document())
        return;

    auto* page = environment_settings_object->responsible_document()->page();
    if (!page)
        return;

    page->did_request_debugger_break();

    environment_settings_object->responsible_event_loop().spin_until([&] {
        return page->should_continue_after_debugger_break();
    });
}

}
