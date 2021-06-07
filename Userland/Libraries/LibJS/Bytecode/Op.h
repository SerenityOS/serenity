/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <LibJS/Bytecode/Instruction.h>
#include <LibJS/Bytecode/Label.h>
#include <LibJS/Bytecode/Register.h>
#include <LibJS/Heap/Cell.h>
#include <LibJS/Runtime/Value.h>

namespace JS::Bytecode::Op {

class Load final : public Instruction {
public:
    Load(Register dst, Value value)
        : Instruction(Type::Load)
        , m_dst(dst)
        , m_value(value)
    {
    }

    void execute(Bytecode::Interpreter&) const;
    String to_string() const;

private:
    Register m_dst;
    Value m_value;
};

class LoadRegister final : public Instruction {
public:
    LoadRegister(Register dst, Register src)
        : Instruction(Type::LoadRegister)
        , m_dst(dst)
        , m_src(src)
    {
    }

    void execute(Bytecode::Interpreter&) const;
    String to_string() const;

private:
    Register m_dst;
    Register m_src;
};

class Add final : public Instruction {
public:
    Add(Register dst, Register src1, Register src2)
        : Instruction(Type::Add)
        , m_dst(dst)
        , m_src1(src1)
        , m_src2(src2)
    {
    }

    void execute(Bytecode::Interpreter&) const;
    String to_string() const;

private:
    Register m_dst;
    Register m_src1;
    Register m_src2;
};

class Sub final : public Instruction {
public:
    Sub(Register dst, Register src1, Register src2)
        : Instruction(Type::Sub)
        , m_dst(dst)
        , m_src1(src1)
        , m_src2(src2)
    {
    }

    void execute(Bytecode::Interpreter&) const;
    String to_string() const;

private:
    Register m_dst;
    Register m_src1;
    Register m_src2;
};

class Mul final : public Instruction {
public:
    Mul(Register dst, Register src1, Register src2)
        : Instruction(Type::Mul)
        , m_dst(dst)
        , m_src1(src1)
        , m_src2(src2)
    {
    }

    void execute(Bytecode::Interpreter&) const;
    String to_string() const;

private:
    Register m_dst;
    Register m_src1;
    Register m_src2;
};

class Div final : public Instruction {
public:
    Div(Register dst, Register src1, Register src2)
        : Instruction(Type::Div)
        , m_dst(dst)
        , m_src1(src1)
        , m_src2(src2)
    {
    }

    void execute(Bytecode::Interpreter&) const;
    String to_string() const;

private:
    Register m_dst;
    Register m_src1;
    Register m_src2;
};

class Mod final : public Instruction {
public:
    Mod(Register dst, Register src1, Register src2)
        : Instruction(Type::Mod)
        , m_dst(dst)
        , m_src1(src1)
        , m_src2(src2)
    {
    }

    void execute(Bytecode::Interpreter&) const;
    String to_string() const;

private:
    Register m_dst;
    Register m_src1;
    Register m_src2;
};

class Exp final : public Instruction {
public:
    Exp(Register dst, Register src1, Register src2)
        : Instruction(Type::Exp)
        , m_dst(dst)
        , m_src1(src1)
        , m_src2(src2)
    {
    }

    void execute(Bytecode::Interpreter&) const;
    String to_string() const;

private:
    Register m_dst;
    Register m_src1;
    Register m_src2;
};

class GreaterThan final : public Instruction {
public:
    GreaterThan(Register dst, Register src1, Register src2)
        : Instruction(Type::GreaterThan)
        , m_dst(dst)
        , m_src1(src1)
        , m_src2(src2)
    {
    }

    void execute(Bytecode::Interpreter&) const;
    String to_string() const;

private:
    Register m_dst;
    Register m_src1;
    Register m_src2;
};

class GreaterThanEquals final : public Instruction {
public:
    GreaterThanEquals(Register dst, Register src1, Register src2)
        : Instruction(Type::GreaterThanEquals)
        , m_dst(dst)
        , m_src1(src1)
        , m_src2(src2)
    {
    }

    void execute(Bytecode::Interpreter&) const;
    String to_string() const;

private:
    Register m_dst;
    Register m_src1;
    Register m_src2;
};

class LessThan final : public Instruction {
public:
    LessThan(Register dst, Register src1, Register src2)
        : Instruction(Type::LessThan)
        , m_dst(dst)
        , m_src1(src1)
        , m_src2(src2)
    {
    }

    void execute(Bytecode::Interpreter&) const;
    String to_string() const;

private:
    Register m_dst;
    Register m_src1;
    Register m_src2;
};

class LessThanEquals final : public Instruction {
public:
    LessThanEquals(Register dst, Register src1, Register src2)
        : Instruction(Type::LessThanEquals)
        , m_dst(dst)
        , m_src1(src1)
        , m_src2(src2)
    {
    }

    void execute(Bytecode::Interpreter&) const;
    String to_string() const;

private:
    Register m_dst;
    Register m_src1;
    Register m_src2;
};

class AbstractInequals final : public Instruction {
public:
    AbstractInequals(Register dst, Register src1, Register src2)
        : Instruction(Type::AbstractInequals)
        , m_dst(dst)
        , m_src1(src1)
        , m_src2(src2)
    {
    }

    void execute(Bytecode::Interpreter&) const;
    String to_string() const;

private:
    Register m_dst;
    Register m_src1;
    Register m_src2;
};

class AbstractEquals final : public Instruction {
public:
    AbstractEquals(Register dst, Register src1, Register src2)
        : Instruction(Type::AbstractEquals)
        , m_dst(dst)
        , m_src1(src1)
        , m_src2(src2)
    {
    }

    void execute(Bytecode::Interpreter&) const;
    String to_string() const;

private:
    Register m_dst;
    Register m_src1;
    Register m_src2;
};

class TypedInequals final : public Instruction {
public:
    TypedInequals(Register dst, Register src1, Register src2)
        : Instruction(Type::TypedInequals)
        , m_dst(dst)
        , m_src1(src1)
        , m_src2(src2)
    {
    }

    void execute(Bytecode::Interpreter&) const;
    String to_string() const;

private:
    Register m_dst;
    Register m_src1;
    Register m_src2;
};

class TypedEquals final : public Instruction {
public:
    TypedEquals(Register dst, Register src1, Register src2)
        : Instruction(Type::TypedEquals)
        , m_dst(dst)
        , m_src1(src1)
        , m_src2(src2)
    {
    }

    void execute(Bytecode::Interpreter&) const;
    String to_string() const;

private:
    Register m_dst;
    Register m_src1;
    Register m_src2;
};

class BitwiseAnd final : public Instruction {
public:
    BitwiseAnd(Register dst, Register src1, Register src2)
        : Instruction(Type::BitwiseAnd)
        , m_dst(dst)
        , m_src1(src1)
        , m_src2(src2)
    {
    }

    void execute(Bytecode::Interpreter&) const;
    String to_string() const;

private:
    Register m_dst;
    Register m_src1;
    Register m_src2;
};

class BitwiseOr final : public Instruction {
public:
    BitwiseOr(Register dst, Register src1, Register src2)
        : Instruction(Type::BitwiseOr)
        , m_dst(dst)
        , m_src1(src1)
        , m_src2(src2)
    {
    }

    void execute(Bytecode::Interpreter&) const;
    String to_string() const;

private:
    Register m_dst;
    Register m_src1;
    Register m_src2;
};

class BitwiseXor final : public Instruction {
public:
    BitwiseXor(Register dst, Register src1, Register src2)
        : Instruction(Type::BitwiseXor)
        , m_dst(dst)
        , m_src1(src1)
        , m_src2(src2)
    {
    }

    void execute(Bytecode::Interpreter&) const;
    String to_string() const;

private:
    Register m_dst;
    Register m_src1;
    Register m_src2;
};

class LeftShift final : public Instruction {
public:
    LeftShift(Register dst, Register src1, Register src2)
        : Instruction(Type::LeftShift)
        , m_dst(dst)
        , m_src1(src1)
        , m_src2(src2)
    {
    }

    void execute(Bytecode::Interpreter&) const;
    String to_string() const;

private:
    Register m_dst;
    Register m_src1;
    Register m_src2;
};

class RightShift final : public Instruction {
public:
    RightShift(Register dst, Register src1, Register src2)
        : Instruction(Type::RightShift)
        , m_dst(dst)
        , m_src1(src1)
        , m_src2(src2)
    {
    }

    void execute(Bytecode::Interpreter&) const;
    String to_string() const;

private:
    Register m_dst;
    Register m_src1;
    Register m_src2;
};

class UnsignedRightShift final : public Instruction {
public:
    UnsignedRightShift(Register dst, Register src1, Register src2)
        : Instruction(Type::UnsignedRightShift)
        , m_dst(dst)
        , m_src1(src1)
        , m_src2(src2)
    {
    }

    void execute(Bytecode::Interpreter&) const;
    String to_string() const;

private:
    Register m_dst;
    Register m_src1;
    Register m_src2;
};

class BitwiseNot final : public Instruction {
public:
    BitwiseNot(Register dst, Register src)
        : Instruction(Type::BitwiseNot)
        , m_dst(dst)
        , m_src(src)
    {
    }

    void execute(Bytecode::Interpreter&) const;
    String to_string() const;

private:
    Register m_dst;
    Register m_src;
};

class Not final : public Instruction {
public:
    Not(Register dst, Register src)
        : Instruction(Type::Not)
        , m_dst(dst)
        , m_src(src)
    {
    }

    void execute(Bytecode::Interpreter&) const;
    String to_string() const;

private:
    Register m_dst;
    Register m_src;
};

class UnaryPlus final : public Instruction {
public:
    UnaryPlus(Register dst, Register src)
        : Instruction(Type::UnaryPlus)
        , m_dst(dst)
        , m_src(src)
    {
    }

    void execute(Bytecode::Interpreter&) const;
    String to_string() const;

private:
    Register m_dst;
    Register m_src;
};

class UnaryMinus final : public Instruction {
public:
    UnaryMinus(Register dst, Register src)
        : Instruction(Type::UnaryMinus)
        , m_dst(dst)
        , m_src(src)
    {
    }

    void execute(Bytecode::Interpreter&) const;
    String to_string() const;

private:
    Register m_dst;
    Register m_src;
};

class Typeof final : public Instruction {
public:
    Typeof(Register dst, Register src)
        : Instruction(Type::Typeof)
        , m_dst(dst)
        , m_src(src)
    {
    }

    void execute(Bytecode::Interpreter&) const;
    String to_string() const;

private:
    Register m_dst;
    Register m_src;
};

class NewString final : public Instruction {
public:
    NewString(Register dst, String string)
        : Instruction(Type::NewString)
        , m_dst(dst)
        , m_string(move(string))
    {
    }

    void execute(Bytecode::Interpreter&) const;
    String to_string() const;

private:
    Register m_dst;
    String m_string;
};

class NewObject final : public Instruction {
public:
    explicit NewObject(Register dst)
        : Instruction(Type::NewObject)
        , m_dst(dst)
    {
    }

    void execute(Bytecode::Interpreter&) const;
    String to_string() const;

private:
    Register m_dst;
};

class SetVariable final : public Instruction {
public:
    SetVariable(FlyString identifier, Register src)
        : Instruction(Type::SetVariable)
        , m_identifier(move(identifier))
        , m_src(src)
    {
    }

    void execute(Bytecode::Interpreter&) const;
    String to_string() const;

private:
    FlyString m_identifier;
    Register m_src;
};

class GetVariable final : public Instruction {
public:
    GetVariable(Register dst, FlyString identifier)
        : Instruction(Type::GetVariable)
        , m_dst(dst)
        , m_identifier(move(identifier))
    {
    }

    void execute(Bytecode::Interpreter&) const;
    String to_string() const;

private:
    Register m_dst;
    FlyString m_identifier;
};

class GetById final : public Instruction {
public:
    GetById(Register dst, Register base, FlyString property)
        : Instruction(Type::GetById)
        , m_dst(dst)
        , m_base(base)
        , m_property(move(property))
    {
    }

    void execute(Bytecode::Interpreter&) const;
    String to_string() const;

private:
    Register m_dst;
    Register m_base;
    FlyString m_property;
};

class PutById final : public Instruction {
public:
    PutById(Register base, FlyString property, Register src)
        : Instruction(Type::PutById)
        , m_base(base)
        , m_property(move(property))
        , m_src(src)
    {
    }

    void execute(Bytecode::Interpreter&) const;
    String to_string() const;

private:
    Register m_base;
    FlyString m_property;
    Register m_src;
};

class Jump final : public Instruction {
public:
    explicit Jump(Optional<Label> target = {})
        : Instruction(Type::Jump)
        , m_target(move(target))
    {
    }

    void set_target(Optional<Label> target) { m_target = move(target); }

    void execute(Bytecode::Interpreter&) const;
    String to_string() const;

private:
    Optional<Label> m_target;
};

class JumpIfFalse final : public Instruction {
public:
    explicit JumpIfFalse(Register result, Optional<Label> target = {})
        : Instruction(Type::JumpIfFalse)
        , m_result(result)
        , m_target(move(target))
    {
    }

    void set_target(Optional<Label> target) { m_target = move(target); }

    void execute(Bytecode::Interpreter&) const;
    String to_string() const;

private:
    Register m_result;
    Optional<Label> m_target;
};

class JumpIfTrue final : public Instruction {
public:
    explicit JumpIfTrue(Register result, Optional<Label> target = {})
        : Instruction(Type::JumpIfTrue)
        , m_result(result)
        , m_target(move(target))
    {
    }

    void set_target(Optional<Label> target) { m_target = move(target); }

    void execute(Bytecode::Interpreter&) const;
    String to_string() const;

private:
    Register m_result;
    Optional<Label> m_target;
};

// NOTE: This instruction is variable-width depending on the number of arguments!
class Call final : public Instruction {
public:
    Call(Register dst, Register callee, Register this_value, Vector<Register> const& arguments)
        : Instruction(Type::Call)
        , m_dst(dst)
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
    Register m_dst;
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
    explicit Return(Optional<Register> argument)
        : Instruction(Type::Return)
        , m_argument(move(argument))
    {
    }

    void execute(Bytecode::Interpreter&) const;
    String to_string() const;

private:
    Optional<Register> m_argument;
};

}
