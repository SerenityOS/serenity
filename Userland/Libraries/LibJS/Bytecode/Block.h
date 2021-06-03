/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/NonnullOwnPtrVector.h>
#include <LibJS/Forward.h>

namespace JS::Bytecode {

class Block {
public:
    static NonnullOwnPtr<Block> create();
    ~Block();

    NonnullOwnPtrVector<Instruction> const& instructions() const { return m_instructions; }
    void dump() const;

    size_t register_count() const { return m_register_count; }

    void append(Badge<Bytecode::Generator>, NonnullOwnPtr<Instruction>);
    void set_register_count(Badge<Bytecode::Generator>, size_t count) { m_register_count = count; }

private:
    Block();

    size_t m_register_count { 0 };
    NonnullOwnPtrVector<Instruction> m_instructions;
};

}
