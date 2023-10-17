/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/OwnPtr.h>
#include <LibJS/Bytecode/Instruction.h>
#include <LibJS/Bytecode/Interpreter.h>
#include <LibJS/JIT/Compiler.h>
#include <LibJS/Runtime/VM.h>
#include <LibJS/Runtime/ValueInlines.h>
#include <sys/mman.h>
#include <unistd.h>

#define TRY_OR_SET_EXCEPTION(expression)                                                                                        \
    ({                                                                                                                          \
        /* Ignore -Wshadow to allow nesting the macro. */                                                                       \
        AK_IGNORE_DIAGNOSTIC("-Wshadow",                                                                                        \
            auto&& _temporary_result = (expression));                                                                           \
        static_assert(!::AK::Detail::IsLvalueReference<decltype(_temporary_result.release_value())>,                            \
            "Do not return a reference from a fallible expression");                                                            \
        if (_temporary_result.is_error()) [[unlikely]] {                                                                        \
            vm.bytecode_interpreter().reg(Bytecode::Register::exception()) = _temporary_result.release_error().value().value(); \
            return {};                                                                                                          \
        }                                                                                                                       \
        _temporary_result.release_value();                                                                                      \
    })

namespace JS::JIT {

void Compiler::store_vm_register(Bytecode::Register dst, Assembler::Reg src)
{
    m_assembler.mov(
        Assembler::Operand::Mem64BaseAndOffset(REGISTER_ARRAY_BASE, dst.index() * sizeof(Value)),
        Assembler::Operand::Register(src));
}

void Compiler::load_vm_register(Assembler::Reg dst, Bytecode::Register src)
{
    m_assembler.mov(
        Assembler::Operand::Register(dst),
        Assembler::Operand::Mem64BaseAndOffset(REGISTER_ARRAY_BASE, src.index() * sizeof(Value)));
}

void Compiler::store_vm_local(size_t dst, Assembler::Reg src)
{
    m_assembler.mov(
        Assembler::Operand::Mem64BaseAndOffset(LOCALS_ARRAY_BASE, dst * sizeof(Value)),
        Assembler::Operand::Register(src));
}

void Compiler::load_vm_local(Assembler::Reg dst, size_t src)
{
    m_assembler.mov(
        Assembler::Operand::Register(dst),
        Assembler::Operand::Mem64BaseAndOffset(LOCALS_ARRAY_BASE, src * sizeof(Value)));
}

void Compiler::compile_load_immediate(Bytecode::Op::LoadImmediate const& op)
{
    m_assembler.mov(
        Assembler::Operand::Register(GPR0),
        Assembler::Operand::Imm64(op.value().encoded()));
    store_vm_register(Bytecode::Register::accumulator(), GPR0);
}

void Compiler::compile_load(Bytecode::Op::Load const& op)
{
    load_vm_register(GPR0, op.src());
    store_vm_register(Bytecode::Register::accumulator(), GPR0);
}

void Compiler::compile_store(Bytecode::Op::Store const& op)
{
    load_vm_register(GPR0, Bytecode::Register::accumulator());
    store_vm_register(op.dst(), GPR0);
}

void Compiler::compile_get_local(Bytecode::Op::GetLocal const& op)
{
    load_vm_local(GPR0, op.index());
    store_vm_register(Bytecode::Register::accumulator(), GPR0);
}

void Compiler::compile_set_local(Bytecode::Op::SetLocal const& op)
{
    load_vm_register(GPR0, Bytecode::Register::accumulator());
    store_vm_local(op.index(), GPR0);
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
        Assembler::Operand::Register(ARG1),
        Assembler::Operand::Register(src));
    m_assembler.native_call((void*)cxx_to_boolean);
    m_assembler.mov(
        Assembler::Operand::Register(dst),
        Assembler::Operand::Register(RET));

    // end:
    end.link(m_assembler);
}

void Compiler::compile_jump_conditional(Bytecode::Op::JumpConditional const& op)
{
    load_vm_register(GPR1, Bytecode::Register::accumulator());

    compile_to_boolean(GPR0, GPR1);

    m_assembler.jump_conditional(GPR0,
        const_cast<Bytecode::BasicBlock&>(op.true_target()->block()),
        const_cast<Bytecode::BasicBlock&>(op.false_target()->block()));
}

[[maybe_unused]] static Value cxx_less_than(VM& vm, Value lhs, Value rhs)
{
    return TRY_OR_SET_EXCEPTION(less_than(vm, lhs, rhs));
}

void Compiler::compile_less_than(Bytecode::Op::LessThan const& op)
{
    load_vm_register(ARG1, op.lhs());
    load_vm_register(ARG2, Bytecode::Register::accumulator());
    m_assembler.native_call((void*)cxx_less_than);
    store_vm_register(Bytecode::Register::accumulator(), RET);
    check_exception();
}

[[maybe_unused]] static Value cxx_increment(VM& vm, Value value)
{
    auto old_value = TRY_OR_SET_EXCEPTION(value.to_numeric(vm));
    if (old_value.is_number())
        return Value(old_value.as_double() + 1);
    return BigInt::create(vm, old_value.as_bigint().big_integer().plus(Crypto::SignedBigInteger { 1 }));
}

void Compiler::compile_increment(Bytecode::Op::Increment const&)
{
    load_vm_register(ARG1, Bytecode::Register::accumulator());
    m_assembler.native_call((void*)cxx_increment);
    store_vm_register(Bytecode::Register::accumulator(), RET);
    check_exception();
}

void Compiler::check_exception()
{
    // if (exception.is_empty()) goto no_exception;
    load_vm_register(GPR0, Bytecode::Register::exception());
    m_assembler.mov(Assembler::Operand::Register(GPR1), Assembler::Operand::Imm64(Value().encoded()));
    auto no_exception = m_assembler.make_label();
    m_assembler.jump_if_equal(Assembler::Operand::Register(GPR0), Assembler::Operand::Register(GPR1), no_exception);

    // We have an exception!

    // if (!unwind_context.valid) return;
    auto handle_exception = m_assembler.make_label();
    m_assembler.mov(
        Assembler::Operand::Register(GPR0),
        Assembler::Operand::Mem64BaseAndOffset(UNWIND_CONTEXT_BASE, 0));
    m_assembler.jump_if_not_equal(
        Assembler::Operand::Register(GPR0),
        Assembler::Operand::Imm32(0),
        handle_exception);

    m_assembler.exit();

    // handle_exception:
    handle_exception.link(m_assembler);

    // if (unwind_context.handler) {
    //     accumulator = exception;
    //     exception = Value();
    //     goto handler;
    // }
    auto no_handler = m_assembler.make_label();
    m_assembler.mov(
        Assembler::Operand::Register(GPR0),
        Assembler::Operand::Mem64BaseAndOffset(UNWIND_CONTEXT_BASE, 8));
    m_assembler.jump_if_equal(
        Assembler::Operand::Register(GPR0),
        Assembler::Operand::Imm32(0),
        no_handler);
    load_vm_register(GPR1, Bytecode::Register::exception());
    store_vm_register(Bytecode::Register::accumulator(), GPR1);
    m_assembler.mov(
        Assembler::Operand::Register(GPR1),
        Assembler::Operand::Imm64(Value().encoded()));
    store_vm_register(Bytecode::Register::exception(), GPR1);
    m_assembler.jump(Assembler::Operand::Register(GPR0));

    // no_handler:
    no_handler.link(m_assembler);

    // if (unwind_context.finalizer) goto finalizer;
    auto no_finalizer = m_assembler.make_label();
    m_assembler.mov(
        Assembler::Operand::Register(GPR0),
        Assembler::Operand::Mem64BaseAndOffset(UNWIND_CONTEXT_BASE, 16));
    m_assembler.jump_if_equal(
        Assembler::Operand::Register(GPR0),
        Assembler::Operand::Imm32(0),
        no_finalizer);

    m_assembler.jump(Assembler::Operand::Register(GPR0));

    // no_finalizer:
    // NOTE: No catch and no finally!? Crash.
    no_finalizer.link(m_assembler);
    m_assembler.verify_not_reached();

    // no_exception:
    no_exception.link(m_assembler);
}

void Compiler::push_unwind_context(bool valid, Optional<Bytecode::Label> const& handler, Optional<Bytecode::Label> const& finalizer)
{
    // Put this on the stack, and then point UNWIND_CONTEXT_BASE at it.
    // struct {
    //     u64 valid;
    //     u64 handler;
    //     u64 finalizer;
    // };

    // push finalizer (patched later)
    m_assembler.mov(
        Assembler::Operand::Register(GPR0),
        Assembler::Operand::Imm64(0));
    if (finalizer.has_value())
        const_cast<Bytecode::BasicBlock&>(finalizer.value().block()).absolute_references_to_here.append(m_assembler.m_output.size() - 8);
    m_assembler.push(Assembler::Operand::Register(GPR0));

    // push handler (patched later)
    m_assembler.mov(
        Assembler::Operand::Register(GPR0),
        Assembler::Operand::Imm64(0));
    if (handler.has_value())
        const_cast<Bytecode::BasicBlock&>(handler.value().block()).absolute_references_to_here.append(m_assembler.m_output.size() - 8);
    m_assembler.push(Assembler::Operand::Register(GPR0));

    // push valid
    m_assembler.push(Assembler::Operand::Imm32(valid));

    // UNWIND_CONTEXT_BASE = STACK_POINTER
    m_assembler.mov(
        Assembler::Operand::Register(UNWIND_CONTEXT_BASE),
        Assembler::Operand::Register(STACK_POINTER));

    // align stack pointer
    m_assembler.sub(Assembler::Operand::Register(STACK_POINTER), Assembler::Operand::Imm8(8));
}

void Compiler::pop_unwind_context()
{
    m_assembler.add(Assembler::Operand::Register(STACK_POINTER), Assembler::Operand::Imm8(32));
    m_assembler.add(Assembler::Operand::Register(UNWIND_CONTEXT_BASE), Assembler::Operand::Imm8(32));
}

void Compiler::compile_enter_unwind_context(Bytecode::Op::EnterUnwindContext const& op)
{
    push_unwind_context(true, op.handler_target(), op.finalizer_target());

    m_assembler.jump(const_cast<Bytecode::BasicBlock&>(op.entry_point().block()));
}

void Compiler::compile_leave_unwind_context(Bytecode::Op::LeaveUnwindContext const&)
{
    pop_unwind_context();
}

void Compiler::compile_throw(Bytecode::Op::Throw const&)
{
    load_vm_register(GPR0, Bytecode::Register::accumulator());	
    store_vm_register(Bytecode::Register::exception(), GPR0);
    check_exception();
}

OwnPtr<NativeExecutable> Compiler::compile(Bytecode::Executable const& bytecode_executable)
{
    if (getenv("LIBJS_NO_JIT"))
        return nullptr;

    Compiler compiler;

    compiler.m_assembler.enter();

    compiler.m_assembler.mov(
        Assembler::Operand::Register(REGISTER_ARRAY_BASE),
        Assembler::Operand::Register(ARG1));

    compiler.m_assembler.mov(
        Assembler::Operand::Register(LOCALS_ARRAY_BASE),
        Assembler::Operand::Register(ARG2));

    compiler.push_unwind_context(false, {}, {});

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
            case Bytecode::Instruction::Type::EnterUnwindContext:
                compiler.compile_enter_unwind_context(static_cast<Bytecode::Op::EnterUnwindContext const&>(op));
                break;
            case Bytecode::Instruction::Type::LeaveUnwindContext:
                compiler.compile_leave_unwind_context(static_cast<Bytecode::Op::LeaveUnwindContext const&>(op));
                break;
            case Bytecode::Instruction::Type::Throw:
                compiler.compile_throw(static_cast<Bytecode::Op::Throw const&>(op));
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

    auto* executable_memory = mmap(nullptr, compiler.m_output.size(), PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE, 0, 0);
    if (executable_memory == MAP_FAILED) {
        perror("mmap");
        return nullptr;
    }

    for (auto& block : bytecode_executable.basic_blocks) {
        // Patch up all the jumps
        for (auto& jump : block->jumps_to_here) {
            auto offset = block->offset - jump - 4;
            compiler.m_output[jump + 0] = (offset >> 0) & 0xff;
            compiler.m_output[jump + 1] = (offset >> 8) & 0xff;
            compiler.m_output[jump + 2] = (offset >> 16) & 0xff;
            compiler.m_output[jump + 3] = (offset >> 24) & 0xff;
        }

        // Patch up all the absolute references
        for (auto& absolute_reference : block->absolute_references_to_here) {
            auto offset = bit_cast<u64>(executable_memory) + block->offset;
            compiler.m_output[absolute_reference + 0] = (offset >> 0) & 0xff;
            compiler.m_output[absolute_reference + 1] = (offset >> 8) & 0xff;
            compiler.m_output[absolute_reference + 2] = (offset >> 16) & 0xff;
            compiler.m_output[absolute_reference + 3] = (offset >> 24) & 0xff;
            compiler.m_output[absolute_reference + 4] = (offset >> 32) & 0xff;
            compiler.m_output[absolute_reference + 5] = (offset >> 40) & 0xff;
            compiler.m_output[absolute_reference + 6] = (offset >> 48) & 0xff;
            compiler.m_output[absolute_reference + 7] = (offset >> 56) & 0xff;
        }
    }

    size_t res = write(STDOUT_FILENO, compiler.m_output.data(), compiler.m_output.size());
    if (!res) {}

    memcpy(executable_memory, compiler.m_output.data(), compiler.m_output.size());
    mprotect(executable_memory, compiler.m_output.size(), PROT_READ | PROT_EXEC);
    return make<NativeExecutable>(executable_memory, compiler.m_output.size());
}

}
