/*
 * Copyright (c) 2021-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StdLibExtras.h>
#include <LibCrypto/BigInt/SignedBigInteger.h>
#include <LibJS/Bytecode/IdentifierTable.h>
#include <LibJS/Bytecode/Instruction.h>
#include <LibJS/Bytecode/Label.h>
#include <LibJS/Bytecode/RegexTable.h>
#include <LibJS/Bytecode/Register.h>
#include <LibJS/Bytecode/StringTable.h>
#include <LibJS/Heap/Cell.h>
#include <LibJS/Runtime/Environment.h>
#include <LibJS/Runtime/EnvironmentCoordinate.h>
#include <LibJS/Runtime/Iterator.h>
#include <LibJS/Runtime/Value.h>
#include <LibJS/Runtime/ValueTraits.h>

namespace JS {
class FunctionExpression;
}

namespace JS::Bytecode::Op {

class Load final : public Instruction {
public:
    explicit Load(Register src)
        : Instruction(Type::Load, sizeof(*this))
        , m_src(src)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;

private:
    Register m_src;
};

class LoadImmediate final : public Instruction {
public:
    explicit LoadImmediate(Value value)
        : Instruction(Type::LoadImmediate, sizeof(*this))
        , m_value(value)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;

private:
    Value m_value;
};

class Store final : public Instruction {
public:
    explicit Store(Register dst)
        : Instruction(Type::Store, sizeof(*this))
        , m_dst(dst)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;

    Register dst() const { return m_dst; }

private:
    Register m_dst;
};

#define JS_ENUMERATE_COMMON_BINARY_OPS(O)     \
    O(Add, add)                               \
    O(Sub, sub)                               \
    O(Mul, mul)                               \
    O(Div, div)                               \
    O(Exp, exp)                               \
    O(Mod, mod)                               \
    O(In, in)                                 \
    O(InstanceOf, instance_of)                \
    O(GreaterThan, greater_than)              \
    O(GreaterThanEquals, greater_than_equals) \
    O(LessThan, less_than)                    \
    O(LessThanEquals, less_than_equals)       \
    O(LooselyInequals, abstract_inequals)     \
    O(LooselyEquals, abstract_equals)         \
    O(StrictlyInequals, typed_inequals)       \
    O(StrictlyEquals, typed_equals)           \
    O(BitwiseAnd, bitwise_and)                \
    O(BitwiseOr, bitwise_or)                  \
    O(BitwiseXor, bitwise_xor)                \
    O(LeftShift, left_shift)                  \
    O(RightShift, right_shift)                \
    O(UnsignedRightShift, unsigned_right_shift)

#define JS_DECLARE_COMMON_BINARY_OP(OpTitleCase, op_snake_case)                        \
    class OpTitleCase final : public Instruction {                                     \
    public:                                                                            \
        explicit OpTitleCase(Register lhs_reg)                                         \
            : Instruction(Type::OpTitleCase, sizeof(*this))                            \
            , m_lhs_reg(lhs_reg)                                                       \
        {                                                                              \
        }                                                                              \
                                                                                       \
        ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;            \
        DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const; \
                                                                                       \
    private:                                                                           \
        Register m_lhs_reg;                                                            \
    };

JS_ENUMERATE_COMMON_BINARY_OPS(JS_DECLARE_COMMON_BINARY_OP)
#undef JS_DECLARE_COMMON_BINARY_OP

#define JS_ENUMERATE_COMMON_UNARY_OPS(O) \
    O(BitwiseNot, bitwise_not)           \
    O(Not, not_)                         \
    O(UnaryPlus, unary_plus)             \
    O(UnaryMinus, unary_minus)           \
    O(Typeof, typeof_)

#define JS_DECLARE_COMMON_UNARY_OP(OpTitleCase, op_snake_case)                         \
    class OpTitleCase final : public Instruction {                                     \
    public:                                                                            \
        OpTitleCase()                                                                  \
            : Instruction(Type::OpTitleCase, sizeof(*this))                            \
        {                                                                              \
        }                                                                              \
                                                                                       \
        ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;            \
        DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const; \
    };

JS_ENUMERATE_COMMON_UNARY_OPS(JS_DECLARE_COMMON_UNARY_OP)
#undef JS_DECLARE_COMMON_UNARY_OP

class NewString final : public Instruction {
public:
    explicit NewString(StringTableIndex string)
        : Instruction(Type::NewString, sizeof(*this))
        , m_string(string)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;

private:
    StringTableIndex m_string;
};

class NewObject final : public Instruction {
public:
    NewObject()
        : Instruction(Type::NewObject, sizeof(*this))
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
};

class NewRegExp final : public Instruction {
public:
    NewRegExp(StringTableIndex source_index, StringTableIndex flags_index, RegexTableIndex regex_index)
        : Instruction(Type::NewRegExp, sizeof(*this))
        , m_source_index(source_index)
        , m_flags_index(flags_index)
        , m_regex_index(regex_index)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;

private:
    StringTableIndex m_source_index;
    StringTableIndex m_flags_index;
    RegexTableIndex m_regex_index;
};

#define JS_ENUMERATE_NEW_BUILTIN_ERROR_OPS(O) \
    O(TypeError)

#define JS_DECLARE_NEW_BUILTIN_ERROR_OP(ErrorName)                                     \
    class New##ErrorName final : public Instruction {                                  \
    public:                                                                            \
        explicit New##ErrorName(StringTableIndex error_string)                         \
            : Instruction(Type::New##ErrorName, sizeof(*this))                         \
            , m_error_string(error_string)                                             \
        {                                                                              \
        }                                                                              \
                                                                                       \
        ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;            \
        DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const; \
                                                                                       \
    private:                                                                           \
        StringTableIndex m_error_string;                                               \
    };

JS_ENUMERATE_NEW_BUILTIN_ERROR_OPS(JS_DECLARE_NEW_BUILTIN_ERROR_OP)
#undef JS_DECLARE_NEW_BUILTIN_ERROR_OP

// NOTE: This instruction is variable-width depending on the number of excluded names
class CopyObjectExcludingProperties final : public Instruction {
public:
    CopyObjectExcludingProperties(Register from_object, Vector<Register> const& excluded_names)
        : Instruction(Type::CopyObjectExcludingProperties, length_impl(excluded_names.size()))
        , m_from_object(from_object)
        , m_excluded_names_count(excluded_names.size())
    {
        for (size_t i = 0; i < m_excluded_names_count; i++)
            m_excluded_names[i] = excluded_names[i];
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;

    size_t length_impl(size_t excluded_names_count) const
    {
        return round_up_to_power_of_two(alignof(void*), sizeof(*this) + sizeof(Register) * excluded_names_count);
    }

private:
    Register m_from_object;
    size_t m_excluded_names_count { 0 };
    Register m_excluded_names[];
};

class NewBigInt final : public Instruction {
public:
    explicit NewBigInt(Crypto::SignedBigInteger bigint)
        : Instruction(Type::NewBigInt, sizeof(*this))
        , m_bigint(move(bigint))
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;

private:
    Crypto::SignedBigInteger m_bigint;
};

// NOTE: This instruction is variable-width depending on the number of elements!
class NewArray final : public Instruction {
public:
    NewArray()
        : Instruction(Type::NewArray, length_impl(0))
        , m_element_count(0)
    {
    }

    explicit NewArray(AK::Array<Register, 2> const& elements_range)
        : Instruction(Type::NewArray, length_impl(elements_range[1].index() - elements_range[0].index() + 1))
        , m_element_count(elements_range[1].index() - elements_range[0].index() + 1)
    {
        m_elements[0] = elements_range[0];
        m_elements[1] = elements_range[1];
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;

    size_t length_impl(size_t element_count) const
    {
        return round_up_to_power_of_two(alignof(void*), sizeof(*this) + sizeof(Register) * (element_count == 0 ? 0 : 2));
    }

    Register start() const
    {
        VERIFY(m_element_count);
        return m_elements[0];
    }

    Register end() const
    {
        VERIFY(m_element_count);
        return m_elements[1];
    }

    size_t element_count() const { return m_element_count; }

private:
    size_t m_element_count { 0 };
    Register m_elements[];
};

class Append final : public Instruction {
public:
    Append(Register lhs, bool is_spread)
        : Instruction(Type::Append, sizeof(*this))
        , m_lhs(lhs)
        , m_is_spread(is_spread)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;

private:
    Register m_lhs;
    bool m_is_spread = false;
};

class ImportCall final : public Instruction {
public:
    ImportCall(Register specifier, Register options)
        : Instruction(Type::ImportCall, sizeof(*this))
        , m_specifier(specifier)
        , m_options(options)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;

private:
    Register m_specifier;
    Register m_options;
};

class IteratorToArray final : public Instruction {
public:
    IteratorToArray()
        : Instruction(Type::IteratorToArray, sizeof(*this))
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
};

class ConcatString final : public Instruction {
public:
    explicit ConcatString(Register lhs)
        : Instruction(Type::ConcatString, sizeof(*this))
        , m_lhs(lhs)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;

private:
    Register m_lhs;
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
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
};

class EnterObjectEnvironment final : public Instruction {
public:
    explicit EnterObjectEnvironment()
        : Instruction(Type::EnterObjectEnvironment, sizeof(*this))
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
};

class CreateVariable final : public Instruction {
public:
    explicit CreateVariable(IdentifierTableIndex identifier, EnvironmentMode mode, bool is_immutable, bool is_global = false)
        : Instruction(Type::CreateVariable, sizeof(*this))
        , m_identifier(identifier)
        , m_mode(mode)
        , m_is_immutable(is_immutable)
        , m_is_global(is_global)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;

private:
    IdentifierTableIndex m_identifier;
    EnvironmentMode m_mode;
    bool m_is_immutable : 4 { false };
    bool m_is_global : 4 { false };
};

class SetVariable final : public Instruction {
public:
    enum class InitializationMode {
        Initialize,
        Set,
        InitializeOrSet,
    };
    explicit SetVariable(IdentifierTableIndex identifier, InitializationMode initialization_mode = InitializationMode::Set, EnvironmentMode mode = EnvironmentMode::Lexical)
        : Instruction(Type::SetVariable, sizeof(*this))
        , m_identifier(identifier)
        , m_mode(mode)
        , m_initialization_mode(initialization_mode)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;

    IdentifierTableIndex identifier() const { return m_identifier; }

private:
    IdentifierTableIndex m_identifier;
    EnvironmentMode m_mode;
    InitializationMode m_initialization_mode { InitializationMode::Set };
};

class SetLocal final : public Instruction {
public:
    explicit SetLocal(size_t index)
        : Instruction(Type::SetLocal, sizeof(*this))
        , m_index(index)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;

    size_t index() const { return m_index; }

private:
    size_t m_index;
};

class GetCalleeAndThisFromEnvironment final : public Instruction {
public:
    explicit GetCalleeAndThisFromEnvironment(IdentifierTableIndex identifier, Register callee_reg, Register this_reg)
        : Instruction(Type::GetCalleeAndThisFromEnvironment, sizeof(*this))
        , m_identifier(identifier)
        , m_callee_reg(callee_reg)
        , m_this_reg(this_reg)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;

    IdentifierTableIndex identifier() const { return m_identifier; }

private:
    IdentifierTableIndex m_identifier;
    Register m_callee_reg;
    Register m_this_reg;

    Optional<EnvironmentCoordinate> mutable m_cached_environment_coordinate;
};

class GetVariable final : public Instruction {
public:
    explicit GetVariable(IdentifierTableIndex identifier)
        : Instruction(Type::GetVariable, sizeof(*this))
        , m_identifier(identifier)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;

    IdentifierTableIndex identifier() const { return m_identifier; }

private:
    IdentifierTableIndex m_identifier;

    Optional<EnvironmentCoordinate> mutable m_cached_environment_coordinate;
};

class GetGlobal final : public Instruction {
public:
    explicit GetGlobal(IdentifierTableIndex identifier, u32 cache_index)
        : Instruction(Type::GetGlobal, sizeof(*this))
        , m_identifier(identifier)
        , m_cache_index(cache_index)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;

private:
    IdentifierTableIndex m_identifier;
    u32 m_cache_index { 0 };
};

class GetLocal final : public Instruction {
public:
    explicit GetLocal(size_t index)
        : Instruction(Type::GetLocal, sizeof(*this))
        , m_index(index)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;

    size_t index() const { return m_index; }

private:
    size_t m_index;
};

class DeleteVariable final : public Instruction {
public:
    explicit DeleteVariable(IdentifierTableIndex identifier)
        : Instruction(Type::DeleteVariable, sizeof(*this))
        , m_identifier(identifier)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;

    IdentifierTableIndex identifier() const { return m_identifier; }

private:
    IdentifierTableIndex m_identifier;
};

class GetById final : public Instruction {
public:
    GetById(IdentifierTableIndex property, u32 cache_index)
        : Instruction(Type::GetById, sizeof(*this))
        , m_property(property)
        , m_cache_index(cache_index)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;

private:
    IdentifierTableIndex m_property;
    u32 m_cache_index { 0 };
};

class GetByIdWithThis final : public Instruction {
public:
    GetByIdWithThis(IdentifierTableIndex property, Register this_value, u32 cache_index)
        : Instruction(Type::GetByIdWithThis, sizeof(*this))
        , m_property(property)
        , m_this_value(this_value)
        , m_cache_index(cache_index)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;

private:
    IdentifierTableIndex m_property;
    Register m_this_value;
    u32 m_cache_index { 0 };
};

class GetPrivateById final : public Instruction {
public:
    explicit GetPrivateById(IdentifierTableIndex property)
        : Instruction(Type::GetPrivateById, sizeof(*this))
        , m_property(property)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;

private:
    IdentifierTableIndex m_property;
};

class HasPrivateId final : public Instruction {
public:
    explicit HasPrivateId(IdentifierTableIndex property)
        : Instruction(Type::HasPrivateId, sizeof(*this))
        , m_property(property)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;

private:
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
    explicit PutById(Register base, IdentifierTableIndex property, PropertyKind kind = PropertyKind::KeyValue)
        : Instruction(Type::PutById, sizeof(*this))
        , m_base(base)
        , m_property(property)
        , m_kind(kind)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;

private:
    Register m_base;
    IdentifierTableIndex m_property;
    PropertyKind m_kind;
};

class PutByIdWithThis final : public Instruction {
public:
    PutByIdWithThis(Register base, Register this_value, IdentifierTableIndex property, PropertyKind kind = PropertyKind::KeyValue)
        : Instruction(Type::PutByIdWithThis, sizeof(*this))
        , m_base(base)
        , m_this_value(this_value)
        , m_property(property)
        , m_kind(kind)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;

private:
    Register m_base;
    Register m_this_value;
    IdentifierTableIndex m_property;
    PropertyKind m_kind;
};

class PutPrivateById final : public Instruction {
public:
    explicit PutPrivateById(Register base, IdentifierTableIndex property, PropertyKind kind = PropertyKind::KeyValue)
        : Instruction(Type::PutPrivateById, sizeof(*this))
        , m_base(base)
        , m_property(property)
        , m_kind(kind)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;

private:
    Register m_base;
    IdentifierTableIndex m_property;
    PropertyKind m_kind;
};

class DeleteById final : public Instruction {
public:
    explicit DeleteById(IdentifierTableIndex property)
        : Instruction(Type::DeleteById, sizeof(*this))
        , m_property(property)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;

private:
    IdentifierTableIndex m_property;
};

class DeleteByIdWithThis final : public Instruction {
public:
    DeleteByIdWithThis(Register this_value, IdentifierTableIndex property)
        : Instruction(Type::DeleteByIdWithThis, sizeof(*this))
        , m_this_value(this_value)
        , m_property(property)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;

private:
    Register m_this_value;
    IdentifierTableIndex m_property;
};

class GetByValue final : public Instruction {
public:
    explicit GetByValue(Register base)
        : Instruction(Type::GetByValue, sizeof(*this))
        , m_base(base)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;

private:
    Register m_base;
};

class GetByValueWithThis final : public Instruction {
public:
    GetByValueWithThis(Register base, Register this_value)
        : Instruction(Type::GetByValueWithThis, sizeof(*this))
        , m_base(base)
        , m_this_value(this_value)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;

private:
    Register m_base;
    Register m_this_value;
};

class PutByValue final : public Instruction {
public:
    PutByValue(Register base, Register property, PropertyKind kind = PropertyKind::KeyValue)
        : Instruction(Type::PutByValue, sizeof(*this))
        , m_base(base)
        , m_property(property)
        , m_kind(kind)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;

private:
    Register m_base;
    Register m_property;
    PropertyKind m_kind;
};

class PutByValueWithThis final : public Instruction {
public:
    PutByValueWithThis(Register base, Register property, Register this_value, PropertyKind kind = PropertyKind::KeyValue)
        : Instruction(Type::PutByValueWithThis, sizeof(*this))
        , m_base(base)
        , m_property(property)
        , m_this_value(this_value)
        , m_kind(kind)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;

private:
    Register m_base;
    Register m_property;
    Register m_this_value;
    PropertyKind m_kind;
};

class DeleteByValue final : public Instruction {
public:
    DeleteByValue(Register base)
        : Instruction(Type::DeleteByValue, sizeof(*this))
        , m_base(base)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;

private:
    Register m_base;
};

class DeleteByValueWithThis final : public Instruction {
public:
    DeleteByValueWithThis(Register base, Register this_value)
        : Instruction(Type::DeleteByValueWithThis, sizeof(*this))
        , m_base(base)
        , m_this_value(this_value)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;

private:
    Register m_base;
    Register m_this_value;
};

class Jump : public Instruction {
public:
    constexpr static bool IsTerminator = true;

    explicit Jump(Type type, Optional<Label> taken_target = {}, Optional<Label> nontaken_target = {})
        : Instruction(type, sizeof(*this))
        , m_true_target(move(taken_target))
        , m_false_target(move(nontaken_target))
    {
    }

    explicit Jump(Optional<Label> taken_target = {}, Optional<Label> nontaken_target = {})
        : Instruction(Type::Jump, sizeof(*this))
        , m_true_target(move(taken_target))
        , m_false_target(move(nontaken_target))
    {
    }

    void set_targets(Optional<Label> true_target, Optional<Label> false_target)
    {
        m_true_target = move(true_target);
        m_false_target = move(false_target);
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;

    auto& true_target() const { return m_true_target; }
    auto& false_target() const { return m_false_target; }

protected:
    Optional<Label> m_true_target;
    Optional<Label> m_false_target;
};

class JumpConditional final : public Jump {
public:
    explicit JumpConditional(Optional<Label> true_target = {}, Optional<Label> false_target = {})
        : Jump(Type::JumpConditional, move(true_target), move(false_target))
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
};

class JumpNullish final : public Jump {
public:
    explicit JumpNullish(Optional<Label> true_target = {}, Optional<Label> false_target = {})
        : Jump(Type::JumpNullish, move(true_target), move(false_target))
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
};

class JumpUndefined final : public Jump {
public:
    explicit JumpUndefined(Optional<Label> true_target = {}, Optional<Label> false_target = {})
        : Jump(Type::JumpUndefined, move(true_target), move(false_target))
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
};

enum class CallType {
    Call,
    Construct,
    DirectEval,
};

class Call final : public Instruction {
public:
    Call(CallType type, Register callee, Register this_value, Register first_argument, u32 argument_count, Optional<StringTableIndex> expression_string = {})
        : Instruction(Type::Call, sizeof(*this))
        , m_callee(callee)
        , m_this_value(this_value)
        , m_first_argument(first_argument)
        , m_argument_count(argument_count)
        , m_type(type)
        , m_expression_string(expression_string)
    {
    }

    CallType call_type() const { return m_type; }
    Register callee() const { return m_callee; }
    Register this_value() const { return m_this_value; }
    Optional<StringTableIndex> const& expression_string() const { return m_expression_string; }

    Register first_argument() const { return m_first_argument; }
    u32 argument_count() const { return m_argument_count; }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;

private:
    Register m_callee;
    Register m_this_value;
    Register m_first_argument;
    u32 m_argument_count { 0 };
    CallType m_type;
    Optional<StringTableIndex> m_expression_string;
};

class CallWithArgumentArray final : public Instruction {
public:
    CallWithArgumentArray(CallType type, Register callee, Register this_value, Optional<StringTableIndex> expression_string = {})
        : Instruction(Type::CallWithArgumentArray, sizeof(*this))
        , m_callee(callee)
        , m_this_value(this_value)
        , m_type(type)
        , m_expression_string(expression_string)
    {
    }

    CallType call_type() const { return m_type; }
    Register callee() const { return m_callee; }
    Register this_value() const { return m_this_value; }
    Optional<StringTableIndex> const& expression_string() const { return m_expression_string; }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;

private:
    Register m_callee;
    Register m_this_value;
    CallType m_type;
    Optional<StringTableIndex> m_expression_string;
};

class SuperCallWithArgumentArray : public Instruction {
public:
    explicit SuperCallWithArgumentArray(bool is_synthetic)
        : Instruction(Type::SuperCallWithArgumentArray, sizeof(*this))
        , m_is_synthetic(is_synthetic)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;

private:
    bool m_is_synthetic;
};

class NewClass final : public Instruction {
public:
    explicit NewClass(ClassExpression const& class_expression, Optional<IdentifierTableIndex> lhs_name)
        : Instruction(Type::NewClass, sizeof(*this))
        , m_class_expression(class_expression)
        , m_lhs_name(lhs_name)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;

private:
    ClassExpression const& m_class_expression;
    Optional<IdentifierTableIndex> m_lhs_name;
};

class NewFunction final : public Instruction {
public:
    explicit NewFunction(FunctionExpression const& function_node, Optional<IdentifierTableIndex> lhs_name, Optional<Register> home_object = {})
        : Instruction(Type::NewFunction, sizeof(*this))
        , m_function_node(function_node)
        , m_lhs_name(lhs_name)
        , m_home_object(move(home_object))
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;

private:
    FunctionExpression const& m_function_node;
    Optional<IdentifierTableIndex> m_lhs_name;
    Optional<Register> m_home_object;
};

class BlockDeclarationInstantiation final : public Instruction {
public:
    explicit BlockDeclarationInstantiation(ScopeNode const& scope_node)
        : Instruction(Type::BlockDeclarationInstantiation, sizeof(*this))
        , m_scope_node(scope_node)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;

private:
    ScopeNode const& m_scope_node;
};

class Return final : public Instruction {
public:
    constexpr static bool IsTerminator = true;

    Return()
        : Instruction(Type::Return, sizeof(*this))
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
};

class Increment final : public Instruction {
public:
    Increment()
        : Instruction(Type::Increment, sizeof(*this))
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
};

class Decrement final : public Instruction {
public:
    Decrement()
        : Instruction(Type::Decrement, sizeof(*this))
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
};

class ToNumeric final : public Instruction {
public:
    ToNumeric()
        : Instruction(Type::ToNumeric, sizeof(*this))
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
};

class Throw final : public Instruction {
public:
    constexpr static bool IsTerminator = true;

    Throw()
        : Instruction(Type::Throw, sizeof(*this))
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
};

class ThrowIfNotObject final : public Instruction {
public:
    ThrowIfNotObject()
        : Instruction(Type::ThrowIfNotObject, sizeof(*this))
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
};

class ThrowIfNullish final : public Instruction {
public:
    ThrowIfNullish()
        : Instruction(Type::ThrowIfNullish, sizeof(*this))
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
};

class EnterUnwindContext final : public Instruction {
public:
    constexpr static bool IsTerminator = true;

    EnterUnwindContext(Label entry_point, Optional<Label> handler_target, Optional<Label> finalizer_target)
        : Instruction(Type::EnterUnwindContext, sizeof(*this))
        , m_entry_point(move(entry_point))
        , m_handler_target(move(handler_target))
        , m_finalizer_target(move(finalizer_target))
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;

    auto& entry_point() const { return m_entry_point; }
    auto& handler_target() const { return m_handler_target; }
    auto& finalizer_target() const { return m_finalizer_target; }

private:
    Label m_entry_point;
    Optional<Label> m_handler_target;
    Optional<Label> m_finalizer_target;
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
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;

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
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
};

class LeaveUnwindContext final : public Instruction {
public:
    LeaveUnwindContext()
        : Instruction(Type::LeaveUnwindContext, sizeof(*this))
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
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
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;

    auto& resume_target() const { return m_resume_target; }

private:
    Label m_resume_target;
};

class Yield final : public Instruction {
public:
    constexpr static bool IsTerminator = true;

    explicit Yield(Label continuation_label)
        : Instruction(Type::Yield, sizeof(*this))
        , m_continuation_label(continuation_label)
    {
    }

    explicit Yield(nullptr_t)
        : Instruction(Type::Yield, sizeof(*this))
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;

    auto& continuation() const { return m_continuation_label; }

private:
    Optional<Label> m_continuation_label;
};

class Await final : public Instruction {
public:
    constexpr static bool IsTerminator = true;

    explicit Await(Label continuation_label)
        : Instruction(Type::Await, sizeof(*this))
        , m_continuation_label(continuation_label)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;

    auto& continuation() const { return m_continuation_label; }

private:
    Label m_continuation_label;
};

class PushDeclarativeEnvironment final : public Instruction {
public:
    explicit PushDeclarativeEnvironment(HashMap<u32, Variable> variables)
        : Instruction(Type::PushDeclarativeEnvironment, sizeof(*this))
        , m_variables(move(variables))
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;

private:
    HashMap<u32, Variable> m_variables;
};

class GetIterator final : public Instruction {
public:
    GetIterator(IteratorHint hint = IteratorHint::Sync)
        : Instruction(Type::GetIterator, sizeof(*this))
        , m_hint(hint)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;

private:
    IteratorHint m_hint { IteratorHint::Sync };
};

class GetMethod final : public Instruction {
public:
    GetMethod(IdentifierTableIndex property)
        : Instruction(Type::GetMethod, sizeof(*this))
        , m_property(property)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;

private:
    IdentifierTableIndex m_property;
};

class GetObjectPropertyIterator final : public Instruction {
public:
    GetObjectPropertyIterator()
        : Instruction(Type::GetObjectPropertyIterator, sizeof(*this))
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
};

class IteratorClose final : public Instruction {
public:
    IteratorClose(Completion::Type completion_type, Optional<Value> completion_value)
        : Instruction(Type::IteratorClose, sizeof(*this))
        , m_completion_type(completion_type)
        , m_completion_value(completion_value)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;

private:
    Completion::Type m_completion_type { Completion::Type::Normal };
    Optional<Value> m_completion_value;
};

class AsyncIteratorClose final : public Instruction {
public:
    AsyncIteratorClose(Completion::Type completion_type, Optional<Value> completion_value)
        : Instruction(Type::AsyncIteratorClose, sizeof(*this))
        , m_completion_type(completion_type)
        , m_completion_value(completion_value)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;

private:
    Completion::Type m_completion_type { Completion::Type::Normal };
    Optional<Value> m_completion_value;
};

class IteratorNext final : public Instruction {
public:
    IteratorNext()
        : Instruction(Type::IteratorNext, sizeof(*this))
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
};

class IteratorResultDone final : public Instruction {
public:
    IteratorResultDone()
        : Instruction(Type::IteratorResultDone, sizeof(*this))
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
};

class IteratorResultValue final : public Instruction {
public:
    IteratorResultValue()
        : Instruction(Type::IteratorResultValue, sizeof(*this))
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
};

class ResolveThisBinding final : public Instruction {
public:
    explicit ResolveThisBinding()
        : Instruction(Type::ResolveThisBinding, sizeof(*this))
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
};

class ResolveSuperBase final : public Instruction {
public:
    explicit ResolveSuperBase()
        : Instruction(Type::ResolveSuperBase, sizeof(*this))
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
};

class GetNewTarget final : public Instruction {
public:
    explicit GetNewTarget()
        : Instruction(Type::GetNewTarget, sizeof(*this))
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
};

class GetImportMeta final : public Instruction {
public:
    explicit GetImportMeta()
        : Instruction(Type::GetImportMeta, sizeof(*this))
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
};

class TypeofVariable final : public Instruction {
public:
    explicit TypeofVariable(IdentifierTableIndex identifier)
        : Instruction(Type::TypeofVariable, sizeof(*this))
        , m_identifier(identifier)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;

private:
    IdentifierTableIndex m_identifier;
};

class TypeofLocal final : public Instruction {
public:
    explicit TypeofLocal(size_t index)
        : Instruction(Type::TypeofLocal, sizeof(*this))
        , m_index(index)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;

private:
    size_t m_index;
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

ALWAYS_INLINE bool Instruction::is_terminator() const
{
#define __BYTECODE_OP(op) \
    case Type::op:        \
        return Op::op::IsTerminator;

    switch (type()) {
        ENUMERATE_BYTECODE_OPS(__BYTECODE_OP)
    default:
        VERIFY_NOT_REACHED();
    }
#undef __BYTECODE_OP
}

}
