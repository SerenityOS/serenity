/*
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/AST.h>
#include <LibJS/Bytecode/Generator.h>
#include <LibJS/Runtime/BigInt.h>

namespace JS::Bytecode {

class VirtualRegister {
public:
    explicit VirtualRegister(Generator& generator, NonnullRefPtr<ASTNode> ast_node)
        : m_generator(generator)
        , m_ast_node(ast_node)
    {
        m_value = ast_node->constant_execute(generator.interpreter(), generator.global_object());
    }

    bool is_constant() const
    {
        return m_value.has_value();
    }

    Value& operator*()
    {
        VERIFY(is_constant());
        return *m_value;
    }

    Value* operator->()
    {
        VERIFY(is_constant());
        return &*m_value;
    }

    Optional<Register> materialize()
    {
        if (m_value.has_value())
            return emit_constant_value(m_generator, *m_value);
        return m_ast_node->generate_bytecode(m_generator);
    }

private:
    Generator& m_generator;
    NonnullRefPtr<ASTNode> m_ast_node;
    Optional<Value> m_value;

    static Register emit_constant_value(Bytecode::Generator& generator, Value& value)
    {
        auto dst = generator.allocate_register();
        if (value.is_string()) {
            generator.emit<Bytecode::Op::NewString>(dst, value.as_string().string());
        } else if (value.is_bigint()) {
            generator.emit<Bytecode::Op::NewBigInt>(dst, value.as_bigint().big_integer());
        } else {
            // This should only emit Load ops for Values which don't depend on the VM heap
            generator.emit<Bytecode::Op::Load>(dst, value);
        }
        return dst;
    }
};

}
