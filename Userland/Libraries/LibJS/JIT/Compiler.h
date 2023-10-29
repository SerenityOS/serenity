/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Platform.h>

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

#    define JS_ENUMERATE_IMPLEMENTED_JIT_OPS(O)                                  \
        JS_ENUMERATE_COMMON_BINARY_OPS(O)                                        \
        JS_ENUMERATE_COMMON_UNARY_OPS(O)                                         \
        O(LoadImmediate, load_immediate)                                         \
        O(Load, load)                                                            \
        O(Store, store)                                                          \
        O(GetLocal, get_local)                                                   \
        O(SetLocal, set_local)                                                   \
        O(TypeofLocal, typeof_local)                                             \
        O(Jump, jump)                                                            \
        O(JumpConditional, jump_conditional)                                     \
        O(JumpNullish, jump_nullish)                                             \
        O(Increment, increment)                                                  \
        O(Decrement, decrement)                                                  \
        O(EnterUnwindContext, enter_unwind_context)                              \
        O(LeaveUnwindContext, leave_unwind_context)                              \
        O(Throw, throw)                                                          \
        O(CreateLexicalEnvironment, create_lexical_environment)                  \
        O(LeaveLexicalEnvironment, leave_lexical_environment)                    \
        O(ToNumeric, to_numeric)                                                 \
        O(ResolveThisBinding, resolve_this_binding)                              \
        O(Return, return)                                                        \
        O(NewString, new_string)                                                 \
        O(NewObject, new_object)                                                 \
        O(NewArray, new_array)                                                   \
        O(NewFunction, new_function)                                             \
        O(NewRegExp, new_regexp)                                                 \
        O(NewBigInt, new_bigint)                                                 \
        O(NewClass, new_class)                                                   \
        O(CreateVariable, create_variable)                                       \
        O(GetById, get_by_id)                                                    \
        O(GetByValue, get_by_value)                                              \
        O(GetGlobal, get_global)                                                 \
        O(GetVariable, get_variable)                                             \
        O(GetCalleeAndThisFromEnvironment, get_callee_and_this_from_environment) \
        O(PutById, put_by_id)                                                    \
        O(PutByValue, put_by_value)                                              \
        O(Call, call)                                                            \
        O(CallWithArgumentArray, call_with_argument_array)                       \
        O(TypeofVariable, typeof_variable)                                       \
        O(SetVariable, set_variable)                                             \
        O(ContinuePendingUnwind, continue_pending_unwind)                        \
        O(ConcatString, concat_string)                                           \
        O(BlockDeclarationInstantiation, block_declaration_instantiation)        \
        O(SuperCallWithArgumentArray, super_call_with_argument_array)            \
        O(GetIterator, get_iterator)                                             \
        O(IteratorNext, iterator_next)                                           \
        O(IteratorResultDone, iterator_result_done)                              \
        O(ThrowIfNotObject, throw_if_not_object)                                 \
        O(ThrowIfNullish, throw_if_nullish)                                      \
        O(IteratorResultValue, iterator_result_value)                            \
        O(IteratorClose, iterator_close)                                         \
        O(IteratorToArray, iterator_to_array)                                    \
        O(Append, append)                                                        \
        O(DeleteById, delete_by_id)

#    define DECLARE_COMPILE_OP(OpTitleCase, op_snake_case) \
        void compile_##op_snake_case(Bytecode::Op::OpTitleCase const&);

    JS_ENUMERATE_IMPLEMENTED_JIT_OPS(DECLARE_COMPILE_OP)
#    undef DECLARE_COMPILE_OP

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
