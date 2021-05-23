/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWasm/AbstractMachine/AbstractMachine.h>

namespace Wasm {

class Configuration {
public:
    explicit Configuration(Store& store)
        : m_store(store)
    {
    }

    Optional<Label> nth_label(size_t);
    void set_frame(NonnullOwnPtr<Frame> frame)
    {
        m_current_frame = frame.ptr();
        m_stack.push(move(frame));
        m_stack.push(Label(m_current_frame->arity(), m_current_frame->expression().instructions().size()));
    }
    auto& frame() const { return m_current_frame; }
    auto& frame() { return m_current_frame; }
    auto& ip() const { return m_ip; }
    auto& ip() { return m_ip; }
    auto& depth() const { return m_depth; }
    auto& depth() { return m_depth; }
    auto& stack() const { return m_stack; }
    auto& stack() { return m_stack; }
    auto& store() const { return m_store; }
    auto& store() { return m_store; }

    Result call(FunctionAddress, Vector<Value> arguments);
    Result execute();

    void dump_stack();

    Function<bool(Configuration&, InstructionPointer&, const Instruction&)>* pre_interpret_hook { nullptr };
    Function<bool(Configuration&, InstructionPointer&, const Instruction&, const Interpreter&)>* post_interpret_hook { nullptr };

private:
    Store& m_store;
    Frame* m_current_frame { nullptr };
    Stack m_stack;
    size_t m_depth { 0 };
    InstructionPointer m_ip;
};

}
