/*
 * Copyright (c) 2020, Matthew Olsson <matthewcolsson@gmail.com>
 * Copyright (c) 2020, Linus Groh <mail@linusgroh.de>
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
#include <LibJS/Runtime/VM.h>

namespace JS {

class Accessor final : public Cell {
public:
    static Accessor* create(VM& vm, Function* getter, Function* setter)
    {
        return vm.heap().allocate_without_global_object<Accessor>(getter, setter);
    }

    Accessor(Function* getter, Function* setter)
        : m_getter(getter)
        , m_setter(setter)
    {
    }

    Function* getter() const { return m_getter; }
    void set_getter(Function* getter) { m_getter = getter; }

    Function* setter() const { return m_setter; }
    void set_setter(Function* setter) { m_setter = setter; }

    Value call_getter(Value this_value)
    {
        if (!m_getter)
            return js_undefined();
        return vm().call(*m_getter, this_value);
    }

    void call_setter(Value this_value, Value setter_value)
    {
        if (!m_setter)
            return;
        // FIXME: It might be nice if we had a way to communicate to our caller if an exception happened after this.
        (void)vm().call(*m_setter, this_value, setter_value);
    }

    void visit_children(Cell::Visitor& visitor) override
    {
        visitor.visit(m_getter);
        visitor.visit(m_setter);
    }

private:
    const char* class_name() const override { return "Accessor"; };

    Function* m_getter { nullptr };
    Function* m_setter { nullptr };
};

}
