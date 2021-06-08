/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <LibJS/Bytecode/Block.h>
#include <LibJS/Bytecode/Label.h>
#include <LibJS/Forward.h>

namespace JS::Bytecode {

class Generator {
public:
    static OwnPtr<Block> generate(ASTNode const&);

    Register allocate_register();

    template<typename OpType, typename... Args>
    InstructionHandle<OpType> emit(Args&&... args)
    {
        return make_instruction<OpType>(0, forward<Args>(args)...);
    }

    template<typename OpType, typename... Args>
    InstructionHandle<OpType> emit_with_extra_register_slots(size_t extra_register_slots, Args&&... args)
    {
        return make_instruction<OpType>(extra_register_slots, forward<Args>(args)...);
    }

    Label make_label() const;

    void begin_continuable_scope();
    void end_continuable_scope();

    Label nearest_continuable_scope() const;

private:
    Generator();
    ~Generator();

    template<typename OpType, typename... Args>
    InstructionHandle<OpType> make_instruction(size_t extra_register_slots, Args&&... args)
    {
        auto& buffer = m_block->buffer();
        auto offset = buffer.size();
        buffer.resize(buffer.size() + sizeof(OpType) + extra_register_slots * sizeof(Register));
        new (buffer.data() + offset) OpType(forward<Args>(args)...);
        return InstructionHandle<OpType>(offset, m_block);
    }

    OwnPtr<Block> m_block;
    u32 m_next_register { 1 };
    Vector<Label> m_continuable_scopes;
};

}
