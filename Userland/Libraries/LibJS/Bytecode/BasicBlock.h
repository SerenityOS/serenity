/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/String.h>
#include <LibJS/Forward.h>

namespace JS::Bytecode {

class InstructionStreamIterator {
public:
    explicit InstructionStreamIterator(ReadonlyBytes bytes)
        : m_bytes(bytes)
    {
    }

    size_t offset() const { return m_offset; }
    bool at_end() const { return m_offset >= m_bytes.size(); }
    void jump(size_t offset)
    {
        VERIFY(offset <= m_bytes.size());
        m_offset = offset;
    }

    Instruction const& operator*() const { return dereference(); }
    void operator++();

private:
    Instruction const& dereference() const { return *reinterpret_cast<Instruction const*>(m_bytes.data() + offset()); }

    ReadonlyBytes m_bytes;
    size_t m_offset { 0 };
};

class BasicBlock {
public:
    static NonnullOwnPtr<BasicBlock> create(String name);
    ~BasicBlock();

    void seal();

    void dump() const;
    ReadonlyBytes instruction_stream() const { return ReadonlyBytes { m_buffer, m_buffer_size }; }

    void* next_slot() { return m_buffer + m_buffer_size; }
    void grow(size_t additional_size);

    void terminate(Badge<Generator>) { m_is_terminated = true; }
    bool is_terminated() const { return m_is_terminated; }

    String const& name() const { return m_name; }

private:
    BasicBlock(String name);

    u8* m_buffer { nullptr };
    size_t m_buffer_capacity { 0 };
    size_t m_buffer_size { 0 };
    bool m_is_terminated { false };
    String m_name;
};

}
