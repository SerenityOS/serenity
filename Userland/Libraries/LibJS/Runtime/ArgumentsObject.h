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

    virtual ThrowCompletionOr<Optional<PropertyDescriptor>> internal_get_own_property(PropertyName const&) const override;
    virtual bool internal_define_own_property(PropertyName const&, PropertyDescriptor const&) override;
    virtual Value internal_get(PropertyName const&, Value receiver) const override;
    virtual bool internal_set(PropertyName const&, Value value, Value receiver) override;
    virtual bool internal_delete(PropertyName const&) override;

    // [[ParameterMap]]
    Object& parameter_map() { return *m_parameter_map; }

private:
    virtual void visit_edges(Cell::Visitor&) override;

    Environment& m_environment;
    Object* m_parameter_map { nullptr };
};

}
