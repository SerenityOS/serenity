/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/OwnPtr.h>
#include <LibJS/Bytecode/CommonImplementations.h>
#include <LibJS/Bytecode/Instruction.h>
#include <LibJS/Bytecode/Interpreter.h>
#include <LibJS/JIT/Compiler.h>
#include <LibJS/Runtime/Array.h>
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

    m_assembler.jump_if_equal(
        Assembler::Operand::Register(GPR0),
        Assembler::Operand::Imm32(0),
        label_for(op.false_target()->block()));

    m_assembler.jump(label_for(op.true_target()->block()));
}

[[maybe_unused]] static Value cxx_increment(VM& vm, Value value)
{
    dbgln("cxx_increment {}", value);
    auto old_value = TRY_OR_SET_EXCEPTION(value.to_numeric(vm));
    if (old_value.is_number())
        return Value(old_value.as_double() + 1);
    return BigInt::create(vm, old_value.as_bigint().big_integer().plus(Crypto::SignedBigInteger { 1 }));
}

template<typename Codegen>
void Compiler::branch_if_int32(Assembler::Reg reg, Codegen codegen)
{
    // GPR0 = reg >> 48;
    m_assembler.mov(Assembler::Operand::Register(GPR0), Assembler::Operand::Register(reg));
    m_assembler.shift_right(Assembler::Operand::Register(GPR0), Assembler::Operand::Imm8(48));

    auto not_int32_case = m_assembler.make_label();
    m_assembler.jump_if_not_equal(
        Assembler::Operand::Register(GPR0),
        Assembler::Operand::Imm32(INT32_TAG),
        not_int32_case);

    codegen();

    not_int32_case.link(m_assembler);
}

template<typename Codegen>
void Compiler::branch_if_both_int32(Assembler::Reg lhs, Assembler::Reg rhs, Codegen codegen)
{
    // GPR0 = lhs >> 48;
    m_assembler.mov(Assembler::Operand::Register(GPR0), Assembler::Operand::Register(lhs));
    m_assembler.shift_right(Assembler::Operand::Register(GPR0), Assembler::Operand::Imm8(48));

    // GPR1 = rhs >> 48;
    m_assembler.mov(Assembler::Operand::Register(GPR1), Assembler::Operand::Register(rhs));
    m_assembler.shift_right(Assembler::Operand::Register(GPR1), Assembler::Operand::Imm8(48));

    auto not_int32_case = m_assembler.make_label();
    m_assembler.jump_if_not_equal(
        Assembler::Operand::Register(GPR0),
        Assembler::Operand::Imm32(INT32_TAG),
        not_int32_case);
    m_assembler.jump_if_not_equal(
        Assembler::Operand::Register(GPR1),
        Assembler::Operand::Imm32(INT32_TAG),
        not_int32_case);

    codegen();

    not_int32_case.link(m_assembler);
}

void Compiler::compile_increment(Bytecode::Op::Increment const&)
{
    load_vm_register(ARG1, Bytecode::Register::accumulator());

    auto end = m_assembler.make_label();
    auto slow_case = m_assembler.make_label();

    branch_if_int32(ARG1, [&] {
        // GPR0 = ARG1 & 0xffffffff;
        m_assembler.mov(
            Assembler::Operand::Register(GPR0),
            Assembler::Operand::Register(ARG1));
        m_assembler.mov(
            Assembler::Operand::Register(GPR1),
            Assembler::Operand::Imm64(0xffffffff));
        m_assembler.bitwise_and(
            Assembler::Operand::Register(GPR0),
            Assembler::Operand::Register(GPR1));

        // if (GPR0 == 0x7fffffff) goto slow_case;
        m_assembler.jump_if_equal(
            Assembler::Operand::Register(GPR0),
            Assembler::Operand::Imm32(0x7fffffff),
            slow_case);

        // GPR0 += 1;
        m_assembler.add(
            Assembler::Operand::Register(GPR0),
            Assembler::Operand::Imm32(1));

        // GPR0 |= INT32_TAG << 48;
        m_assembler.mov(
            Assembler::Operand::Register(GPR1),
            Assembler::Operand::Imm64(SHIFTED_INT32_TAG));
        m_assembler.bitwise_or(
            Assembler::Operand::Register(GPR0),
            Assembler::Operand::Register(GPR1));

        // accumulator = GPR0;
        store_vm_register(Bytecode::Register::accumulator(), GPR0);

        m_assembler.jump(end);
    });

    slow_case.link(m_assembler);
    m_assembler.native_call((void*)cxx_increment);
    store_vm_register(Bytecode::Register::accumulator(), RET);
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
    load_vm_register(ARG1, Bytecode::Register::accumulator());
    m_assembler.native_call((void*)cxx_decrement);
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
        block_data_for(finalizer.value().block()).absolute_references_to_here.append(m_assembler.m_output.size() - 8);
    m_assembler.push(Assembler::Operand::Register(GPR0));

    // push handler (patched later)
    m_assembler.mov(
        Assembler::Operand::Register(GPR0),
        Assembler::Operand::Imm64(0));
    if (handler.has_value())
        block_data_for(handler.value().block()).absolute_references_to_here.append(m_assembler.m_output.size() - 8);
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

    m_assembler.jump(label_for(op.entry_point().block()));
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

#define DO_COMPILE_COMMON_BINARY_OP(TitleCaseName, snake_case_name)                 \
    static Value cxx_##snake_case_name(VM& vm, Value lhs, Value rhs)                \
    {                                                                               \
        return TRY_OR_SET_EXCEPTION(snake_case_name(vm, lhs, rhs));                 \
    }                                                                               \
                                                                                    \
    void Compiler::compile_##snake_case_name(Bytecode::Op::TitleCaseName const& op) \
    {                                                                               \
        load_vm_register(ARG1, op.lhs());                                           \
        load_vm_register(ARG2, Bytecode::Register::accumulator());                  \
        m_assembler.native_call((void*)cxx_##snake_case_name);                      \
        store_vm_register(Bytecode::Register::accumulator(), RET);                  \
        check_exception();                                                          \
    }

JS_ENUMERATE_COMMON_BINARY_OPS_WITHOUT_FAST_PATH(DO_COMPILE_COMMON_BINARY_OP)
#undef DO_COMPILE_COMMON_BINARY_OP

static Value cxx_less_than(VM& vm, Value lhs, Value rhs)
{
    return TRY_OR_SET_EXCEPTION(less_than(vm, lhs, rhs));
}

void Compiler::compile_less_than(Bytecode::Op::LessThan const& op)
{
    load_vm_register(ARG1, op.lhs());
    load_vm_register(ARG2, Bytecode::Register::accumulator());

    auto end = m_assembler.make_label();

    branch_if_both_int32(ARG1, ARG2, [&] {
        // if (ARG1 < ARG2) return true;
        // else return false;

        auto true_case = m_assembler.make_label();

        m_assembler.sign_extend_32_to_64_bits(ARG1);
        m_assembler.sign_extend_32_to_64_bits(ARG2);

        m_assembler.jump_if_less_than(
            Assembler::Operand::Register(ARG1),
            Assembler::Operand::Register(ARG2),
            true_case);

        m_assembler.mov(
            Assembler::Operand::Register(GPR0),
            Assembler::Operand::Imm64(Value(false).encoded()));
        store_vm_register(Bytecode::Register::accumulator(), GPR0);
        m_assembler.jump(end);

        true_case.link(m_assembler);
        m_assembler.mov(
            Assembler::Operand::Register(GPR0),
            Assembler::Operand::Imm64(Value(true).encoded()));
        store_vm_register(Bytecode::Register::accumulator(), GPR0);

        m_assembler.jump(end);
    });

    m_assembler.native_call((void*)cxx_less_than);
    store_vm_register(Bytecode::Register::accumulator(), RET);
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

#define DO_COMPILE_COMMON_UNARY_OP(TitleCaseName, snake_case_name)               \
    static Value cxx_##snake_case_name(VM& vm, Value value)                      \
    {                                                                            \
        return TRY_OR_SET_EXCEPTION(snake_case_name(vm, value));                 \
    }                                                                            \
                                                                                 \
    void Compiler::compile_##snake_case_name(Bytecode::Op::TitleCaseName const&) \
    {                                                                            \
        load_vm_register(ARG1, Bytecode::Register::accumulator());               \
        m_assembler.native_call((void*)cxx_##snake_case_name);                   \
        store_vm_register(Bytecode::Register::accumulator(), RET);               \
        check_exception();                                                       \
    }

JS_ENUMERATE_COMMON_UNARY_OPS(DO_COMPILE_COMMON_UNARY_OP)
#undef DO_COMPILE_COMMON_UNARY_OP

void Compiler::compile_return(Bytecode::Op::Return const&)
{
    load_vm_register(GPR0, Bytecode::Register::accumulator());
    store_vm_register(Bytecode::Register::return_value(), GPR0);
    m_assembler.exit();
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
        Assembler::Operand::Imm64(bit_cast<u64>(&string)));
    m_assembler.native_call((void*)cxx_new_string);
    store_vm_register(Bytecode::Register::accumulator(), RET);
}

static Value cxx_new_object(VM& vm)
{
    auto& realm = *vm.current_realm();
    return Object::create(realm, realm.intrinsics().object_prototype());
}

void Compiler::compile_new_object(Bytecode::Op::NewObject const&)
{
    m_assembler.native_call((void*)cxx_new_object);
    store_vm_register(Bytecode::Register::accumulator(), RET);
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
        Assembler::Operand::Imm64(op.element_count()));
    m_assembler.mov(
        Assembler::Operand::Register(ARG2),
        Assembler::Operand::Imm64(op.element_count() ? op.start().index() : 0));
    m_assembler.native_call((void*)cxx_new_array);
    store_vm_register(Bytecode::Register::accumulator(), RET);
}

Value cxx_new_function(
    VM& vm,
    FunctionExpression const& function_node,
    Optional<Bytecode::IdentifierTableIndex> const& lhs_name,
    Optional<Bytecode::Register> const& home_object)
{
    return Bytecode::new_function(vm, function_node, lhs_name, home_object);
}

void Compiler::compile_new_function(Bytecode::Op::NewFunction const& op)
{
    m_assembler.mov(
        Assembler::Operand::Register(ARG1),
        Assembler::Operand::Imm64(bit_cast<u64>(&op.function_node())));
    m_assembler.mov(
        Assembler::Operand::Register(ARG2),
        Assembler::Operand::Imm64(bit_cast<u64>(&op.lhs_name())));
    m_assembler.mov(
        Assembler::Operand::Register(ARG3),
        Assembler::Operand::Imm64(bit_cast<u64>(&op.home_object())));
    m_assembler.native_call((void*)cxx_new_function);
    store_vm_register(Bytecode::Register::accumulator(), RET);
}

static Value cxx_get_by_id(VM& vm, Value base, Bytecode::IdentifierTableIndex property, u32 cache_index)
{
    return TRY_OR_SET_EXCEPTION(Bytecode::get_by_id(vm.bytecode_interpreter(), property, base, base, cache_index));
}

void Compiler::compile_get_by_id(Bytecode::Op::GetById const& op)
{
    load_vm_register(ARG1, Bytecode::Register::accumulator());
    m_assembler.mov(
        Assembler::Operand::Register(ARG2),
        Assembler::Operand::Imm64(op.property().value()));
    m_assembler.mov(
        Assembler::Operand::Register(ARG3),
        Assembler::Operand::Imm64(op.cache_index()));
    m_assembler.native_call((void*)cxx_get_by_id);
    store_vm_register(Bytecode::Register::accumulator(), RET);
    check_exception();
}

static Value cxx_get_by_value(VM& vm, Value base, Value property)
{
    return TRY_OR_SET_EXCEPTION(Bytecode::get_by_value(vm.bytecode_interpreter(), base, property));
}

void Compiler::compile_get_by_value(Bytecode::Op::GetByValue const& op)
{
    load_vm_register(ARG1, op.base());
    load_vm_register(ARG2, Bytecode::Register::accumulator());
    m_assembler.native_call((void*)cxx_get_by_value);
    store_vm_register(Bytecode::Register::accumulator(), RET);
    check_exception();
}

static Value cxx_get_global(VM& vm, Bytecode::IdentifierTableIndex identifier, u32 cache_index)
{
    return TRY_OR_SET_EXCEPTION(Bytecode::get_global(vm.bytecode_interpreter(), identifier, cache_index));
}

void Compiler::compile_get_global(Bytecode::Op::GetGlobal const& op)
{
    m_assembler.mov(
        Assembler::Operand::Register(ARG1),
        Assembler::Operand::Imm64(op.identifier().value()));
    m_assembler.mov(
        Assembler::Operand::Register(ARG2),
        Assembler::Operand::Imm64(op.cache_index()));
    m_assembler.native_call((void*)cxx_get_global);
    store_vm_register(Bytecode::Register::accumulator(), RET);
    check_exception();
}

static Value cxx_to_numeric(VM& vm, Value value)
{
    return TRY_OR_SET_EXCEPTION(value.to_numeric(vm));
}

void Compiler::compile_to_numeric(Bytecode::Op::ToNumeric const&)
{
    load_vm_register(ARG1, Bytecode::Register::accumulator());
    m_assembler.native_call((void*)cxx_to_numeric);
    store_vm_register(Bytecode::Register::accumulator(), RET);
    check_exception();
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
        Assembler::Operand::Imm64(Value().encoded()));

    auto slow_case = m_assembler.make_label();
    m_assembler.jump_if_equal(
        Assembler::Operand::Register(GPR0),
        Assembler::Operand::Register(GPR1),
        slow_case);

    // Fast case: We have a cached `this` value!
    store_vm_register(Bytecode::Register::accumulator(), GPR0);
    auto end = m_assembler.jump();

    slow_case.link(m_assembler);
    m_assembler.native_call((void*)cxx_resolve_this_binding);
    store_vm_register(Bytecode::Register::accumulator(), RET);
    check_exception();

    end.link(m_assembler);
}

static Value cxx_put_by_id(VM& vm, Value base, Bytecode::IdentifierTableIndex property, Value value, Bytecode::Op::PropertyKind kind)
{
    PropertyKey name = vm.bytecode_interpreter().current_executable().get_identifier(property);
    TRY_OR_SET_EXCEPTION(Bytecode::put_by_property_key(vm, base, base, value, name, kind));
    vm.bytecode_interpreter().accumulator() = value;
    return {};
}

void Compiler::compile_put_by_id(Bytecode::Op::PutById const& op)
{
    load_vm_register(ARG1, op.base());
    m_assembler.mov(
        Assembler::Operand::Register(ARG2),
        Assembler::Operand::Imm64(op.property().value()));
    load_vm_register(ARG3, Bytecode::Register::accumulator());
    m_assembler.mov(
        Assembler::Operand::Register(ARG4),
        Assembler::Operand::Imm64(to_underlying(op.kind())));
    m_assembler.native_call((void*)cxx_put_by_id);
    check_exception();
}

static Value cxx_put_by_value(VM& vm, Value base, Value property, Value value, Bytecode::Op::PropertyKind kind)
{
    TRY_OR_SET_EXCEPTION(Bytecode::put_by_value(vm, base, property, value, kind));
    vm.bytecode_interpreter().accumulator() = value;
    return {};
}

void Compiler::compile_put_by_value(Bytecode::Op::PutByValue const& op)
{
    load_vm_register(ARG1, op.base());
    load_vm_register(ARG2, op.property());
    load_vm_register(ARG3, Bytecode::Register::accumulator());
    m_assembler.mov(
        Assembler::Operand::Register(ARG4),
        Assembler::Operand::Imm64(to_underlying(op.kind())));
    m_assembler.native_call((void*)cxx_put_by_value);
    check_exception();
}

static Value cxx_call(VM& vm, Value callee, u32 first_argument_index, u32 argument_count, Value this_value, Bytecode::Op::CallType call_type)
{
    // FIXME: Uncomment this and deal with it.
    // TRY_OR_SET_EXCEPTION(throw_if_needed_for_call(vm.bytecode_interpreter(), *this, callee));

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
        Assembler::Operand::Imm64(op.first_argument().index()));
    m_assembler.mov(
        Assembler::Operand::Register(ARG3),
        Assembler::Operand::Imm64(op.argument_count()));
    load_vm_register(ARG4, op.this_value());
    m_assembler.mov(
        Assembler::Operand::Register(ARG5),
        Assembler::Operand::Imm64(to_underlying(op.call_type())));
    m_assembler.native_call((void*)cxx_call);
    store_vm_register(Bytecode::Register::accumulator(), RET);
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
        Assembler::Operand::Imm64(bit_cast<u64>(&m_bytecode_executable.get_identifier(op.identifier().value()))));
    m_assembler.native_call((void*)cxx_typeof_variable);
    store_vm_register(Bytecode::Register::accumulator(), RET);
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
        Assembler::Operand::Imm64(bit_cast<u64>(&m_bytecode_executable.get_identifier(op.identifier().value()))));
    load_vm_register(ARG2, Bytecode::Register::accumulator());
    m_assembler.mov(
        Assembler::Operand::Register(ARG3),
        Assembler::Operand::Imm64(to_underlying(op.mode())));
    m_assembler.mov(
        Assembler::Operand::Register(ARG4),
        Assembler::Operand::Imm64(to_underlying(op.initialization_mode())));
    m_assembler.native_call((void*)cxx_set_variable);
    check_exception();
}

OwnPtr<NativeExecutable> Compiler::compile(Bytecode::Executable& bytecode_executable)
{
    if (getenv("LIBJS_NO_JIT"))
        return nullptr;

    Compiler compiler { bytecode_executable };

    compiler.m_assembler.enter();

    compiler.m_assembler.mov(
        Assembler::Operand::Register(REGISTER_ARRAY_BASE),
        Assembler::Operand::Register(ARG1));

    compiler.m_assembler.mov(
        Assembler::Operand::Register(LOCALS_ARRAY_BASE),
        Assembler::Operand::Register(ARG2));

    compiler.push_unwind_context(false, {}, {});

    for (auto& block : bytecode_executable.basic_blocks) {
        compiler.block_data_for(*block).start_offset = compiler.m_output.size();
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
            case Bytecode::Instruction::Type::Increment:
                compiler.compile_increment(static_cast<Bytecode::Op::Increment const&>(op));
                break;
            case Bytecode::Instruction::Type::Decrement:
                compiler.compile_decrement(static_cast<Bytecode::Op::Decrement const&>(op));
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
            case Bytecode::Instruction::Type::Return:
                compiler.compile_return(static_cast<Bytecode::Op::Return const&>(op));
                break;
            case Bytecode::Instruction::Type::NewString:
                compiler.compile_new_string(static_cast<Bytecode::Op::NewString const&>(op));
                break;
            case Bytecode::Instruction::Type::NewObject:
                compiler.compile_new_object(static_cast<Bytecode::Op::NewObject const&>(op));
                break;
            case Bytecode::Instruction::Type::NewArray:
                compiler.compile_new_array(static_cast<Bytecode::Op::NewArray const&>(op));
                break;
            case Bytecode::Instruction::Type::NewFunction:
                compiler.compile_new_function(static_cast<Bytecode::Op::NewFunction const&>(op));
                break;
            case Bytecode::Instruction::Type::GetById:
                compiler.compile_get_by_id(static_cast<Bytecode::Op::GetById const&>(op));
                break;
            case Bytecode::Instruction::Type::GetByValue:
                compiler.compile_get_by_value(static_cast<Bytecode::Op::GetByValue const&>(op));
                break;
            case Bytecode::Instruction::Type::GetGlobal:
                compiler.compile_get_global(static_cast<Bytecode::Op::GetGlobal const&>(op));
                break;
            case Bytecode::Instruction::Type::PutById:
                compiler.compile_put_by_id(static_cast<Bytecode::Op::PutById const&>(op));
                break;
            case Bytecode::Instruction::Type::PutByValue:
                compiler.compile_put_by_value(static_cast<Bytecode::Op::PutByValue const&>(op));
                break;
            case Bytecode::Instruction::Type::ToNumeric:
                compiler.compile_to_numeric(static_cast<Bytecode::Op::ToNumeric const&>(op));
                break;
            case Bytecode::Instruction::Type::ResolveThisBinding:
                compiler.compile_resolve_this_binding(static_cast<Bytecode::Op::ResolveThisBinding const&>(op));
                break;
            case Bytecode::Instruction::Type::Call:
                compiler.compile_call(static_cast<Bytecode::Op::Call const&>(op));
                break;
            case Bytecode::Instruction::Type::TypeofVariable:
                compiler.compile_typeof_variable(static_cast<Bytecode::Op::TypeofVariable const&>(op));
                break;
            case Bytecode::Instruction::Type::SetVariable:
                compiler.compile_set_variable(static_cast<Bytecode::Op::SetVariable const&>(op));
                break;
            case Bytecode::Instruction::Type::LessThan:
                compiler.compile_less_than(static_cast<Bytecode::Op::LessThan const&>(op));
                break;

#define DO_COMPILE_COMMON_BINARY_OP(TitleCaseName, snake_case_name)                              \
    case Bytecode::Instruction::Type::TitleCaseName:                                             \
        compiler.compile_##snake_case_name(static_cast<Bytecode::Op::TitleCaseName const&>(op)); \
        break;
                JS_ENUMERATE_COMMON_BINARY_OPS_WITHOUT_FAST_PATH(DO_COMPILE_COMMON_BINARY_OP)
#undef DO_COMPILE_COMMON_BINARY_OP

#define DO_COMPILE_COMMON_UNARY_OP(TitleCaseName, snake_case_name)                               \
    case Bytecode::Instruction::Type::TitleCaseName:                                             \
        compiler.compile_##snake_case_name(static_cast<Bytecode::Op::TitleCaseName const&>(op)); \
        break;
                JS_ENUMERATE_COMMON_UNARY_OPS(DO_COMPILE_COMMON_UNARY_OP)
#undef DO_COMPILE_COMMON_UNARY_OP

            default:
                dbgln("\033[31;1mJIT compilation failed\033[0m: {}", bytecode_executable.name);
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
        auto& block_data = compiler.block_data_for(*block);

        block_data.label.link_to(compiler.m_assembler, block_data.start_offset);

        // Patch up all the absolute references
        for (auto& absolute_reference : block_data.absolute_references_to_here) {
            auto offset = bit_cast<u64>(executable_memory) + block_data.start_offset;
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
    if (!res) { }

    memcpy(executable_memory, compiler.m_output.data(), compiler.m_output.size());
    mprotect(executable_memory, compiler.m_output.size(), PROT_READ | PROT_EXEC);

    dbgln("\033[32;1mJIT compilation succeeded!\033[0m {}", bytecode_executable.name);

    return make<NativeExecutable>(executable_memory, compiler.m_output.size());
}

}
