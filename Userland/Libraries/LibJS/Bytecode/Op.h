/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <LibCrypto/BigInt/SignedBigInteger.h>
#include <LibJS/Bytecode/Instruction.h>
#include <LibJS/Bytecode/Label.h>
#include <LibJS/Bytecode/Register.h>
#include <LibJS/Heap/Cell.h>
#include <LibJS/Runtime/Value.h>

namespace JS::Bytecode::Op {

class Load final : public Instruction {
public:
    Load(Register src)
        : Instruction(Type::Load)
        , m_src(src)
    {
    }

    void execute(Bytecode::Interpreter&) const;
    String to_string() const;

private:
    Register m_src;
};

class LoadImmediate final : public Instruction {
public:
    LoadImmediate(Value value)
        : Instruction(Type::LoadImmediate)
        , m_value(value)
    {
    }

    void execute(Bytecode::Interpreter&) const;
    String to_string() const;

private:
    Value m_value;
};

class Store final : public Instruction {
public:
    Store(Register dst)
        : Instruction(Type::Store)
        , m_dst(dst)
    {
    }

    void execute(Bytecode::Interpreter&) const;
    String to_string() const;

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
    O(AbstractInequals, abstract_inequals)    \
    O(AbstractEquals, abstract_equals)        \
    O(TypedInequals, typed_inequals)          \
    O(TypedEquals, typed_equals)              \
    O(BitwiseAnd, bitwise_and)                \
    O(BitwiseOr, bitwise_or)                  \
    O(BitwiseXor, bitwise_xor)                \
    O(LeftShift, left_shift)                  \
    O(RightShift, right_shift)                \
    O(UnsignedRightShift, unsigned_right_shift)

#define JS_DECLARE_COMMON_BINARY_OP(OpTitleCase, op_snake_case) \
    class OpTitleCase final : public Instruction {              \
    public:                                                     \
        OpTitleCase(Register lhs_reg)                           \
            : Instruction(Type::OpTitleCase)                    \
            , m_lhs_reg(lhs_reg)                                \
        {                                                       \
        }                                                       \
                                                                \
        void execute(Bytecode::Interpreter&) const;             \
        String to_string() const;                               \
                                                                \
    private:                                                    \
        Register m_lhs_reg;                                     \
    };

JS_ENUMERATE_COMMON_BINARY_OPS(JS_DECLARE_COMMON_BINARY_OP)
#undef JS_DECLARE_COMMON_BINARY_OP

#define JS_ENUMERATE_COMMON_UNARY_OPS(O) \
    O(BitwiseNot, bitwise_not)           \
    O(Not, not_)                         \
    O(UnaryPlus, unary_plus)             \
    O(UnaryMinus, unary_minus)           \
    O(Typeof, typeof_)

#define JS_DECLARE_COMMON_UNARY_OP(OpTitleCase, op_snake_case) \
    class OpTitleCase final : public Instruction {             \
    public:                                                    \
        OpTitleCase()                                          \
            : Instruction(Type::OpTitleCase)                   \
        {                                                      \
        }                                                      \
                                                               \
        void execute(Bytecode::Interpreter&) const;            \
        String to_string() const;                              \
    };

JS_ENUMERATE_COMMON_UNARY_OPS(JS_DECLARE_COMMON_UNARY_OP)
#undef JS_DECLARE_COMMON_UNARY_OP

class NewString final : public Instruction {
public:
    NewString(String string)
        : Instruction(Type::NewString)
        , m_string(move(string))
    {
    }

    void execute(Bytecode::Interpreter&) const;
    String to_string() const;

private:
    String m_string;
};

class NewObject final : public Instruction {
public:
    NewObject()
        : Instruction(Type::NewObject)
    {
    }

    void execute(Bytecode::Interpreter&) const;
    String to_string() const;
};

class NewBigInt final : public Instruction {
public:
    explicit NewBigInt(Crypto::SignedBigInteger bigint)
        : Instruction(Type::NewBigInt)
        , m_bigint(move(bigint))
    {
    }

    void execute(Bytecode::Interpreter&) const;
    String to_string() const;

private:
    Crypto::SignedBigInteger m_bigint;
};

class ConcatString final : public Instruction {
public:
    ConcatString(Register lhs)
        : Instruction(Type::ConcatString)
        , m_lhs(lhs)
    {
    }

    void execute(Bytecode::Interpreter&) const;
    String to_string() const;

private:
    Register m_lhs;
};

class SetVariable final : public Instruction {
public:
    SetVariable(FlyString identifier)
        : Instruction(Type::SetVariable)
        , m_identifier(move(identifier))
    {
    }

    void execute(Bytecode::Interpreter&) const;
    String to_string() const;

private:
    FlyString m_identifier;
};

class GetVariable final : public Instruction {
public:
    GetVariable(FlyString identifier)
        : Instruction(Type::GetVariable)
        , m_identifier(move(identifier))
    {
    }

    void execute(Bytecode::Interpreter&) const;
    String to_string() const;

private:
    FlyString m_identifier;
};

class GetById final : public Instruction {
public:
    GetById(FlyString property)
        : Instruction(Type::GetById)
        , m_property(move(property))
    {
    }

    void execute(Bytecode::Interpreter&) const;
    String to_string() const;

private:
    FlyString m_property;
};

class PutById final : public Instruction {
public:
    PutById(Register base, FlyString property)
        : Instruction(Type::PutById)
        , m_base(base)
        , m_property(move(property))
    {
    }

    void execute(Bytecode::Interpreter&) const;
    String to_string() const;

private:
    Register m_base;
    FlyString m_property;
};

class Jump : public Instruction {
public:
    explicit Jump(Type type, Optional<Label> target = {})
        : Instruction(type)
        , m_target(move(target))
    {
    }

    explicit Jump(Optional<Label> target = {})
        : Instruction(Type::Jump)
        , m_target(move(target))
    {
    }

    void set_target(Optional<Label> target) { m_target = move(target); }

    void execute(Bytecode::Interpreter&) const;
    String to_string() const;

protected:
    Optional<Label> m_target;
};

class JumpIfFalse final : public Jump {
public:
    explicit JumpIfFalse(Optional<Label> target = {})
        : Jump(Type::JumpIfFalse, move(target))
    {
    }

    void execute(Bytecode::Interpreter&) const;
    String to_string() const;
};

class JumpIfTrue : public Jump {
public:
    explicit JumpIfTrue(Optional<Label> target = {})
        : Jump(Type::JumpIfTrue, move(target))
    {
    }

    void execute(Bytecode::Interpreter&) const;
    String to_string() const;
};

class JumpIfNotNullish final : public Jump {
public:
    explicit JumpIfNotNullish(Optional<Label> target = {})
        : Jump(Type::JumpIfNotNullish, move(target))
    {
    }

    void execute(Bytecode::Interpreter&) const;
    String to_string() const;
};

// NOTE: This instruction is variable-width depending on the number of arguments!
class Call final : public Instruction {
public:
    Call(Register callee, Register this_value, Vector<Register> const& arguments)
        : Instruction(Type::Call)
        , m_callee(callee)
        , m_this_value(this_value)
        , m_argument_count(arguments.size())
    {
        for (size_t i = 0; i < m_argument_count; ++i)
            m_arguments[i] = arguments[i];
    }

    void execute(Bytecode::Interpreter&) const;
    String to_string() const;

    size_t length() const { return sizeof(*this) + sizeof(Register) * m_argument_count; }

private:
    Register m_callee;
    Register m_this_value;
    size_t m_argument_count { 0 };
    Register m_arguments[];
};

class EnterScope final : public Instruction {
public:
    explicit EnterScope(ScopeNode const& scope_node)
        : Instruction(Type::EnterScope)
        , m_scope_node(scope_node)
    {
    }

    void execute(Bytecode::Interpreter&) const;
    String to_string() const;

private:
    ScopeNode const& m_scope_node;
};

class Return final : public Instruction {
public:
    Return()
        : Instruction(Type::Return)
    {
    }

    void execute(Bytecode::Interpreter&) const;
    String to_string() const;
};

}
