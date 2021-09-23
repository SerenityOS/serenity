/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/FunctionObject.h>
#include <LibJS/Runtime/VM.h>

namespace JS {

class Accessor final : public Cell {
public:
    static Accessor* create(VM& vm, FunctionObject* getter, FunctionObject* setter)
    {
        return vm.heap().allocate_without_global_object<Accessor>(getter, setter);
    }

    Accessor(FunctionObject* getter, FunctionObject* setter)
        : m_getter(getter)
        , m_setter(setter)
    {
    }

    FunctionObject* getter() const { return m_getter; }
    void set_getter(FunctionObject* getter) { m_getter = getter; }

    FunctionObject* setter() const { return m_setter; }
    void set_setter(FunctionObject* setter) { m_setter = setter; }

    Value call_getter(Value this_value)
    {
        if (!m_getter)
            return js_undefined();
        return TRY_OR_DISCARD(vm().call(*m_getter, this_value));
    }

    void call_setter(Value this_value, Value setter_value)
    {
        if (!m_setter)
            return;
        // FIXME: It might be nice if we had a way to communicate to our caller if an exception happened after this.
        [[maybe_unused]] auto rc = vm().call(*m_setter, this_value, setter_value);
    }

    void visit_edges(Cell::Visitor& visitor) override
    {
        visitor.visit(m_getter);
        visitor.visit(m_setter);
    }

private:
    const char* class_name() const override { return "Accessor"; };

    FunctionObject* m_getter { nullptr };
    FunctionObject* m_setter { nullptr };
};

}
