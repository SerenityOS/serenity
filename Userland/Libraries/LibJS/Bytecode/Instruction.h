/*
 * Copyright (c) 2021-2024, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/Span.h>
#include <LibJS/Bytecode/Executable.h>
#include <LibJS/Forward.h>
#include <LibJS/SourceRange.h>

#define ENUMERATE_BYTECODE_OPS(O)      \
    O(Add)                             \
    O(ArrayAppend)                     \
    O(AsyncIteratorClose)              \
    O(Await)                           \
    O(BitwiseAnd)                      \
    O(BitwiseNot)                      \
    O(BitwiseOr)                       \
    O(BitwiseXor)                      \
    O(BlockDeclarationInstantiation)   \
    O(Call)                            \
    O(CallWithArgumentArray)           \
    O(Catch)                           \
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
    O(Dump)                            \
    O(End)                             \
    O(EnterUnwindContext)              \
    O(EnterObjectEnvironment)          \
    O(Exp)                             \
    O(GetById)                         \
    O(GetByIdWithThis)                 \
    O(GetByValue)                      \
    O(GetByValueWithThis)              \
    O(GetCalleeAndThisFromEnvironment) \
    O(GetIterator)                     \
    O(GetObjectFromIteratorRecord)     \
    O(GetMethod)                       \
    O(GetNewTarget)                    \
    O(GetNextMethodFromIteratorRecord) \
    O(GetImportMeta)                   \
    O(GetObjectPropertyIterator)       \
    O(GetPrivateById)                  \
    O(GetVariable)                     \
    O(GetGlobal)                       \
    O(GreaterThan)                     \
    O(GreaterThanEquals)               \
    O(HasPrivateId)                    \
    O(ImportCall)                      \
    O(In)                              \
    O(Increment)                       \
    O(InstanceOf)                      \
    O(IteratorClose)                   \
    O(IteratorNext)                    \
    O(IteratorToArray)                 \
    O(Jump)                            \
    O(JumpIf)                          \
    O(JumpNullish)                     \
    O(JumpUndefined)                   \
    O(LeaveLexicalEnvironment)         \
    O(LeaveUnwindContext)              \
    O(LeftShift)                       \
    O(LessThan)                        \
    O(LessThanEquals)                  \
    O(LooselyEquals)                   \
    O(LooselyInequals)                 \
    O(Mod)                             \
    O(Mov)                             \
    O(Mul)                             \
    O(NewArray)                        \
    O(NewClass)                        \
    O(NewFunction)                     \
    O(NewObject)                       \
    O(NewPrimitiveArray)               \
    O(NewRegExp)                       \
    O(NewTypeError)                    \
    O(Not)                             \
    O(PostfixDecrement)                \
    O(PostfixIncrement)                \
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
    O(StrictlyEquals)                  \
    O(StrictlyInequals)                \
    O(Sub)                             \
    O(SuperCallWithArgumentArray)      \
    O(Throw)                           \
    O(ThrowIfNotObject)                \
    O(ThrowIfNullish)                  \
    O(ThrowIfTDZ)                      \
    O(Typeof)                          \
    O(TypeofVariable)                  \
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

    Type type() const { return m_type; }
    size_t length() const { return m_length; }
    ByteString to_byte_string(Bytecode::Executable const&) const;
    ThrowCompletionOr<void> execute(Bytecode::Interpreter&) const;
    static void destroy(Instruction&);

    // FIXME: Find a better way to organize this information
    void set_source_record(SourceRecord rec) { m_source_record = rec; }
    SourceRecord source_record() const { return m_source_record; }

protected:
    Instruction(Type type, size_t length)
        : m_type(type)
        , m_length(length)
    {
    }

private:
    SourceRecord m_source_record {};
    Type m_type {};
    size_t m_length {};
};

class InstructionStreamIterator {
public:
    InstructionStreamIterator(ReadonlyBytes bytes, Executable const* executable = nullptr, size_t offset = 0)
        : m_begin(bytes.data())
        , m_end(bytes.data() + bytes.size())
        , m_ptr(bytes.data() + offset)
        , m_executable(executable)
    {
    }

    size_t offset() const { return m_ptr - m_begin; }
    bool at_end() const { return m_ptr >= m_end; }

    Instruction const& operator*() const { return dereference(); }

    ALWAYS_INLINE void operator++()
    {
        m_ptr += dereference().length();
    }

    UnrealizedSourceRange source_range() const;
    RefPtr<SourceCode> source_code() const;

    Executable const* executable() const { return m_executable; }

private:
    Instruction const& dereference() const { return *reinterpret_cast<Instruction const*>(m_ptr); }

    u8 const* m_begin { nullptr };
    u8 const* m_end { nullptr };
    u8 const* m_ptr { nullptr };
    GCPtr<Executable const> m_executable;
};

}
