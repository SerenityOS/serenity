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
#include <LibJS/Bytecode/Register.h>
#include <LibJS/Bytecode/StringTable.h>
#include <LibJS/Heap/Cell.h>
#include <LibJS/Runtime/Environment.h>
#include <LibJS/Runtime/EnvironmentCoordinate.h>
#include <LibJS/Runtime/Value.h>
#include <LibJS/Runtime/ValueTraits.h>

namespace JS {
class FunctionExpression;
}

namespace JS::Bytecode::Op {

class Load final : public Instruction {
public:
    explicit Load(Register src)
        : Instruction(Type::Load)
        , m_src(src)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
    void replace_references_impl(BasicBlock const&, BasicBlock const&) { }
    void replace_references_impl(Register from, Register to)
    {
        if (m_src == from)
            m_src = to;
    }

private:
    Register m_src;
};

class LoadImmediate final : public Instruction {
public:
    explicit LoadImmediate(Value value)
        : Instruction(Type::LoadImmediate)
        , m_value(value)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
    void replace_references_impl(BasicBlock const&, BasicBlock const&) { }
    void replace_references_impl(Register, Register) { }

private:
    Value m_value;
};

class Store final : public Instruction {
public:
    explicit Store(Register dst)
        : Instruction(Type::Store)
        , m_dst(dst)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
    void replace_references_impl(BasicBlock const&, BasicBlock const&) { }
    void replace_references_impl(Register, Register) { }

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
            : Instruction(Type::OpTitleCase)                                           \
            , m_lhs_reg(lhs_reg)                                                       \
        {                                                                              \
        }                                                                              \
                                                                                       \
        ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;            \
        DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const; \
        void replace_references_impl(BasicBlock const&, BasicBlock const&)             \
        {                                                                              \
        }                                                                              \
        void replace_references_impl(Register from, Register to)                       \
        {                                                                              \
            if (m_lhs_reg == from)                                                     \
                m_lhs_reg = to;                                                        \
        }                                                                              \
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
            : Instruction(Type::OpTitleCase)                                           \
        {                                                                              \
        }                                                                              \
                                                                                       \
        ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;            \
        DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const; \
        void replace_references_impl(BasicBlock const&, BasicBlock const&)             \
        {                                                                              \
        }                                                                              \
        void replace_references_impl(Register, Register)                               \
        {                                                                              \
        }                                                                              \
    };

JS_ENUMERATE_COMMON_UNARY_OPS(JS_DECLARE_COMMON_UNARY_OP)
#undef JS_DECLARE_COMMON_UNARY_OP

class NewString final : public Instruction {
public:
    explicit NewString(StringTableIndex string)
        : Instruction(Type::NewString)
        , m_string(string)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
    void replace_references_impl(BasicBlock const&, BasicBlock const&) { }
    void replace_references_impl(Register, Register) { }

private:
    StringTableIndex m_string;
};

class NewObject final : public Instruction {
public:
    NewObject()
        : Instruction(Type::NewObject)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
    void replace_references_impl(BasicBlock const&, BasicBlock const&) { }
    void replace_references_impl(Register, Register) { }
};

class NewRegExp final : public Instruction {
public:
    NewRegExp(StringTableIndex source_index, StringTableIndex flags_index)
        : Instruction(Type::NewRegExp)
        , m_source_index(source_index)
        , m_flags_index(flags_index)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
    void replace_references_impl(BasicBlock const&, BasicBlock const&) { }
    void replace_references_impl(Register, Register) { }

private:
    StringTableIndex m_source_index;
    StringTableIndex m_flags_index;
};

#define JS_ENUMERATE_NEW_BUILTIN_ERROR_OPS(O) \
    O(TypeError)

#define JS_DECLARE_NEW_BUILTIN_ERROR_OP(ErrorName)                                     \
    class New##ErrorName final : public Instruction {                                  \
    public:                                                                            \
        explicit New##ErrorName(StringTableIndex error_string)                         \
            : Instruction(Type::New##ErrorName)                                        \
            , m_error_string(error_string)                                             \
        {                                                                              \
        }                                                                              \
                                                                                       \
        ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;            \
        DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const; \
        void replace_references_impl(BasicBlock const&, BasicBlock const&)             \
        {                                                                              \
        }                                                                              \
        void replace_references_impl(Register, Register)                               \
        {                                                                              \
        }                                                                              \
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
        : Instruction(Type::CopyObjectExcludingProperties)
        , m_from_object(from_object)
        , m_excluded_names_count(excluded_names.size())
    {
        for (size_t i = 0; i < m_excluded_names_count; i++)
            m_excluded_names[i] = excluded_names[i];
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
    void replace_references_impl(BasicBlock const&, BasicBlock const&) { }
    void replace_references_impl(Register from, Register to);

    size_t length_impl() const { return sizeof(*this) + sizeof(Register) * m_excluded_names_count; }

private:
    Register m_from_object;
    size_t m_excluded_names_count { 0 };
    Register m_excluded_names[];
};

class NewBigInt final : public Instruction {
public:
    explicit NewBigInt(Crypto::SignedBigInteger bigint)
        : Instruction(Type::NewBigInt)
        , m_bigint(move(bigint))
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
    void replace_references_impl(BasicBlock const&, BasicBlock const&) { }
    void replace_references_impl(Register, Register) { }

private:
    Crypto::SignedBigInteger m_bigint;
};

// NOTE: This instruction is variable-width depending on the number of elements!
class NewArray final : public Instruction {
public:
    NewArray()
        : Instruction(Type::NewArray)
        , m_element_count(0)
    {
    }

    explicit NewArray(AK::Array<Register, 2> const& elements_range)
        : Instruction(Type::NewArray)
        , m_element_count(elements_range[1].index() - elements_range[0].index() + 1)
    {
        m_elements[0] = elements_range[0];
        m_elements[1] = elements_range[1];
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
    void replace_references_impl(BasicBlock const&, BasicBlock const&) { }
    // Note: The underlying element range shall never be changed item, by item
    //       shifting it may be done in the future
    void replace_references_impl(Register from, Register) { VERIFY(!m_element_count || from.index() < start().index() || from.index() > end().index()); }

    size_t length_impl() const
    {
        return sizeof(*this) + sizeof(Register) * (m_element_count == 0 ? 0 : 2);
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
        : Instruction(Type::Append)
        , m_lhs(lhs)
        , m_is_spread(is_spread)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
    void replace_references_impl(BasicBlock const&, BasicBlock const&) { }

    // Note: This should never do anything, the lhs should always be an array, that is currently being constructed
    void replace_references_impl(Register from, Register) { VERIFY(from != m_lhs); }

private:
    Register m_lhs;
    bool m_is_spread = false;
};

class ImportCall final : public Instruction {
public:
    ImportCall(Register specifier, Register options)
        : Instruction(Type::ImportCall)
        , m_specifier(specifier)
        , m_options(options)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
    void replace_references_impl(BasicBlock const&, BasicBlock const&) { }
    void replace_references_impl(Register, Register);

private:
    Register m_specifier;
    Register m_options;
};

class IteratorToArray final : public Instruction {
public:
    IteratorToArray()
        : Instruction(Type::IteratorToArray)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
    void replace_references_impl(BasicBlock const&, BasicBlock const&) { }
    void replace_references_impl(Register, Register) { }
};

class ConcatString final : public Instruction {
public:
    explicit ConcatString(Register lhs)
        : Instruction(Type::ConcatString)
        , m_lhs(lhs)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
    void replace_references_impl(BasicBlock const&, BasicBlock const&) { }
    // Note: lhs should always be a string in construction, so this should never do anything
    void replace_references_impl(Register from, Register) { VERIFY(from != m_lhs); }

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
        : Instruction(Type::CreateLexicalEnvironment)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
    void replace_references_impl(BasicBlock const&, BasicBlock const&) { }
    void replace_references_impl(Register, Register) { }
};

class EnterObjectEnvironment final : public Instruction {
public:
    explicit EnterObjectEnvironment()
        : Instruction(Type::EnterObjectEnvironment)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
    void replace_references_impl(BasicBlock const&, BasicBlock const&) { }
    void replace_references_impl(Register, Register) { }
};

class CreateVariable final : public Instruction {
public:
    explicit CreateVariable(IdentifierTableIndex identifier, EnvironmentMode mode, bool is_immutable, bool is_global = false)
        : Instruction(Type::CreateVariable)
        , m_identifier(identifier)
        , m_mode(mode)
        , m_is_immutable(is_immutable)
        , m_is_global(is_global)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
    void replace_references_impl(BasicBlock const&, BasicBlock const&) { }
    void replace_references_impl(Register, Register) { }

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
        : Instruction(Type::SetVariable)
        , m_identifier(identifier)
        , m_mode(mode)
        , m_initialization_mode(initialization_mode)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
    void replace_references_impl(BasicBlock const&, BasicBlock const&) { }
    void replace_references_impl(Register, Register) { }

    IdentifierTableIndex identifier() const { return m_identifier; }

private:
    IdentifierTableIndex m_identifier;
    EnvironmentMode m_mode;
    InitializationMode m_initialization_mode { InitializationMode::Set };
};

class GetVariable final : public Instruction {
public:
    explicit GetVariable(IdentifierTableIndex identifier)
        : Instruction(Type::GetVariable)
        , m_identifier(identifier)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
    void replace_references_impl(BasicBlock const&, BasicBlock const&) { }
    void replace_references_impl(Register, Register) { }

    IdentifierTableIndex identifier() const { return m_identifier; }

private:
    IdentifierTableIndex m_identifier;

    Optional<EnvironmentCoordinate> mutable m_cached_environment_coordinate;
};

class DeleteVariable final : public Instruction {
public:
    explicit DeleteVariable(IdentifierTableIndex identifier)
        : Instruction(Type::DeleteVariable)
        , m_identifier(identifier)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
    void replace_references_impl(BasicBlock const&, BasicBlock const&) { }
    void replace_references_impl(Register, Register) { }

    IdentifierTableIndex identifier() const { return m_identifier; }

private:
    IdentifierTableIndex m_identifier;
};

class GetById final : public Instruction {
public:
    explicit GetById(IdentifierTableIndex property)
        : Instruction(Type::GetById)
        , m_property(property)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
    void replace_references_impl(BasicBlock const&, BasicBlock const&) { }
    void replace_references_impl(Register, Register) { }

private:
    IdentifierTableIndex m_property;
};

class GetPrivateById final : public Instruction {
public:
    explicit GetPrivateById(IdentifierTableIndex property)
        : Instruction(Type::GetPrivateById)
        , m_property(property)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
    void replace_references_impl(BasicBlock const&, BasicBlock const&) { }
    void replace_references_impl(Register, Register) { }

private:
    IdentifierTableIndex m_property;
};

enum class PropertyKind {
    Getter,
    Setter,
    KeyValue,
    Spread,
    ProtoSetter,
};

class PutById final : public Instruction {
public:
    explicit PutById(Register base, IdentifierTableIndex property, PropertyKind kind = PropertyKind::KeyValue)
        : Instruction(Type::PutById)
        , m_base(base)
        , m_property(property)
        , m_kind(kind)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
    void replace_references_impl(BasicBlock const&, BasicBlock const&) { }
    void replace_references_impl(Register from, Register to)
    {
        if (m_base == from)
            m_base = to;
    }

private:
    Register m_base;
    IdentifierTableIndex m_property;
    PropertyKind m_kind;
};

class PutPrivateById final : public Instruction {
public:
    explicit PutPrivateById(Register base, IdentifierTableIndex property, PropertyKind kind = PropertyKind::KeyValue)
        : Instruction(Type::PutPrivateById)
        , m_base(base)
        , m_property(property)
        , m_kind(kind)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
    void replace_references_impl(BasicBlock const&, BasicBlock const&) { }
    void replace_references_impl(Register from, Register to)
    {
        if (m_base == from)
            m_base = to;
    }

private:
    Register m_base;
    IdentifierTableIndex m_property;
    PropertyKind m_kind;
};

class DeleteById final : public Instruction {
public:
    explicit DeleteById(IdentifierTableIndex property)
        : Instruction(Type::DeleteById)
        , m_property(property)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
    void replace_references_impl(BasicBlock const&, BasicBlock const&) { }
    void replace_references_impl(Register, Register) { }

private:
    IdentifierTableIndex m_property;
};

class GetByValue final : public Instruction {
public:
    explicit GetByValue(Register base)
        : Instruction(Type::GetByValue)
        , m_base(base)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
    void replace_references_impl(BasicBlock const&, BasicBlock const&) { }
    void replace_references_impl(Register from, Register to)
    {
        if (m_base == from)
            m_base = to;
    }

private:
    Register m_base;
};

class PutByValue final : public Instruction {
public:
    PutByValue(Register base, Register property, PropertyKind kind = PropertyKind::KeyValue)
        : Instruction(Type::PutByValue)
        , m_base(base)
        , m_property(property)
        , m_kind(kind)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
    void replace_references_impl(BasicBlock const&, BasicBlock const&) { }
    void replace_references_impl(Register from, Register to)
    {
        if (m_base == from)
            m_base = to;
    }

private:
    Register m_base;
    Register m_property;
    PropertyKind m_kind;
};

class DeleteByValue final : public Instruction {
public:
    DeleteByValue(Register base)
        : Instruction(Type::DeleteByValue)
        , m_base(base)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
    void replace_references_impl(BasicBlock const&, BasicBlock const&) { }
    void replace_references_impl(Register from, Register to)
    {
        if (m_base == from)
            m_base = to;
    }

private:
    Register m_base;
};

class Jump : public Instruction {
public:
    constexpr static bool IsTerminator = true;

    explicit Jump(Type type, Optional<Label> taken_target = {}, Optional<Label> nontaken_target = {})
        : Instruction(type)
        , m_true_target(move(taken_target))
        , m_false_target(move(nontaken_target))
    {
    }

    explicit Jump(Optional<Label> taken_target = {}, Optional<Label> nontaken_target = {})
        : Instruction(Type::Jump)
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
    void replace_references_impl(BasicBlock const&, BasicBlock const&);
    void replace_references_impl(Register, Register) { }

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

// NOTE: This instruction is variable-width depending on the number of arguments!
class Call final : public Instruction {
public:
    enum class CallType {
        Call,
        Construct,
        DirectEval,
    };

    Call(CallType type, Register callee, Register this_value, Optional<StringTableIndex> expression_string = {})
        : Instruction(Type::Call)
        , m_callee(callee)
        , m_this_value(this_value)
        , m_type(type)
        , m_expression_string(expression_string)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
    void replace_references_impl(BasicBlock const&, BasicBlock const&) { }
    void replace_references_impl(Register, Register);

    Completion throw_type_error_for_callee(Bytecode::Interpreter&, StringView callee_type) const;

private:
    Register m_callee;
    Register m_this_value;
    CallType m_type;
    Optional<StringTableIndex> m_expression_string;
};

// NOTE: This instruction is variable-width depending on the number of arguments!
class SuperCall : public Instruction {
public:
    explicit SuperCall(bool is_synthetic)
        : Instruction(Type::SuperCall)
        , m_is_synthetic(is_synthetic)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
    void replace_references_impl(BasicBlock const&, BasicBlock const&) { }
    void replace_references_impl(Register, Register) { }

private:
    bool m_is_synthetic;
};

class NewClass final : public Instruction {
public:
    explicit NewClass(ClassExpression const& class_expression, Optional<DeprecatedFlyString const&> lhs_name)
        : Instruction(Type::NewClass)
        , m_class_expression(class_expression)
        , m_lhs_name(lhs_name.has_value() ? *lhs_name : Optional<DeprecatedFlyString> {})
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
    void replace_references_impl(BasicBlock const&, BasicBlock const&) { }
    void replace_references_impl(Register, Register) { }

private:
    ClassExpression const& m_class_expression;
    Optional<DeprecatedFlyString> m_lhs_name;
};

class NewFunction final : public Instruction {
public:
    explicit NewFunction(FunctionExpression const& function_node, Optional<DeprecatedFlyString const&> lhs_name, Optional<Register> home_object = {})
        : Instruction(Type::NewFunction)
        , m_function_node(function_node)
        , m_lhs_name(lhs_name.has_value() ? *lhs_name : Optional<DeprecatedFlyString> {})
        , m_home_object(move(home_object))
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
    void replace_references_impl(BasicBlock const&, BasicBlock const&) { }
    void replace_references_impl(Register, Register);

private:
    FunctionExpression const& m_function_node;
    Optional<DeprecatedFlyString> m_lhs_name;
    Optional<Register> m_home_object;
};

class BlockDeclarationInstantiation final : public Instruction {
public:
    explicit BlockDeclarationInstantiation(ScopeNode const& scope_node)
        : Instruction(Type::BlockDeclarationInstantiation)
        , m_scope_node(scope_node)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
    void replace_references_impl(BasicBlock const&, BasicBlock const&) { }
    void replace_references_impl(Register, Register) { }

private:
    ScopeNode const& m_scope_node;
};

class Return final : public Instruction {
public:
    constexpr static bool IsTerminator = true;

    Return()
        : Instruction(Type::Return)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
    void replace_references_impl(BasicBlock const&, BasicBlock const&) { }
    void replace_references_impl(Register, Register) { }
};

class Increment final : public Instruction {
public:
    Increment()
        : Instruction(Type::Increment)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
    void replace_references_impl(BasicBlock const&, BasicBlock const&) { }
    void replace_references_impl(Register, Register) { }
};

class Decrement final : public Instruction {
public:
    Decrement()
        : Instruction(Type::Decrement)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
    void replace_references_impl(BasicBlock const&, BasicBlock const&) { }
    void replace_references_impl(Register, Register) { }
};

class ToNumeric final : public Instruction {
public:
    ToNumeric()
        : Instruction(Type::ToNumeric)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
    void replace_references_impl(BasicBlock const&, BasicBlock const&) { }
    void replace_references_impl(Register, Register) { }
};

class Throw final : public Instruction {
public:
    constexpr static bool IsTerminator = true;

    Throw()
        : Instruction(Type::Throw)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
    void replace_references_impl(BasicBlock const&, BasicBlock const&) { }
    void replace_references_impl(Register, Register) { }
};

class ThrowIfNotObject final : public Instruction {
public:
    ThrowIfNotObject()
        : Instruction(Type::ThrowIfNotObject)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
    void replace_references_impl(BasicBlock const&, BasicBlock const&) { }
    void replace_references_impl(Register, Register) { }
};

class ThrowIfNullish final : public Instruction {
public:
    ThrowIfNullish()
        : Instruction(Type::ThrowIfNullish)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
    void replace_references_impl(BasicBlock const&, BasicBlock const&) { }
    void replace_references_impl(Register, Register) { }
};

class EnterUnwindContext final : public Instruction {
public:
    constexpr static bool IsTerminator = true;

    EnterUnwindContext(Label entry_point, Optional<Label> handler_target, Optional<Label> finalizer_target)
        : Instruction(Type::EnterUnwindContext)
        , m_entry_point(move(entry_point))
        , m_handler_target(move(handler_target))
        , m_finalizer_target(move(finalizer_target))
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
    void replace_references_impl(BasicBlock const&, BasicBlock const&);
    void replace_references_impl(Register, Register) { }

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
        : Instruction(Type::ScheduleJump)
        , m_target(target)
    {
    }

    Label target() const { return m_target; }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    void replace_references_impl(BasicBlock const& from, BasicBlock const& to)
    {
        if (&m_target.block() == &from)
            m_target = Label { to };
    }
    void replace_references_impl(Register, Register) { }

    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;

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
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
    void replace_references_impl(BasicBlock const&, BasicBlock const&) { }
    void replace_references_impl(Register, Register) { }
};

class LeaveUnwindContext final : public Instruction {
public:
    LeaveUnwindContext()
        : Instruction(Type::LeaveUnwindContext)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
    void replace_references_impl(BasicBlock const&, BasicBlock const&) { }
    void replace_references_impl(Register, Register) { }
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
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
    void replace_references_impl(BasicBlock const&, BasicBlock const&);
    void replace_references_impl(Register, Register) { }

    auto& resume_target() const { return m_resume_target; }

private:
    Label m_resume_target;
};

class Yield final : public Instruction {
public:
    constexpr static bool IsTerminator = true;

    explicit Yield(Label continuation_label)
        : Instruction(Type::Yield)
        , m_continuation_label(continuation_label)
    {
    }

    explicit Yield(nullptr_t)
        : Instruction(Type::Yield)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
    void replace_references_impl(BasicBlock const&, BasicBlock const&);
    void replace_references_impl(Register, Register) { }

    auto& continuation() const { return m_continuation_label; }

private:
    Optional<Label> m_continuation_label;
};

class PushDeclarativeEnvironment final : public Instruction {
public:
    explicit PushDeclarativeEnvironment(HashMap<u32, Variable> variables)
        : Instruction(Type::PushDeclarativeEnvironment)
        , m_variables(move(variables))
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
    void replace_references_impl(BasicBlock const&, BasicBlock const&) { }
    void replace_references_impl(Register, Register) { }

private:
    HashMap<u32, Variable> m_variables;
};

class GetIterator final : public Instruction {
public:
    GetIterator()
        : Instruction(Type::GetIterator)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
    void replace_references_impl(BasicBlock const&, BasicBlock const&) { }
    void replace_references_impl(Register, Register) { }
};

class GetMethod final : public Instruction {
public:
    GetMethod(IdentifierTableIndex property)
        : Instruction(Type::GetMethod)
        , m_property(property)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
    void replace_references_impl(BasicBlock const&, BasicBlock const&) { }
    void replace_references_impl(Register, Register) { }

private:
    IdentifierTableIndex m_property;
};

class GetObjectPropertyIterator final : public Instruction {
public:
    GetObjectPropertyIterator()
        : Instruction(Type::GetObjectPropertyIterator)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
    void replace_references_impl(BasicBlock const&, BasicBlock const&) { }
    void replace_references_impl(Register, Register) { }
};

class IteratorClose final : public Instruction {
public:
    IteratorClose(Completion::Type completion_type, Optional<Value> completion_value)
        : Instruction(Type::IteratorClose)
        , m_completion_type(completion_type)
        , m_completion_value(completion_value)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
    void replace_references_impl(BasicBlock const&, BasicBlock const&) { }
    void replace_references_impl(Register, Register) { }

private:
    Completion::Type m_completion_type { Completion::Type::Normal };
    Optional<Value> m_completion_value;
};

class IteratorNext final : public Instruction {
public:
    IteratorNext()
        : Instruction(Type::IteratorNext)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
    void replace_references_impl(BasicBlock const&, BasicBlock const&) { }
    void replace_references_impl(Register, Register) { }
};

class IteratorResultDone final : public Instruction {
public:
    IteratorResultDone()
        : Instruction(Type::IteratorResultDone)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
    void replace_references_impl(BasicBlock const&, BasicBlock const&) { }
    void replace_references_impl(Register, Register) { }
};

class IteratorResultValue final : public Instruction {
public:
    IteratorResultValue()
        : Instruction(Type::IteratorResultValue)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
    void replace_references_impl(BasicBlock const&, BasicBlock const&) { }
    void replace_references_impl(Register, Register) { }
};

class ResolveThisBinding final : public Instruction {
public:
    explicit ResolveThisBinding()
        : Instruction(Type::ResolveThisBinding)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
    void replace_references_impl(BasicBlock const&, BasicBlock const&) { }
    void replace_references_impl(Register, Register) { }
};

class ResolveSuperBase final : public Instruction {
public:
    explicit ResolveSuperBase()
        : Instruction(Type::ResolveSuperBase)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
    void replace_references_impl(BasicBlock const&, BasicBlock const&) { }
    void replace_references_impl(Register, Register) { }
};

class GetNewTarget final : public Instruction {
public:
    explicit GetNewTarget()
        : Instruction(Type::GetNewTarget)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
    void replace_references_impl(BasicBlock const&, BasicBlock const&) { }
    void replace_references_impl(Register, Register) { }
};

class TypeofVariable final : public Instruction {
public:
    explicit TypeofVariable(IdentifierTableIndex identifier)
        : Instruction(Type::TypeofVariable)
        , m_identifier(identifier)
    {
    }

    ThrowCompletionOr<void> execute_impl(Bytecode::Interpreter&) const;
    DeprecatedString to_deprecated_string_impl(Bytecode::Executable const&) const;
    void replace_references_impl(BasicBlock const&, BasicBlock const&) { }
    void replace_references_impl(Register, Register) { }

private:
    IdentifierTableIndex m_identifier;
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

ALWAYS_INLINE void Instruction::replace_references(BasicBlock const& from, BasicBlock const& to)
{
#define __BYTECODE_OP(op)       \
    case Instruction::Type::op: \
        return static_cast<Bytecode::Op::op&>(*this).replace_references_impl(from, to);

    switch (type()) {
        ENUMERATE_BYTECODE_OPS(__BYTECODE_OP)
    default:
        VERIFY_NOT_REACHED();
    }

#undef __BYTECODE_OP
}

ALWAYS_INLINE void Instruction::replace_references(Register from, Register to)
{
#define __BYTECODE_OP(op)       \
    case Instruction::Type::op: \
        return static_cast<Bytecode::Op::op&>(*this).replace_references_impl(from, to);

    switch (type()) {
        ENUMERATE_BYTECODE_OPS(__BYTECODE_OP)
    default:
        VERIFY_NOT_REACHED();
    }

#undef __BYTECODE_OP
}

ALWAYS_INLINE size_t Instruction::length() const
{
    if (type() == Type::NewArray)
        return round_up_to_power_of_two(static_cast<Op::NewArray const&>(*this).length_impl(), alignof(void*));
    if (type() == Type::CopyObjectExcludingProperties)
        return round_up_to_power_of_two(static_cast<Op::CopyObjectExcludingProperties const&>(*this).length_impl(), alignof(void*));

#define __BYTECODE_OP(op) \
    case Type::op:        \
        return sizeof(Op::op);

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
