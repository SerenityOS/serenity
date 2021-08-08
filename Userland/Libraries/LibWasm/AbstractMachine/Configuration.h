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

    Optional<Label> nth_label(size_t label)
    {
        auto index = nth_label_index(label);
        if (index.has_value())
            return m_stack.entries()[index.value()].get<Label>();
        return {};
    }
    Optional<size_t> nth_label_index(size_t);
    void set_frame(Frame&& frame)
    {
        m_current_frame_index = m_stack.size();
        Label label(frame.arity(), frame.expression().instructions().size());
        m_stack.push(move(frame));
        m_stack.push(label);
    }
    ALWAYS_INLINE auto& frame() const { return m_stack.entries()[m_current_frame_index].get<Frame>(); }
    ALWAYS_INLINE auto& frame() { return m_stack.entries()[m_current_frame_index].get<Frame>(); }
    ALWAYS_INLINE auto& ip() const { return m_ip; }
    ALWAYS_INLINE auto& ip() { return m_ip; }
    ALWAYS_INLINE auto& depth() const { return m_depth; }
    ALWAYS_INLINE auto& depth() { return m_depth; }
    ALWAYS_INLINE auto& stack() const { return m_stack; }
    ALWAYS_INLINE auto& stack() { return m_stack; }
    ALWAYS_INLINE auto& store() const { return m_store; }
    ALWAYS_INLINE auto& store() { return m_store; }

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

    void unwind(Badge<CallFrameHandle>, CallFrameHandle const&);
    Result call(Interpreter&, FunctionAddress, Vector<Value> arguments);
    Result execute(Interpreter&);

    void enable_instruction_count_limit() { m_should_limit_instruction_count = true; }
    bool should_limit_instruction_count() const { return m_should_limit_instruction_count; }

    void dump_stack();

private:
    Store& m_store;
    size_t m_current_frame_index { 0 };
    Stack m_stack;
    size_t m_depth { 0 };
    InstructionPointer m_ip;
    bool m_should_limit_instruction_count { false };
};

}
