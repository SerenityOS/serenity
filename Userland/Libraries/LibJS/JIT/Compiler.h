/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#if ARCH(X86_64)
#    include <LibJIT/Assembler.h>
#    include <LibJS/Bytecode/Executable.h>
#    include <LibJS/Bytecode/Op.h>
#    include <LibJS/JIT/NativeExecutable.h>

namespace JS::JIT {

using ::JIT::Assembler;

class Compiler {
public:
    static OwnPtr<NativeExecutable> compile(Bytecode::Executable&);

private:
#    if ARCH(X86_64)
    static constexpr auto GPR0 = Assembler::Reg::RAX;
    static constexpr auto GPR1 = Assembler::Reg::RCX;
    static constexpr auto ARG0 = Assembler::Reg::RDI;
    static constexpr auto ARG1 = Assembler::Reg::RSI;
    static constexpr auto ARG2 = Assembler::Reg::RDX;
    static constexpr auto ARG3 = Assembler::Reg::RCX;
    static constexpr auto ARG4 = Assembler::Reg::R8;
    static constexpr auto ARG5 = Assembler::Reg::R9;
    static constexpr auto RET = Assembler::Reg::RAX;
    static constexpr auto STACK_POINTER = Assembler::Reg::RSP;
    static constexpr auto REGISTER_ARRAY_BASE = Assembler::Reg::RBX;
    static constexpr auto LOCALS_ARRAY_BASE = Assembler::Reg::R14;
    static constexpr auto UNWIND_CONTEXT_BASE = Assembler::Reg::R15;
#    endif

    void compile_load_immediate(Bytecode::Op::LoadImmediate const&);
    void compile_load(Bytecode::Op::Load const&);
    void compile_store(Bytecode::Op::Store const&);
    void compile_get_local(Bytecode::Op::GetLocal const&);
    void compile_set_local(Bytecode::Op::SetLocal const&);
    void compile_typeof_local(Bytecode::Op::TypeofLocal const&);
    void compile_jump(Bytecode::Op::Jump const&);
    void compile_jump_conditional(Bytecode::Op::JumpConditional const&);
    void compile_jump_nullish(Bytecode::Op::JumpNullish const&);
    void compile_increment(Bytecode::Op::Increment const&);
    void compile_decrement(Bytecode::Op::Decrement const&);
    void compile_enter_unwind_context(Bytecode::Op::EnterUnwindContext const&);
    void compile_leave_unwind_context(Bytecode::Op::LeaveUnwindContext const&);
    void compile_throw(Bytecode::Op::Throw const&);
    void compile_create_lexical_environment(Bytecode::Op::CreateLexicalEnvironment const&);
    void compile_leave_lexical_environment(Bytecode::Op::LeaveLexicalEnvironment const&);
    void compile_to_numeric(Bytecode::Op::ToNumeric const&);
    void compile_resolve_this_binding(Bytecode::Op::ResolveThisBinding const&);

#    define JS_ENUMERATE_COMMON_BINARY_OPS_WITHOUT_FAST_PATH(O) \
        O(Sub, sub)                                             \
        O(Mul, mul)                                             \
        O(Div, div)                                             \
        O(Exp, exp)                                             \
        O(Mod, mod)                                             \
        O(In, in)                                               \
        O(InstanceOf, instance_of)                              \
        O(GreaterThan, greater_than)                            \
        O(GreaterThanEquals, greater_than_equals)               \
        O(LessThanEquals, less_than_equals)                     \
        O(LooselyInequals, abstract_inequals)                   \
        O(LooselyEquals, abstract_equals)                       \
        O(StrictlyInequals, typed_inequals)                     \
        O(StrictlyEquals, typed_equals)                         \
        O(BitwiseAnd, bitwise_and)                              \
        O(BitwiseOr, bitwise_or)                                \
        O(BitwiseXor, bitwise_xor)                              \
        O(LeftShift, left_shift)                                \
        O(RightShift, right_shift)                              \
        O(UnsignedRightShift, unsigned_right_shift)

#    define DO_COMPILE_COMMON_BINARY_OP(OpTitleCase, op_snake_case) \
        void compile_##op_snake_case(Bytecode::Op::OpTitleCase const&);

    JS_ENUMERATE_COMMON_BINARY_OPS_WITHOUT_FAST_PATH(DO_COMPILE_COMMON_BINARY_OP)
#    undef DO_COMPILE_COMMON_BINARY_OP

#    define DO_COMPILE_COMMON_UNARY_OP(OpTitleCase, op_snake_case) \
        void compile_##op_snake_case(Bytecode::Op::OpTitleCase const&);

    JS_ENUMERATE_COMMON_UNARY_OPS(DO_COMPILE_COMMON_UNARY_OP)
#    undef DO_COMPILE_COMMON_UNARY_OP

    void compile_add(Bytecode::Op::Add const&);
    void compile_less_than(Bytecode::Op::LessThan const&);

    void compile_return(Bytecode::Op::Return const&);
    void compile_new_string(Bytecode::Op::NewString const&);
    void compile_new_object(Bytecode::Op::NewObject const&);
    void compile_new_array(Bytecode::Op::NewArray const&);
    void compile_new_function(Bytecode::Op::NewFunction const&);
    void compile_new_regexp(Bytecode::Op::NewRegExp const&);
    void compile_new_bigint(Bytecode::Op::NewBigInt const&);
    void compile_new_class(Bytecode::Op::NewClass const&);

    void compile_create_variable(Bytecode::Op::CreateVariable const&);

    void compile_get_by_id(Bytecode::Op::GetById const&);
    void compile_get_by_value(Bytecode::Op::GetByValue const&);
    void compile_get_global(Bytecode::Op::GetGlobal const&);
    void compile_get_variable(Bytecode::Op::GetVariable const&);
    void compile_get_callee_and_this_from_environment(Bytecode::Op::GetCalleeAndThisFromEnvironment const&);

    void compile_put_by_id(Bytecode::Op::PutById const&);
    void compile_put_by_value(Bytecode::Op::PutByValue const&);

    void compile_call(Bytecode::Op::Call const&);
    void compile_call_with_argument_array(Bytecode::Op::CallWithArgumentArray const&);
    void compile_typeof_variable(Bytecode::Op::TypeofVariable const&);
    void compile_set_variable(Bytecode::Op::SetVariable const&);
    void compile_continue_pending_unwind(Bytecode::Op::ContinuePendingUnwind const&);
    void compile_concat_string(Bytecode::Op::ConcatString const&);

    void store_vm_register(Bytecode::Register, Assembler::Reg);
    void load_vm_register(Assembler::Reg, Bytecode::Register);

    void store_vm_local(size_t, Assembler::Reg);
    void load_vm_local(Assembler::Reg, size_t);

    void compile_to_boolean(Assembler::Reg dst, Assembler::Reg src);

    void check_exception();
    void handle_exception();

    void push_unwind_context(bool valid, Optional<Bytecode::Label> const& handler, Optional<Bytecode::Label> const& finalizer);
    void pop_unwind_context();

    void jump_to_exit();

    void native_call(void* function_address, Vector<Assembler::Operand> const& stack_arguments = {});

    template<typename Codegen>
    void branch_if_int32(Assembler::Reg, Codegen);

    template<typename Codegen>
    void branch_if_both_int32(Assembler::Reg, Assembler::Reg, Codegen);

    explicit Compiler(Bytecode::Executable& bytecode_executable)
        : m_bytecode_executable(bytecode_executable)
    {
    }

    Assembler::Label& label_for(Bytecode::BasicBlock const& block)
    {
        return block_data_for(block).label;
    }

    struct BasicBlockData {
        size_t start_offset { 0 };
        Assembler::Label label;
        Vector<size_t> absolute_references_to_here;
    };

    BasicBlockData& block_data_for(Bytecode::BasicBlock const& block)
    {
        return *m_basic_block_data.ensure(&block, [] {
            return make<BasicBlockData>();
        });
    }

    HashMap<Bytecode::BasicBlock const*, NonnullOwnPtr<BasicBlockData>> m_basic_block_data;

    Vector<u8> m_output;
    Assembler m_assembler { m_output };
    Assembler::Label m_exit_label;
    Assembler::Label m_exception_handler;
    Bytecode::Executable& m_bytecode_executable;
};

}

#else

namespace JS::JIT {
class Compiler {
public:
    static OwnPtr<NativeExecutable> compile(Bytecode::Executable&) { return nullptr; }
};
}

#endif
