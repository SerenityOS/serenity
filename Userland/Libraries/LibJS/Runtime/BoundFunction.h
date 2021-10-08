/*
 * Copyright (c) 2020, Jack Karamanian <karamanian.jack@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/FunctionObject.h>

namespace JS {

class BoundFunction final : public FunctionObject {
    JS_OBJECT(BoundFunction, FunctionObject);

public:
    BoundFunction(GlobalObject&, FunctionObject& target_function, Value bound_this, Vector<Value> bound_arguments, i32 length, Object* constructor_prototype);
    virtual void initialize(GlobalObject&) override;
    virtual ~BoundFunction();

    virtual ThrowCompletionOr<Value> internal_call(Value this_argument, MarkedValueList arguments_list) override;
    virtual ThrowCompletionOr<Object*> internal_construct(MarkedValueList arguments_list, FunctionObject& new_target) override;

    virtual const FlyString& name() const override { return m_name; }
    virtual bool is_strict_mode() const override { return m_bound_target_function->is_strict_mode(); }
    virtual bool has_constructor() const override { return m_bound_target_function->has_constructor(); }

    FunctionObject& bound_target_function() const { return *m_bound_target_function; }
    Value bound_this() const { return m_bound_this; }
    Vector<Value> const& bound_arguments() const { return m_bound_arguments; }

private:
    virtual void visit_edges(Visitor&) override;

    FunctionObject* m_bound_target_function { nullptr }; // [[BoundTargetFunction]]
    Value m_bound_this;                                  // [[BoundThis]]
    Vector<Value> m_bound_arguments;                     // [[BoundArguments]]

    Object* m_constructor_prototype { nullptr };
    FlyString m_name;
    i32 m_length { 0 };
};

}
