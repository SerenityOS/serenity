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
    void set_frame(Frame&& frame)
    {
        m_current_frame_index = m_stack.size();
        Label label(frame.arity(), frame.expression().instructions().size());
        m_stack.push(move(frame));
        m_stack.push(label);
    }
    auto& frame() const { return m_stack.entries()[m_current_frame_index].get<Frame>(); }
    auto& frame() { return m_stack.entries()[m_current_frame_index].get<Frame>(); }
    auto& ip() const { return m_ip; }
    auto& ip() { return m_ip; }
    auto& depth() const { return m_depth; }
    auto& depth() { return m_depth; }
    auto& stack() const { return m_stack; }
    auto& stack() { return m_stack; }
    auto& store() const { return m_store; }
    auto& store() { return m_store; }

    struct CallFrameHandle {
        explicit CallFrameHandle(Configuration& configuration)
            : frame_index(configuration.m_current_frame_index)
            , stack_size(configuration.m_stack.size())
            , ip(configuration.ip())
            , configuration(configuration)
        {
            configuration.depth()++;
        }

        ~CallFrameHandle()
        {
            configuration.unwind({}, *this);
        }

        size_t frame_index { 0 };
        size_t stack_size { 0 };
        InstructionPointer ip { 0 };
        Configuration& configuration;
    };

    void unwind(Badge<CallFrameHandle>, const CallFrameHandle&);
    Result call(FunctionAddress, Vector<Value> arguments);
    Result execute();

    void dump_stack();

    Function<bool(Configuration&, InstructionPointer&, const Instruction&)>* pre_interpret_hook { nullptr };
    Function<bool(Configuration&, InstructionPointer&, const Instruction&, const Interpreter&)>* post_interpret_hook { nullptr };

private:
    Store& m_store;
    size_t m_current_frame_index { 0 };
    Stack m_stack;
    size_t m_depth { 0 };
    InstructionPointer m_ip;
};

}
