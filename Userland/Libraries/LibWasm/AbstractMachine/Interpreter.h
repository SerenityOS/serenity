/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWasm/AbstractMachine/Configuration.h>

namespace Wasm {

struct Interpreter {
    void interpret(Configuration&);
    bool did_trap() const { return m_do_trap; }

private:
    void interpret(Configuration&, InstructionPointer&, const Instruction&);
    void branch_to_label(Configuration&, LabelIndex);
    ReadonlyBytes load_from_memory(Configuration&, const Instruction&, size_t);
    void store_to_memory(Configuration&, const Instruction&, ReadonlyBytes data);
    void call_address(Configuration&, FunctionAddress);

    template<typename V, typename T>
    MakeUnsigned<T> checked_unsigned_truncate(V);

    template<typename V, typename T>
    MakeSigned<T> checked_signed_truncate(V);

    Vector<NonnullOwnPtr<Value>> pop_values(Configuration& configuration, size_t count);
    bool trap_if_not(bool value)
    {
        if (!value)
            m_do_trap = true;
        return m_do_trap;
    }
    bool m_do_trap { false };
};

}
