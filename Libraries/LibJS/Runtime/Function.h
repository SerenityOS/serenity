/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/String.h>
#include <LibJS/Runtime/Object.h>

namespace JS {

class Function : public Object {
public:
    virtual ~Function();

    virtual Value call(Interpreter&) = 0;
    virtual Value construct(Interpreter&) = 0;
    virtual const FlyString& name() const = 0;
    virtual LexicalEnvironment* create_environment() = 0;

    virtual void visit_children(Visitor&) override;

    BoundFunction* bind(Value bound_this_value, Vector<Value> arguments);

    Value bound_this() const
    {
        return m_bound_this;
    }

    const Vector<Value>& bound_arguments() const
    {
        return m_bound_arguments;
    }

protected:
    explicit Function(Object& prototype);
    explicit Function(Object& prototype, Value bound_this, Vector<Value> bound_arguments);
    virtual const char* class_name() const override { return "Function"; }

private:
    virtual bool is_function() const final { return true; }
    Value m_bound_this;
    Vector<Value> m_bound_arguments;
};

}
