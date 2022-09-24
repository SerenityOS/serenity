/*
 * Copyright (c) 2022, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/HashMap.h>
#include <AK/String.h>
#include <LibJS/Forward.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/Object.h>
#include <LibWeb/Bindings/Intrinsics.h>

namespace Web::Bindings {

JS::Object& Intrinsics::cached_web_prototype(String const& class_name)
{
    auto it = m_prototypes.find(class_name);
    if (it == m_prototypes.end()) {
        dbgln("Missing prototype: {}", class_name);
    }
    VERIFY(it != m_prototypes.end());
    return *it->value;
}

void Intrinsics::visit_edges(JS::Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);

    for (auto& it : m_prototypes)
        visitor.visit(it.value);
    for (auto& it : m_constructors)
        visitor.visit(it.value);
}

}
