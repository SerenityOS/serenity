/*
 * Copyright (c) 2022, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/HashMap.h>
#include <LibJS/Forward.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/Object.h>
#include <LibWeb/Bindings/Intrinsics.h>

namespace Web::Bindings {

JS_DEFINE_ALLOCATOR(Intrinsics);

void Intrinsics::visit_edges(JS::Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);

    for (auto& it : m_namespaces)
        visitor.visit(it.value);
    for (auto& it : m_prototypes)
        visitor.visit(it.value);
    for (auto& it : m_constructors)
        visitor.visit(it.value);
    visitor.visit(m_realm);
}

bool Intrinsics::is_exposed(StringView name) const
{
    return m_constructors.contains(name) || m_prototypes.contains(name) || m_namespaces.contains(name);
}

}
