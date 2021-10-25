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

struct UnwindInfo {
    Executable const* executable;
    BasicBlock const* handler;
    BasicBlock const* finalizer;
};

class BasicBlock {
    AK_MAKE_NONCOPYABLE(BasicBlock);

public:
    static NonnullOwnPtr<BasicBlock> create(String name, size_t size = 4 * KiB);
    ~BasicBlock();

    void seal();

    void dump(Executable const&) const;
    ReadonlyBytes instruction_stream() const { return ReadonlyBytes { m_buffer, m_buffer_size }; }
    size_t size() const { return m_buffer_size; }

    void* next_slot() { return m_buffer + m_buffer_size; }
    bool can_grow(size_t additional_size) const { return m_buffer_size + additional_size <= m_buffer_capacity; }
    void grow(size_t additional_size);

    void terminate(Badge<Generator>) { m_is_terminated = true; }
    bool is_terminated() const { return m_is_terminated; }

    String const& name() const { return m_name; }

private:
    BasicBlock(String name, size_t size);

    u8* m_buffer { nullptr };
    size_t m_buffer_capacity { 0 };
    size_t m_buffer_size { 0 };
    bool m_is_terminated { false };
    String m_name;
};

}
