/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/Environment.h>
#include <LibJS/Runtime/Object.h>

namespace JS {

class ArgumentsObject final : public Object {
    JS_OBJECT(ArgumentsObject, Object);

public:
    ArgumentsObject(GlobalObject&, Environment&);

    virtual void initialize(GlobalObject&) override;
    virtual ~ArgumentsObject() override;

    Environment& environment() { return m_environment; }

    virtual ThrowCompletionOr<Optional<PropertyDescriptor>> internal_get_own_property(PropertyKey const&) const override;
    virtual ThrowCompletionOr<bool> internal_define_own_property(PropertyKey const&, PropertyDescriptor const&) override;
    virtual ThrowCompletionOr<Value> internal_get(PropertyKey const&, Value receiver) const override;
    virtual ThrowCompletionOr<bool> internal_set(PropertyKey const&, Value value, Value receiver) override;
    virtual ThrowCompletionOr<bool> internal_delete(PropertyKey const&) override;

    // [[ParameterMap]]
    Object& parameter_map() { return *m_parameter_map; }

private:
    virtual void visit_edges(Cell::Visitor&) override;

    Environment& m_environment;
    Object* m_parameter_map { nullptr };
};

}
