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
    static OwnPtr<Block> generate(JS::Interpreter&, GlobalObject&, ASTNode const&);

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

    [[nodiscard]] JS::Interpreter& interpreter() const { return m_interpreter; }
    [[nodiscard]] GlobalObject& global_object() const { return m_global_object; }

    Label make_label() const;

    void begin_continuable_scope();
    void end_continuable_scope();

    Label nearest_continuable_scope() const;

private:
    Generator(JS::Interpreter&, GlobalObject&);
    ~Generator();

    void grow(size_t);
    void* next_slot();

    JS::Interpreter& m_interpreter;
    GlobalObject& m_global_object;
    OwnPtr<Block> m_block;
    u32 m_next_register { 1 };
    Vector<Label> m_continuable_scopes;
};

}
