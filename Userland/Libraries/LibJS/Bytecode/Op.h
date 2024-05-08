/*
 * Copyright (c) 2021-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FixedArray.h>
#include <AK/StdLibExtras.h>
#include <LibCrypto/BigInt/SignedBigInteger.h>
#include <LibJS/Bytecode/Builtins.h>
#include <LibJS/Bytecode/IdentifierTable.h>
#include <LibJS/Bytecode/Instruction.h>
#include <LibJS/Bytecode/Label.h>
#include <LibJS/Bytecode/Operand.h>
#include <LibJS/Bytecode/RegexTable.h>
#include <LibJS/Bytecode/Register.h>
#include <LibJS/Bytecode/ScopedOperand.h>
#include <LibJS/Bytecode/StringTable.h>
#include <LibJS/Heap/Cell.h>
#include <LibJS/Runtime/Environment.h>
#include <LibJS/Runtime/Iterator.h>
#include <LibJS/Runtime/Value.h>
#include <LibJS/Runtime/ValueTraits.h>

namespace JS {
class FunctionExpression;
}

namespace JS::Bytecode::Op {

class Mov final : public Instruction {
public:
    Mov(Operand dst, Operand src)
        : Instruction(Type::Mov)
        , m_dst(dst)
        , m_src(src)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand dst() const { return m_dst; }
    Operand src() const { return m_src; }

private:
    Operand m_dst;
    Operand m_src;
};

#define JS_ENUMERATE_COMMON_BINARY_OPS_WITH_FAST_PATH(O) \
    O(Add, add)                                          \
    O(BitwiseAnd, bitwise_and)                           \
    O(BitwiseOr, bitwise_or)                             \
    O(BitwiseXor, bitwise_xor)                           \
    O(GreaterThan, greater_than)                         \
    O(GreaterThanEquals, greater_than_equals)            \
    O(LeftShift, left_shift)                             \
    O(LessThan, less_than)                               \
    O(LessThanEquals, less_than_equals)                  \
    O(Mul, mul)                                          \
    O(RightShift, right_shift)                           \
    O(Sub, sub)                                          \
    O(UnsignedRightShift, unsigned_right_shift)

#define JS_ENUMERATE_COMMON_BINARY_OPS_WITHOUT_FAST_PATH(O) \
    O(Div, div)                                             \
    O(Exp, exp)                                             \
    O(Mod, mod)                                             \
    O(In, in)                                               \
    O(InstanceOf, instance_of)                              \
    O(LooselyInequals, loosely_inequals)                    \
    O(LooselyEquals, loosely_equals)                        \
    O(StrictlyInequals, strict_inequals)                    \
    O(StrictlyEquals, strict_equals)

#define JS_DECLARE_COMMON_BINARY_OP(OpTitleCase, op_snake_case)             \
    class OpTitleCase final : public Instruction {                          \
    public:                                                                 \
        explicit OpTitleCase(Operand dst, Operand lhs, Operand rhs)         \
            : Instruction(Type::OpTitleCase)                                \
            , m_dst(dst)                                                    \
            , m_lhs(lhs)                                                    \
            , m_rhs(rhs)                                                    \
        {                                                                   \
        }                                                                   \
                                                                            \
        ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const; \
        ByteString to_byte_string_impl(Bytecode::Executable const&) const;  \
                                                                            \
        Operand dst() const { return m_dst; }                               \
        Operand lhs() const { return m_lhs; }                               \
        Operand rhs() const { return m_rhs; }                               \
                                                                            \
    private:                                                                \
        Operand m_dst;                                                      \
        Operand m_lhs;                                                      \
        Operand m_rhs;                                                      \
    };

JS_ENUMERATE_COMMON_BINARY_OPS_WITHOUT_FAST_PATH(JS_DECLARE_COMMON_BINARY_OP)
JS_ENUMERATE_COMMON_BINARY_OPS_WITH_FAST_PATH(JS_DECLARE_COMMON_BINARY_OP)
#undef JS_DECLARE_COMMON_BINARY_OP

#define JS_ENUMERATE_COMMON_UNARY_OPS(O) \
    O(BitwiseNot, bitwise_not)           \
    O(Not, not_)                         \
    O(UnaryPlus, unary_plus)             \
    O(UnaryMinus, unary_minus)           \
    O(Typeof, typeof_)

#define JS_DECLARE_COMMON_UNARY_OP(OpTitleCase, op_snake_case)              \
    class OpTitleCase final : public Instruction {                          \
    public:                                                                 \
        OpTitleCase(Operand dst, Operand src)                               \
            : Instruction(Type::OpTitleCase)                                \
            , m_dst(dst)                                                    \
            , m_src(src)                                                    \
        {                                                                   \
        }                                                                   \
                                                                            \
        ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const; \
        ByteString to_byte_string_impl(Bytecode::Executable const&) const;  \
                                                                            \
        Operand dst() const { return m_dst; }                               \
        Operand src() const { return m_src; }                               \
                                                                            \
    private:                                                                \
        Operand m_dst;                                                      \
        Operand m_src;                                                      \
    };

JS_ENUMERATE_COMMON_UNARY_OPS(JS_DECLARE_COMMON_UNARY_OP)
#undef JS_DECLARE_COMMON_UNARY_OP

class NewObject final : public Instruction {
public:
    explicit NewObject(Operand dst)
        : Instruction(Type::NewObject)
        , m_dst(dst)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand dst() const { return m_dst; }

private:
    Operand m_dst;
};

class NewRegExp final : public Instruction {
public:
    NewRegExp(Operand dst, StringTableIndex source_index, StringTableIndex flags_index, RegexTableIndex regex_index)
        : Instruction(Type::NewRegExp)
        , m_dst(dst)
        , m_source_index(source_index)
        , m_flags_index(flags_index)
        , m_regex_index(regex_index)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand dst() const { return m_dst; }
    StringTableIndex source_index() const { return m_source_index; }
    StringTableIndex flags_index() const { return m_flags_index; }
    RegexTableIndex regex_index() const { return m_regex_index; }

private:
    Operand m_dst;
    StringTableIndex m_source_index;
    StringTableIndex m_flags_index;
    RegexTableIndex m_regex_index;
};

#define JS_ENUMERATE_NEW_BUILTIN_ERROR_OPS(O) \
    O(TypeError)

#define JS_DECLARE_NEW_BUILTIN_ERROR_OP(ErrorName)                          \
    class New##ErrorName final : public Instruction {                       \
    public:                                                                 \
        New##ErrorName(Operand dst, StringTableIndex error_string)          \
            : Instruction(Type::New##ErrorName)                             \
            , m_dst(dst)                                                    \
            , m_error_string(error_string)                                  \
        {                                                                   \
        }                                                                   \
                                                                            \
        ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const; \
        ByteString to_byte_string_impl(Bytecode::Executable const&) const;  \
                                                                            \
        Operand dst() const { return m_dst; }                               \
        StringTableIndex error_string() const { return m_error_string; }    \
                                                                            \
    private:                                                                \
        Operand m_dst;                                                      \
        StringTableIndex m_error_string;                                    \
    };

JS_ENUMERATE_NEW_BUILTIN_ERROR_OPS(JS_DECLARE_NEW_BUILTIN_ERROR_OP)
#undef JS_DECLARE_NEW_BUILTIN_ERROR_OP

// NOTE: This instruction is variable-width depending on the number of excluded names
class CopyObjectExcludingProperties final : public Instruction {
public:
    static constexpr bool IsVariableLength = true;

    CopyObjectExcludingProperties(Operand dst, Operand from_object, Vector<ScopedOperand> const& excluded_names)
        : Instruction(Type::CopyObjectExcludingProperties)
        , m_dst(dst)
        , m_from_object(from_object)
        , m_excluded_names_count(excluded_names.size())
    {
        for (size_t i = 0; i < m_excluded_names_count; i++)
            m_excluded_names[i] = excluded_names[i];
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    size_t length_impl() const
    {
        return round_up_to_power_of_two(alignof(void*), sizeof(*this) + sizeof(Operand) * m_excluded_names_count);
    }

    Operand dst() const { return m_dst; }
    Operand from_object() const { return m_from_object; }
    size_t excluded_names_count() const { return m_excluded_names_count; }
    Operand const* excluded_names() const { return m_excluded_names; }

private:
    Operand m_dst;
    Operand m_from_object;
    size_t m_excluded_names_count { 0 };
    Operand m_excluded_names[];
};

// NOTE: This instruction is variable-width depending on the number of elements!
class NewArray final : public Instruction {
public:
    static constexpr bool IsVariableLength = true;

    explicit NewArray(Operand dst)
        : Instruction(Type::NewArray)
        , m_dst(dst)
        , m_element_count(0)
    {
    }

    NewArray(Operand dst, ReadonlySpan<ScopedOperand> elements)
        : Instruction(Type::NewArray)
        , m_dst(dst)
        , m_element_count(elements.size())
    {
        for (size_t i = 0; i < m_element_count; ++i)
            m_elements[i] = elements[i];
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand dst() const { return m_dst; }

    size_t length_impl() const
    {
        return round_up_to_power_of_two(alignof(void*), sizeof(*this) + sizeof(Operand) * m_element_count);
    }

    size_t element_count() const { return m_element_count; }

private:
    Operand m_dst;
    size_t m_element_count { 0 };
    Operand m_elements[];
};

class NewPrimitiveArray final : public Instruction {
public:
    static constexpr bool IsVariableLength = true;

    NewPrimitiveArray(Operand dst, ReadonlySpan<Value> elements)
        : Instruction(Type::NewPrimitiveArray)
        , m_dst(dst)
        , m_element_count(elements.size())
    {
        for (size_t i = 0; i < m_element_count; ++i)
            m_elements[i] = elements[i];
    }

    size_t length_impl() const
    {
        return round_up_to_power_of_two(alignof(void*), sizeof(*this) + sizeof(Value) * m_element_count);
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand dst() const { return m_dst; }
    ReadonlySpan<Value> elements() const { return { m_elements, m_element_count }; }

private:
    Operand m_dst;
    size_t m_element_count { 0 };
    Value m_elements[];
};

class ArrayAppend final : public Instruction {
public:
    ArrayAppend(Operand dst, Operand src, bool is_spread)
        : Instruction(Type::ArrayAppend)
        , m_dst(dst)
        , m_src(src)
        , m_is_spread(is_spread)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand dst() const { return m_dst; }
    Operand src() const { return m_src; }
    bool is_spread() const { return m_is_spread; }

private:
    Operand m_dst;
    Operand m_src;
    bool m_is_spread = false;
};

class ImportCall final : public Instruction {
public:
    ImportCall(Operand dst, Operand specifier, Operand options)
        : Instruction(Type::ImportCall)
        , m_dst(dst)
        , m_specifier(specifier)
        , m_options(options)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand dst() const { return m_dst; }
    Operand specifier() const { return m_specifier; }
    Operand options() const { return m_options; }

private:
    Operand m_dst;
    Operand m_specifier;
    Operand m_options;
};

class IteratorToArray final : public Instruction {
public:
    explicit IteratorToArray(Operand dst, Operand iterator)
        : Instruction(Type::IteratorToArray)
        , m_dst(dst)
        , m_iterator(iterator)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand dst() const { return m_dst; }
    Operand iterator() const { return m_iterator; }

private:
    Operand m_dst;
    Operand m_iterator;
};

class ConcatString final : public Instruction {
public:
    explicit ConcatString(Operand dst, Operand src)
        : Instruction(Type::ConcatString)
        , m_dst(dst)
        , m_src(src)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand dst() const { return m_dst; }
    Operand src() const { return m_src; }

private:
    Operand m_dst;
    Operand m_src;
};

enum class EnvironmentMode {
    Lexical,
    Var,
};

class CreateLexicalEnvironment final : public Instruction {
public:
    explicit CreateLexicalEnvironment()
        : Instruction(Type::CreateLexicalEnvironment)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;
};

class EnterObjectEnvironment final : public Instruction {
public:
    explicit EnterObjectEnvironment(Operand object)
        : Instruction(Type::EnterObjectEnvironment)
        , m_object(object)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand object() const { return m_object; }

private:
    Operand m_object;
};

class Catch final : public Instruction {
public:
    explicit Catch(Operand dst)
        : Instruction(Type::Catch)
        , m_dst(dst)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand dst() const { return m_dst; }

private:
    Operand m_dst;
};

class LeaveFinally final : public Instruction {
public:
    explicit LeaveFinally()
        : Instruction(Type::LeaveFinally)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;
};

class RestoreScheduledJump final : public Instruction {
public:
    explicit RestoreScheduledJump()
        : Instruction(Type::RestoreScheduledJump)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;
};

class CreateVariable final : public Instruction {
public:
    explicit CreateVariable(IdentifierTableIndex identifier, EnvironmentMode mode, bool is_immutable, bool is_global = false, bool is_strict = false)
        : Instruction(Type::CreateVariable)
        , m_identifier(identifier)
        , m_mode(mode)
        , m_is_immutable(is_immutable)
        , m_is_global(is_global)
        , m_is_strict(is_strict)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    IdentifierTableIndex identifier() const { return m_identifier; }
    EnvironmentMode mode() const { return m_mode; }
    bool is_immutable() const { return m_is_immutable; }
    bool is_global() const { return m_is_global; }
    bool is_strict() const { return m_is_strict; }

private:
    IdentifierTableIndex m_identifier;
    EnvironmentMode m_mode;
    bool m_is_immutable : 4 { false };
    bool m_is_global : 4 { false };
    bool m_is_strict { false };
};

class SetVariable final : public Instruction {
public:
    enum class InitializationMode {
        Initialize,
        Set,
    };
    explicit SetVariable(IdentifierTableIndex identifier, Operand src, u32 cache_index, InitializationMode initialization_mode = InitializationMode::Set, EnvironmentMode mode = EnvironmentMode::Lexical)
        : Instruction(Type::SetVariable)
        , m_identifier(identifier)
        , m_src(src)
        , m_mode(mode)
        , m_initialization_mode(initialization_mode)
        , m_cache_index(cache_index)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    IdentifierTableIndex identifier() const { return m_identifier; }
    Operand src() const { return m_src; }
    EnvironmentMode mode() const { return m_mode; }
    InitializationMode initialization_mode() const { return m_initialization_mode; }
    u32 cache_index() const { return m_cache_index; }

private:
    IdentifierTableIndex m_identifier;
    Operand m_src;
    EnvironmentMode m_mode;
    InitializationMode m_initialization_mode { InitializationMode::Set };
    u32 m_cache_index { 0 };
};

class SetLocal final : public Instruction {
public:
    SetLocal(size_t index, Operand src)
        : Instruction(Type::SetLocal)
        , m_index(index)
        , m_src(src)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    u32 index() const { return m_index; }
    Operand dst() const { return Operand(Operand::Type::Local, m_index); }
    Operand src() const { return m_src; }

private:
    u32 m_index;
    Operand m_src;
};

class GetCalleeAndThisFromEnvironment final : public Instruction {
public:
    explicit GetCalleeAndThisFromEnvironment(Operand callee, Operand this_value, IdentifierTableIndex identifier, u32 cache_index)
        : Instruction(Type::GetCalleeAndThisFromEnvironment)
        , m_identifier(identifier)
        , m_callee(callee)
        , m_this_value(this_value)
        , m_cache_index(cache_index)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    IdentifierTableIndex identifier() const { return m_identifier; }
    u32 cache_index() const { return m_cache_index; }
    Operand callee() const { return m_callee; }
    Operand this_() const { return m_this_value; }

private:
    IdentifierTableIndex m_identifier;
    Operand m_callee;
    Operand m_this_value;
    u32 m_cache_index { 0 };
};

class GetVariable final : public Instruction {
public:
    explicit GetVariable(Operand dst, IdentifierTableIndex identifier, u32 cache_index)
        : Instruction(Type::GetVariable)
        , m_dst(dst)
        , m_identifier(identifier)
        , m_cache_index(cache_index)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand dst() const { return m_dst; }
    IdentifierTableIndex identifier() const { return m_identifier; }
    u32 cache_index() const { return m_cache_index; }

private:
    Operand m_dst;
    IdentifierTableIndex m_identifier;
    u32 m_cache_index { 0 };
};

class GetGlobal final : public Instruction {
public:
    GetGlobal(Operand dst, IdentifierTableIndex identifier, u32 cache_index)
        : Instruction(Type::GetGlobal)
        , m_dst(dst)
        , m_identifier(identifier)
        , m_cache_index(cache_index)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand dst() const { return m_dst; }
    IdentifierTableIndex identifier() const { return m_identifier; }
    u32 cache_index() const { return m_cache_index; }

private:
    Operand m_dst;
    IdentifierTableIndex m_identifier;
    u32 m_cache_index { 0 };
};

class DeleteVariable final : public Instruction {
public:
    explicit DeleteVariable(Operand dst, IdentifierTableIndex identifier)
        : Instruction(Type::DeleteVariable)
        , m_dst(dst)
        , m_identifier(identifier)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand dst() const { return m_dst; }
    IdentifierTableIndex identifier() const { return m_identifier; }

private:
    Operand m_dst;
    IdentifierTableIndex m_identifier;
};

class GetById final : public Instruction {
public:
    GetById(Operand dst, Operand base, IdentifierTableIndex property, Optional<IdentifierTableIndex> base_identifier, u32 cache_index)
        : Instruction(Type::GetById)
        , m_dst(dst)
        , m_base(base)
        , m_property(property)
        , m_base_identifier(move(base_identifier))
        , m_cache_index(cache_index)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand dst() const { return m_dst; }
    Operand base() const { return m_base; }
    IdentifierTableIndex property() const { return m_property; }
    u32 cache_index() const { return m_cache_index; }

private:
    Operand m_dst;
    Operand m_base;
    IdentifierTableIndex m_property;
    Optional<IdentifierTableIndex> m_base_identifier;
    u32 m_cache_index { 0 };
};

class GetByIdWithThis final : public Instruction {
public:
    GetByIdWithThis(Operand dst, Operand base, IdentifierTableIndex property, Operand this_value, u32 cache_index)
        : Instruction(Type::GetByIdWithThis)
        , m_dst(dst)
        , m_base(base)
        , m_property(property)
        , m_this_value(this_value)
        , m_cache_index(cache_index)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand dst() const { return m_dst; }
    Operand base() const { return m_base; }
    IdentifierTableIndex property() const { return m_property; }
    Operand this_value() const { return m_this_value; }
    u32 cache_index() const { return m_cache_index; }

private:
    Operand m_dst;
    Operand m_base;
    IdentifierTableIndex m_property;
    Operand m_this_value;
    u32 m_cache_index { 0 };
};

class GetPrivateById final : public Instruction {
public:
    explicit GetPrivateById(Operand dst, Operand base, IdentifierTableIndex property)
        : Instruction(Type::GetPrivateById)
        , m_dst(dst)
        , m_base(base)
        , m_property(property)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand dst() const { return m_dst; }
    Operand base() const { return m_base; }
    IdentifierTableIndex property() const { return m_property; }

private:
    Operand m_dst;
    Operand m_base;
    IdentifierTableIndex m_property;
};

class HasPrivateId final : public Instruction {
public:
    HasPrivateId(Operand dst, Operand base, IdentifierTableIndex property)
        : Instruction(Type::HasPrivateId)
        , m_dst(dst)
        , m_base(base)
        , m_property(property)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand dst() const { return m_dst; }
    Operand base() const { return m_base; }
    IdentifierTableIndex property() const { return m_property; }

private:
    Operand m_dst;
    Operand m_base;
    IdentifierTableIndex m_property;
};

enum class PropertyKind {
    Getter,
    Setter,
    KeyValue,
    DirectKeyValue, // Used for Object expressions. Always sets an own property, never calls a setter.
    Spread,
    ProtoSetter,
};

class PutById final : public Instruction {
public:
    explicit PutById(Operand base, IdentifierTableIndex property, Operand src, PropertyKind kind, u32 cache_index, Optional<IdentifierTableIndex> base_identifier = {})
        : Instruction(Type::PutById)
        , m_base(base)
        , m_property(property)
        , m_src(src)
        , m_kind(kind)
        , m_cache_index(cache_index)
        , m_base_identifier(move(base_identifier))
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand base() const { return m_base; }
    IdentifierTableIndex property() const { return m_property; }
    Operand src() const { return m_src; }
    PropertyKind kind() const { return m_kind; }
    u32 cache_index() const { return m_cache_index; }

private:
    Operand m_base;
    IdentifierTableIndex m_property;
    Operand m_src;
    PropertyKind m_kind;
    u32 m_cache_index { 0 };
    Optional<IdentifierTableIndex> m_base_identifier {};
};

class PutByIdWithThis final : public Instruction {
public:
    PutByIdWithThis(Operand base, Operand this_value, IdentifierTableIndex property, Operand src, PropertyKind kind, u32 cache_index)
        : Instruction(Type::PutByIdWithThis)
        , m_base(base)
        , m_this_value(this_value)
        , m_property(property)
        , m_src(src)
        , m_kind(kind)
        , m_cache_index(cache_index)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand base() const { return m_base; }
    Operand this_value() const { return m_this_value; }
    IdentifierTableIndex property() const { return m_property; }
    Operand src() const { return m_src; }
    PropertyKind kind() const { return m_kind; }
    u32 cache_index() const { return m_cache_index; }

private:
    Operand m_base;
    Operand m_this_value;
    IdentifierTableIndex m_property;
    Operand m_src;
    PropertyKind m_kind;
    u32 m_cache_index { 0 };
};

class PutPrivateById final : public Instruction {
public:
    explicit PutPrivateById(Operand base, IdentifierTableIndex property, Operand src, PropertyKind kind = PropertyKind::KeyValue)
        : Instruction(Type::PutPrivateById)
        , m_base(base)
        , m_property(property)
        , m_src(src)
        , m_kind(kind)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand base() const { return m_base; }
    IdentifierTableIndex property() const { return m_property; }
    Operand src() const { return m_src; }

private:
    Operand m_base;
    IdentifierTableIndex m_property;
    Operand m_src;
    PropertyKind m_kind;
};

class DeleteById final : public Instruction {
public:
    explicit DeleteById(Operand dst, Operand base, IdentifierTableIndex property)
        : Instruction(Type::DeleteById)
        , m_dst(dst)
        , m_base(base)
        , m_property(property)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand dst() const { return m_dst; }
    Operand base() const { return m_base; }
    IdentifierTableIndex property() const { return m_property; }

private:
    Operand m_dst;
    Operand m_base;
    IdentifierTableIndex m_property;
};

class DeleteByIdWithThis final : public Instruction {
public:
    DeleteByIdWithThis(Operand dst, Operand base, Operand this_value, IdentifierTableIndex property)
        : Instruction(Type::DeleteByIdWithThis)
        , m_dst(dst)
        , m_base(base)
        , m_this_value(this_value)
        , m_property(property)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand dst() const { return m_dst; }
    Operand base() const { return m_base; }
    Operand this_value() const { return m_this_value; }
    IdentifierTableIndex property() const { return m_property; }

private:
    Operand m_dst;
    Operand m_base;
    Operand m_this_value;
    IdentifierTableIndex m_property;
};

class GetByValue final : public Instruction {
public:
    GetByValue(Operand dst, Operand base, Operand property, Optional<IdentifierTableIndex> base_identifier = {})
        : Instruction(Type::GetByValue)
        , m_dst(dst)
        , m_base(base)
        , m_property(property)
        , m_base_identifier(move(base_identifier))
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand dst() const { return m_dst; }
    Operand base() const { return m_base; }
    Operand property() const { return m_property; }

    Optional<DeprecatedFlyString const&> base_identifier(Bytecode::Interpreter const&) const;

private:
    Operand m_dst;
    Operand m_base;
    Operand m_property;
    Optional<IdentifierTableIndex> m_base_identifier;
};

class GetByValueWithThis final : public Instruction {
public:
    GetByValueWithThis(Operand dst, Operand base, Operand property, Operand this_value)
        : Instruction(Type::GetByValueWithThis)
        , m_dst(dst)
        , m_base(base)
        , m_property(property)
        , m_this_value(this_value)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand dst() const { return m_dst; }
    Operand base() const { return m_base; }
    Operand property() const { return m_property; }
    Operand this_value() const { return m_this_value; }

private:
    Operand m_dst;
    Operand m_base;
    Operand m_property;
    Operand m_this_value;
};

class PutByValue final : public Instruction {
public:
    PutByValue(Operand base, Operand property, Operand src, PropertyKind kind = PropertyKind::KeyValue, Optional<IdentifierTableIndex> base_identifier = {})
        : Instruction(Type::PutByValue)
        , m_base(base)
        , m_property(property)
        , m_src(src)
        , m_kind(kind)
        , m_base_identifier(move(base_identifier))
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand base() const { return m_base; }
    Operand property() const { return m_property; }
    Operand src() const { return m_src; }
    PropertyKind kind() const { return m_kind; }

private:
    Operand m_base;
    Operand m_property;
    Operand m_src;
    PropertyKind m_kind;
    Optional<IdentifierTableIndex> m_base_identifier;
};

class PutByValueWithThis final : public Instruction {
public:
    PutByValueWithThis(Operand base, Operand property, Operand this_value, Operand src, PropertyKind kind = PropertyKind::KeyValue)
        : Instruction(Type::PutByValueWithThis)
        , m_base(base)
        , m_property(property)
        , m_this_value(this_value)
        , m_src(src)
        , m_kind(kind)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand base() const { return m_base; }
    Operand property() const { return m_property; }
    Operand this_value() const { return m_this_value; }
    Operand src() const { return m_src; }
    PropertyKind kind() const { return m_kind; }

private:
    Operand m_base;
    Operand m_property;
    Operand m_this_value;
    Operand m_src;
    PropertyKind m_kind;
};

class DeleteByValue final : public Instruction {
public:
    DeleteByValue(Operand dst, Operand base, Operand property)
        : Instruction(Type::DeleteByValue)
        , m_dst(dst)
        , m_base(base)
        , m_property(property)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand dst() const { return m_dst; }
    Operand base() const { return m_base; }
    Operand property() const { return m_property; }

private:
    Operand m_dst;
    Operand m_base;
    Operand m_property;
};

class DeleteByValueWithThis final : public Instruction {
public:
    DeleteByValueWithThis(Operand dst, Operand base, Operand this_value, Operand property)
        : Instruction(Type::DeleteByValueWithThis)
        , m_dst(dst)
        , m_base(base)
        , m_this_value(this_value)
        , m_property(property)
    {
    }

    Operand dst() const { return m_dst; }
    Operand base() const { return m_base; }
    Operand this_value() const { return m_this_value; }
    Operand property() const { return m_property; }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

private:
    Operand m_dst;
    Operand m_base;
    Operand m_this_value;
    Operand m_property;
};

class Jump final : public Instruction {
public:
    constexpr static bool IsTerminator = true;

    explicit Jump(Label target)
        : Instruction(Type::Jump)
        , m_target(target)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;
    void visit_labels_impl(Function<void(Label&)> visitor)
    {
        visitor(m_target);
    }

    auto& target() const { return m_target; }

protected:
    Label m_target;
};

class JumpIf final : public Instruction {
public:
    constexpr static bool IsTerminator = true;

    explicit JumpIf(Operand condition, Label true_target, Label false_target)
        : Instruction(Type::JumpIf)
        , m_condition(condition)
        , m_true_target(true_target)
        , m_false_target(false_target)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;
    void visit_labels_impl(Function<void(Label&)> visitor)
    {
        visitor(m_true_target);
        visitor(m_false_target);
    }

    Operand condition() const { return m_condition; }
    auto& true_target() const { return m_true_target; }
    auto& false_target() const { return m_false_target; }

private:
    Operand m_condition;
    Label m_true_target;
    Label m_false_target;
};

class JumpTrue final : public Instruction {
public:
    constexpr static bool IsTerminator = true;

    explicit JumpTrue(Operand condition, Label target)
        : Instruction(Type::JumpTrue)
        , m_condition(condition)
        , m_target(target)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;
    void visit_labels_impl(Function<void(Label&)> visitor)
    {
        visitor(m_target);
    }

    Operand condition() const { return m_condition; }
    auto& target() const { return m_target; }

private:
    Operand m_condition;
    Label m_target;
};

class JumpFalse final : public Instruction {
public:
    constexpr static bool IsTerminator = true;

    explicit JumpFalse(Operand condition, Label target)
        : Instruction(Type::JumpFalse)
        , m_condition(condition)
        , m_target(target)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;
    void visit_labels_impl(Function<void(Label&)> visitor)
    {
        visitor(m_target);
    }

    Operand condition() const { return m_condition; }
    auto& target() const { return m_target; }

private:
    Operand m_condition;
    Label m_target;
};

class JumpNullish final : public Instruction {
public:
    constexpr static bool IsTerminator = true;

    explicit JumpNullish(Operand condition, Label true_target, Label false_target)
        : Instruction(Type::JumpNullish)
        , m_condition(condition)
        , m_true_target(true_target)
        , m_false_target(false_target)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;
    void visit_labels_impl(Function<void(Label&)> visitor)
    {
        visitor(m_true_target);
        visitor(m_false_target);
    }

    Operand condition() const { return m_condition; }
    auto& true_target() const { return m_true_target; }
    auto& false_target() const { return m_false_target; }

private:
    Operand m_condition;
    Label m_true_target;
    Label m_false_target;
};

class JumpUndefined final : public Instruction {
public:
    constexpr static bool IsTerminator = true;

    explicit JumpUndefined(Operand condition, Label true_target, Label false_target)
        : Instruction(Type::JumpUndefined)
        , m_condition(condition)
        , m_true_target(true_target)
        , m_false_target(false_target)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;
    void visit_labels_impl(Function<void(Label&)> visitor)
    {
        visitor(m_true_target);
        visitor(m_false_target);
    }

    Operand condition() const { return m_condition; }
    auto& true_target() const { return m_true_target; }
    auto& false_target() const { return m_false_target; }

private:
    Operand m_condition;
    Label m_true_target;
    Label m_false_target;
};

enum class CallType {
    Call,
    Construct,
    DirectEval,
};

class Call final : public Instruction {
public:
    static constexpr bool IsVariableLength = true;

    Call(CallType type, Operand dst, Operand callee, Operand this_value, ReadonlySpan<ScopedOperand> arguments, Optional<StringTableIndex> expression_string = {}, Optional<Builtin> builtin = {})
        : Instruction(Type::Call)
        , m_dst(dst)
        , m_callee(callee)
        , m_this_value(this_value)
        , m_argument_count(arguments.size())
        , m_type(type)
        , m_expression_string(expression_string)
        , m_builtin(builtin)
    {
        for (size_t i = 0; i < arguments.size(); ++i)
            m_arguments[i] = arguments[i];
    }

    size_t length_impl() const
    {
        return round_up_to_power_of_two(alignof(void*), sizeof(*this) + sizeof(Operand) * m_argument_count);
    }

    CallType call_type() const { return m_type; }
    Operand dst() const { return m_dst; }
    Operand callee() const { return m_callee; }
    Operand this_value() const { return m_this_value; }
    Optional<StringTableIndex> const& expression_string() const { return m_expression_string; }

    u32 argument_count() const { return m_argument_count; }

    Optional<Builtin> const& builtin() const { return m_builtin; }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

private:
    Operand m_dst;
    Operand m_callee;
    Operand m_this_value;
    u32 m_argument_count { 0 };
    CallType m_type;
    Optional<StringTableIndex> m_expression_string;
    Optional<Builtin> m_builtin;
    Operand m_arguments[];
};

class CallWithArgumentArray final : public Instruction {
public:
    CallWithArgumentArray(CallType type, Operand dst, Operand callee, Operand this_value, Operand arguments, Optional<StringTableIndex> expression_string = {})
        : Instruction(Type::CallWithArgumentArray)
        , m_dst(dst)
        , m_callee(callee)
        , m_this_value(this_value)
        , m_arguments(arguments)
        , m_type(type)
        , m_expression_string(expression_string)
    {
    }

    Operand dst() const { return m_dst; }
    CallType call_type() const { return m_type; }
    Operand callee() const { return m_callee; }
    Operand this_value() const { return m_this_value; }
    Operand arguments() const { return m_arguments; }
    Optional<StringTableIndex> const& expression_string() const { return m_expression_string; }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

private:
    Operand m_dst;
    Operand m_callee;
    Operand m_this_value;
    Operand m_arguments;
    CallType m_type;
    Optional<StringTableIndex> m_expression_string;
};

class SuperCallWithArgumentArray : public Instruction {
public:
    explicit SuperCallWithArgumentArray(Operand dst, Operand arguments, bool is_synthetic)
        : Instruction(Type::SuperCallWithArgumentArray)
        , m_dst(dst)
        , m_arguments(arguments)
        , m_is_synthetic(is_synthetic)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand dst() const { return m_dst; }
    Operand arguments() const { return m_arguments; }
    bool is_synthetic() const { return m_is_synthetic; }

private:
    Operand m_dst;
    Operand m_arguments;
    bool m_is_synthetic;
};

class NewClass final : public Instruction {
public:
    explicit NewClass(Operand dst, Optional<Operand> super_class, ClassExpression const& class_expression, Optional<IdentifierTableIndex> lhs_name)
        : Instruction(Type::NewClass)
        , m_dst(dst)
        , m_super_class(super_class)
        , m_class_expression(class_expression)
        , m_lhs_name(lhs_name)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand dst() const { return m_dst; }
    Optional<Operand> const& super_class() const { return m_super_class; }
    ClassExpression const& class_expression() const { return m_class_expression; }
    Optional<IdentifierTableIndex> const& lhs_name() const { return m_lhs_name; }

private:
    Operand m_dst;
    Optional<Operand> m_super_class;
    ClassExpression const& m_class_expression;
    Optional<IdentifierTableIndex> m_lhs_name;
};

class NewFunction final : public Instruction {
public:
    explicit NewFunction(Operand dst, FunctionExpression const& function_node, Optional<IdentifierTableIndex> lhs_name, Optional<Operand> home_object = {})
        : Instruction(Type::NewFunction)
        , m_dst(dst)
        , m_function_node(function_node)
        , m_lhs_name(lhs_name)
        , m_home_object(move(home_object))
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand dst() const { return m_dst; }
    FunctionExpression const& function_node() const { return m_function_node; }
    Optional<IdentifierTableIndex> const& lhs_name() const { return m_lhs_name; }
    Optional<Operand> const& home_object() const { return m_home_object; }

private:
    Operand m_dst;
    FunctionExpression const& m_function_node;
    Optional<IdentifierTableIndex> m_lhs_name;
    Optional<Operand> m_home_object;
};

class BlockDeclarationInstantiation final : public Instruction {
public:
    explicit BlockDeclarationInstantiation(ScopeNode const& scope_node)
        : Instruction(Type::BlockDeclarationInstantiation)
        , m_scope_node(scope_node)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    ScopeNode const& scope_node() const { return m_scope_node; }

private:
    ScopeNode const& m_scope_node;
};

class Return final : public Instruction {
public:
    constexpr static bool IsTerminator = true;

    explicit Return(Optional<Operand> value = {})
        : Instruction(Type::Return)
        , m_value(value)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Optional<Operand> const& value() const { return m_value; }

private:
    Optional<Operand> m_value;
};

class Increment final : public Instruction {
public:
    explicit Increment(Operand dst)
        : Instruction(Type::Increment)
        , m_dst(dst)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand dst() const { return m_dst; }

private:
    Operand m_dst;
};

class PostfixIncrement final : public Instruction {
public:
    explicit PostfixIncrement(Operand dst, Operand src)
        : Instruction(Type::PostfixIncrement)
        , m_dst(dst)
        , m_src(src)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand dst() const { return m_dst; }
    Operand src() const { return m_src; }

private:
    Operand m_dst;
    Operand m_src;
};

class Decrement final : public Instruction {
public:
    explicit Decrement(Operand dst)
        : Instruction(Type::Decrement)
        , m_dst(dst)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand dst() const { return m_dst; }

private:
    Operand m_dst;
};

class PostfixDecrement final : public Instruction {
public:
    explicit PostfixDecrement(Operand dst, Operand src)
        : Instruction(Type::PostfixDecrement)
        , m_dst(dst)
        , m_src(src)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand dst() const { return m_dst; }
    Operand src() const { return m_src; }

private:
    Operand m_dst;
    Operand m_src;
};

class Throw final : public Instruction {
public:
    constexpr static bool IsTerminator = true;

    explicit Throw(Operand src)
        : Instruction(Type::Throw)
        , m_src(src)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand src() const { return m_src; }

private:
    Operand m_src;
};

class ThrowIfNotObject final : public Instruction {
public:
    ThrowIfNotObject(Operand src)
        : Instruction(Type::ThrowIfNotObject)
        , m_src(src)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand src() const { return m_src; }

private:
    Operand m_src;
};

class ThrowIfNullish final : public Instruction {
public:
    explicit ThrowIfNullish(Operand src)
        : Instruction(Type::ThrowIfNullish)
        , m_src(src)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand src() const { return m_src; }

private:
    Operand m_src;
};

class ThrowIfTDZ final : public Instruction {
public:
    explicit ThrowIfTDZ(Operand src)
        : Instruction(Type::ThrowIfTDZ)
        , m_src(src)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand src() const { return m_src; }

private:
    Operand m_src;
};

class EnterUnwindContext final : public Instruction {
public:
    constexpr static bool IsTerminator = true;

    EnterUnwindContext(Label entry_point)
        : Instruction(Type::EnterUnwindContext)
        , m_entry_point(move(entry_point))
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;
    void visit_labels_impl(Function<void(Label&)> visitor)
    {
        visitor(m_entry_point);
    }

    auto& entry_point() const { return m_entry_point; }

private:
    Label m_entry_point;
};

class ScheduleJump final : public Instruction {
public:
    // Note: We use this instruction to tell the next `finally` block to
    //       continue execution with a specific break/continue target;
    constexpr static bool IsTerminator = true;

    ScheduleJump(Label target)
        : Instruction(Type::ScheduleJump)
        , m_target(target)
    {
    }

    Label target() const { return m_target; }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;
    void visit_labels_impl(Function<void(Label&)> visitor)
    {
        visitor(m_target);
    }

private:
    Label m_target;
};

class LeaveLexicalEnvironment final : public Instruction {
public:
    LeaveLexicalEnvironment()
        : Instruction(Type::LeaveLexicalEnvironment)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;
};

class LeaveUnwindContext final : public Instruction {
public:
    LeaveUnwindContext()
        : Instruction(Type::LeaveUnwindContext)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;
};

class ContinuePendingUnwind final : public Instruction {
public:
    constexpr static bool IsTerminator = true;

    explicit ContinuePendingUnwind(Label resume_target)
        : Instruction(Type::ContinuePendingUnwind)
        , m_resume_target(resume_target)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;
    void visit_labels_impl(Function<void(Label&)> visitor)
    {
        visitor(m_resume_target);
    }

    auto& resume_target() const { return m_resume_target; }

private:
    Label m_resume_target;
};

class Yield final : public Instruction {
public:
    constexpr static bool IsTerminator = true;

    explicit Yield(Label continuation_label, Operand value)
        : Instruction(Type::Yield)
        , m_continuation_label(continuation_label)
        , m_value(value)
    {
    }

    explicit Yield(nullptr_t, Operand value)
        : Instruction(Type::Yield)
        , m_value(value)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;
    void visit_labels_impl(Function<void(Label&)> visitor)
    {
        if (m_continuation_label.has_value())
            visitor(m_continuation_label.value());
    }

    auto& continuation() const { return m_continuation_label; }
    Operand value() const { return m_value; }

private:
    Optional<Label> m_continuation_label;
    Operand m_value;
};

class Await final : public Instruction {
public:
    constexpr static bool IsTerminator = true;

    explicit Await(Label continuation_label, Operand argument)
        : Instruction(Type::Await)
        , m_continuation_label(continuation_label)
        , m_argument(argument)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;
    void visit_labels_impl(Function<void(Label&)> visitor)
    {
        visitor(m_continuation_label);
    }

    auto& continuation() const { return m_continuation_label; }
    Operand argument() const { return m_argument; }

private:
    Label m_continuation_label;
    Operand m_argument;
};

class GetIterator final : public Instruction {
public:
    GetIterator(Operand dst, Operand iterable, IteratorHint hint = IteratorHint::Sync)
        : Instruction(Type::GetIterator)
        , m_dst(dst)
        , m_iterable(iterable)
        , m_hint(hint)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand dst() const { return m_dst; }
    Operand iterable() const { return m_iterable; }
    IteratorHint hint() const { return m_hint; }

private:
    Operand m_dst;
    Operand m_iterable;
    IteratorHint m_hint { IteratorHint::Sync };
};

class GetObjectFromIteratorRecord final : public Instruction {
public:
    GetObjectFromIteratorRecord(Operand object, Operand iterator_record)
        : Instruction(Type::GetObjectFromIteratorRecord)
        , m_object(object)
        , m_iterator_record(iterator_record)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand object() const { return m_object; }
    Operand iterator_record() const { return m_iterator_record; }

private:
    Operand m_object;
    Operand m_iterator_record;
};

class GetNextMethodFromIteratorRecord final : public Instruction {
public:
    GetNextMethodFromIteratorRecord(Operand next_method, Operand iterator_record)
        : Instruction(Type::GetNextMethodFromIteratorRecord)
        , m_next_method(next_method)
        , m_iterator_record(iterator_record)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand next_method() const { return m_next_method; }
    Operand iterator_record() const { return m_iterator_record; }

private:
    Operand m_next_method;
    Operand m_iterator_record;
};

class GetMethod final : public Instruction {
public:
    GetMethod(Operand dst, Operand object, IdentifierTableIndex property)
        : Instruction(Type::GetMethod)
        , m_dst(dst)
        , m_object(object)
        , m_property(property)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand dst() const { return m_dst; }
    Operand object() const { return m_object; }
    IdentifierTableIndex property() const { return m_property; }

private:
    Operand m_dst;
    Operand m_object;
    IdentifierTableIndex m_property;
};

class GetObjectPropertyIterator final : public Instruction {
public:
    GetObjectPropertyIterator(Operand dst, Operand object)
        : Instruction(Type::GetObjectPropertyIterator)
        , m_dst(dst)
        , m_object(object)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand dst() const { return m_dst; }
    Operand object() const { return m_object; }

private:
    Operand m_dst;
    Operand m_object;
};

class IteratorClose final : public Instruction {
public:
    IteratorClose(Operand iterator_record, Completion::Type completion_type, Optional<Value> completion_value)
        : Instruction(Type::IteratorClose)
        , m_iterator_record(iterator_record)
        , m_completion_type(completion_type)
        , m_completion_value(completion_value)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand iterator_record() const { return m_iterator_record; }
    Completion::Type completion_type() const { return m_completion_type; }
    Optional<Value> const& completion_value() const { return m_completion_value; }

private:
    Operand m_iterator_record;
    Completion::Type m_completion_type { Completion::Type::Normal };
    Optional<Value> m_completion_value;
};

class AsyncIteratorClose final : public Instruction {
public:
    AsyncIteratorClose(Operand iterator_record, Completion::Type completion_type, Optional<Value> completion_value)
        : Instruction(Type::AsyncIteratorClose)
        , m_iterator_record(iterator_record)
        , m_completion_type(completion_type)
        , m_completion_value(completion_value)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand iterator_record() const { return m_iterator_record; }
    Completion::Type completion_type() const { return m_completion_type; }
    Optional<Value> const& completion_value() const { return m_completion_value; }

private:
    Operand m_iterator_record;
    Completion::Type m_completion_type { Completion::Type::Normal };
    Optional<Value> m_completion_value;
};

class IteratorNext final : public Instruction {
public:
    IteratorNext(Operand dst, Operand iterator_record)
        : Instruction(Type::IteratorNext)
        , m_dst(dst)
        , m_iterator_record(iterator_record)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand dst() const { return m_dst; }
    Operand iterator_record() const { return m_iterator_record; }

private:
    Operand m_dst;
    Operand m_iterator_record;
};

class ResolveThisBinding final : public Instruction {
public:
    explicit ResolveThisBinding(Operand dst)
        : Instruction(Type::ResolveThisBinding)
        , m_dst(dst)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand dst() const { return m_dst; }

private:
    Operand m_dst;
};

class ResolveSuperBase final : public Instruction {
public:
    explicit ResolveSuperBase(Operand dst)
        : Instruction(Type::ResolveSuperBase)
        , m_dst(dst)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand dst() const { return m_dst; }

private:
    Operand m_dst;
};

class GetNewTarget final : public Instruction {
public:
    explicit GetNewTarget(Operand dst)
        : Instruction(Type::GetNewTarget)
        , m_dst(dst)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand dst() const { return m_dst; }

private:
    Operand m_dst;
};

class GetImportMeta final : public Instruction {
public:
    explicit GetImportMeta(Operand dst)
        : Instruction(Type::GetImportMeta)
        , m_dst(dst)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand dst() const { return m_dst; }

private:
    Operand m_dst;
};

class TypeofVariable final : public Instruction {
public:
    TypeofVariable(Operand dst, IdentifierTableIndex identifier)
        : Instruction(Type::TypeofVariable)
        , m_dst(dst)
        , m_identifier(identifier)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand dst() const { return m_dst; }
    IdentifierTableIndex identifier() const { return m_identifier; }

private:
    Operand m_dst;
    IdentifierTableIndex m_identifier;
};

class End final : public Instruction {
public:
    constexpr static bool IsTerminator = true;

    explicit End(Operand value)
        : Instruction(Type::End)
        , m_value(value)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand value() const { return m_value; }

private:
    Operand m_value;
};

class Dump final : public Instruction {
public:
    explicit Dump(StringView text, Operand value)
        : Instruction(Type::Dump)
        , m_text(text)
        , m_value(value)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

private:
    StringView m_text;
    Operand m_value;
};

}

namespace JS::Bytecode {

ALWAYS_INLINE ThrowCompletionOr<void> Instruction::execute(Bytecode::Interpreter& interpreter) const
{
#define __BYTECODE_OP(op)       \
    case Instruction::Type::op: \
        return static_cast<Bytecode::Op::op const&>(*this).execute_impl(interpreter);

    switch (type()) {
        ENUMERATE_BYTECODE_OPS(__BYTECODE_OP)
    default:
        VERIFY_NOT_REACHED();
    }

#undef __BYTECODE_OP
}

}
