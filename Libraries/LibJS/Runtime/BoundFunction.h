/*
 * Copyright (c) 2020, Jack Karamanian <karamanian.jack@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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

    virtual void visit_children(Visitor&) override;

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
    virtual bool is_bound_function() const override { return true; }

    Function* m_target_function = nullptr;
    Object* m_constructor_prototype = nullptr;
    FlyString m_name;
    i32 m_length { 0 };
};

}
