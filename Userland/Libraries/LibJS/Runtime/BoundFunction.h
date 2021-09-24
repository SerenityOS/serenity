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
    BoundFunction(GlobalObject&, FunctionObject& target_function, Value bound_this, Vector<Value> arguments, i32 length, Object* constructor_prototype);
    virtual void initialize(GlobalObject&) override;
    virtual ~BoundFunction();

    virtual Value call() override;
    virtual Value construct(FunctionObject& new_target) override;
    virtual FunctionEnvironment* create_environment(FunctionObject&) override;
    virtual const FlyString& name() const override { return m_name; }
    virtual bool is_strict_mode() const override { return m_bound_target_function->is_strict_mode(); }

    FunctionObject& bound_target_function() const { return *m_bound_target_function; }

private:
    virtual void visit_edges(Visitor&) override;

    FunctionObject* m_bound_target_function { nullptr }; // [[BoundTargetFunction]]

    Object* m_constructor_prototype { nullptr };
    FlyString m_name;
    i32 m_length { 0 };
};

}
