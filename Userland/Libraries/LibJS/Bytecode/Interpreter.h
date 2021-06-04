/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Bytecode/Label.h>
#include <LibJS/Bytecode/Register.h>
#include <LibJS/Forward.h>
#include <LibJS/Heap/Cell.h>
#include <LibJS/Runtime/Value.h>

namespace JS::Bytecode {

class Interpreter {
public:
    explicit Interpreter(GlobalObject&);
    ~Interpreter();

    GlobalObject& global_object() { return m_global_object; }
    VM& vm() { return m_vm; }

    void run(Bytecode::Block const&);

    Value& reg(Register const& r) { return m_registers[r.index()]; }

    void jump(Label const& label) { m_pending_jump = label.address(); }

private:
    VM& m_vm;
    GlobalObject& m_global_object;
    Vector<Value> m_registers;
    Optional<size_t> m_pending_jump;
};

}
