/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtrVector.h>
#include <LibJS/Bytecode/Label.h>
#include <LibJS/Bytecode/Register.h>
#include <LibJS/Forward.h>
#include <LibJS/Heap/Cell.h>
#include <LibJS/Runtime/Value.h>

namespace JS::Bytecode {

using RegisterWindow = Vector<Value>;

class Interpreter {
public:
    explicit Interpreter(GlobalObject&);
    ~Interpreter();

    // FIXME: Remove this thing once we don't need it anymore!
    static Interpreter* current();

    GlobalObject& global_object() { return m_global_object; }
    VM& vm() { return m_vm; }

    Value run(Bytecode::Block const&);

    ALWAYS_INLINE Value& accumulator() { return reg(Register::accumulator()); }
    Value& reg(Register const& r) { return registers()[r.index()]; }

    void jump(Label const& label) { m_pending_jump = label.address(); }
    void do_return(Value return_value) { m_return_value = return_value; }

private:
    RegisterWindow& registers() { return m_register_windows.last(); }

    VM& m_vm;
    GlobalObject& m_global_object;
    NonnullOwnPtrVector<RegisterWindow> m_register_windows;
    Optional<size_t> m_pending_jump;
    Value m_return_value;
};

}
