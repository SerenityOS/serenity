/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/AST.h>
#include <LibJS/Runtime/WithScope.h>

namespace JS {

WithScope::WithScope(Object& object, ScopeObject* parent_scope)
    : ScopeObject(parent_scope)
    , m_object(object)
{
}

void WithScope::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(&m_object);
}

Optional<Variable> WithScope::get_from_scope(const FlyString& name) const
{
    auto value = m_object.get(name);
    if (value.is_empty())
        return {};
    return Variable { value, DeclarationKind::Var };
}

void WithScope::put_to_scope(const FlyString& name, Variable variable)
{
    m_object.put(name, variable.value);
}

bool WithScope::has_this_binding() const
{
    return parent()->has_this_binding();
}

Value WithScope::get_this_binding(GlobalObject& global_object) const
{
    return parent()->get_this_binding(global_object);
}

}
