/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/NonnullOwnPtrVector.h>
#include <LibJS/Bytecode/Register.h>
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

class Block {
public:
    static NonnullOwnPtr<Block> create();
    ~Block();

    void dump() const;
    ReadonlyBytes instruction_stream() const { return m_buffer.span(); }

    size_t register_count() const { return m_register_count; }

    void set_register_count(Badge<Bytecode::Generator>, size_t count) { m_register_count = count; }

    Vector<u8>& buffer() { return m_buffer; }

private:
    Block() = default;

    size_t m_register_count { 0 };
    Vector<u8> m_buffer;
};

}
