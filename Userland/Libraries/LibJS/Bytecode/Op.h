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
        : m_dst(dst)
        , m_value(value)
    {
    }

    virtual ~Load() override { }
    virtual void execute(Bytecode::Interpreter&) const override;
    virtual String to_string() const override;

private:
    Register m_dst;
    Value m_value;
};

class Add final : public Instruction {
public:
    Add(Register dst, Register src1, Register src2)
        : m_dst(dst)
        , m_src1(src1)
        , m_src2(src2)
    {
    }

    virtual ~Add() override { }
    virtual void execute(Bytecode::Interpreter&) const override;
    virtual String to_string() const override;

private:
    Register m_dst;
    Register m_src1;
    Register m_src2;
};

class Sub final : public Instruction {
public:
    Sub(Register dst, Register src1, Register src2)
        : m_dst(dst)
        , m_src1(src1)
        , m_src2(src2)
    {
    }

    virtual ~Sub() override { }
    virtual void execute(Bytecode::Interpreter&) const override;
    virtual String to_string() const override;

private:
    Register m_dst;
    Register m_src1;
    Register m_src2;
};

class LessThan final : public Instruction {
public:
    LessThan(Register dst, Register src1, Register src2)
        : m_dst(dst)
        , m_src1(src1)
        , m_src2(src2)
    {
    }

    virtual ~LessThan() override { }
    virtual void execute(Bytecode::Interpreter&) const override;
    virtual String to_string() const override;

private:
    Register m_dst;
    Register m_src1;
    Register m_src2;
};

class AbstractInequals final : public Instruction {
public:
    AbstractInequals(Register dst, Register src1, Register src2)
        : m_dst(dst)
        , m_src1(src1)
        , m_src2(src2)
    {
    }

    virtual ~AbstractInequals() override { }
    virtual void execute(Bytecode::Interpreter&) const override;
    virtual String to_string() const override;

private:
    Register m_dst;
    Register m_src1;
    Register m_src2;
};

class NewString final : public Instruction {
public:
    NewString(Register dst, String string)
        : m_dst(dst)
        , m_string(move(string))
    {
    }

    virtual ~NewString() override { }
    virtual void execute(Bytecode::Interpreter&) const override;
    virtual String to_string() const override;

private:
    Register m_dst;
    String m_string;
};

class SetVariable final : public Instruction {
public:
    SetVariable(FlyString identifier, Register src)
        : m_identifier(move(identifier))
        , m_src(src)
    {
    }

    virtual ~SetVariable() override { }
    virtual void execute(Bytecode::Interpreter&) const override;
    virtual String to_string() const override;

private:
    FlyString m_identifier;
    Register m_src;
};

class GetVariable final : public Instruction {
public:
    GetVariable(Register dst, FlyString identifier)
        : m_dst(dst)
        , m_identifier(move(identifier))
    {
    }

    virtual ~GetVariable() override { }
    virtual void execute(Bytecode::Interpreter&) const override;
    virtual String to_string() const override;

private:
    Register m_dst;
    FlyString m_identifier;
};

class Jump final : public Instruction {
public:
    explicit Jump(Label target)
        : m_target(target)
    {
    }

    virtual ~Jump() override { }
    virtual void execute(Bytecode::Interpreter&) const override;
    virtual String to_string() const override;

private:
    Label m_target;
};

class JumpIfFalse final : public Instruction {
public:
    explicit JumpIfFalse(Register result, Optional<Label> target = {})
        : m_result(result)
        , m_target(move(target))
    {
    }

    void set_target(Optional<Label> target) { m_target = move(target); }

    virtual ~JumpIfFalse() override { }
    virtual void execute(Bytecode::Interpreter&) const override;
    virtual String to_string() const override;

private:
    Register m_result;
    Optional<Label> m_target;
};

class JumpIfTrue final : public Instruction {
public:
    explicit JumpIfTrue(Register result, Optional<Label> target = {})
        : m_result(result)
        , m_target(move(target))
    {
    }

    void set_target(Optional<Label> target) { m_target = move(target); }

    virtual ~JumpIfTrue() override { }
    virtual void execute(Bytecode::Interpreter&) const override;
    virtual String to_string() const override;

private:
    Register m_result;
    Optional<Label> m_target;
};

}
