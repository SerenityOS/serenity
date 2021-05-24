/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWasm/AbstractMachine/Configuration.h>

namespace Wasm {

struct Interpreter {
    virtual ~Interpreter() = default;
    virtual void interpret(Configuration&) = 0;
    virtual bool did_trap() const = 0;
    virtual void clear_trap() = 0;
};

struct BytecodeInterpreter : public Interpreter {
    virtual void interpret(Configuration&) override;
    virtual ~BytecodeInterpreter() override = default;
    virtual bool did_trap() const override { return m_do_trap; }
    virtual void clear_trap() override { m_do_trap = false; }

    struct CallFrameHandle {
        explicit CallFrameHandle(BytecodeInterpreter& interpreter, Configuration& configuration)
            : m_configuration_handle(configuration)
            , m_interpreter(interpreter)
        {
        }

        ~CallFrameHandle() = default;

        Configuration::CallFrameHandle m_configuration_handle;
        BytecodeInterpreter& m_interpreter;
    };

protected:
    virtual void interpret(Configuration&, InstructionPointer&, const Instruction&);
    void branch_to_label(Configuration&, LabelIndex);
    ReadonlyBytes load_from_memory(Configuration&, const Instruction&, size_t);
    void store_to_memory(Configuration&, const Instruction&, ReadonlyBytes data);
    void call_address(Configuration&, FunctionAddress);

    template<typename V, typename T>
    MakeUnsigned<T> checked_unsigned_truncate(V);

    template<typename V, typename T>
    MakeSigned<T> checked_signed_truncate(V);

    template<typename T>
    T read_value(ReadonlyBytes data);

    Vector<Value> pop_values(Configuration& configuration, size_t count);
    bool trap_if_not(bool value)
    {
        if (!value)
            m_do_trap = true;
        return m_do_trap;
    }
    bool m_do_trap { false };
};

struct DebuggerBytecodeInterpreter : public BytecodeInterpreter {
    virtual ~DebuggerBytecodeInterpreter() override = default;

    Function<bool(Configuration&, InstructionPointer&, const Instruction&)> pre_interpret_hook;
    Function<bool(Configuration&, InstructionPointer&, const Instruction&, const Interpreter&)> post_interpret_hook;

private:
    virtual void interpret(Configuration&, InstructionPointer&, const Instruction&) override;
};

}
