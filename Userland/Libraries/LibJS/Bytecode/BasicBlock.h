/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/String.h>
#include <LibJS/Bytecode/Operand.h>
#include <LibJS/Forward.h>
#include <LibJS/Heap/Handle.h>

namespace JS::Bytecode {

struct UnwindInfo {
    JS::GCPtr<Executable const> executable;
    JS::GCPtr<Environment> lexical_environment;

    bool handler_called { false };
};

class BasicBlock {
    AK_MAKE_NONCOPYABLE(BasicBlock);

public:
    static NonnullOwnPtr<BasicBlock> create(String name);
    ~BasicBlock();

    void dump(Executable const&) const;
    ReadonlyBytes instruction_stream() const { return m_buffer.span(); }
    u8* data() { return m_buffer.data(); }
    u8 const* data() const { return m_buffer.data(); }
    size_t size() const { return m_buffer.size(); }

    void grow(size_t additional_size);

    void terminate(Badge<Generator>, size_t slot_offset) { terminate(slot_offset); }
    bool is_terminated() const { return m_terminated; }

    String const& name() const { return m_name; }

    void set_handler(BasicBlock const& handler) { m_handler = &handler; }
    void set_finalizer(BasicBlock const& finalizer) { m_finalizer = &finalizer; }

    BasicBlock const* handler() const { return m_handler; }
    BasicBlock const* finalizer() const { return m_finalizer; }

    Instruction const* terminator() const
    {
        VERIFY(m_terminated);
        return reinterpret_cast<Instruction const*>(data() + m_terminator_offset);
    }

    template<typename OpType, typename... Args>
    void append(u32 start_offset, u32 end_offset, Args&&... args)
    {
        VERIFY(!m_terminated);
        size_t const slot_offset = size();
        grow(sizeof(OpType));
        void* slot = data() + slot_offset;
        new (slot) OpType(forward<Args>(args)...);
        if constexpr (OpType::IsTerminator)
            terminate(slot_offset);
        auto* op = static_cast<OpType*>(slot);
        op->set_source_record({ start_offset, end_offset });
    }

    template<typename OpType, typename... Args>
    void append_with_extra_operand_slots(u32 start_offset, u32 end_offset, size_t extra_operand_slots, Args&&... args)
    {
        VERIFY(!m_terminated);
        size_t size_to_allocate = round_up_to_power_of_two(sizeof(OpType) + extra_operand_slots * sizeof(Operand), alignof(void*));
        size_t slot_offset = size();
        grow(size_to_allocate);
        void* slot = data() + slot_offset;
        new (slot) OpType(forward<Args>(args)...);
        if constexpr (OpType::IsTerminator)
            terminate(slot_offset);
        auto* op = static_cast<OpType*>(slot);
        op->set_source_record({ start_offset, end_offset });
    }

private:
    explicit BasicBlock(String name);

    void terminate(size_t slot_offset)
    {
        m_terminated = true;
        m_terminator_offset = slot_offset;
    }

    Vector<u8> m_buffer;
    BasicBlock const* m_handler { nullptr };
    BasicBlock const* m_finalizer { nullptr };
    String m_name;
    bool m_terminated { false };
    size_t m_terminator_offset { 0 };
};

}
