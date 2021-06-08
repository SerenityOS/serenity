/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <LibJS/Forward.h>

#define ENUMERATE_BYTECODE_OPS(O) \
    O(Load)                       \
    O(LoadImmediate)              \
    O(Store)                      \
    O(Add)                        \
    O(Sub)                        \
    O(Mul)                        \
    O(Div)                        \
    O(Mod)                        \
    O(Exp)                        \
    O(GreaterThan)                \
    O(GreaterThanEquals)          \
    O(LessThan)                   \
    O(LessThanEquals)             \
    O(AbstractInequals)           \
    O(AbstractEquals)             \
    O(TypedInequals)              \
    O(TypedEquals)                \
    O(NewBigInt)                  \
    O(NewString)                  \
    O(NewObject)                  \
    O(GetVariable)                \
    O(SetVariable)                \
    O(PutById)                    \
    O(GetById)                    \
    O(Jump)                       \
    O(JumpIfFalse)                \
    O(JumpIfTrue)                 \
    O(JumpIfNotNullish)           \
    O(Call)                       \
    O(EnterScope)                 \
    O(Return)                     \
    O(BitwiseAnd)                 \
    O(BitwiseOr)                  \
    O(BitwiseXor)                 \
    O(BitwiseNot)                 \
    O(Not)                        \
    O(UnaryPlus)                  \
    O(UnaryMinus)                 \
    O(Typeof)                     \
    O(LeftShift)                  \
    O(RightShift)                 \
    O(UnsignedRightShift)         \
    O(In)                         \
    O(InstanceOf)                 \
    O(ConcatString)

namespace JS::Bytecode {

class Instruction {
public:
    enum class Type {
#define __BYTECODE_OP(op) \
    op,
        ENUMERATE_BYTECODE_OPS(__BYTECODE_OP)
#undef __BYTECODE_OP
    };

    Type type() const { return m_type; }
    size_t length() const;
    String to_string() const;
    void execute(Bytecode::Interpreter&) const;
    static void destroy(Instruction&);

protected:
    explicit Instruction(Type type)
        : m_type(type)
    {
    }

private:
    Type m_type {};
};

}
