/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/OwnPtr.h>
#include <LibJS/Bytecode/Instruction.h>
#include <LibJS/JIT/Compiler.h>
#include <LibJS/Runtime/ValueInlines.h>
#include <sys/mman.h>

namespace JS::JIT {

void Compiler::store_vm_register(Bytecode::Register dst, Assembler::Reg src)
{
    m_assembler.mov(
        Assembler::Operand::Mem64BaseAndOffset(Assembler::Reg::RegisterArrayBase, dst.index() * sizeof(Value)),
        Assembler::Operand::Register(src));
}

void Compiler::load_vm_register(Assembler::Reg dst, Bytecode::Register src)
{
    m_assembler.mov(
        Assembler::Operand::Register(dst),
        Assembler::Operand::Mem64BaseAndOffset(Assembler::Reg::RegisterArrayBase, src.index() * sizeof(Value)));
}

void Compiler::store_vm_local(size_t dst, Assembler::Reg src)
{
    m_assembler.mov(
        Assembler::Operand::Mem64BaseAndOffset(Assembler::Reg::LocalsArrayBase, dst * sizeof(Value)),
        Assembler::Operand::Register(src));
}

void Compiler::load_vm_local(Assembler::Reg dst, size_t src)
{
    m_assembler.mov(
        Assembler::Operand::Register(dst),
        Assembler::Operand::Mem64BaseAndOffset(Assembler::Reg::LocalsArrayBase, src * sizeof(Value)));
}

void Compiler::compile_load_immediate(Bytecode::Op::LoadImmediate const& op)
{
    m_assembler.mov(
        Assembler::Operand::Register(Assembler::Reg::GPR0),
        Assembler::Operand::Imm64(op.value().encoded()));
    store_vm_register(Bytecode::Register::accumulator(), Assembler::Reg::GPR0);
}

void Compiler::compile_load(Bytecode::Op::Load const& op)
{
    load_vm_register(Assembler::Reg::GPR0, op.src());
    store_vm_register(Bytecode::Register::accumulator(), Assembler::Reg::GPR0);
}

void Compiler::compile_store(Bytecode::Op::Store const& op)
{
    load_vm_register(Assembler::Reg::GPR0, Bytecode::Register::accumulator());
    store_vm_register(op.dst(), Assembler::Reg::GPR0);
}

void Compiler::compile_get_local(Bytecode::Op::GetLocal const& op)
{
    load_vm_local(Assembler::Reg::GPR0, op.index());
    store_vm_register(Bytecode::Register::accumulator(), Assembler::Reg::GPR0);
}

void Compiler::compile_set_local(Bytecode::Op::SetLocal const& op)
{
    load_vm_register(Assembler::Reg::GPR0, Bytecode::Register::accumulator());
    store_vm_local(op.index(), Assembler::Reg::GPR0);
}

void Compiler::compile_jump(Bytecode::Op::Jump const& op)
{
    m_assembler.jump(const_cast<Bytecode::BasicBlock&>(op.true_target()->block()));
}

static bool cxx_to_boolean(VM&, Value value)
{
    return value.to_boolean();
}

void Compiler::compile_to_boolean(Assembler::Reg dst, Assembler::Reg src)
{
    // dst = src;
    m_assembler.mov(
        Assembler::Operand::Register(dst),
        Assembler::Operand::Register(src));

    // dst >>= 48;
    m_assembler.shift_right(
        Assembler::Operand::Register(dst),
        Assembler::Operand::Imm8(48));

    // if (dst != BOOLEAN_TAG) goto slow_case;
    auto slow_case = m_assembler.make_label();
    m_assembler.jump_if_not_equal(
        Assembler::Operand::Register(dst),
        Assembler::Operand::Imm32(BOOLEAN_TAG),
        slow_case);

    // Fast path for JS::Value booleans.

    // dst = src;
    m_assembler.mov(
        Assembler::Operand::Register(dst),
        Assembler::Operand::Register(src));

    // dst &= 1;
    m_assembler.bitwise_and(
        Assembler::Operand::Register(dst),
        Assembler::Operand::Imm32(1));

    // goto end;
    auto end = m_assembler.jump();

    // slow_case: // call C++ helper
    slow_case.link(m_assembler);
    m_assembler.mov(
        Assembler::Operand::Register(Assembler::Reg::Arg1),
        Assembler::Operand::Register(src));
    m_assembler.native_call((void*)cxx_to_boolean);
    m_assembler.mov(
        Assembler::Operand::Register(dst),
        Assembler::Operand::Register(Assembler::Reg::Ret));

    // end:
    end.link(m_assembler);
}

void Compiler::compile_jump_conditional(Bytecode::Op::JumpConditional const& op)
{
    load_vm_register(Assembler::Reg::GPR1, Bytecode::Register::accumulator());

    compile_to_boolean(Assembler::Reg::GPR0, Assembler::Reg::GPR1);

    m_assembler.jump_conditional(Assembler::Reg::GPR0,
        const_cast<Bytecode::BasicBlock&>(op.true_target()->block()),
        const_cast<Bytecode::BasicBlock&>(op.false_target()->block()));
}

[[maybe_unused]] static Value cxx_less_than(VM& vm, Value lhs, Value rhs)
{
    // FIXME: Handle exceptions!
    return MUST(less_than(vm, lhs, rhs));
}

void Compiler::compile_less_than(Bytecode::Op::LessThan const& op)
{
    load_vm_register(Assembler::Reg::Arg1, op.lhs());
    load_vm_register(Assembler::Reg::Arg2, Bytecode::Register::accumulator());
    m_assembler.native_call((void*)cxx_less_than);
    store_vm_register(Bytecode::Register::accumulator(), Assembler::Reg::Ret);
}

[[maybe_unused]] static Value cxx_increment(VM& vm, Value value)
{
    // FIXME: Handle exceptions!
    auto old_value = MUST(value.to_numeric(vm));
    if (old_value.is_number())
        return Value(old_value.as_double() + 1);
    return BigInt::create(vm, old_value.as_bigint().big_integer().plus(Crypto::SignedBigInteger { 1 }));
}

void Compiler::compile_increment(Bytecode::Op::Increment const&)
{
    load_vm_register(Assembler::Reg::Arg1, Bytecode::Register::accumulator());
    m_assembler.native_call((void*)cxx_increment);
    store_vm_register(Bytecode::Register::accumulator(), Assembler::Reg::Ret);
}

OwnPtr<NativeExecutable> Compiler::compile(Bytecode::Executable const& bytecode_executable)
{
    if (getenv("LIBJS_NO_JIT"))
        return nullptr;

    Compiler compiler;

    compiler.m_assembler.mov(
        Assembler::Operand::Register(Assembler::Reg::RegisterArrayBase),
        Assembler::Operand::Register(Assembler::Reg::Arg1));

    compiler.m_assembler.mov(
        Assembler::Operand::Register(Assembler::Reg::LocalsArrayBase),
        Assembler::Operand::Register(Assembler::Reg::Arg2));

    for (auto& block : bytecode_executable.basic_blocks) {
        block->offset = compiler.m_output.size();
        auto it = Bytecode::InstructionStreamIterator(block->instruction_stream());
        while (!it.at_end()) {
            auto const& op = *it;
            switch (op.type()) {
            case Bytecode::Instruction::Type::LoadImmediate:
                compiler.compile_load_immediate(static_cast<Bytecode::Op::LoadImmediate const&>(op));
                break;
            case Bytecode::Instruction::Type::Store:
                compiler.compile_store(static_cast<Bytecode::Op::Store const&>(op));
                break;
            case Bytecode::Instruction::Type::Load:
                compiler.compile_load(static_cast<Bytecode::Op::Load const&>(op));
                break;
            case Bytecode::Instruction::Type::GetLocal:
                compiler.compile_get_local(static_cast<Bytecode::Op::GetLocal const&>(op));
                break;
            case Bytecode::Instruction::Type::SetLocal:
                compiler.compile_set_local(static_cast<Bytecode::Op::SetLocal const&>(op));
                break;
            case Bytecode::Instruction::Type::Jump:
                compiler.compile_jump(static_cast<Bytecode::Op::Jump const&>(op));
                break;
            case Bytecode::Instruction::Type::JumpConditional:
                compiler.compile_jump_conditional(static_cast<Bytecode::Op::JumpConditional const&>(op));
                break;
            case Bytecode::Instruction::Type::LessThan:
                compiler.compile_less_than(static_cast<Bytecode::Op::LessThan const&>(op));
                break;
            case Bytecode::Instruction::Type::Increment:
                compiler.compile_increment(static_cast<Bytecode::Op::Increment const&>(op));
                break;
            default:
                dbgln("JIT compilation failed: {}", bytecode_executable.name);
                dbgln("Unsupported bytecode op: {}", op.to_deprecated_string(bytecode_executable));
                return nullptr;
            }

            ++it;
        }
        if (!block->is_terminated())
            compiler.m_assembler.exit();
    }

    // Patch up all the jumps
    for (auto& block : bytecode_executable.basic_blocks) {
        for (auto& jump : block->jumps_to_here) {
            auto offset = block->offset - jump - 4;
            compiler.m_output[jump + 0] = (offset >> 0) & 0xff;
            compiler.m_output[jump + 1] = (offset >> 8) & 0xff;
            compiler.m_output[jump + 2] = (offset >> 16) & 0xff;
            compiler.m_output[jump + 3] = (offset >> 24) & 0xff;
        }
    }

    write(STDOUT_FILENO, compiler.m_output.data(), compiler.m_output.size());

    auto* executable_memory = mmap(nullptr, compiler.m_output.size(), PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE, 0, 0);
    if (executable_memory == MAP_FAILED) {
        perror("mmap");
        return nullptr;
    }

    memcpy(executable_memory, compiler.m_output.data(), compiler.m_output.size());
    mprotect(executable_memory, compiler.m_output.size(), PROT_READ | PROT_EXEC);
    return make<NativeExecutable>(executable_memory, compiler.m_output.size());
}

}
