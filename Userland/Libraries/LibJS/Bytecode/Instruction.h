/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <LibJS/Forward.h>

#define ENUMERATE_BYTECODE_OPS(O)    \
    O(Load)                          \
    O(LoadImmediate)                 \
    O(Store)                         \
    O(Add)                           \
    O(Sub)                           \
    O(Mul)                           \
    O(Div)                           \
    O(Mod)                           \
    O(Exp)                           \
    O(GreaterThan)                   \
    O(GreaterThanEquals)             \
    O(LessThan)                      \
    O(LessThanEquals)                \
    O(LooselyInequals)               \
    O(LooselyEquals)                 \
    O(StrictlyInequals)              \
    O(StrictlyEquals)                \
    O(NewBigInt)                     \
    O(NewArray)                      \
    O(IteratorToArray)               \
    O(NewString)                     \
    O(NewObject)                     \
    O(NewRegExp)                     \
    O(CopyObjectExcludingProperties) \
    O(GetVariable)                   \
    O(SetVariable)                   \
    O(PutById)                       \
    O(GetById)                       \
    O(PutByValue)                    \
    O(GetByValue)                    \
    O(Jump)                          \
    O(JumpConditional)               \
    O(JumpNullish)                   \
    O(JumpUndefined)                 \
    O(Call)                          \
    O(NewFunction)                   \
    O(Return)                        \
    O(BitwiseAnd)                    \
    O(BitwiseOr)                     \
    O(BitwiseXor)                    \
    O(BitwiseNot)                    \
    O(Not)                           \
    O(UnaryPlus)                     \
    O(UnaryMinus)                    \
    O(Typeof)                        \
    O(LeftShift)                     \
    O(RightShift)                    \
    O(UnsignedRightShift)            \
    O(In)                            \
    O(InstanceOf)                    \
    O(ConcatString)                  \
    O(Increment)                     \
    O(Decrement)                     \
    O(Throw)                         \
    O(PushDeclarativeEnvironment)    \
    O(EnterUnwindContext)            \
    O(LeaveUnwindContext)            \
    O(ContinuePendingUnwind)         \
    O(Yield)                         \
    O(GetIterator)                   \
    O(IteratorNext)                  \
    O(IteratorResultDone)            \
    O(IteratorResultValue)           \
    O(NewClass)

namespace JS::Bytecode {

class Instruction {
public:
    constexpr static bool IsTerminator = false;

    enum class Type {
#define __BYTECODE_OP(op) \
    op,
        ENUMERATE_BYTECODE_OPS(__BYTECODE_OP)
#undef __BYTECODE_OP
    };

    bool is_terminator() const;
    Type type() const { return m_type; }
    size_t length() const;
    String to_string(Bytecode::Executable const&) const;
    void execute(Bytecode::Interpreter&) const;
    void replace_references(BasicBlock const&, BasicBlock const&);
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
