/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Simon Wanner <simon@skyrising.xyz>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Platform.h>
#include <LibJIT/Assembler.h>
#include <LibJS/Bytecode/Builtins.h>
#include <LibJS/Bytecode/Executable.h>
#include <LibJS/Bytecode/Op.h>
#include <LibJS/JIT/NativeExecutable.h>

#ifdef JIT_ARCH_SUPPORTED

namespace JS::JIT {

using ::JIT::Assembler;

class Compiler {
public:
    static OwnPtr<NativeExecutable> compile(Bytecode::Executable&);

private:
#    if ARCH(X86_64)
    static constexpr auto GPR0 = Assembler::Reg::RAX;
    static constexpr auto GPR1 = Assembler::Reg::RCX;
    static constexpr auto GPR2 = Assembler::Reg::R13;
    static constexpr auto ARG0 = Assembler::Reg::RDI;
    static constexpr auto ARG1 = Assembler::Reg::RSI;
    static constexpr auto ARG2 = Assembler::Reg::RDX;
    static constexpr auto ARG3 = Assembler::Reg::RCX;
    static constexpr auto ARG4 = Assembler::Reg::R8;
    static constexpr auto ARG5 = Assembler::Reg::R9;
    static constexpr auto FPR0 = Assembler::Reg::XMM0;
    static constexpr auto FPR1 = Assembler::Reg::XMM1;
    static constexpr auto RET = Assembler::Reg::RAX;
    static constexpr auto STACK_POINTER = Assembler::Reg::RSP;
    static constexpr auto REGISTER_ARRAY_BASE = Assembler::Reg::RBX;
    static constexpr auto LOCALS_ARRAY_BASE = Assembler::Reg::R14;
    static constexpr auto CACHED_ACCUMULATOR = Assembler::Reg::R12;
    static constexpr auto RUNNING_EXECUTION_CONTEXT_BASE = Assembler::Reg::R15;
#    endif

    static Assembler::Reg argument_register(u32);

#    define JS_ENUMERATE_COMMON_BINARY_OPS_WITHOUT_FAST_PATH(O) \
        O(Div, div)                                             \
        O(Exp, exp)                                             \
        O(Mod, mod)                                             \
        O(In, in)                                               \
        O(InstanceOf, instance_of)

#    define JS_ENUMERATE_COMMON_UNARY_OPS_WITHOUT_FAST_PATH(O) \
        O(BitwiseNot, bitwise_not)                             \
        O(Not, not_)                                           \
        O(UnaryPlus, unary_plus)                               \
        O(Typeof, typeof_)

#    define JS_ENUMERATE_COMPARISON_OPS(O)                                         \
        O(LessThan, less_than, SignedLessThan, Below)                              \
        O(LessThanEquals, less_than_equals, SignedLessThanOrEqualTo, BelowOrEqual) \
        O(GreaterThan, greater_than, SignedGreaterThan, Above)                     \
        O(GreaterThanEquals, greater_than_equals, SignedGreaterThanOrEqualTo, AboveOrEqual)

#    define JS_ENUMERATE_NEW_BUILTIN_ERROR_BYTECODE_OPS(O) \
        O(NewTypeError, new_type_error, TypeError)

#    define JS_ENUMERATE_IMPLEMENTED_JIT_OPS(O)                                  \
        JS_ENUMERATE_COMMON_BINARY_OPS(O)                                        \
        JS_ENUMERATE_COMMON_UNARY_OPS(O)                                         \
        JS_ENUMERATE_NEW_BUILTIN_ERROR_BYTECODE_OPS(O)                           \
        O(LoadImmediate, load_immediate)                                         \
        O(Load, load)                                                            \
        O(Store, store)                                                          \
        O(GetLocal, get_local)                                                   \
        O(SetLocal, set_local)                                                   \
        O(TypeofLocal, typeof_local)                                             \
        O(Jump, jump)                                                            \
        O(JumpConditional, jump_conditional)                                     \
        O(JumpNullish, jump_nullish)                                             \
        O(JumpUndefined, jump_undefined)                                         \
        O(Increment, increment)                                                  \
        O(Decrement, decrement)                                                  \
        O(EnterUnwindContext, enter_unwind_context)                              \
        O(LeaveUnwindContext, leave_unwind_context)                              \
        O(Throw, throw)                                                          \
        O(Catch, catch)                                                          \
        O(CreateLexicalEnvironment, create_lexical_environment)                  \
        O(LeaveLexicalEnvironment, leave_lexical_environment)                    \
        O(EnterObjectEnvironment, enter_object_environment)                      \
        O(ToNumeric, to_numeric)                                                 \
        O(ResolveThisBinding, resolve_this_binding)                              \
        O(Return, return)                                                        \
        O(NewString, new_string)                                                 \
        O(NewObject, new_object)                                                 \
        O(NewArray, new_array)                                                   \
        O(NewPrimitiveArray, new_primitive_array)                                \
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
        O(GetObjectFromIteratorRecord, get_object_from_iterator_record)          \
        O(GetNextMethodFromIteratorRecord, get_next_method_from_iterator_record) \
        O(IteratorNext, iterator_next)                                           \
        O(ThrowIfNotObject, throw_if_not_object)                                 \
        O(ThrowIfNullish, throw_if_nullish)                                      \
        O(IteratorClose, iterator_close)                                         \
        O(IteratorToArray, iterator_to_array)                                    \
        O(Append, append)                                                        \
        O(DeleteById, delete_by_id)                                              \
        O(DeleteByValue, delete_by_value)                                        \
        O(DeleteByValueWithThis, delete_by_value_with_this)                      \
        O(GetObjectPropertyIterator, get_object_property_iterator)               \
        O(GetPrivateById, get_private_by_id)                                     \
        O(ResolveSuperBase, resolve_super_base)                                  \
        O(GetByIdWithThis, get_by_id_with_this)                                  \
        O(GetByValueWithThis, get_by_value_with_this)                            \
        O(DeleteByIdWithThis, delete_by_id_with_this)                            \
        O(PutByIdWithThis, put_by_id_with_this)                                  \
        O(PutPrivateById, put_private_by_id)                                     \
        O(ImportCall, import_call)                                               \
        O(GetImportMeta, get_import_meta)                                        \
        O(DeleteVariable, delete_variable)                                       \
        O(GetMethod, get_method)                                                 \
        O(GetNewTarget, get_new_target)                                          \
        O(HasPrivateId, has_private_id)                                          \
        O(PutByValueWithThis, put_by_value_with_this)                            \
        O(CopyObjectExcludingProperties, copy_object_excluding_properties)       \
        O(AsyncIteratorClose, async_iterator_close)                              \
        O(Yield, yield)                                                          \
        O(Await, await)

#    define DECLARE_COMPILE_OP(OpTitleCase, op_snake_case, ...) \
        void compile_##op_snake_case(Bytecode::Op::OpTitleCase const&);

    JS_ENUMERATE_IMPLEMENTED_JIT_OPS(DECLARE_COMPILE_OP)
#    undef DECLARE_COMPILE_OP

    void compile_builtin(Bytecode::Builtin, Assembler::Label& slow_case, Assembler::Label& end);
#    define DECLARE_COMPILE_BUILTIN(name, snake_case_name, ...) \
        void compile_builtin_##snake_case_name(Assembler::Label& slow_case, Assembler::Label& end);
    JS_ENUMERATE_BUILTINS(DECLARE_COMPILE_BUILTIN)
#    undef DECLARE_COMPILE_BUILTIN

    void store_vm_register(Bytecode::Register, Assembler::Reg);
    void load_vm_register(Assembler::Reg, Bytecode::Register);

    void store_vm_local(size_t, Assembler::Reg);
    void load_vm_local(Assembler::Reg, size_t);

    void reload_cached_accumulator();
    void flush_cached_accumulator();
    void load_accumulator(Assembler::Reg);
    void store_accumulator(Assembler::Reg);

    void compile_continuation(Optional<Bytecode::Label>, bool is_await);

    template<typename Codegen>
    void branch_if_same_type_for_equality(Assembler::Reg, Assembler::Reg, Codegen);
    void compile_is_strictly_equal(Assembler::Reg, Assembler::Reg, Assembler::Label& slow_case);

    void check_exception();
    void handle_exception();

    void jump_to_exit();

    void native_call(void* function_address, Vector<Assembler::Operand> const& stack_arguments = {});

    void jump_if_int32(Assembler::Reg, Assembler::Label&);

    template<typename Codegen>
    void branch_if_type(Assembler::Reg, u16 type_tag, Codegen);

    template<typename Codegen>
    void branch_if_int32(Assembler::Reg reg, Codegen codegen)
    {
        branch_if_type(reg, INT32_TAG, codegen);
    }

    template<typename Codegen>
    void branch_if_boolean(Assembler::Reg reg, Codegen codegen)
    {
        branch_if_type(reg, BOOLEAN_TAG, codegen);
    }

    template<typename Codegen>
    void branch_if_object(Assembler::Reg reg, Codegen codegen)
    {
        branch_if_type(reg, OBJECT_TAG, codegen);
    }

    void extract_object_pointer(Assembler::Reg dst_object, Assembler::Reg src_value);
    void convert_to_double(Assembler::Reg dst, Assembler::Reg src, Assembler::Reg nan, Assembler::Reg temp, Assembler::Label& not_number);

    template<typename Codegen>
    void branch_if_both_int32(Assembler::Reg, Assembler::Reg, Codegen);

    void jump_if_not_double(Assembler::Reg reg, Assembler::Reg nan, Assembler::Reg temp, Assembler::Label&);

    template<typename CodegenI32, typename CodegenDouble, typename CodegenValue>
    void compile_binary_op_fastpaths(Assembler::Reg lhs, Assembler::Reg rhs, CodegenI32, CodegenDouble, CodegenValue);
    template<typename CodegenI32, typename CodegenDouble, typename CodegenValue>
    void compiler_comparison_fastpaths(Assembler::Reg lhs, Assembler::Reg rhs, CodegenI32, CodegenDouble, CodegenValue);

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
    };

    BasicBlockData& block_data_for(Bytecode::BasicBlock const& block)
    {
        return *m_basic_block_data.ensure(&block, [] {
            return make<BasicBlockData>();
        });
    }

    void set_current_block(Bytecode::BasicBlock const& block)
    {
        m_current_block = &block;
    }

    Bytecode::BasicBlock const& current_block()
    {
        return *m_current_block;
    }

    HashMap<Bytecode::BasicBlock const*, NonnullOwnPtr<BasicBlockData>> m_basic_block_data;

    Vector<u8> m_output;
    Assembler m_assembler { m_output };
    Assembler::Label m_exit_label;
    Bytecode::Executable& m_bytecode_executable;
    Bytecode::BasicBlock const* m_current_block;
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
