/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <LibJS/Bytecode/Label.h>
#include <LibJS/Bytecode/Register.h>
#include <LibJS/Forward.h>

namespace JS::Bytecode {

class Generator {
public:
    static OwnPtr<Block> generate(ASTNode const&);

    Register allocate_register();

    template<typename OpType, typename... Args>
    OpType& emit(Args&&... args)
    {
        void* slot = next_slot();
        grow(sizeof(OpType));
        new (slot) OpType(forward<Args>(args)...);
        return *static_cast<OpType*>(slot);
    }

    template<typename OpType, typename... Args>
    OpType& emit_with_extra_register_slots(size_t extra_register_slots, Args&&... args)
    {
        void* slot = next_slot();
        grow(sizeof(OpType) + extra_register_slots * sizeof(Register));
        new (slot) OpType(forward<Args>(args)...);
        return *static_cast<OpType*>(slot);
    }

    Label make_label() const;

    void begin_continuable_scope();
    void end_continuable_scope();

    Label nearest_continuable_scope() const;

private:
    Generator();
    ~Generator();

    void grow(size_t);
    void* next_slot();

    OwnPtr<Block> m_block;
    u32 m_next_register { 1 };
    Vector<Label> m_continuable_scopes;
};

}
