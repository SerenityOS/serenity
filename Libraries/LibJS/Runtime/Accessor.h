/*
 * Copyright (c) 2020, Matthew Olsson <matthewcolsson@gmail.com>
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

#include <LibJS/Runtime/Cell.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

class Accessor final : public Cell {
public:
    static Accessor* create(Interpreter& interpreter, Value getter, Value setter)
    {
        return interpreter.heap().allocate<Accessor>(getter, setter);
    }

    Accessor(Value getter, Value setter)
        : m_getter(getter)
        , m_setter(setter)
    {
    }

    Value getter() { return m_getter; }
    Value setter() { return m_setter; }

    Value call_getter(Value this_object)
    {
        if (!getter().is_function())
            return js_undefined();
        return interpreter().call(getter().as_function(), this_object);
    }

    void call_setter(Value this_object, Value setter_value)
    {
        if (!setter().is_function())
            return;
        MarkedValueList arguments(interpreter().heap());
        arguments.values().append(setter_value);
        interpreter().call(setter().as_function(), this_object, move(arguments));
    }

    void visit_children(Cell::Visitor& visitor) override
    {
        visitor.visit(m_getter);
        visitor.visit(m_setter);
    }

private:
    const char* class_name() const override { return "Accessor"; };

    Value m_getter;
    Value m_setter;
};

}
