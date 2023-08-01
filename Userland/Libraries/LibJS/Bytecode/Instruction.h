/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/Span.h>
#include <LibJS/Forward.h>

#define ENUMERATE_BYTECODE_OPS(O)      \
    O(Add)                             \
    O(Append)                          \
    O(AsyncIteratorClose)              \
    O(Await)                           \
    O(BitwiseAnd)                      \
    O(BitwiseNot)                      \
    O(BitwiseOr)                       \
    O(BitwiseXor)                      \
    O(BlockDeclarationInstantiation)   \
    O(Call)                            \
    O(CallWithArgumentArray)           \
    O(ConcatString)                    \
    O(ContinuePendingUnwind)           \
    O(CopyObjectExcludingProperties)   \
    O(CreateLexicalEnvironment)        \
    O(CreateVariable)                  \
    O(Decrement)                       \
    O(DeleteById)                      \
    O(DeleteByIdWithThis)              \
    O(DeleteByValue)                   \
    O(DeleteByValueWithThis)           \
    O(DeleteVariable)                  \
    O(Div)                             \
    O(EnterUnwindContext)              \
    O(EnterObjectEnvironment)          \
    O(Exp)                             \
    O(GetById)                         \
    O(GetByIdWithThis)                 \
    O(GetByValue)                      \
    O(GetByValueWithThis)              \
    O(GetCalleeAndThisFromEnvironment) \
    O(GetIterator)                     \
    O(GetMethod)                       \
    O(GetNewTarget)                    \
    O(GetImportMeta)                   \
    O(GetObjectPropertyIterator)       \
    O(GetPrivateById)                  \
    O(GetVariable)                     \
    O(GetGlobal)                       \
    O(GetLocal)                        \
    O(GreaterThan)                     \
    O(GreaterThanEquals)               \
    O(HasPrivateId)                    \
    O(ImportCall)                      \
    O(In)                              \
    O(Increment)                       \
    O(InstanceOf)                      \
    O(IteratorClose)                   \
    O(IteratorNext)                    \
    O(IteratorResultDone)              \
    O(IteratorResultValue)             \
    O(IteratorToArray)                 \
    O(Jump)                            \
    O(JumpConditional)                 \
    O(JumpNullish)                     \
    O(JumpUndefined)                   \
    O(LeaveLexicalEnvironment)         \
    O(LeaveUnwindContext)              \
    O(LeftShift)                       \
    O(LessThan)                        \
    O(LessThanEquals)                  \
    O(Load)                            \
    O(LoadImmediate)                   \
    O(LooselyEquals)                   \
    O(LooselyInequals)                 \
    O(Mod)                             \
    O(Mul)                             \
    O(NewArray)                        \
    O(NewBigInt)                       \
    O(NewClass)                        \
    O(NewFunction)                     \
    O(NewObject)                       \
    O(NewRegExp)                       \
    O(NewString)                       \
    O(NewTypeError)                    \
    O(Not)                             \
    O(PushDeclarativeEnvironment)      \
    O(PutById)                         \
    O(PutByIdWithThis)                 \
    O(PutByValue)                      \
    O(PutByValueWithThis)              \
    O(PutPrivateById)                  \
    O(ResolveThisBinding)              \
    O(ResolveSuperBase)                \
    O(Return)                          \
    O(RightShift)                      \
    O(ScheduleJump)                    \
    O(SetVariable)                     \
    O(SetLocal)                        \
    O(Store)                           \
    O(StrictlyEquals)                  \
    O(StrictlyInequals)                \
    O(Sub)                             \
    O(SuperCallWithArgumentArray)      \
    O(Throw)                           \
    O(ThrowIfNotObject)                \
    O(ThrowIfNullish)                  \
    O(ToNumeric)                       \
    O(Typeof)                          \
    O(TypeofVariable)                  \
    O(TypeofLocal)                     \
    O(UnaryMinus)                      \
    O(UnaryPlus)                       \
    O(UnsignedRightShift)              \
    O(Yield)

namespace JS::Bytecode {

class alignas(void*) Instruction {
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
    DeprecatedString to_deprecated_string(Bytecode::Executable const&) const;
    ThrowCompletionOr<void> execute(Bytecode::Interpreter&) const;
    static void destroy(Instruction&);

protected:
    explicit Instruction(Type type)
        : m_type(type)
    {
    }

private:
    Type m_type {};
};

class InstructionStreamIterator {
public:
    explicit InstructionStreamIterator(ReadonlyBytes bytes)
        : m_bytes(bytes)
    {
    }

    size_t offset() const { return m_offset; }
    bool at_end() const { return m_offset >= m_bytes.size(); }
    void jump(size_t offset)
    {
        VERIFY(offset <= m_bytes.size());
        m_offset = offset;
    }

    Instruction const& operator*() const { return dereference(); }

    ALWAYS_INLINE void operator++()
    {
        VERIFY(!at_end());
        m_offset += dereference().length();
    }

private:
    Instruction const& dereference() const { return *reinterpret_cast<Instruction const*>(m_bytes.data() + offset()); }

    ReadonlyBytes m_bytes;
    size_t m_offset { 0 };
};

}
