/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtrVector.h>
#include <AK/OwnPtr.h>
#include <AK/SinglyLinkedList.h>
#include <LibJS/Bytecode/BasicBlock.h>
#include <LibJS/Bytecode/Label.h>
#include <LibJS/Bytecode/Register.h>
#include <LibJS/Forward.h>

namespace JS::Bytecode {

struct ExecutionUnit {
    NonnullOwnPtrVector<BasicBlock> basic_blocks;
    size_t number_of_registers { 0 };
};

class Generator {
public:
    static ExecutionUnit generate(ASTNode const&);

    Register allocate_register();

    template<typename OpType, typename... Args>
    OpType& emit(Args&&... args)
    {
        VERIFY(!is_current_block_terminated());
        void* slot = next_slot();
        grow(sizeof(OpType));
        new (slot) OpType(forward<Args>(args)...);
        if constexpr (OpType::IsTerminator)
            m_current_basic_block->terminate({});
        return *static_cast<OpType*>(slot);
    }

    template<typename OpType, typename... Args>
    OpType& emit_with_extra_register_slots(size_t extra_register_slots, Args&&... args)
    {
        VERIFY(!is_current_block_terminated());
        void* slot = next_slot();
        grow(sizeof(OpType) + extra_register_slots * sizeof(Register));
        new (slot) OpType(forward<Args>(args)...);
        if constexpr (OpType::IsTerminator)
            m_current_basic_block->terminate({});
        return *static_cast<OpType*>(slot);
    }

    void begin_continuable_scope(Label continue_target);
    void end_continuable_scope();

    [[nodiscard]] Label nearest_continuable_scope() const;

    void switch_to_basic_block(BasicBlock& block)
    {
        m_current_basic_block = &block;
    }

    BasicBlock& make_block(String name = {})
    {
        if (name.is_empty())
            name = String::number(m_next_block++);
        m_root_basic_blocks.append(BasicBlock::create(name));
        return m_root_basic_blocks.last();
    }

    bool is_current_block_terminated() const
    {
        return m_current_basic_block->is_terminated();
    }

private:
    Generator();
    ~Generator();

    void grow(size_t);
    void* next_slot();

    BasicBlock* m_current_basic_block { nullptr };
    NonnullOwnPtrVector<BasicBlock> m_root_basic_blocks;

    u32 m_next_register { 1 };
    u32 m_next_block { 1 };
    Vector<Label> m_continuable_scopes;
};

}
