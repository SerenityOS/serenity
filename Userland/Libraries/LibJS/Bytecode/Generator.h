/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <LibJS/Forward.h>

namespace JS::Bytecode {

class Generator {
public:
    static OwnPtr<Block> generate(ASTNode const&);

    Register allocate_register();

    template<typename OpType, typename... Args>
    void emit(Args&&... args)
    {
        auto instruction = make<OpType>(forward<Args>(args)...);
        append(move(instruction));
    }

private:
    Generator();
    ~Generator();

    void append(NonnullOwnPtr<Instruction>);

    OwnPtr<Block> m_block;
    u32 m_next_register { 1 };
};

}
