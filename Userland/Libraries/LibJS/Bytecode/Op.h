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
        : Instruction(Type::Mov, sizeof(*this))
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
            : Instruction(Type::OpTitleCase, sizeof(*this))                 \
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
            : Instruction(Type::OpTitleCase, sizeof(*this))                 \
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
        : Instruction(Type::NewObject, sizeof(*this))
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
        : Instruction(Type::NewRegExp, sizeof(*this))
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
            : Instruction(Type::New##ErrorName, sizeof(*this))              \
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
    CopyObjectExcludingProperties(Operand dst, Operand from_object, Vector<Operand> const& excluded_names)
        : Instruction(Type::CopyObjectExcludingProperties, length_impl(excluded_names.size()))
        , m_dst(dst)
        , m_from_object(from_object)
        , m_excluded_names_count(excluded_names.size())
    {
        for (size_t i = 0; i < m_excluded_names_count; i++)
            m_excluded_names[i] = excluded_names[i];
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    size_t length_impl(size_t excluded_names_count) const
    {
        return round_up_to_power_of_two(alignof(void*), sizeof(*this) + sizeof(Operand) * excluded_names_count);
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
    explicit NewArray(Operand dst)
        : Instruction(Type::NewArray, length_impl(0))
        , m_dst(dst)
        , m_element_count(0)
    {
    }

    NewArray(Operand dst, AK::Array<Operand, 2> const& elements_range)
        : Instruction(Type::NewArray, length_impl(elements_range[1].index() - elements_range[0].index() + 1))
        , m_dst(dst)
        , m_element_count(elements_range[1].index() - elements_range[0].index() + 1)
    {
        m_elements[0] = elements_range[0];
        m_elements[1] = elements_range[1];
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand dst() const { return m_dst; }

    size_t length_impl(size_t element_count) const
    {
        return round_up_to_power_of_two(alignof(void*), sizeof(*this) + sizeof(Operand) * (element_count == 0 ? 0 : 2));
    }

    Operand start() const
    {
        VERIFY(m_element_count);
        return m_elements[0];
    }

    Operand end() const
    {
        VERIFY(m_element_count);
        return m_elements[1];
    }

    size_t element_count() const { return m_element_count; }

private:
    Operand m_dst;
    size_t m_element_count { 0 };
    Operand m_elements[];
};

class NewPrimitiveArray final : public Instruction {
public:
    NewPrimitiveArray(Operand dst, ReadonlySpan<Value> elements)
        : Instruction(Type::NewPrimitiveArray, length_impl(elements.size()))
        , m_dst(dst)
        , m_element_count(elements.size())
    {
        for (size_t i = 0; i < m_element_count; ++i)
            m_elements[i] = elements[i];
    }

    size_t length_impl(size_t element_count) const
    {
        return round_up_to_power_of_two(alignof(void*), sizeof(*this) + sizeof(Value) * element_count);
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
        : Instruction(Type::ArrayAppend, sizeof(*this))
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
        : Instruction(Type::ImportCall, sizeof(*this))
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
        : Instruction(Type::IteratorToArray, sizeof(*this))
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
        : Instruction(Type::ConcatString, sizeof(*this))
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
        : Instruction(Type::CreateLexicalEnvironment, sizeof(*this))
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;
};

class EnterObjectEnvironment final : public Instruction {
public:
    explicit EnterObjectEnvironment(Operand object)
        : Instruction(Type::EnterObjectEnvironment, sizeof(*this))
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
        : Instruction(Type::Catch, sizeof(*this))
        , m_dst(dst)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand dst() const { return m_dst; }

private:
    Operand m_dst;
};

class CreateVariable final : public Instruction {
public:
    explicit CreateVariable(IdentifierTableIndex identifier, EnvironmentMode mode, bool is_immutable, bool is_global = false, bool is_strict = false)
        : Instruction(Type::CreateVariable, sizeof(*this))
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
        : Instruction(Type::SetVariable, sizeof(*this))
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
        : Instruction(Type::SetLocal, sizeof(*this))
        , m_index(index)
        , m_src(src)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    size_t index() const { return m_index; }
    Operand dst() const { return Operand(Operand::Type::Local, m_index); }
    Operand src() const { return m_src; }

private:
    size_t m_index;
    Operand m_src;
};

class GetCalleeAndThisFromEnvironment final : public Instruction {
public:
    explicit GetCalleeAndThisFromEnvironment(Operand callee, Operand this_value, IdentifierTableIndex identifier, u32 cache_index)
        : Instruction(Type::GetCalleeAndThisFromEnvironment, sizeof(*this))
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
        : Instruction(Type::GetVariable, sizeof(*this))
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
        : Instruction(Type::GetGlobal, sizeof(*this))
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
        : Instruction(Type::DeleteVariable, sizeof(*this))
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
        : Instruction(Type::GetById, sizeof(*this))
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
        : Instruction(Type::GetByIdWithThis, sizeof(*this))
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
        : Instruction(Type::GetPrivateById, sizeof(*this))
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
        : Instruction(Type::HasPrivateId, sizeof(*this))
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
        : Instruction(Type::PutById, sizeof(*this))
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
        : Instruction(Type::PutByIdWithThis, sizeof(*this))
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
        : Instruction(Type::PutPrivateById, sizeof(*this))
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
        : Instruction(Type::DeleteById, sizeof(*this))
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
        : Instruction(Type::DeleteByIdWithThis, sizeof(*this))
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
        : Instruction(Type::GetByValue, sizeof(*this))
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
        : Instruction(Type::GetByValueWithThis, sizeof(*this))
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
        : Instruction(Type::PutByValue, sizeof(*this))
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
        : Instruction(Type::PutByValueWithThis, sizeof(*this))
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
        : Instruction(Type::DeleteByValue, sizeof(*this))
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
        : Instruction(Type::DeleteByValueWithThis, sizeof(*this))
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

class Jump : public Instruction {
public:
    constexpr static bool IsTerminator = true;

    explicit Jump(Type type, Label taken_target, Optional<Label> nontaken_target = {})
        : Instruction(type, sizeof(*this))
        , m_true_target(move(taken_target))
        , m_false_target(move(nontaken_target))
    {
    }

    explicit Jump(Type type, Label taken_target, Label nontaken_target, size_t sizeof_self)
        : Instruction(type, sizeof_self)
        , m_true_target(move(taken_target))
        , m_false_target(move(nontaken_target))
    {
    }

    explicit Jump(Label taken_target, Optional<Label> nontaken_target = {})
        : Instruction(Type::Jump, sizeof(*this))
        , m_true_target(move(taken_target))
        , m_false_target(move(nontaken_target))
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    auto& true_target() const { return m_true_target; }
    auto& false_target() const { return m_false_target; }

protected:
    Optional<Label> m_true_target;
    Optional<Label> m_false_target;
};

class JumpIf final : public Jump {
public:
    explicit JumpIf(Operand condition, Label true_target, Label false_target)
        : Jump(Type::JumpIf, move(true_target), move(false_target), sizeof(*this))
        , m_condition(condition)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand condition() const { return m_condition; }

private:
    Operand m_condition;
};

class JumpNullish final : public Jump {
public:
    explicit JumpNullish(Operand condition, Label true_target, Label false_target)
        : Jump(Type::JumpNullish, move(true_target), move(false_target), sizeof(*this))
        , m_condition(condition)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand condition() const { return m_condition; }

private:
    Operand m_condition;
};

class JumpUndefined final : public Jump {
public:
    explicit JumpUndefined(Operand condition, Label true_target, Label false_target)
        : Jump(Type::JumpUndefined, move(true_target), move(false_target), sizeof(*this))
        , m_condition(condition)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    Operand condition() const { return m_condition; }

private:
    Operand m_condition;
};

enum class CallType {
    Call,
    Construct,
    DirectEval,
};

class Call final : public Instruction {
public:
    Call(CallType type, Operand dst, Operand callee, Operand this_value, ReadonlySpan<Operand> arguments, Optional<StringTableIndex> expression_string = {}, Optional<Builtin> builtin = {})
        : Instruction(Type::Call, length_impl(arguments.size()))
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

    size_t length_impl(size_t argument_count) const
    {
        return round_up_to_power_of_two(alignof(void*), sizeof(*this) + sizeof(Operand) * argument_count);
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
        : Instruction(Type::CallWithArgumentArray, sizeof(*this))
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
        : Instruction(Type::SuperCallWithArgumentArray, sizeof(*this))
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
        : Instruction(Type::NewClass, sizeof(*this))
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
        : Instruction(Type::NewFunction, sizeof(*this))
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
        : Instruction(Type::BlockDeclarationInstantiation, sizeof(*this))
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
        : Instruction(Type::Return, sizeof(*this))
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
        : Instruction(Type::Increment, sizeof(*this))
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
        : Instruction(Type::PostfixIncrement, sizeof(*this))
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
        : Instruction(Type::Decrement, sizeof(*this))
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
        : Instruction(Type::PostfixDecrement, sizeof(*this))
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
        : Instruction(Type::Throw, sizeof(*this))
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
        : Instruction(Type::ThrowIfNotObject, sizeof(*this))
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
        : Instruction(Type::ThrowIfNullish, sizeof(*this))
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
        : Instruction(Type::ThrowIfTDZ, sizeof(*this))
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
        : Instruction(Type::EnterUnwindContext, sizeof(*this))
        , m_entry_point(move(entry_point))
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    auto& entry_point() const { return m_entry_point; }

private:
    Label m_entry_point;
};

class ScheduleJump final : public Instruction {
public:
    // Note: We use this instruction to tell the next `finally` block to
    //       continue execution with a specific break/continue target;
    // FIXME: We currently don't clear the interpreter internal flag, when we change
    //        the control-flow (`break`, `continue`) in a finally-block,
    // FIXME: .NET on x86_64 uses a call to the finally instead, which could make this
    //        easier, at the cost of making control-flow changes (`break`, `continue`, `return`)
    //        in the finally-block more difficult, but as stated above, those
    //        aren't handled 100% correctly at the moment anyway
    //        It might be worth investigating a similar mechanism
    constexpr static bool IsTerminator = true;

    ScheduleJump(Label target)
        : Instruction(Type::ScheduleJump, sizeof(*this))
        , m_target(target)
    {
    }

    Label target() const { return m_target; }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

private:
    Label m_target;
};

class LeaveLexicalEnvironment final : public Instruction {
public:
    LeaveLexicalEnvironment()
        : Instruction(Type::LeaveLexicalEnvironment, sizeof(*this))
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;
};

class LeaveUnwindContext final : public Instruction {
public:
    LeaveUnwindContext()
        : Instruction(Type::LeaveUnwindContext, sizeof(*this))
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;
};

class ContinuePendingUnwind final : public Instruction {
public:
    constexpr static bool IsTerminator = true;

    explicit ContinuePendingUnwind(Label resume_target)
        : Instruction(Type::ContinuePendingUnwind, sizeof(*this))
        , m_resume_target(resume_target)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    auto& resume_target() const { return m_resume_target; }

private:
    Label m_resume_target;
};

class Yield final : public Instruction {
public:
    constexpr static bool IsTerminator = true;

    explicit Yield(Label continuation_label, Operand value)
        : Instruction(Type::Yield, sizeof(*this))
        , m_continuation_label(continuation_label)
        , m_value(value)
    {
    }

    explicit Yield(nullptr_t, Operand value)
        : Instruction(Type::Yield, sizeof(*this))
        , m_value(value)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

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
        : Instruction(Type::Await, sizeof(*this))
        , m_continuation_label(continuation_label)
        , m_argument(argument)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    ByteString to_byte_string_impl(Bytecode::Executable const&) const;

    auto& continuation() const { return m_continuation_label; }
    Operand argument() const { return m_argument; }

private:
    Label m_continuation_label;
    Operand m_argument;
};

class GetIterator final : public Instruction {
public:
    GetIterator(Operand dst, Operand iterable, IteratorHint hint = IteratorHint::Sync)
        : Instruction(Type::GetIterator, sizeof(*this))
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
        : Instruction(Type::GetObjectFromIteratorRecord, sizeof(*this))
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
        : Instruction(Type::GetNextMethodFromIteratorRecord, sizeof(*this))
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
        : Instruction(Type::GetMethod, sizeof(*this))
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
        : Instruction(Type::GetObjectPropertyIterator, sizeof(*this))
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
        : Instruction(Type::IteratorClose, sizeof(*this))
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
        : Instruction(Type::AsyncIteratorClose, sizeof(*this))
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
        : Instruction(Type::IteratorNext, sizeof(*this))
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
        : Instruction(Type::ResolveThisBinding, sizeof(*this))
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
        : Instruction(Type::ResolveSuperBase, sizeof(*this))
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
        : Instruction(Type::GetNewTarget, sizeof(*this))
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
        : Instruction(Type::GetImportMeta, sizeof(*this))
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
        : Instruction(Type::TypeofVariable, sizeof(*this))
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
        : Instruction(Type::End, sizeof(*this))
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
        : Instruction(Type::Dump, sizeof(*this))
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
