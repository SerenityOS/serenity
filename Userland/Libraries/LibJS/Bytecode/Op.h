/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Bytecode/Instruction.h>
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

}
