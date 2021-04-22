/*
 * Copyright (c) 2020, Jack Karamanian <karamanian.jack@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Function.h>

namespace JS {

class BoundFunction final : public Function {
    JS_OBJECT(BoundFunction, Function);

public:
    BoundFunction(GlobalObject&, Function& target_function, Value bound_this, Vector<Value> arguments, i32 length, Object* constructor_prototype);
    virtual void initialize(GlobalObject&) override;
    virtual ~BoundFunction();

    virtual Value call() override;

    virtual Value construct(Function& new_target) override;

    virtual LexicalEnvironment* create_environment() override;

    virtual void visit_edges(Visitor&) override;

    virtual const FlyString& name() const override
    {
        return m_name;
    }

    Function& target_function() const
    {
        return *m_target_function;
    }

    virtual bool is_strict_mode() const override { return m_target_function->is_strict_mode(); }

private:
    Function* m_target_function = nullptr;
    Object* m_constructor_prototype = nullptr;
    FlyString m_name;
    i32 m_length { 0 };
};

}
