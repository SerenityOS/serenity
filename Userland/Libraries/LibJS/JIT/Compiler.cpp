/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Simon Wanner <simon@skyrising.xyz>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/OwnPtr.h>
#include <AK/Platform.h>
#include <LibJS/Bytecode/CommonImplementations.h>
#include <LibJS/Bytecode/Instruction.h>
#include <LibJS/Bytecode/Interpreter.h>
#include <LibJS/Bytecode/RegexTable.h>
#include <LibJS/JIT/Compiler.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/DeclarativeEnvironment.h>
#include <LibJS/Runtime/ECMAScriptFunctionObject.h>
#include <LibJS/Runtime/FunctionEnvironment.h>
#include <LibJS/Runtime/VM.h>
#include <LibJS/Runtime/ValueInlines.h>
#include <sys/mman.h>
#include <unistd.h>

#ifdef JIT_ARCH_SUPPORTED

#    define LOG_JIT_SUCCESS 0
#    define LOG_JIT_FAILURE 1
#    define DUMP_JIT_MACHINE_CODE_TO_STDOUT 0
#    define DUMP_JIT_DISASSEMBLY 0

#    define TRY_OR_SET_EXCEPTION(expression)                                                                                        \
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

void Compiler::load_accumulator(Assembler::Reg dst)
{
    m_assembler.mov(
        Assembler::Operand::Register(dst),
        Assembler::Operand::Register(CACHED_ACCUMULATOR));
}

void Compiler::store_accumulator(Assembler::Reg src)
{
    m_assembler.mov(
        Assembler::Operand::Register(CACHED_ACCUMULATOR),
        Assembler::Operand::Register(src));
}

void Compiler::reload_cached_accumulator()
{
    m_assembler.mov(
        Assembler::Operand::Register(CACHED_ACCUMULATOR),
        Assembler::Operand::Mem64BaseAndOffset(REGISTER_ARRAY_BASE, Bytecode::Register::accumulator_index * sizeof(Value)));
}

void Compiler::flush_cached_accumulator()
{
    m_assembler.mov(
        Assembler::Operand::Mem64BaseAndOffset(REGISTER_ARRAY_BASE, Bytecode::Register::accumulator_index * sizeof(Value)),
        Assembler::Operand::Register(CACHED_ACCUMULATOR));
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
        Assembler::Operand::Imm(op.value().encoded()));
    store_accumulator(GPR0);
}

void Compiler::compile_load(Bytecode::Op::Load const& op)
{
    load_vm_register(GPR0, op.src());
    store_accumulator(GPR0);
}

void Compiler::compile_store(Bytecode::Op::Store const& op)
{
    load_accumulator(GPR0);
    store_vm_register(op.dst(), GPR0);
}

static Value cxx_throw_binding_not_initialized(VM& vm, size_t index)
{
    auto const& variable_name = vm.running_execution_context().function->local_variables_names()[index];
    TRY_OR_SET_EXCEPTION(vm.throw_completion<ReferenceError>(ErrorType::BindingNotInitialized, variable_name));
    return {};
}

void Compiler::compile_get_local(Bytecode::Op::GetLocal const& op)
{
    load_vm_local(GPR0, op.index());

    // if (GPR0 == <empty>) throw ReferenceError(BindingNotInitialized)
    Assembler::Label not_empty {};
    m_assembler.mov(
        Assembler::Operand::Register(GPR1),
        Assembler::Operand::Imm(Value().encoded()));
    m_assembler.jump_if(
        Assembler::Operand::Register(GPR0),
        Assembler::Condition::NotEqualTo,
        Assembler::Operand::Register(GPR1),
        not_empty);
    m_assembler.mov(Assembler::Operand::Register(ARG1), Assembler::Operand::Imm(op.index()));
    native_call((void*)cxx_throw_binding_not_initialized);
    check_exception();
    not_empty.link(m_assembler);

    store_accumulator(GPR0);
}

void Compiler::compile_set_local(Bytecode::Op::SetLocal const& op)
{
    load_accumulator(GPR0);
    store_vm_local(op.index(), GPR0);
}

static Value cxx_typeof_local(VM& vm, Value value)
{
    return PrimitiveString::create(vm, value.typeof());
}

void Compiler::compile_typeof_local(Bytecode::Op::TypeofLocal const& op)
{
    load_vm_local(ARG1, op.index());
    native_call((void*)cxx_typeof_local);
    store_accumulator(GPR0);
}

void Compiler::compile_jump(Bytecode::Op::Jump const& op)
{
    m_assembler.jump(label_for(op.true_target()->block()));
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
        Assembler::Operand::Imm(48));

    // if (dst != BOOLEAN_TAG) goto slow_case;
    Assembler::Label slow_case {};
    m_assembler.jump_if(
        Assembler::Operand::Register(dst),
        Assembler::Condition::NotEqualTo,
        Assembler::Operand::Imm(BOOLEAN_TAG),
        slow_case);

    // Fast path for JS::Value booleans.

    // dst = src;
    m_assembler.mov(
        Assembler::Operand::Register(dst),
        Assembler::Operand::Register(src));

    // goto end;
    auto end = m_assembler.jump();

    // slow_case: // call C++ helper
    slow_case.link(m_assembler);
    m_assembler.mov(
        Assembler::Operand::Register(ARG1),
        Assembler::Operand::Register(src));
    native_call((void*)cxx_to_boolean);
    m_assembler.mov(
        Assembler::Operand::Register(dst),
        Assembler::Operand::Register(RET));

    // end:
    end.link(m_assembler);

    // dst &= 1;
    m_assembler.bitwise_and(
        Assembler::Operand::Register(dst),
        Assembler::Operand::Imm(1));
}

void Compiler::compile_jump_conditional(Bytecode::Op::JumpConditional const& op)
{
    load_accumulator(GPR1);

    compile_to_boolean(GPR0, GPR1);

    m_assembler.jump_if(
        Assembler::Operand::Register(GPR0),
        Assembler::Condition::EqualTo,
        Assembler::Operand::Imm(0),
        label_for(op.false_target()->block()));

    m_assembler.jump(label_for(op.true_target()->block()));
}

void Compiler::compile_jump_nullish(Bytecode::Op::JumpNullish const& op)
{
    load_accumulator(GPR0);

    m_assembler.shift_right(
        Assembler::Operand::Register(GPR0),
        Assembler::Operand::Imm(48));

    m_assembler.bitwise_and(
        Assembler::Operand::Register(GPR0),
        Assembler::Operand::Imm(IS_NULLISH_EXTRACT_PATTERN));

    m_assembler.jump_if(
        Assembler::Operand::Register(GPR0),
        Assembler::Condition::EqualTo,
        Assembler::Operand::Imm(IS_NULLISH_PATTERN),
        label_for(op.true_target()->block()));

    m_assembler.jump(label_for(op.false_target()->block()));
}

void Compiler::compile_jump_undefined(Bytecode::Op::JumpUndefined const& op)
{
    load_accumulator(GPR0);

    m_assembler.shift_right(
        Assembler::Operand::Register(GPR0),
        Assembler::Operand::Imm(48));

    m_assembler.jump_if(
        Assembler::Operand::Register(GPR0),
        Assembler::Condition::EqualTo,
        Assembler::Operand::Imm(UNDEFINED_TAG),
        label_for(op.true_target()->block()));

    m_assembler.jump(label_for(op.false_target()->block()));
}

[[maybe_unused]] static Value cxx_increment(VM& vm, Value value)
{
    auto old_value = TRY_OR_SET_EXCEPTION(value.to_numeric(vm));
    if (old_value.is_number())
        return Value(old_value.as_double() + 1);
    return BigInt::create(vm, old_value.as_bigint().big_integer().plus(Crypto::SignedBigInteger { 1 }));
}

void Compiler::jump_if_int32(Assembler::Reg reg, Assembler::Label& label)
{
    // GPR0 = reg >> 48;
    m_assembler.mov(Assembler::Operand::Register(GPR0), Assembler::Operand::Register(reg));
    m_assembler.shift_right(Assembler::Operand::Register(GPR0), Assembler::Operand::Imm(48));

    m_assembler.jump_if(
        Assembler::Operand::Register(GPR0),
        Assembler::Condition::EqualTo,
        Assembler::Operand::Imm(INT32_TAG),
        label);
}

template<typename Codegen>
void Compiler::branch_if_int32(Assembler::Reg reg, Codegen codegen)
{
    // GPR0 = reg >> 48;
    m_assembler.mov(Assembler::Operand::Register(GPR0), Assembler::Operand::Register(reg));
    m_assembler.shift_right(Assembler::Operand::Register(GPR0), Assembler::Operand::Imm(48));

    Assembler::Label not_int32_case {};
    m_assembler.jump_if(
        Assembler::Operand::Register(GPR0),
        Assembler::Condition::NotEqualTo,
        Assembler::Operand::Imm(INT32_TAG),
        not_int32_case);

    codegen();

    not_int32_case.link(m_assembler);
}

template<typename Codegen>
void Compiler::branch_if_both_int32(Assembler::Reg lhs, Assembler::Reg rhs, Codegen codegen)
{
    // GPR0 = lhs >> 48;
    m_assembler.mov(Assembler::Operand::Register(GPR0), Assembler::Operand::Register(lhs));
    m_assembler.shift_right(Assembler::Operand::Register(GPR0), Assembler::Operand::Imm(48));

    // GPR1 = rhs >> 48;
    m_assembler.mov(Assembler::Operand::Register(GPR1), Assembler::Operand::Register(rhs));
    m_assembler.shift_right(Assembler::Operand::Register(GPR1), Assembler::Operand::Imm(48));

    Assembler::Label not_int32_case {};
    m_assembler.jump_if(
        Assembler::Operand::Register(GPR0),
        Assembler::Condition::NotEqualTo,
        Assembler::Operand::Imm(INT32_TAG),
        not_int32_case);
    m_assembler.jump_if(
        Assembler::Operand::Register(GPR1),
        Assembler::Condition::NotEqualTo,
        Assembler::Operand::Imm(INT32_TAG),
        not_int32_case);

    codegen();

    not_int32_case.link(m_assembler);
}

void Compiler::compile_increment(Bytecode::Op::Increment const&)
{
    load_accumulator(ARG1);

    Assembler::Label end {};
    Assembler::Label slow_case {};

    branch_if_int32(ARG1, [&] {
        // GPR0 = ARG1
        m_assembler.mov(
            Assembler::Operand::Register(GPR0),
            Assembler::Operand::Register(ARG1));
        // GPR0++;
        m_assembler.inc32(
            Assembler::Operand::Register(GPR0),
            slow_case);

        // accumulator = GPR0 | SHIFTED_INT32_TAG;
        m_assembler.mov(
            Assembler::Operand::Register(GPR1),
            Assembler::Operand::Imm(SHIFTED_INT32_TAG));
        m_assembler.bitwise_or(
            Assembler::Operand::Register(GPR0),
            Assembler::Operand::Register(GPR1));
        store_accumulator(GPR0);

        m_assembler.jump(end);
    });

    slow_case.link(m_assembler);
    native_call((void*)cxx_increment);
    store_accumulator(RET);
    check_exception();

    end.link(m_assembler);
}

static Value cxx_decrement(VM& vm, Value value)
{
    auto old_value = TRY_OR_SET_EXCEPTION(value.to_numeric(vm));
    if (old_value.is_number())
        return Value(old_value.as_double() - 1);
    return BigInt::create(vm, old_value.as_bigint().big_integer().minus(Crypto::SignedBigInteger { 1 }));
}

void Compiler::compile_decrement(Bytecode::Op::Decrement const&)
{
    load_accumulator(ARG1);
    native_call((void*)cxx_decrement);
    store_accumulator(RET);
    check_exception();
}

void Compiler::check_exception()
{
    load_vm_register(GPR0, Bytecode::Register::exception());
    m_assembler.mov(Assembler::Operand::Register(GPR1), Assembler::Operand::Imm(Value().encoded()));

    if (auto const* handler = current_block().handler(); handler) {
        Assembler::Label no_exception;
        m_assembler.jump_if(
            Assembler::Operand::Register(GPR0),
            Assembler::Condition::EqualTo,
            Assembler::Operand::Register(GPR1),
            no_exception);
        store_accumulator(GPR0);
        store_vm_register(Bytecode::Register::exception(), GPR1);
        m_assembler.jump(label_for(*handler));
        no_exception.link(m_assembler);
    } else if (auto const* finalizer = current_block().finalizer(); finalizer) {
        store_vm_register(Bytecode::Register::saved_exception(), GPR0);
        store_vm_register(Bytecode::Register::exception(), GPR1);
        m_assembler.jump_if(Assembler::Operand::Register(GPR0),
            Assembler::Condition::NotEqualTo,
            Assembler::Operand::Register(GPR1),
            label_for(*finalizer));
    } else {
        m_assembler.jump_if(Assembler::Operand::Register(GPR0),
            Assembler::Condition::NotEqualTo,
            Assembler::Operand::Register(GPR1),
            m_exit_label);
    }
}

void Compiler::compile_enter_unwind_context(Bytecode::Op::EnterUnwindContext const& op)
{
    m_assembler.jump(label_for(op.entry_point().block()));
}

void Compiler::compile_leave_unwind_context(Bytecode::Op::LeaveUnwindContext const&)
{
    /* Nothing */
}

void Compiler::compile_throw(Bytecode::Op::Throw const&)
{
    load_accumulator(GPR0);
    store_vm_register(Bytecode::Register::exception(), GPR0);
    check_exception();
}

static ThrowCompletionOr<Value> abstract_inequals(VM& vm, Value src1, Value src2)
{
    return Value(!TRY(is_loosely_equal(vm, src1, src2)));
}

static ThrowCompletionOr<Value> abstract_equals(VM& vm, Value src1, Value src2)
{
    return Value(TRY(is_loosely_equal(vm, src1, src2)));
}

static ThrowCompletionOr<Value> typed_inequals(VM&, Value src1, Value src2)
{
    return Value(!is_strictly_equal(src1, src2));
}

static ThrowCompletionOr<Value> typed_equals(VM&, Value src1, Value src2)
{
    return Value(is_strictly_equal(src1, src2));
}

#    define DO_COMPILE_COMMON_BINARY_OP(TitleCaseName, snake_case_name)                 \
        static Value cxx_##snake_case_name(VM& vm, Value lhs, Value rhs)                \
        {                                                                               \
            return TRY_OR_SET_EXCEPTION(snake_case_name(vm, lhs, rhs));                 \
        }                                                                               \
                                                                                        \
        void Compiler::compile_##snake_case_name(Bytecode::Op::TitleCaseName const& op) \
        {                                                                               \
            load_vm_register(ARG1, op.lhs());                                           \
            load_accumulator(ARG2);                                                     \
            native_call((void*)cxx_##snake_case_name);                                  \
            store_accumulator(RET);                                                     \
            check_exception();                                                          \
        }

JS_ENUMERATE_COMMON_BINARY_OPS_WITHOUT_FAST_PATH(DO_COMPILE_COMMON_BINARY_OP)
#    undef DO_COMPILE_COMMON_BINARY_OP

static Value cxx_add(VM& vm, Value lhs, Value rhs)
{
    return TRY_OR_SET_EXCEPTION(add(vm, lhs, rhs));
}

void Compiler::compile_add(Bytecode::Op::Add const& op)
{
    load_vm_register(ARG1, op.lhs());
    load_accumulator(ARG2);

    Assembler::Label end {};
    Assembler::Label slow_case {};

    branch_if_both_int32(ARG1, ARG2, [&] {
        // GPR0 = ARG1 + ARG2 (32-bit)
        // if (overflow) goto slow_case;
        m_assembler.mov(
            Assembler::Operand::Register(GPR0),
            Assembler::Operand::Register(ARG1));
        m_assembler.add32(
            Assembler::Operand::Register(GPR0),
            Assembler::Operand::Register(ARG2),
            slow_case);

        // accumulator = GPR0 | SHIFTED_INT32_TAG;
        m_assembler.mov(
            Assembler::Operand::Register(GPR1),
            Assembler::Operand::Imm(SHIFTED_INT32_TAG));
        m_assembler.bitwise_or(
            Assembler::Operand::Register(GPR0),
            Assembler::Operand::Register(GPR1));
        store_accumulator(GPR0);
        m_assembler.jump(end);
    });

    slow_case.link(m_assembler);
    native_call((void*)cxx_add);
    store_accumulator(RET);
    check_exception();
    end.link(m_assembler);
}

static Value cxx_sub(VM& vm, Value lhs, Value rhs)
{
    return TRY_OR_SET_EXCEPTION(sub(vm, lhs, rhs));
}

void Compiler::compile_sub(Bytecode::Op::Sub const& op)
{
    load_vm_register(ARG1, op.lhs());
    load_accumulator(ARG2);

    Assembler::Label end {};
    Assembler::Label slow_case {};

    branch_if_both_int32(ARG1, ARG2, [&] {
        // GPR0 = ARG1 + ARG2 (32-bit)
        // if (overflow) goto slow_case;
        m_assembler.mov(
            Assembler::Operand::Register(GPR0),
            Assembler::Operand::Register(ARG1));
        m_assembler.sub32(
            Assembler::Operand::Register(GPR0),
            Assembler::Operand::Register(ARG2),
            slow_case);

        // accumulator = GPR0 | SHIFTED_INT32_TAG;
        m_assembler.mov(
            Assembler::Operand::Register(GPR1),
            Assembler::Operand::Imm(SHIFTED_INT32_TAG));
        m_assembler.bitwise_or(
            Assembler::Operand::Register(GPR0),
            Assembler::Operand::Register(GPR1));
        store_accumulator(GPR0);
        m_assembler.jump(end);
    });

    slow_case.link(m_assembler);
    native_call((void*)cxx_sub);
    store_accumulator(RET);
    check_exception();
    end.link(m_assembler);
}

static Value cxx_mul(VM& vm, Value lhs, Value rhs)
{
    return TRY_OR_SET_EXCEPTION(mul(vm, lhs, rhs));
}

void Compiler::compile_mul(Bytecode::Op::Mul const& op)
{
    load_vm_register(ARG1, op.lhs());
    load_accumulator(ARG2);

    Assembler::Label end {};
    Assembler::Label slow_case {};

    branch_if_both_int32(ARG1, ARG2, [&] {
        // GPR0 = ARG1 * ARG2 (32-bit)
        // if (overflow) goto slow_case;
        m_assembler.mov(
            Assembler::Operand::Register(GPR0),
            Assembler::Operand::Register(ARG1));
        m_assembler.mul32(
            Assembler::Operand::Register(GPR0),
            Assembler::Operand::Register(ARG2),
            slow_case);

        // accumulator = GPR0 | SHIFTED_INT32_TAG;
        m_assembler.mov(
            Assembler::Operand::Register(GPR1),
            Assembler::Operand::Imm(SHIFTED_INT32_TAG));
        m_assembler.bitwise_or(
            Assembler::Operand::Register(GPR0),
            Assembler::Operand::Register(GPR1));
        store_accumulator(GPR0);
        m_assembler.jump(end);
    });

    slow_case.link(m_assembler);
    native_call((void*)cxx_mul);
    store_accumulator(RET);
    check_exception();
    end.link(m_assembler);
}

static Value cxx_less_than(VM& vm, Value lhs, Value rhs)
{
    return TRY_OR_SET_EXCEPTION(less_than(vm, lhs, rhs));
}

void Compiler::compile_less_than(Bytecode::Op::LessThan const& op)
{
    load_vm_register(ARG1, op.lhs());
    load_accumulator(ARG2);

    Assembler::Label end {};

    branch_if_both_int32(ARG1, ARG2, [&] {
        // if (ARG1 < ARG2) return true;
        // else return false;

        Assembler::Label true_case {};

        m_assembler.sign_extend_32_to_64_bits(ARG1);
        m_assembler.sign_extend_32_to_64_bits(ARG2);

        m_assembler.jump_if(
            Assembler::Operand::Register(ARG1),
            Assembler::Condition::SignedLessThan,
            Assembler::Operand::Register(ARG2),
            true_case);

        m_assembler.mov(
            Assembler::Operand::Register(GPR0),
            Assembler::Operand::Imm(Value(false).encoded()));
        store_accumulator(GPR0);
        m_assembler.jump(end);

        true_case.link(m_assembler);
        m_assembler.mov(
            Assembler::Operand::Register(GPR0),
            Assembler::Operand::Imm(Value(true).encoded()));
        store_accumulator(GPR0);

        m_assembler.jump(end);
    });

    native_call((void*)cxx_less_than);
    store_accumulator(RET);
    check_exception();
    end.link(m_assembler);
}

static Value cxx_bitwise_and(VM& vm, Value lhs, Value rhs)
{
    return TRY_OR_SET_EXCEPTION(bitwise_and(vm, lhs, rhs));
}

void Compiler::compile_bitwise_and(Bytecode::Op::BitwiseAnd const& op)
{
    load_vm_register(ARG1, op.lhs());
    load_accumulator(ARG2);

    Assembler::Label end {};

    branch_if_both_int32(ARG1, ARG2, [&] {
        // NOTE: Since both sides are Int32, we know that the upper 32 bits are nothing but the INT32_TAG.
        //       This means we can get away with just a simple 64-bit bitwise and.
        m_assembler.bitwise_and(
            Assembler::Operand::Register(ARG1),
            Assembler::Operand::Register(ARG2));

        store_accumulator(ARG1);
        m_assembler.jump(end);
    });

    native_call((void*)cxx_bitwise_and);
    store_accumulator(RET);
    check_exception();
    end.link(m_assembler);
}

static Value cxx_bitwise_or(VM& vm, Value lhs, Value rhs)
{
    return TRY_OR_SET_EXCEPTION(bitwise_or(vm, lhs, rhs));
}

void Compiler::compile_bitwise_or(Bytecode::Op::BitwiseOr const& op)
{
    load_vm_register(ARG1, op.lhs());
    load_accumulator(ARG2);

    Assembler::Label end {};

    branch_if_both_int32(ARG1, ARG2, [&] {
        // NOTE: Since both sides are Int32, we know that the upper 32 bits are nothing but the INT32_TAG.
        //       This means we can get away with just a simple 64-bit bitwise or.
        m_assembler.bitwise_or(
            Assembler::Operand::Register(ARG1),
            Assembler::Operand::Register(ARG2));

        store_accumulator(ARG1);
        m_assembler.jump(end);
    });

    native_call((void*)cxx_bitwise_or);
    store_accumulator(RET);
    check_exception();
    end.link(m_assembler);
}

static Value cxx_bitwise_xor(VM& vm, Value lhs, Value rhs)
{
    return TRY_OR_SET_EXCEPTION(bitwise_xor(vm, lhs, rhs));
}

void Compiler::compile_bitwise_xor(Bytecode::Op::BitwiseXor const& op)
{
    load_vm_register(ARG1, op.lhs());
    load_accumulator(ARG2);

    Assembler::Label end {};

    branch_if_both_int32(ARG1, ARG2, [&] {
        // ARG1 ^= ARG2 (32-bit)
        m_assembler.bitwise_xor32(
            Assembler::Operand::Register(ARG1),
            Assembler::Operand::Register(ARG2));

        // accumulator = ARG1 | SHIFTED_INT32_TAG;
        m_assembler.mov(
            Assembler::Operand::Register(GPR0),
            Assembler::Operand::Imm(SHIFTED_INT32_TAG));
        m_assembler.bitwise_or(
            Assembler::Operand::Register(ARG1),
            Assembler::Operand::Register(GPR0));
        store_accumulator(ARG1);
        m_assembler.jump(end);
    });

    native_call((void*)cxx_bitwise_xor);
    store_accumulator(RET);
    check_exception();
    end.link(m_assembler);
}

static Value cxx_left_shift(VM& vm, Value lhs, Value rhs)
{
    return TRY_OR_SET_EXCEPTION(left_shift(vm, lhs, rhs));
}

void Compiler::compile_left_shift(Bytecode::Op::LeftShift const& op)
{
    load_vm_register(ARG1, op.lhs());
    load_accumulator(ARG2);

    Assembler::Label end {};

    branch_if_both_int32(ARG1, ARG2, [&] {
        // RCX = ARG2
        m_assembler.mov(
            Assembler::Operand::Register(Assembler::Reg::RCX),
            Assembler::Operand::Register(ARG2));

        // ARG1 <<= CL (32-bit)
        m_assembler.shift_left32(Assembler::Operand::Register(ARG1), {});

        // accumulator = ARG1 | SHIFTED_INT32_TAG;
        m_assembler.mov(
            Assembler::Operand::Register(GPR0),
            Assembler::Operand::Imm(SHIFTED_INT32_TAG));
        m_assembler.bitwise_or(
            Assembler::Operand::Register(ARG1),
            Assembler::Operand::Register(GPR0));
        store_accumulator(ARG1);
        m_assembler.jump(end);
    });

    native_call((void*)cxx_left_shift);
    store_accumulator(RET);
    check_exception();
    end.link(m_assembler);
}

static Value cxx_right_shift(VM& vm, Value lhs, Value rhs)
{
    return TRY_OR_SET_EXCEPTION(right_shift(vm, lhs, rhs));
}

void Compiler::compile_right_shift(Bytecode::Op::RightShift const& op)
{
    load_vm_register(ARG1, op.lhs());
    load_accumulator(ARG2);

    Assembler::Label end {};

    branch_if_both_int32(ARG1, ARG2, [&] {
        // RCX = ARG2
        m_assembler.mov(
            Assembler::Operand::Register(Assembler::Reg::RCX),
            Assembler::Operand::Register(ARG2));

        // ARG1 >>= CL (32-bit)
        m_assembler.arithmetic_right_shift32(Assembler::Operand::Register(ARG1), {});

        // accumulator = ARG1 | SHIFTED_INT32_TAG;
        m_assembler.mov(
            Assembler::Operand::Register(GPR0),
            Assembler::Operand::Imm(SHIFTED_INT32_TAG));
        m_assembler.bitwise_or(
            Assembler::Operand::Register(ARG1),
            Assembler::Operand::Register(GPR0));
        store_accumulator(ARG1);
        m_assembler.jump(end);
    });

    native_call((void*)cxx_right_shift);
    store_accumulator(RET);
    check_exception();
    end.link(m_assembler);
}

static Value cxx_unsigned_right_shift(VM& vm, Value lhs, Value rhs)
{
    return TRY_OR_SET_EXCEPTION(unsigned_right_shift(vm, lhs, rhs));
}

void Compiler::compile_unsigned_right_shift(Bytecode::Op::UnsignedRightShift const& op)
{
    load_vm_register(ARG1, op.lhs());
    load_accumulator(ARG2);

    Assembler::Label end {};
    Assembler::Label slow_case {};

    branch_if_both_int32(ARG1, ARG2, [&] {
        // GPR0 = ARG1
        m_assembler.mov(
            Assembler::Operand::Register(GPR0),
            Assembler::Operand::Register(ARG1));

        // RCX = ARG2
        m_assembler.mov(
            Assembler::Operand::Register(Assembler::Reg::RCX),
            Assembler::Operand::Register(ARG2));

        // GPR0 >>>= CL (32-bit)
        m_assembler.shift_right32(Assembler::Operand::Register(GPR0), {});

        // GPR1 = sign_extended(GPR0)
        m_assembler.mov32(
            Assembler::Operand::Register(GPR1),
            Assembler::Operand::Register(GPR0),
            Assembler::Extension::SignExtend);

        // if (GPR1 < 0) goto slow_case;
        m_assembler.jump_if(
            Assembler::Operand::Register(GPR1),
            Assembler::Condition::SignedLessThan,
            Assembler::Operand::Imm(0),
            slow_case);

        // accumulator = GPR0 | SHIFTED_INT32_TAG;
        m_assembler.mov(
            Assembler::Operand::Register(GPR1),
            Assembler::Operand::Imm(SHIFTED_INT32_TAG));
        m_assembler.bitwise_or(
            Assembler::Operand::Register(GPR0),
            Assembler::Operand::Register(GPR1));
        store_accumulator(GPR0);
        m_assembler.jump(end);
    });

    slow_case.link(m_assembler);
    native_call((void*)cxx_unsigned_right_shift);
    store_accumulator(RET);
    check_exception();
    end.link(m_assembler);
}

static ThrowCompletionOr<Value> not_(VM&, Value value)
{
    return Value(!value.to_boolean());
}

static ThrowCompletionOr<Value> typeof_(VM& vm, Value value)
{
    return PrimitiveString::create(vm, value.typeof());
}

#    define DO_COMPILE_COMMON_UNARY_OP(TitleCaseName, snake_case_name)               \
        static Value cxx_##snake_case_name(VM& vm, Value value)                      \
        {                                                                            \
            return TRY_OR_SET_EXCEPTION(snake_case_name(vm, value));                 \
        }                                                                            \
                                                                                     \
        void Compiler::compile_##snake_case_name(Bytecode::Op::TitleCaseName const&) \
        {                                                                            \
            load_accumulator(ARG1);                                                  \
            native_call((void*)cxx_##snake_case_name);                               \
            store_accumulator(RET);                                                  \
            check_exception();                                                       \
        }

JS_ENUMERATE_COMMON_UNARY_OPS(DO_COMPILE_COMMON_UNARY_OP)
#    undef DO_COMPILE_COMMON_UNARY_OP

void Compiler::compile_return(Bytecode::Op::Return const&)
{
    load_accumulator(GPR0);

    if (auto const* finalizer = current_block().finalizer(); finalizer) {
        store_vm_register(Bytecode::Register::saved_return_value(), GPR0);
        m_assembler.jump(label_for(*finalizer));
    } else {
        store_vm_register(Bytecode::Register::return_value(), GPR0);
        jump_to_exit();
    }
}

static Value cxx_new_string(VM& vm, DeprecatedString const& string)
{
    return PrimitiveString::create(vm, string);
}

void Compiler::compile_new_string(Bytecode::Op::NewString const& op)
{
    auto const& string = m_bytecode_executable.string_table->get(op.index());
    m_assembler.mov(
        Assembler::Operand::Register(ARG1),
        Assembler::Operand::Imm(bit_cast<u64>(&string)));
    native_call((void*)cxx_new_string);
    store_accumulator(RET);
}

void Compiler::compile_new_regexp(Bytecode::Op::NewRegExp const& op)
{
    auto const& parsed_regex = m_bytecode_executable.regex_table->get(op.regex_index());
    auto const& pattern = m_bytecode_executable.string_table->get(op.source_index());
    auto const& flags = m_bytecode_executable.string_table->get(op.flags_index());

    m_assembler.mov(
        Assembler::Operand::Register(ARG1),
        Assembler::Operand::Imm(bit_cast<u64>(&parsed_regex)));
    m_assembler.mov(
        Assembler::Operand::Register(ARG2),
        Assembler::Operand::Imm(bit_cast<u64>(&pattern)));
    m_assembler.mov(
        Assembler::Operand::Register(ARG3),
        Assembler::Operand::Imm(bit_cast<u64>(&flags)));

    native_call((void*)Bytecode::new_regexp);
    store_accumulator(RET);
}

static Value cxx_new_bigint(VM& vm, Crypto::SignedBigInteger const& bigint)
{
    return BigInt::create(vm, bigint);
}

void Compiler::compile_new_bigint(Bytecode::Op::NewBigInt const& op)
{
    m_assembler.mov(
        Assembler::Operand::Register(ARG1),
        Assembler::Operand::Imm(bit_cast<u64>(&op.bigint())));
    native_call((void*)cxx_new_bigint);
    store_accumulator(RET);
}

static Value cxx_new_object(VM& vm)
{
    auto& realm = *vm.current_realm();
    return Object::create(realm, realm.intrinsics().object_prototype());
}

void Compiler::compile_new_object(Bytecode::Op::NewObject const&)
{
    native_call((void*)cxx_new_object);
    store_accumulator(RET);
}

static Value cxx_new_array(VM& vm, size_t element_count, u32 first_register_index)
{
    auto& realm = *vm.current_realm();
    auto array = MUST(Array::create(realm, 0));
    for (size_t i = 0; i < element_count; ++i) {
        auto& value = vm.bytecode_interpreter().reg(Bytecode::Register(first_register_index + i));
        array->indexed_properties().put(i, value, default_attributes);
    }
    return array;
}

void Compiler::compile_new_array(Bytecode::Op::NewArray const& op)
{
    m_assembler.mov(
        Assembler::Operand::Register(ARG1),
        Assembler::Operand::Imm(op.element_count()));
    m_assembler.mov(
        Assembler::Operand::Register(ARG2),
        Assembler::Operand::Imm(op.element_count() ? op.start().index() : 0));
    native_call((void*)cxx_new_array);
    store_accumulator(RET);
}

void Compiler::compile_new_function(Bytecode::Op::NewFunction const& op)
{
    m_assembler.mov(
        Assembler::Operand::Register(ARG1),
        Assembler::Operand::Imm(bit_cast<u64>(&op.function_node())));
    m_assembler.mov(
        Assembler::Operand::Register(ARG2),
        Assembler::Operand::Imm(bit_cast<u64>(&op.lhs_name())));
    m_assembler.mov(
        Assembler::Operand::Register(ARG3),
        Assembler::Operand::Imm(bit_cast<u64>(&op.home_object())));
    native_call((void*)Bytecode::new_function);
    store_accumulator(RET);
}

static Value cxx_new_class(VM& vm, Value super_class, ClassExpression const& class_expression, Optional<Bytecode::IdentifierTableIndex> const& lhs_name)
{
    return TRY_OR_SET_EXCEPTION(Bytecode::new_class(vm, super_class, class_expression, lhs_name));
}

void Compiler::compile_new_class(Bytecode::Op::NewClass const& op)
{
    load_accumulator(ARG1);
    m_assembler.mov(
        Assembler::Operand::Register(ARG2),
        Assembler::Operand::Imm(bit_cast<u64>(&op.class_expression())));
    m_assembler.mov(
        Assembler::Operand::Register(ARG3),
        Assembler::Operand::Imm(bit_cast<u64>(&op.lhs_name())));
    native_call((void*)cxx_new_class);
    store_accumulator(RET);
}

static Value cxx_get_by_id(VM& vm, Value base, DeprecatedFlyString const& property, Bytecode::PropertyLookupCache& cache)
{
    return TRY_OR_SET_EXCEPTION(Bytecode::get_by_id(vm, property, base, base, cache));
}

void Compiler::compile_get_by_id(Bytecode::Op::GetById const& op)
{
    load_accumulator(ARG1);
    m_assembler.mov(
        Assembler::Operand::Register(ARG2),
        Assembler::Operand::Imm(bit_cast<u64>(&m_bytecode_executable.get_identifier(op.property()))));
    m_assembler.mov(
        Assembler::Operand::Register(ARG3),
        Assembler::Operand::Imm(bit_cast<u64>(&m_bytecode_executable.property_lookup_caches[op.cache_index()])));
    native_call((void*)cxx_get_by_id);
    store_accumulator(RET);
    check_exception();
}

static Value cxx_get_by_value(VM& vm, Value base, Value property)
{
    return TRY_OR_SET_EXCEPTION(Bytecode::get_by_value(vm, base, property));
}

void Compiler::compile_get_by_value(Bytecode::Op::GetByValue const& op)
{
    load_vm_register(ARG1, op.base());
    load_accumulator(ARG2);
    native_call((void*)cxx_get_by_value);
    store_accumulator(RET);
    check_exception();
}

static Value cxx_get_global(VM& vm, DeprecatedFlyString const& identifier, Bytecode::GlobalVariableCache& cache)
{
    return TRY_OR_SET_EXCEPTION(Bytecode::get_global(vm.bytecode_interpreter(), identifier, cache));
}

void Compiler::compile_get_global(Bytecode::Op::GetGlobal const& op)
{
    m_assembler.mov(
        Assembler::Operand::Register(ARG1),
        Assembler::Operand::Imm(bit_cast<u64>(&m_bytecode_executable.get_identifier(op.identifier()))));
    m_assembler.mov(
        Assembler::Operand::Register(ARG2),
        Assembler::Operand::Imm(bit_cast<u64>(&m_bytecode_executable.global_variable_caches[op.cache_index()])));
    native_call((void*)cxx_get_global);
    store_accumulator(RET);
    check_exception();
}

static Value cxx_get_variable(VM& vm, DeprecatedFlyString const& name, Bytecode::EnvironmentVariableCache& cache)
{
    return TRY_OR_SET_EXCEPTION(Bytecode::get_variable(vm.bytecode_interpreter(), name, cache));
}

void Compiler::compile_get_variable(Bytecode::Op::GetVariable const& op)
{
    Assembler::Label slow_case;

    // if (!cache.has_value()) goto slow_case;
    m_assembler.mov(
        Assembler::Operand::Register(ARG2),
        Assembler::Operand::Imm(bit_cast<u64>(&m_bytecode_executable.environment_variable_caches[op.cache_index()])));

    m_assembler.mov8(
        Assembler::Operand::Register(GPR0),
        Assembler::Operand::Mem64BaseAndOffset(ARG2, Bytecode::EnvironmentVariableCache::has_value_offset()));

    m_assembler.jump_if(
        Assembler::Operand::Register(GPR0),
        Assembler::Condition::EqualTo,
        Assembler::Operand::Imm(0),
        slow_case);

    // auto environment = vm.running_execution_context().lexical_environment;
    // GPR1 = current lexical environment
    m_assembler.mov(
        Assembler::Operand::Register(GPR1),
        Assembler::Operand::Mem64BaseAndOffset(RUNNING_EXECUTION_CONTEXT_BASE, ExecutionContext::lexical_environment_offset()));

    // for (size_t i = 0; i < cache->hops; ++i)
    //     environment = environment->outer_environment();

    // GPR0 = hops
    m_assembler.mov32(
        Assembler::Operand::Register(GPR0),
        Assembler::Operand::Mem64BaseAndOffset(ARG2, Bytecode::EnvironmentVariableCache::value_offset() + EnvironmentCoordinate::hops_offset()));

    {
        // while (GPR0--)
        //     GPR1 = GPR1->outer_environment()
        Assembler::Label loop_start;
        Assembler::Label loop_end;
        loop_start.link(m_assembler);
        m_assembler.jump_if(
            Assembler::Operand::Register(GPR0),
            Assembler::Condition::EqualTo,
            Assembler::Operand::Imm(0),
            loop_end);
        m_assembler.sub(
            Assembler::Operand::Register(GPR0),
            Assembler::Operand::Imm(1));
        m_assembler.mov(
            Assembler::Operand::Register(GPR1),
            Assembler::Operand::Mem64BaseAndOffset(GPR1, Environment::outer_environment_offset()));
        m_assembler.jump(loop_start);
        loop_end.link(m_assembler);
    }

    // GPR1 now points to the environment holding our binding.

    // if (environment->is_permanently_screwed_by_eval()) goto slow_case;
    m_assembler.mov8(
        Assembler::Operand::Register(GPR0),
        Assembler::Operand::Mem64BaseAndOffset(GPR1, Environment::is_permanently_screwed_by_eval_offset()));
    m_assembler.jump_if(
        Assembler::Operand::Register(GPR0),
        Assembler::Condition::NotEqualTo,
        Assembler::Operand::Imm(0),
        slow_case);

    // GPR1 = environment->m_bindings.outline_buffer()
    m_assembler.mov(
        Assembler::Operand::Register(GPR1),
        Assembler::Operand::Mem64BaseAndOffset(GPR1, DeclarativeEnvironment::bindings_offset() + Vector<DeclarativeEnvironment::Binding>::outline_buffer_offset()));

    // GPR0 = index
    m_assembler.mov32(
        Assembler::Operand::Register(GPR0),
        Assembler::Operand::Mem64BaseAndOffset(ARG2, Bytecode::EnvironmentVariableCache::value_offset() + EnvironmentCoordinate::index_offset()));

    // GPR0 *= sizeof(DeclarativeEnvironment::Binding)
    m_assembler.mul32(
        Assembler::Operand::Register(GPR0),
        Assembler::Operand::Imm(sizeof(DeclarativeEnvironment::Binding)),
        slow_case);

    // GPR1 = &binding
    m_assembler.add(
        Assembler::Operand::Register(GPR1),
        Assembler::Operand::Register(GPR0));

    // if (!binding.initialized) goto slow_case;
    m_assembler.mov(
        Assembler ::Operand::Register(GPR0),
        Assembler::Operand::Mem64BaseAndOffset(GPR1, DeclarativeEnvironment::Binding::initialized_offset()));
    m_assembler.bitwise_and(
        Assembler::Operand::Register(GPR0),
        Assembler::Operand::Imm(0xff));
    m_assembler.jump_if(
        Assembler::Operand::Register(GPR0),
        Assembler::Condition::EqualTo,
        Assembler::Operand::Imm(0),
        slow_case);

    // accumulator = binding.value;
    m_assembler.mov(
        Assembler::Operand::Register(GPR0),
        Assembler::Operand::Mem64BaseAndOffset(GPR1, DeclarativeEnvironment::Binding::value_offset()));

    store_accumulator(GPR0);
    Assembler::Label end;
    m_assembler.jump(end);

    // Slow case: Uncached access. Call C++ helper.
    slow_case.link(m_assembler);
    m_assembler.mov(
        Assembler::Operand::Register(ARG1),
        Assembler::Operand::Imm(bit_cast<u64>(&m_bytecode_executable.get_identifier(op.identifier()))));
    native_call((void*)cxx_get_variable);
    store_accumulator(RET);
    check_exception();

    end.link(m_assembler);
}

static Value cxx_get_callee_and_this_from_environment(VM& vm, DeprecatedFlyString const& name, u32 cache_index, Bytecode::Register callee_reg, Bytecode::Register this_reg)
{
    auto& bytecode_interpreter = vm.bytecode_interpreter();
    auto callee_and_this = TRY_OR_SET_EXCEPTION(Bytecode::get_callee_and_this_from_environment(
        bytecode_interpreter,
        name,
        cache_index));

    bytecode_interpreter.reg(callee_reg) = callee_and_this.callee;
    bytecode_interpreter.reg(this_reg) = callee_and_this.this_value;
    return {};
}

void Compiler::compile_get_callee_and_this_from_environment(Bytecode::Op::GetCalleeAndThisFromEnvironment const& op)
{
    m_assembler.mov(
        Assembler::Operand::Register(ARG1),
        Assembler::Operand::Imm(bit_cast<u64>(&m_bytecode_executable.get_identifier(op.identifier()))));
    m_assembler.mov(
        Assembler::Operand::Register(ARG2),
        Assembler::Operand::Imm(op.cache_index()));
    m_assembler.mov(
        Assembler::Operand::Register(ARG3),
        Assembler::Operand::Imm(op.callee().index()));
    m_assembler.mov(
        Assembler::Operand::Register(ARG4),
        Assembler::Operand::Imm(op.this_().index()));
    native_call((void*)cxx_get_callee_and_this_from_environment);
    check_exception();
}

static Value cxx_to_numeric(VM& vm, Value value)
{
    return TRY_OR_SET_EXCEPTION(value.to_numeric(vm));
}

void Compiler::compile_to_numeric(Bytecode::Op::ToNumeric const&)
{
    Assembler::Label fast_case {};

    load_accumulator(ARG1);
    jump_if_int32(ARG1, fast_case);

    native_call((void*)cxx_to_numeric);
    store_accumulator(RET);
    check_exception();

    fast_case.link(m_assembler);
}

static Value cxx_resolve_this_binding(VM& vm)
{
    auto this_value = TRY_OR_SET_EXCEPTION(vm.resolve_this_binding());
    vm.bytecode_interpreter().reg(Bytecode::Register::this_value()) = this_value;
    return this_value;
}

void Compiler::compile_resolve_this_binding(Bytecode::Op::ResolveThisBinding const&)
{
    // OPTIMIZATION: We cache the `this` value in a special VM register.
    //               So first we check if the cache is non-empty, and if so,
    //               we can avoid calling out to C++ at all. :^)
    load_vm_register(GPR0, Bytecode::Register::this_value());
    m_assembler.mov(
        Assembler::Operand::Register(GPR1),
        Assembler::Operand::Imm(Value().encoded()));

    Assembler::Label slow_case {};
    m_assembler.jump_if(
        Assembler::Operand::Register(GPR0),
        Assembler::Condition::EqualTo,
        Assembler::Operand::Register(GPR1),
        slow_case);

    // Fast case: We have a cached `this` value!
    store_accumulator(GPR0);
    auto end = m_assembler.jump();

    slow_case.link(m_assembler);
    native_call((void*)cxx_resolve_this_binding);
    store_accumulator(RET);
    check_exception();

    end.link(m_assembler);
}

static Value cxx_put_by_id(VM& vm, Value base, Bytecode::IdentifierTableIndex property, Value value, Bytecode::Op::PropertyKind kind)
{
    PropertyKey name = vm.bytecode_interpreter().current_executable().get_identifier(property);
    TRY_OR_SET_EXCEPTION(Bytecode::put_by_property_key(vm, base, base, value, name, kind));
    return value;
}

void Compiler::compile_put_by_id(Bytecode::Op::PutById const& op)
{
    load_vm_register(ARG1, op.base());
    m_assembler.mov(
        Assembler::Operand::Register(ARG2),
        Assembler::Operand::Imm(op.property().value()));
    load_accumulator(ARG3);
    m_assembler.mov(
        Assembler::Operand::Register(ARG4),
        Assembler::Operand::Imm(to_underlying(op.kind())));
    native_call((void*)cxx_put_by_id);
    store_accumulator(RET);
    check_exception();
}

static Value cxx_put_by_value(VM& vm, Value base, Value property, Value value, Bytecode::Op::PropertyKind kind)
{
    TRY_OR_SET_EXCEPTION(Bytecode::put_by_value(vm, base, property, value, kind));
    return value;
}

void Compiler::compile_put_by_value(Bytecode::Op::PutByValue const& op)
{
    load_vm_register(ARG1, op.base());
    load_vm_register(ARG2, op.property());
    load_accumulator(ARG3);
    m_assembler.mov(
        Assembler::Operand::Register(ARG4),
        Assembler::Operand::Imm(to_underlying(op.kind())));
    native_call((void*)cxx_put_by_value);
    store_accumulator(RET);
    check_exception();
}

static Value cxx_call(VM& vm, Value callee, u32 first_argument_index, u32 argument_count, Value this_value, Bytecode::Op::CallType call_type, Optional<Bytecode::StringTableIndex> const& expression_string)
{
    TRY_OR_SET_EXCEPTION(throw_if_needed_for_call(vm.bytecode_interpreter(), callee, call_type, expression_string));

    MarkedVector<Value> argument_values(vm.heap());
    argument_values.ensure_capacity(argument_count);
    for (u32 i = 0; i < argument_count; ++i) {
        argument_values.unchecked_append(vm.bytecode_interpreter().reg(Bytecode::Register { first_argument_index + i }));
    }
    return TRY_OR_SET_EXCEPTION(perform_call(vm.bytecode_interpreter(), this_value, call_type, callee, move(argument_values)));
}

void Compiler::compile_call(Bytecode::Op::Call const& op)
{
    load_vm_register(ARG1, op.callee());
    m_assembler.mov(
        Assembler::Operand::Register(ARG2),
        Assembler::Operand::Imm(op.first_argument().index()));
    m_assembler.mov(
        Assembler::Operand::Register(ARG3),
        Assembler::Operand::Imm(op.argument_count()));
    load_vm_register(ARG4, op.this_value());
    m_assembler.mov(
        Assembler::Operand::Register(ARG5),
        Assembler::Operand::Imm(to_underlying(op.call_type())));
    m_assembler.mov(
        Assembler::Operand::Register(GPR0),
        Assembler::Operand::Imm(bit_cast<u64>(&op.expression_string())));
    native_call((void*)cxx_call, { Assembler::Operand::Register(GPR0) });
    store_accumulator(RET);
    check_exception();
}

static Value cxx_call_with_argument_array(VM& vm, Value arguments, Value callee, Value this_value, Bytecode::Op::CallType call_type, Optional<Bytecode::StringTableIndex> const& expression_string)
{
    TRY_OR_SET_EXCEPTION(throw_if_needed_for_call(vm.bytecode_interpreter(), callee, call_type, expression_string));
    auto argument_values = Bytecode::argument_list_evaluation(vm, arguments);
    return TRY_OR_SET_EXCEPTION(perform_call(vm.bytecode_interpreter(), this_value, call_type, callee, move(argument_values)));
}

void Compiler::compile_call_with_argument_array(Bytecode::Op::CallWithArgumentArray const& op)
{
    load_accumulator(ARG1);
    load_vm_register(ARG2, op.callee());
    load_vm_register(ARG3, op.this_value());
    m_assembler.mov(
        Assembler::Operand::Register(ARG4),
        Assembler::Operand::Imm(to_underlying(op.call_type())));
    m_assembler.mov(
        Assembler::Operand::Register(ARG5),
        Assembler::Operand::Imm(bit_cast<u64>(&op.expression_string())));
    native_call((void*)cxx_call_with_argument_array);
    store_accumulator(RET);
    check_exception();
}

static Value cxx_typeof_variable(VM& vm, DeprecatedFlyString const& identifier)
{
    return TRY_OR_SET_EXCEPTION(Bytecode::typeof_variable(vm, identifier));
}

void Compiler::compile_typeof_variable(Bytecode::Op::TypeofVariable const& op)
{
    m_assembler.mov(
        Assembler::Operand::Register(ARG1),
        Assembler::Operand::Imm(bit_cast<u64>(&m_bytecode_executable.get_identifier(op.identifier().value()))));
    native_call((void*)cxx_typeof_variable);
    store_accumulator(RET);
    check_exception();
}

static Value cxx_create_variable(
    VM& vm,
    DeprecatedFlyString const& name,
    Bytecode::Op::EnvironmentMode mode,
    bool is_global,
    bool is_immutable,
    bool is_strict)
{
    TRY_OR_SET_EXCEPTION(Bytecode::create_variable(vm, name, mode, is_global, is_immutable, is_strict));
    return {};
}

void Compiler::compile_create_variable(Bytecode::Op::CreateVariable const& op)
{
    m_assembler.mov(
        Assembler::Operand::Register(ARG1),
        Assembler::Operand::Imm(bit_cast<u64>(&m_bytecode_executable.get_identifier(op.identifier().value()))));
    m_assembler.mov(
        Assembler::Operand::Register(ARG2),
        Assembler::Operand::Imm(to_underlying(op.mode())));
    m_assembler.mov(
        Assembler::Operand::Register(ARG3),
        Assembler::Operand::Imm(static_cast<u64>(op.is_global())));
    m_assembler.mov(
        Assembler::Operand::Register(ARG4),
        Assembler::Operand::Imm(static_cast<u64>(op.is_immutable())));
    m_assembler.mov(
        Assembler::Operand::Register(ARG5),
        Assembler::Operand::Imm(static_cast<u64>(op.is_strict())));
    native_call((void*)cxx_create_variable);
    check_exception();
}

static Value cxx_set_variable(
    VM& vm,
    DeprecatedFlyString const& identifier,
    Value value,
    Bytecode::Op::EnvironmentMode environment_mode,
    Bytecode::Op::SetVariable::InitializationMode initialization_mode)
{
    TRY_OR_SET_EXCEPTION(Bytecode::set_variable(vm, identifier, value, environment_mode, initialization_mode));
    return {};
}

void Compiler::compile_set_variable(Bytecode::Op::SetVariable const& op)
{
    m_assembler.mov(
        Assembler::Operand::Register(ARG1),
        Assembler::Operand::Imm(bit_cast<u64>(&m_bytecode_executable.get_identifier(op.identifier().value()))));
    load_accumulator(ARG2);
    m_assembler.mov(
        Assembler::Operand::Register(ARG3),
        Assembler::Operand::Imm(to_underlying(op.mode())));
    m_assembler.mov(
        Assembler::Operand::Register(ARG4),
        Assembler::Operand::Imm(to_underlying(op.initialization_mode())));
    native_call((void*)cxx_set_variable);
    check_exception();
}

void Compiler::compile_continue_pending_unwind(Bytecode::Op::ContinuePendingUnwind const& op)
{
    // re-throw the exception if we reached the end of the finally block and there was no catch block to handle it
    load_vm_register(GPR0, Bytecode::Register::saved_exception());
    store_vm_register(Bytecode::Register::exception(), GPR0);
    m_assembler.mov(Assembler::Operand::Register(GPR1), Assembler::Operand::Imm(Value().encoded()));
    store_vm_register(Bytecode::Register::saved_exception(), GPR1);
    check_exception();

    // if (saved_return_value.is_empty()) goto resume_block;
    load_vm_register(GPR0, Bytecode::Register::saved_return_value());
    m_assembler.jump_if(
        Assembler::Operand::Register(GPR0),
        Assembler::Condition::EqualTo,
        Assembler::Operand::Register(GPR1),
        label_for(op.resume_target().block()));

    // finish the pending return from the try block
    store_vm_register(Bytecode::Register::return_value(), GPR0);
    jump_to_exit();
}

static void cxx_create_lexical_environment(VM& vm)
{
    auto make_and_swap_envs = [&](auto& old_environment) {
        GCPtr<Environment> environment = new_declarative_environment(*old_environment).ptr();
        swap(old_environment, environment);
        return environment;
    };
    vm.bytecode_interpreter().saved_lexical_environment_stack().append(make_and_swap_envs(vm.running_execution_context().lexical_environment));
}

void Compiler::compile_create_lexical_environment(Bytecode::Op::CreateLexicalEnvironment const&)
{
    native_call((void*)cxx_create_lexical_environment);
}

static void cxx_leave_lexical_environment(VM& vm)
{
    vm.running_execution_context().lexical_environment = vm.bytecode_interpreter().saved_lexical_environment_stack().take_last();
}

void Compiler::compile_leave_lexical_environment(Bytecode::Op::LeaveLexicalEnvironment const&)
{
    native_call((void*)cxx_leave_lexical_environment);
}

static Value cxx_concat_string(VM& vm, Value lhs, Value rhs)
{
    auto string = TRY_OR_SET_EXCEPTION(rhs.to_primitive_string(vm));
    return PrimitiveString::create(vm, lhs.as_string(), string);
}

void Compiler::compile_concat_string(Bytecode::Op::ConcatString const& op)
{
    load_vm_register(ARG1, op.lhs());
    load_accumulator(ARG2);
    native_call((void*)cxx_concat_string);
    store_vm_register(op.lhs(), RET);
    check_exception();
}

static void cxx_block_declaration_instantiation(VM& vm, ScopeNode const& scope_node)
{
    auto old_environment = vm.running_execution_context().lexical_environment;
    vm.bytecode_interpreter().saved_lexical_environment_stack().append(old_environment);
    vm.running_execution_context().lexical_environment = new_declarative_environment(*old_environment);
    scope_node.block_declaration_instantiation(vm, vm.running_execution_context().lexical_environment);
}

void Compiler::compile_block_declaration_instantiation(Bytecode::Op::BlockDeclarationInstantiation const& op)
{
    m_assembler.mov(
        Assembler::Operand::Register(ARG1),
        Assembler::Operand::Imm(bit_cast<u64>(&op.scope_node())));
    native_call((void*)cxx_block_declaration_instantiation);
}

static Value cxx_super_call_with_argument_array(VM& vm, Value argument_array, bool is_synthetic)
{
    return TRY_OR_SET_EXCEPTION(Bytecode::super_call_with_argument_array(vm, argument_array, is_synthetic));
}

void Compiler::compile_super_call_with_argument_array(Bytecode::Op::SuperCallWithArgumentArray const& op)
{
    load_accumulator(ARG1);
    m_assembler.mov(
        Assembler::Operand::Register(ARG2),
        Assembler::Operand::Imm(static_cast<u64>(op.is_synthetic())));
    native_call((void*)cxx_super_call_with_argument_array);
    store_accumulator(RET);
    check_exception();
}

static Value cxx_get_iterator(VM& vm, Value value, IteratorHint hint)
{
    auto iterator = TRY_OR_SET_EXCEPTION(get_iterator(vm, value, hint));
    return Bytecode::iterator_to_object(vm, iterator);
}

void Compiler::compile_get_iterator(Bytecode::Op::GetIterator const& op)
{
    load_accumulator(ARG1);
    m_assembler.mov(
        Assembler::Operand::Register(ARG2),
        Assembler::Operand::Imm(to_underlying(op.hint())));
    native_call((void*)cxx_get_iterator);
    store_accumulator(RET);
    check_exception();
}

static Value cxx_iterator_next(VM& vm, Value iterator)
{
    auto iterator_object = TRY_OR_SET_EXCEPTION(iterator.to_object(vm));
    auto iterator_record = Bytecode::object_to_iterator(vm, iterator_object);
    return TRY_OR_SET_EXCEPTION(iterator_next(vm, iterator_record));
}

void Compiler::compile_iterator_next(Bytecode::Op::IteratorNext const&)
{
    load_accumulator(ARG1);
    native_call((void*)cxx_iterator_next);
    store_accumulator(RET);
    check_exception();
}

static Value cxx_iterator_result_done(VM& vm, Value iterator)
{
    auto iterator_result = TRY_OR_SET_EXCEPTION(iterator.to_object(vm));
    return Value(TRY_OR_SET_EXCEPTION(iterator_complete(vm, iterator_result)));
}

void Compiler::compile_iterator_result_done(Bytecode::Op::IteratorResultDone const&)
{
    load_accumulator(ARG1);
    native_call((void*)cxx_iterator_result_done);
    store_accumulator(RET);
    check_exception();
}

static Value cxx_throw_if_not_object(VM& vm, Value value)
{
    if (!value.is_object())
        TRY_OR_SET_EXCEPTION(vm.throw_completion<TypeError>(ErrorType::NotAnObject, value.to_string_without_side_effects()));
    return {};
}

void Compiler::compile_throw_if_not_object(Bytecode::Op::ThrowIfNotObject const&)
{
    load_accumulator(ARG1);
    native_call((void*)cxx_throw_if_not_object);
    check_exception();
}

static Value cxx_throw_if_nullish(VM& vm, Value value)
{
    if (value.is_nullish())
        TRY_OR_SET_EXCEPTION(vm.throw_completion<TypeError>(ErrorType::NotObjectCoercible, value.to_string_without_side_effects()));
    return {};
}

void Compiler::compile_throw_if_nullish(Bytecode::Op::ThrowIfNullish const&)
{
    load_accumulator(ARG1);
    native_call((void*)cxx_throw_if_nullish);
    check_exception();
}

static Value cxx_iterator_result_value(VM& vm, Value iterator)
{
    auto iterator_result = TRY_OR_SET_EXCEPTION(iterator.to_object(vm));
    return TRY_OR_SET_EXCEPTION(iterator_value(vm, iterator_result));
}

void Compiler::compile_iterator_result_value(Bytecode::Op::IteratorResultValue const&)
{
    load_accumulator(ARG1);
    native_call((void*)cxx_iterator_result_value);
    store_accumulator(RET);
    check_exception();
}

static Value cxx_iterator_close(VM& vm, Value iterator, Completion::Type completion_type, Optional<Value> const& completion_value)
{
    auto iterator_object = TRY_OR_SET_EXCEPTION(iterator.to_object(vm));
    auto iterator_record = Bytecode::object_to_iterator(vm, iterator_object);

    // FIXME: Return the value of the resulting completion. (Note that m_completion_value can be empty!)
    TRY_OR_SET_EXCEPTION(iterator_close(vm, iterator_record, Completion { completion_type, completion_value, {} }));
    return {};
}

void Compiler::compile_iterator_close(Bytecode::Op::IteratorClose const& op)
{
    load_accumulator(ARG1);
    m_assembler.mov(
        Assembler::Operand::Register(ARG2),
        Assembler::Operand::Imm(to_underlying(op.completion_type())));
    m_assembler.mov(
        Assembler::Operand::Register(ARG3),
        Assembler::Operand::Imm(bit_cast<u64>(&op.completion_value())));
    native_call((void*)cxx_iterator_close);
    check_exception();
}

static Value iterator_to_array(VM& vm, Value iterator)
{
    return TRY_OR_SET_EXCEPTION(Bytecode::iterator_to_array(vm, iterator));
}

void Compiler::compile_iterator_to_array(Bytecode::Op::IteratorToArray const&)
{
    load_accumulator(ARG1);
    native_call((void*)iterator_to_array);
    store_accumulator(RET);
    check_exception();
}

static Value cxx_append(VM& vm, Value lhs, Value rhs, bool is_spread)
{
    TRY_OR_SET_EXCEPTION(Bytecode::append(vm, lhs, rhs, is_spread));
    return {};
}

void Compiler::compile_append(Bytecode::Op::Append const& op)
{
    load_vm_register(ARG1, op.lhs());
    load_accumulator(ARG2);
    m_assembler.mov(
        Assembler::Operand::Register(ARG3),
        Assembler::Operand::Imm(static_cast<u64>(op.is_spread())));
    native_call((void*)cxx_append);
    check_exception();
}

static Value cxx_delete_by_id(VM& vm, Value base, Bytecode::IdentifierTableIndex property)
{
    return TRY_OR_SET_EXCEPTION(Bytecode::delete_by_id(vm.bytecode_interpreter(), base, property));
}

void Compiler::compile_delete_by_id(Bytecode::Op::DeleteById const& op)
{
    load_accumulator(ARG1);
    m_assembler.mov(
        Assembler::Operand::Register(ARG2),
        Assembler::Operand::Imm(op.property().value()));
    native_call((void*)cxx_delete_by_id);
    store_accumulator(RET);
    check_exception();
}

static Value cxx_delete_by_value(VM& vm, Value base_value, Value property_key_value)
{
    return TRY_OR_SET_EXCEPTION(Bytecode::delete_by_value(vm.bytecode_interpreter(), base_value, property_key_value));
}

void Compiler::compile_delete_by_value(Bytecode::Op::DeleteByValue const& op)
{
    load_vm_register(ARG1, op.base());
    load_accumulator(ARG2);
    native_call((void*)cxx_delete_by_value);
    store_accumulator(RET);
    check_exception();
}

static Value cxx_delete_by_value_with_this(VM& vm, Value base_value, Value property_key_value, Value this_value)
{
    return TRY_OR_SET_EXCEPTION(Bytecode::delete_by_value_with_this(vm.bytecode_interpreter(), base_value, property_key_value, this_value));
}

void Compiler::compile_delete_by_value_with_this(Bytecode::Op::DeleteByValueWithThis const& op)
{
    load_vm_register(ARG1, op.base());
    load_accumulator(ARG2);
    load_vm_register(ARG3, op.this_value());
    native_call((void*)cxx_delete_by_value_with_this);
    store_accumulator(RET);
    check_exception();
}

static Value cxx_get_object_property_iterator(VM& vm, Value object)
{
    return TRY_OR_SET_EXCEPTION(Bytecode::get_object_property_iterator(vm, object));
}

void Compiler::compile_get_object_property_iterator(Bytecode::Op::GetObjectPropertyIterator const&)
{
    load_accumulator(ARG1);
    native_call((void*)cxx_get_object_property_iterator);
    store_accumulator(RET);
    check_exception();
}

static Value cxx_get_private_by_id(VM& vm, Value base_value, DeprecatedFlyString& name)
{
    auto private_reference = make_private_reference(vm, base_value, name);
    return TRY_OR_SET_EXCEPTION(private_reference.get_value(vm));
}

void Compiler::compile_get_private_by_id(Bytecode::Op::GetPrivateById const& op)
{
    load_accumulator(ARG1);
    m_assembler.mov(
        Assembler::Operand::Register(ARG2),
        Assembler::Operand::Imm(bit_cast<u64>(&m_bytecode_executable.get_identifier(op.property()))));
    native_call((void*)cxx_get_private_by_id);
    store_accumulator(RET);
    check_exception();
}

static Value cxx_resolve_super_base(VM& vm)
{
    // 1. Let env be GetThisEnvironment().
    auto& env = verify_cast<FunctionEnvironment>(*get_this_environment(vm));

    // 2. Assert: env.HasSuperBinding() is true.
    VERIFY(env.has_super_binding());

    // 3. Let baseValue be ? env.GetSuperBase().
    return TRY_OR_SET_EXCEPTION(env.get_super_base());
}

void Compiler::compile_resolve_super_base(Bytecode::Op::ResolveSuperBase const&)
{
    native_call((void*)cxx_resolve_super_base);
    store_accumulator(RET);
    check_exception();
}

static Value cxx_get_by_id_with_this(VM& vm, DeprecatedFlyString const& property, Value base_value, Value this_value, Bytecode::PropertyLookupCache& cache)
{
    return TRY_OR_SET_EXCEPTION(Bytecode::get_by_id(vm, property, base_value, this_value, cache));
}

void Compiler::compile_get_by_id_with_this(Bytecode::Op::GetByIdWithThis const& op)
{
    m_assembler.mov(
        Assembler::Operand::Register(ARG1),
        Assembler::Operand::Imm(bit_cast<u64>(&m_bytecode_executable.get_identifier(op.property()))));
    load_accumulator(ARG2);
    load_vm_register(ARG3, op.this_value());
    m_assembler.mov(
        Assembler::Operand::Register(ARG4),
        Assembler::Operand::Imm(bit_cast<u64>(&m_bytecode_executable.property_lookup_caches[op.cache_index()])));
    native_call((void*)cxx_get_by_id_with_this);
    store_accumulator(RET);
    check_exception();
}

static Value cxx_get_by_value_with_this(VM& vm, Value property_key_value, Value base, Value this_value)
{
    auto object = TRY_OR_SET_EXCEPTION(base.to_object(vm));
    auto property_key = TRY_OR_SET_EXCEPTION(property_key_value.to_property_key(vm));
    return TRY_OR_SET_EXCEPTION(object->internal_get(property_key, this_value));
}

void Compiler::compile_get_by_value_with_this(Bytecode::Op::GetByValueWithThis const& op)
{
    load_accumulator(ARG1);
    load_vm_register(ARG2, op.base());
    load_vm_register(ARG3, op.this_value());
    native_call((void*)cxx_get_by_value_with_this);
    store_accumulator(RET);
    check_exception();
}

static Value cxx_delete_by_id_with_this(VM& vm, Value base_value, DeprecatedFlyString const& identifier, Value this_value)
{
    auto reference = Reference { base_value, identifier, this_value, vm.in_strict_mode() };
    return Value(TRY_OR_SET_EXCEPTION(reference.delete_(vm)));
}

void Compiler::compile_delete_by_id_with_this(Bytecode::Op::DeleteByIdWithThis const& op)
{
    load_accumulator(ARG1);
    m_assembler.mov(
        Assembler::Operand::Register(ARG2),
        Assembler::Operand::Imm(bit_cast<u64>(&m_bytecode_executable.get_identifier(op.property()))));
    load_vm_register(ARG3, op.this_value());
    native_call((void*)cxx_delete_by_id_with_this);
    store_accumulator(RET);
}

static Value cxx_put_by_id_with_this(VM& vm, Value base, Value value, DeprecatedFlyString const& name, Value this_value, Bytecode::Op::PropertyKind kind)
{
    TRY_OR_SET_EXCEPTION(Bytecode::put_by_property_key(vm, base, this_value, value, name, kind));
    return {};
}

void Compiler::compile_put_by_id_with_this(Bytecode::Op::PutByIdWithThis const& op)
{
    load_vm_register(ARG1, op.base());
    load_accumulator(ARG2);
    m_assembler.mov(
        Assembler::Operand::Register(ARG3),
        Assembler::Operand::Imm(bit_cast<u64>(&m_bytecode_executable.get_identifier(op.property()))));
    load_vm_register(ARG4, op.this_value());
    m_assembler.mov(
        Assembler::Operand::Register(ARG5),
        Assembler::Operand::Imm(to_underlying(op.kind())));
    native_call((void*)cxx_put_by_id_with_this);
    check_exception();
}

static Value cxx_put_private_by_id(VM& vm, Value base, Value value, DeprecatedFlyString const& name)
{
    auto object = TRY_OR_SET_EXCEPTION(base.to_object(vm));
    auto private_reference = make_private_reference(vm, object, name);
    TRY_OR_SET_EXCEPTION(private_reference.put_value(vm, value));
    return value;
}

void Compiler::compile_put_private_by_id(Bytecode::Op::PutPrivateById const& op)
{
    load_vm_register(ARG1, op.base());
    load_accumulator(ARG2);
    m_assembler.mov(
        Assembler::Operand::Register(ARG3),
        Assembler::Operand::Imm(bit_cast<u64>(&m_bytecode_executable.get_identifier(op.property()))));
    native_call((void*)cxx_put_private_by_id);
    store_accumulator(RET);
    check_exception();
}

static Value cxx_import_call(VM& vm, Value specifier, Value options)
{
    return TRY_OR_SET_EXCEPTION(perform_import_call(vm, specifier, options));
}

void Compiler::compile_import_call(Bytecode::Op::ImportCall const& op)
{
    load_vm_register(ARG1, op.specifier());
    load_vm_register(ARG2, op.options());
    native_call((void*)cxx_import_call);
    store_accumulator(RET);
    check_exception();
}

static Value cxx_get_import_meta(VM& vm)
{
    return vm.get_import_meta();
}

void Compiler::compile_get_import_meta(Bytecode::Op::GetImportMeta const&)
{
    native_call((void*)cxx_get_import_meta);
    store_accumulator(RET);
}

static Value cxx_delete_variable(VM& vm, DeprecatedFlyString const& identifier)
{
    auto reference = TRY_OR_SET_EXCEPTION(vm.resolve_binding(identifier));
    return Value(TRY_OR_SET_EXCEPTION(reference.delete_(vm)));
}

void Compiler::compile_delete_variable(Bytecode::Op::DeleteVariable const& op)
{
    m_assembler.mov(
        Assembler::Operand::Register(ARG1),
        Assembler::Operand::Imm(bit_cast<u64>(&m_bytecode_executable.get_identifier(op.identifier().value()))));
    native_call((void*)cxx_delete_variable);
    store_accumulator(RET);
    check_exception();
}

static Value cxx_get_method(VM& vm, Value value, DeprecatedFlyString const& identifier)
{
    auto method = TRY_OR_SET_EXCEPTION(value.get_method(vm, identifier));
    return method ?: js_undefined();
}

void Compiler::compile_get_method(Bytecode::Op::GetMethod const& op)
{
    load_accumulator(ARG1);
    m_assembler.mov(
        Assembler::Operand::Register(ARG2),
        Assembler::Operand::Imm(bit_cast<u64>(&m_bytecode_executable.get_identifier(op.property()))));
    native_call((void*)cxx_get_method);
    store_accumulator(RET);
    check_exception();
}

static Value cxx_get_new_target(VM& vm)
{
    return vm.get_new_target();
}

void Compiler::compile_get_new_target(Bytecode::Op::GetNewTarget const&)
{
    native_call((void*)cxx_get_new_target);
    store_accumulator(RET);
}

static Value cxx_has_private_id(VM& vm, Value object, DeprecatedFlyString const& identifier)
{
    if (!object.is_object())
        TRY_OR_SET_EXCEPTION(vm.throw_completion<TypeError>(ErrorType::InOperatorWithObject));

    auto private_environment = vm.running_execution_context().private_environment;
    VERIFY(private_environment);
    auto private_name = private_environment->resolve_private_identifier(identifier);
    return Value(object.as_object().private_element_find(private_name) != nullptr);
}

void Compiler::compile_has_private_id(Bytecode::Op::HasPrivateId const& op)
{
    load_accumulator(ARG1);
    m_assembler.mov(
        Assembler::Operand::Register(ARG2),
        Assembler::Operand::Imm(bit_cast<u64>(&m_bytecode_executable.get_identifier(op.property()))));
    native_call((void*)cxx_has_private_id);
    store_accumulator(RET);
    check_exception();
}

#    define COMPILE_NEW_BUILTIN_ERROR_OP(NewErrorName, new_error_name, ErrorName)                              \
        static Value cxx_##new_error_name(VM& vm, DeprecatedString const& error_string)                        \
        {                                                                                                      \
            return ErrorName::create(*vm.current_realm(), error_string);                                       \
        }                                                                                                      \
                                                                                                               \
        void Compiler::compile_##new_error_name(Bytecode::Op::NewErrorName const& op)                          \
        {                                                                                                      \
            m_assembler.mov(                                                                                   \
                Assembler::Operand::Register(ARG1),                                                            \
                Assembler::Operand::Imm(bit_cast<u64>(&m_bytecode_executable.get_string(op.error_string())))); \
            native_call((void*)cxx_##new_error_name);                                                          \
            store_accumulator(RET);                                                                            \
        }
JS_ENUMERATE_NEW_BUILTIN_ERROR_BYTECODE_OPS(COMPILE_NEW_BUILTIN_ERROR_OP)
#    undef COMPILE_NEW_BUILTIN_ERROR_OP

static Value cxx_put_by_value_with_this(VM& vm, Value base, Value value, Value name, Value this_value, Bytecode::Op::PropertyKind kind)
{
    auto property_key = kind != Bytecode::Op::PropertyKind::Spread ? TRY_OR_SET_EXCEPTION(name.to_property_key(vm)) : PropertyKey {};
    TRY_OR_SET_EXCEPTION(Bytecode::put_by_property_key(vm, base, this_value, value, property_key, kind));
    return value;
}

void Compiler::compile_put_by_value_with_this(Bytecode::Op::PutByValueWithThis const& op)
{
    load_vm_register(ARG1, op.base());
    load_accumulator(ARG2);
    if (op.kind() != Bytecode::Op::PropertyKind::Spread) {
        load_vm_register(ARG3, op.property());
    } else {
        m_assembler.mov(
            Assembler::Operand::Register(ARG3),
            Assembler::Operand::Imm(Value().encoded()));
    }
    load_vm_register(ARG4, op.this_value());
    m_assembler.mov(
        Assembler::Operand::Register(ARG5),
        Assembler::Operand::Imm(to_underlying(op.kind())));
    native_call((void*)cxx_put_by_value_with_this);
    store_accumulator(RET);
    check_exception();
}

static Value cxx_copy_object_excluding_properties(VM& vm, Value from_object, u64 excluded_names_count, Value* excluded_names)
{
    auto& realm = *vm.current_realm();
    auto to_object = Object::create(realm, realm.intrinsics().object_prototype());

    HashTable<PropertyKey> excluded_names_table;
    for (size_t i = 0; i < excluded_names_count; ++i) {
        excluded_names_table.set(TRY_OR_SET_EXCEPTION(excluded_names[i].to_property_key(vm)));
    }
    TRY_OR_SET_EXCEPTION(to_object->copy_data_properties(vm, from_object, excluded_names_table));
    return to_object;
}

void Compiler::compile_copy_object_excluding_properties(Bytecode::Op::CopyObjectExcludingProperties const& op)
{
    load_vm_register(ARG1, op.from_object());
    m_assembler.mov(
        Assembler::Operand::Register(ARG2),
        Assembler::Operand::Imm(op.excluded_names_count()));

    // Build `Value arg3[op.excluded_names_count()] {...}` on the stack.
    auto stack_space = align_up_to(op.excluded_names_count() * sizeof(Value), 16);
    m_assembler.sub(Assembler::Operand::Register(STACK_POINTER), Assembler::Operand::Imm(stack_space));
    m_assembler.mov(Assembler::Operand::Register(ARG3), Assembler::Operand::Register(STACK_POINTER));
    for (size_t i = 0; i < op.excluded_names_count(); ++i) {
        load_vm_register(GPR0, op.excluded_names()[i]);
        m_assembler.mov(Assembler::Operand::Mem64BaseAndOffset(ARG3, i * sizeof(Value)), Assembler::Operand::Register(GPR0));
    }

    native_call((void*)cxx_copy_object_excluding_properties);

    // Restore the stack pointer / discard array.
    m_assembler.add(Assembler::Operand::Register(STACK_POINTER), Assembler::Operand::Imm(stack_space));

    store_accumulator(RET);
    check_exception();
}

static Value cxx_async_iterator_close(VM& vm, Value iterator, Completion::Type completion_type, Optional<Value> const& completion_value)
{
    auto iterator_object = TRY_OR_SET_EXCEPTION(iterator.to_object(vm));
    auto iterator_record = Bytecode::object_to_iterator(vm, iterator_object);

    // FIXME: Return the value of the resulting completion. (Note that completion_value can be empty!)
    TRY_OR_SET_EXCEPTION(async_iterator_close(vm, iterator_record, Completion { completion_type, completion_value, {} }));
    return {};
}

void Compiler::compile_async_iterator_close(Bytecode::Op::AsyncIteratorClose const& op)
{
    load_accumulator(ARG1);
    m_assembler.mov(
        Assembler::Operand::Register(ARG2),
        Assembler::Operand::Imm(to_underlying(op.completion_type())));
    m_assembler.mov(
        Assembler::Operand::Register(ARG3),
        Assembler::Operand::Imm(bit_cast<u64>(&op.completion_value())));
    native_call((void*)cxx_async_iterator_close);
    check_exception();
}

static Value cxx_continuation(VM& vm, Value value, Value continuation, Value is_await)
{
    auto object = Object::create(*vm.current_realm(), nullptr);
    object->define_direct_property("result", value.value_or(js_undefined()), JS::default_attributes);
    object->define_direct_property("continuation", continuation, JS::default_attributes);
    object->define_direct_property("isAwait", is_await, JS::default_attributes);
    return object;
}

void Compiler::compile_continuation(Optional<Bytecode::Label> continuation, bool is_await)
{
    load_accumulator(ARG1);
    if (continuation.has_value()) {
        // FIXME: If we get a pointer, which is not accurately representable as a double
        //        will cause this to explode
        auto continuation_value = Value(static_cast<double>(bit_cast<u64>(&continuation->block())));
        m_assembler.mov(
            Assembler::Operand::Register(ARG2),
            Assembler::Operand::Imm(continuation_value.encoded()));
    } else {
        m_assembler.mov(
            Assembler::Operand::Register(ARG2),
            Assembler::Operand::Imm(Value(0).encoded()));
    }
    m_assembler.mov(
        Assembler::Operand::Register(ARG3),
        Assembler::Operand::Imm(Value(is_await).encoded()));
    native_call((void*)cxx_continuation);
    store_vm_register(Bytecode::Register::return_value(), RET);

    // FIXME: This should run the finalizer if it is a return
    jump_to_exit();
}

void Compiler::compile_yield(Bytecode::Op::Yield const& op)
{
    compile_continuation(op.continuation(), false);
}

void Compiler::compile_await(Bytecode::Op::Await const& op)
{
    compile_continuation(op.continuation(), true);
}

void Compiler::jump_to_exit()
{
    m_assembler.jump(m_exit_label);
}

void Compiler::native_call(void* function_address, Vector<Assembler::Operand> const& stack_arguments)
{
    // NOTE: We don't preserve caller-saved registers when making a native call.
    //       This means that they may have changed after we return from the call.
    m_assembler.native_call(bit_cast<u64>(function_address), { Assembler::Operand::Register(ARG0) }, stack_arguments);
}

OwnPtr<NativeExecutable> Compiler::compile(Bytecode::Executable& bytecode_executable)
{
    if (!getenv("LIBJS_JIT"))
        return nullptr;

    Compiler compiler { bytecode_executable };

    Vector<BytecodeMapping> mapping;

    mapping.append({
        .native_offset = compiler.m_output.size(),
        .block_index = BytecodeMapping::EXECUTABLE,
        .bytecode_offset = 0,
    });

    compiler.m_assembler.enter();

    compiler.m_assembler.mov(
        Assembler::Operand::Register(REGISTER_ARRAY_BASE),
        Assembler::Operand::Register(ARG1));

    compiler.m_assembler.mov(
        Assembler::Operand::Register(LOCALS_ARRAY_BASE),
        Assembler::Operand::Register(ARG2));

    compiler.m_assembler.mov(
        Assembler::Operand::Register(RUNNING_EXECUTION_CONTEXT_BASE),
        Assembler::Operand::Register(ARG4));

    compiler.reload_cached_accumulator();

    Assembler::Label normal_entry {};

    compiler.m_assembler.jump_if(
        Assembler::Operand::Register(ARG3),
        Assembler::Condition::EqualTo,
        Assembler::Operand::Imm(0),
        normal_entry);

    compiler.m_assembler.jump(Assembler::Operand::Register(ARG3));

    normal_entry.link(compiler.m_assembler);

    for (size_t block_index = 0; block_index < bytecode_executable.basic_blocks.size(); block_index++) {
        auto& block = bytecode_executable.basic_blocks[block_index];
        compiler.block_data_for(*block).start_offset = compiler.m_output.size();
        compiler.set_current_block(*block);
        auto it = Bytecode::InstructionStreamIterator(block->instruction_stream());

        if (it.at_end()) {
            mapping.append({
                .native_offset = compiler.m_output.size(),
                .block_index = block_index,
                .bytecode_offset = 0,
            });
        }

        while (!it.at_end()) {
            auto const& op = *it;

            mapping.append({
                .native_offset = compiler.m_output.size(),
                .block_index = block_index,
                .bytecode_offset = it.offset(),
            });

            switch (op.type()) {
#    define CASE_BYTECODE_OP(OpTitleCase, op_snake_case, ...)                                \
    case Bytecode::Instruction::Type::OpTitleCase:                                           \
        compiler.compile_##op_snake_case(static_cast<Bytecode::Op::OpTitleCase const&>(op)); \
        break;
                JS_ENUMERATE_IMPLEMENTED_JIT_OPS(CASE_BYTECODE_OP)
#    undef CASE_BYTECODE_OP
            default:
                if constexpr (LOG_JIT_FAILURE) {
                    dbgln("\033[31;1mJIT compilation failed\033[0m: {}", bytecode_executable.name);
                    dbgln("Unsupported bytecode op: {}", op.to_deprecated_string(bytecode_executable));
                }
                return nullptr;
            }

            ++it;
        }
        if (!block->is_terminated())
            compiler.jump_to_exit();
    }

    mapping.append({
        .native_offset = compiler.m_output.size(),
        .block_index = BytecodeMapping::EXECUTABLE,
        .bytecode_offset = 1,
    });

    compiler.m_exit_label.link(compiler.m_assembler);
    compiler.flush_cached_accumulator();
    compiler.m_assembler.exit();

    auto* executable_memory = mmap(nullptr, compiler.m_output.size(), PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE, 0, 0);
    if (executable_memory == MAP_FAILED) {
        dbgln("mmap: {}", strerror(errno));
        return nullptr;
    }

    for (auto& block : bytecode_executable.basic_blocks) {
        auto& block_data = compiler.block_data_for(*block);
        block_data.label.link_to(compiler.m_assembler, block_data.start_offset);
    }

    if constexpr (DUMP_JIT_MACHINE_CODE_TO_STDOUT) {
        (void)write(STDOUT_FILENO, compiler.m_output.data(), compiler.m_output.size());
    }

    memcpy(executable_memory, compiler.m_output.data(), compiler.m_output.size());

    if (mprotect(executable_memory, compiler.m_output.size(), PROT_READ | PROT_EXEC) < 0) {
        dbgln("mprotect: {}", strerror(errno));
        return nullptr;
    }

    if constexpr (LOG_JIT_SUCCESS) {
        dbgln("\033[32;1mJIT compilation succeeded!\033[0m {}", bytecode_executable.name);
    }

    auto executable = make<NativeExecutable>(executable_memory, compiler.m_output.size(), mapping);
    if constexpr (DUMP_JIT_DISASSEMBLY)
        executable->dump_disassembly(bytecode_executable);
    return executable;
}

}

#endif
