/*
 * Copyright (c) 2021-2024, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/HashTable.h>
#include <AK/TemporaryChange.h>
#include <LibJS/AST.h>
#include <LibJS/Bytecode/BasicBlock.h>
#include <LibJS/Bytecode/CommonImplementations.h>
#include <LibJS/Bytecode/Generator.h>
#include <LibJS/Bytecode/Instruction.h>
#include <LibJS/Bytecode/Interpreter.h>
#include <LibJS/Bytecode/Label.h>
#include <LibJS/Bytecode/Op.h>
#include <LibJS/JIT/Compiler.h>
#include <LibJS/JIT/NativeExecutable.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/BigInt.h>
#include <LibJS/Runtime/DeclarativeEnvironment.h>
#include <LibJS/Runtime/ECMAScriptFunctionObject.h>
#include <LibJS/Runtime/Environment.h>
#include <LibJS/Runtime/FunctionEnvironment.h>
#include <LibJS/Runtime/GlobalEnvironment.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Iterator.h>
#include <LibJS/Runtime/MathObject.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/ObjectEnvironment.h>
#include <LibJS/Runtime/Realm.h>
#include <LibJS/Runtime/Reference.h>
#include <LibJS/Runtime/RegExpObject.h>
#include <LibJS/Runtime/Value.h>
#include <LibJS/Runtime/ValueInlines.h>
#include <LibJS/SourceTextModule.h>

namespace JS::Bytecode {

bool g_dump_bytecode = false;

NonnullOwnPtr<CallFrame> CallFrame::create(size_t register_count)
{
    size_t allocation_size = sizeof(CallFrame) + sizeof(Value) * register_count;
    auto* memory = malloc(allocation_size);
    VERIFY(memory);
    auto call_frame = adopt_own(*new (memory) CallFrame);
    call_frame->register_count = register_count;
    for (auto i = 0u; i < register_count; ++i)
        new (&call_frame->register_values[i]) Value();
    return call_frame;
}

Interpreter::Interpreter(VM& vm)
    : m_vm(vm)
{
}

Interpreter::~Interpreter()
{
}

void Interpreter::visit_edges(Cell::Visitor& visitor)
{
    for (auto& frame : m_call_frames) {
        frame.visit([&](auto& value) { value->visit_edges(visitor); });
    }
}

// 16.1.6 ScriptEvaluation ( scriptRecord ), https://tc39.es/ecma262/#sec-runtime-semantics-scriptevaluation
ThrowCompletionOr<Value> Interpreter::run(Script& script_record, JS::GCPtr<Environment> lexical_environment_override)
{
    auto& vm = this->vm();

    // 1. Let globalEnv be scriptRecord.[[Realm]].[[GlobalEnv]].
    auto& global_environment = script_record.realm().global_environment();

    // 2. Let scriptContext be a new ECMAScript code execution context.
    auto script_context = ExecutionContext::create(vm.heap());

    // 3. Set the Function of scriptContext to null.
    // NOTE: This was done during execution context construction.

    // 4. Set the Realm of scriptContext to scriptRecord.[[Realm]].
    script_context->realm = &script_record.realm();

    // 5. Set the ScriptOrModule of scriptContext to scriptRecord.
    script_context->script_or_module = NonnullGCPtr<Script>(script_record);

    // 6. Set the VariableEnvironment of scriptContext to globalEnv.
    script_context->variable_environment = &global_environment;

    // 7. Set the LexicalEnvironment of scriptContext to globalEnv.
    script_context->lexical_environment = &global_environment;

    // Non-standard: Override the lexical environment if requested.
    if (lexical_environment_override)
        script_context->lexical_environment = lexical_environment_override;

    // 8. Set the PrivateEnvironment of scriptContext to null.

    // NOTE: This isn't in the spec, but we require it.
    script_context->is_strict_mode = script_record.parse_node().is_strict_mode();

    // FIXME: 9. Suspend the currently running execution context.

    // 10. Push scriptContext onto the execution context stack; scriptContext is now the running execution context.
    TRY(vm.push_execution_context(*script_context, {}));

    // 11. Let script be scriptRecord.[[ECMAScriptCode]].
    auto& script = script_record.parse_node();

    // 12. Let result be Completion(GlobalDeclarationInstantiation(script, globalEnv)).
    auto instantiation_result = script.global_declaration_instantiation(vm, global_environment);
    Completion result = instantiation_result.is_throw_completion() ? instantiation_result.throw_completion() : normal_completion({});

    // 13. If result.[[Type]] is normal, then
    if (result.type() == Completion::Type::Normal) {
        auto executable_result = JS::Bytecode::Generator::generate(vm, script);

        if (executable_result.is_error()) {
            if (auto error_string = executable_result.error().to_string(); error_string.is_error())
                result = vm.template throw_completion<JS::InternalError>(vm.error_message(JS::VM::ErrorMessage::OutOfMemory));
            else if (error_string = String::formatted("TODO({})", error_string.value()); error_string.is_error())
                result = vm.template throw_completion<JS::InternalError>(vm.error_message(JS::VM::ErrorMessage::OutOfMemory));
            else
                result = JS::throw_completion(JS::InternalError::create(realm(), error_string.release_value()));
        } else {
            auto executable = executable_result.release_value();

            if (g_dump_bytecode)
                executable->dump();

            // a. Set result to the result of evaluating script.
            auto result_or_error = run_and_return_frame(*executable, nullptr);
            if (result_or_error.value.is_error())
                result = result_or_error.value.release_error();
            else
                result = result_or_error.frame->registers()[0];
        }
    }

    // 14. If result.[[Type]] is normal and result.[[Value]] is empty, then
    if (result.type() == Completion::Type::Normal && !result.value().has_value()) {
        // a. Set result to NormalCompletion(undefined).
        result = normal_completion(js_undefined());
    }

    // FIXME: 15. Suspend scriptContext and remove it from the execution context stack.
    vm.pop_execution_context();

    // 16. Assert: The execution context stack is not empty.
    VERIFY(!vm.execution_context_stack().is_empty());

    // FIXME: 17. Resume the context that is now on the top of the execution context stack as the running execution context.

    // At this point we may have already run any queued promise jobs via on_call_stack_emptied,
    // in which case this is a no-op.
    // FIXME: These three should be moved out of Interpreter::run and give the host an option to run these, as it's up to the host when these get run.
    //        https://tc39.es/ecma262/#sec-jobs for jobs and https://tc39.es/ecma262/#_ref_3508 for ClearKeptObjects
    //        finish_execution_generation is particularly an issue for LibWeb, as the HTML spec wants to run it specifically after performing a microtask checkpoint.
    //        The promise and registry cleanup queues don't cause LibWeb an issue, as LibWeb overrides the hooks that push onto these queues.
    vm.run_queued_promise_jobs();

    vm.run_queued_finalization_registry_cleanup_jobs();

    vm.finish_execution_generation();

    // 18. Return ? result.
    if (result.is_abrupt()) {
        VERIFY(result.type() == Completion::Type::Throw);
        return result.release_error();
    }

    VERIFY(result.value().has_value());
    return *result.value();
}

ThrowCompletionOr<Value> Interpreter::run(SourceTextModule& module)
{
    // FIXME: This is not a entry point as defined in the spec, but is convenient.
    //        To avoid work we use link_and_eval_module however that can already be
    //        dangerous if the vm loaded other modules.
    auto& vm = this->vm();

    TRY(vm.link_and_eval_module(Badge<Bytecode::Interpreter> {}, module));

    vm.run_queued_promise_jobs();

    vm.run_queued_finalization_registry_cleanup_jobs();

    return js_undefined();
}

void Interpreter::run_bytecode()
{
    auto* locals = vm().running_execution_context().locals.data();
    auto* registers = this->registers().data();
    auto& accumulator = this->accumulator();
    for (;;) {
    start:
        auto pc = InstructionStreamIterator { m_current_block->instruction_stream(), m_current_executable };
        TemporaryChange temp_change { m_pc, Optional<InstructionStreamIterator&>(pc) };

        bool will_return = false;
        bool will_yield = false;

        ThrowCompletionOr<void> result;

        while (!pc.at_end()) {
            auto& instruction = *pc;

            switch (instruction.type()) {
            case Instruction::Type::GetLocal: {
                auto& local = locals[static_cast<Op::GetLocal const&>(instruction).index()];
                if (local.is_empty()) {
                    auto const& variable_name = vm().running_execution_context().function->local_variables_names()[static_cast<Op::GetLocal const&>(instruction).index()];
                    result = vm().throw_completion<ReferenceError>(ErrorType::BindingNotInitialized, variable_name);
                    break;
                }
                accumulator = local;
                break;
            }
            case Instruction::Type::SetLocal:
                locals[static_cast<Op::SetLocal const&>(instruction).index()] = accumulator;
                break;
            case Instruction::Type::Load:
                accumulator = registers[static_cast<Op::Load const&>(instruction).src().index()];
                break;
            case Instruction::Type::Store:
                registers[static_cast<Op::Store const&>(instruction).dst().index()] = accumulator;
                break;
            case Instruction::Type::LoadImmediate:
                accumulator = static_cast<Op::LoadImmediate const&>(instruction).value();
                break;
            case Instruction::Type::Jump:
                m_current_block = &static_cast<Op::Jump const&>(instruction).true_target()->block();
                goto start;
            case Instruction::Type::JumpConditional:
                if (accumulator.to_boolean())
                    m_current_block = &static_cast<Op::Jump const&>(instruction).true_target()->block();
                else
                    m_current_block = &static_cast<Op::Jump const&>(instruction).false_target()->block();
                goto start;
            case Instruction::Type::JumpNullish:
                if (accumulator.is_nullish())
                    m_current_block = &static_cast<Op::Jump const&>(instruction).true_target()->block();
                else
                    m_current_block = &static_cast<Op::Jump const&>(instruction).false_target()->block();
                goto start;
            case Instruction::Type::JumpUndefined:
                if (accumulator.is_undefined())
                    m_current_block = &static_cast<Op::Jump const&>(instruction).true_target()->block();
                else
                    m_current_block = &static_cast<Op::Jump const&>(instruction).false_target()->block();
                goto start;
            case Instruction::Type::EnterUnwindContext:
                enter_unwind_context();
                m_current_block = &static_cast<Op::EnterUnwindContext const&>(instruction).entry_point().block();
                goto start;
            case Instruction::Type::ContinuePendingUnwind: {
                if (auto exception = reg(Register::exception()); !exception.is_empty()) {
                    result = throw_completion(exception);
                    break;
                }
                if (!saved_return_value().is_empty()) {
                    do_return(saved_return_value());
                    break;
                }
                auto const* old_scheduled_jump = call_frame().previously_scheduled_jumps.take_last();
                if (m_scheduled_jump) {
                    // FIXME: If we `break` or `continue` in the finally, we need to clear
                    //        this field
                    //        Same goes for popping an old_scheduled_jump form the stack
                    m_current_block = exchange(m_scheduled_jump, nullptr);
                } else {
                    m_current_block = &static_cast<Op::ContinuePendingUnwind const&>(instruction).resume_target().block();
                    // set the scheduled jump to the old value if we continue
                    // where we left it
                    m_scheduled_jump = old_scheduled_jump;
                }
                goto start;
            }
            case Instruction::Type::ScheduleJump: {
                m_scheduled_jump = &static_cast<Op::ScheduleJump const&>(instruction).target().block();
                auto const* finalizer = m_current_block->finalizer();
                VERIFY(finalizer);
                m_current_block = finalizer;
                goto start;
            }
            default:
                result = instruction.execute(*this);
                break;
            }

            if (result.is_error()) [[unlikely]] {
                reg(Register::exception()) = *result.throw_completion().value();
                m_scheduled_jump = {};
                auto const* handler = m_current_block->handler();
                auto const* finalizer = m_current_block->finalizer();
                if (!handler && !finalizer)
                    return;

                auto& unwind_context = unwind_contexts().last();
                VERIFY(unwind_context.executable == m_current_executable);

                if (handler) {
                    m_current_block = handler;
                    goto start;
                }
                if (finalizer) {
                    m_current_block = finalizer;
                    // If an exception was thrown inside the corresponding `catch` block, we need to rethrow it
                    // from the `finally` block. But if the exception is from the `try` block, and has already been
                    // handled by `catch`, we swallow it.
                    if (!unwind_context.handler_called)
                        reg(Register::exception()) = {};
                    goto start;
                }
                // An unwind context with no handler or finalizer? We have nowhere to jump, and continuing on will make us crash on the next `Call` to a non-native function if there's an exception! So let's crash here instead.
                // If you run into this, you probably forgot to remove the current unwind_context somewhere.
                VERIFY_NOT_REACHED();
            }

            if (!reg(Register::return_value()).is_empty()) {
                will_return = true;
                // Note: A `yield` statement will not go through a finally statement,
                //       hence we need to set a flag to not do so,
                //       but we generate a Yield Operation in the case of returns in
                //       generators as well, so we need to check if it will actually
                //       continue or is a `return` in disguise
                will_yield = (instruction.type() == Instruction::Type::Yield && static_cast<Op::Yield const&>(instruction).continuation().has_value()) || instruction.type() == Instruction::Type::Await;
                break;
            }
            ++pc;
        }

        if (auto const* finalizer = m_current_block->finalizer(); finalizer && !will_yield) {
            auto& unwind_context = unwind_contexts().last();
            VERIFY(unwind_context.executable == m_current_executable);
            reg(Register::saved_return_value()) = reg(Register::return_value());
            reg(Register::return_value()) = {};
            m_current_block = finalizer;
            // the unwind_context will be pop'ed when entering the finally block
            continue;
        }

        if (pc.at_end())
            break;

        if (will_return)
            break;
    }
}

Interpreter::ValueAndFrame Interpreter::run_and_return_frame(Executable& executable, BasicBlock const* entry_point, CallFrame* in_frame)
{
    dbgln_if(JS_BYTECODE_DEBUG, "Bytecode::Interpreter will run unit {:p}", &executable);

    TemporaryChange restore_executable { m_current_executable, &executable };
    TemporaryChange restore_saved_jump { m_scheduled_jump, static_cast<BasicBlock const*>(nullptr) };

    VERIFY(!vm().execution_context_stack().is_empty());

    TemporaryChange restore_current_block { m_current_block, entry_point ?: executable.basic_blocks.first() };

    if (in_frame)
        push_call_frame(in_frame);
    else
        push_call_frame(CallFrame::create(executable.number_of_registers));

    vm().execution_context_stack().last()->executable = &executable;

    if (auto native_executable = executable.get_or_create_native_executable()) {
        auto block_index = 0;
        if (entry_point)
            block_index = executable.basic_blocks.find_first_index_if([&](auto const& block) { return block.ptr() == entry_point; }).value();
        native_executable->run(vm(), block_index);

#if 0
        for (size_t i = 0; i < vm().running_execution_context().local_variables.size(); ++i) {
            dbgln("%{}: {}", i, vm().running_execution_context().local_variables[i]);
        }
#endif

    } else {
        run_bytecode();
    }

    dbgln_if(JS_BYTECODE_DEBUG, "Bytecode::Interpreter did run unit {:p}", &executable);

    if constexpr (JS_BYTECODE_DEBUG) {
        for (size_t i = 0; i < registers().size(); ++i) {
            String value_string;
            if (registers()[i].is_empty())
                value_string = "(empty)"_string;
            else
                value_string = registers()[i].to_string_without_side_effects();
            dbgln("[{:3}] {}", i, value_string);
        }
    }

    auto return_value = js_undefined();
    if (!reg(Register::return_value()).is_empty())
        return_value = reg(Register::return_value());
    else if (!reg(Register::saved_return_value()).is_empty())
        return_value = reg(Register::saved_return_value());
    auto exception = reg(Register::exception());

    auto frame = pop_call_frame();

    // NOTE: The return value from a called function is put into $0 in the caller context.
    if (!m_call_frames.is_empty())
        call_frame().registers()[0] = return_value;

    // At this point we may have already run any queued promise jobs via on_call_stack_emptied,
    // in which case this is a no-op.
    vm().run_queued_promise_jobs();

    vm().finish_execution_generation();

    if (!exception.is_empty()) {
        if (auto* call_frame = frame.get_pointer<NonnullOwnPtr<CallFrame>>())
            return { throw_completion(exception), move(*call_frame) };
        return { throw_completion(exception), nullptr };
    }

    if (auto* call_frame = frame.get_pointer<NonnullOwnPtr<CallFrame>>())
        return { return_value, move(*call_frame) };
    return { return_value, nullptr };
}

void Interpreter::enter_unwind_context()
{
    unwind_contexts().empend(
        m_current_executable,
        vm().running_execution_context().lexical_environment);
    call_frame().previously_scheduled_jumps.append(m_scheduled_jump);
    m_scheduled_jump = nullptr;
}

void Interpreter::leave_unwind_context()
{
    unwind_contexts().take_last();
}

void Interpreter::catch_exception()
{
    accumulator() = reg(Register::exception());
    reg(Register::exception()) = {};
    auto& context = unwind_contexts().last();
    VERIFY(!context.handler_called);
    VERIFY(context.executable == &current_executable());
    context.handler_called = true;
    vm().running_execution_context().lexical_environment = context.lexical_environment;
}

void Interpreter::enter_object_environment(Object& object)
{
    auto& old_environment = vm().running_execution_context().lexical_environment;
    saved_lexical_environment_stack().append(old_environment);
    vm().running_execution_context().lexical_environment = new_object_environment(object, true, old_environment);
}

ThrowCompletionOr<NonnullGCPtr<Bytecode::Executable>> compile(VM& vm, ASTNode const& node, FunctionKind kind, DeprecatedFlyString const& name)
{
    auto executable_result = Bytecode::Generator::generate(vm, node, kind);
    if (executable_result.is_error())
        return vm.throw_completion<InternalError>(ErrorType::NotImplemented, TRY_OR_THROW_OOM(vm, executable_result.error().to_string()));

    auto bytecode_executable = executable_result.release_value();
    bytecode_executable->name = name;

    if (Bytecode::g_dump_bytecode)
        bytecode_executable->dump();

    return bytecode_executable;
}

Realm& Interpreter::realm()
{
    return *m_vm.current_realm();
}

void Interpreter::push_call_frame(Variant<NonnullOwnPtr<CallFrame>, CallFrame*> frame)
{
    m_call_frames.append(move(frame));
    m_current_call_frame = this->call_frame().registers();
    reg(Register::return_value()) = {};
}

Variant<NonnullOwnPtr<CallFrame>, CallFrame*> Interpreter::pop_call_frame()
{
    auto frame = m_call_frames.take_last();
    m_current_call_frame = m_call_frames.is_empty() ? Span<Value> {} : this->call_frame().registers();
    return frame;
}

}

namespace JS::Bytecode {

ByteString Instruction::to_byte_string(Bytecode::Executable const& executable) const
{
#define __BYTECODE_OP(op)       \
    case Instruction::Type::op: \
        return static_cast<Bytecode::Op::op const&>(*this).to_byte_string_impl(executable);

    switch (type()) {
        ENUMERATE_BYTECODE_OPS(__BYTECODE_OP)
    default:
        VERIFY_NOT_REACHED();
    }

#undef __BYTECODE_OP
}

}

namespace JS::Bytecode::Op {

ThrowCompletionOr<void> Load::execute_impl(Bytecode::Interpreter&) const
{
    // Handled in the interpreter loop.
    __builtin_unreachable();
}

ThrowCompletionOr<void> LoadImmediate::execute_impl(Bytecode::Interpreter&) const
{
    // Handled in the interpreter loop.
    __builtin_unreachable();
}

ThrowCompletionOr<void> Store::execute_impl(Bytecode::Interpreter&) const
{
    // Handled in the interpreter loop.
    __builtin_unreachable();
}

static ThrowCompletionOr<Value> loosely_inequals(VM& vm, Value src1, Value src2)
{
    return Value(!TRY(is_loosely_equal(vm, src1, src2)));
}

static ThrowCompletionOr<Value> loosely_equals(VM& vm, Value src1, Value src2)
{
    return Value(TRY(is_loosely_equal(vm, src1, src2)));
}

static ThrowCompletionOr<Value> strict_inequals(VM&, Value src1, Value src2)
{
    return Value(!is_strictly_equal(src1, src2));
}

static ThrowCompletionOr<Value> strict_equals(VM&, Value src1, Value src2)
{
    return Value(is_strictly_equal(src1, src2));
}

#define JS_DEFINE_COMMON_BINARY_OP(OpTitleCase, op_snake_case)                                  \
    ThrowCompletionOr<void> OpTitleCase::execute_impl(Bytecode::Interpreter& interpreter) const \
    {                                                                                           \
        auto& vm = interpreter.vm();                                                            \
        auto lhs = interpreter.reg(m_lhs_reg);                                                  \
        auto rhs = interpreter.accumulator();                                                   \
        interpreter.accumulator() = TRY(op_snake_case(vm, lhs, rhs));                           \
        return {};                                                                              \
    }                                                                                           \
    ByteString OpTitleCase::to_byte_string_impl(Bytecode::Executable const&) const              \
    {                                                                                           \
        return ByteString::formatted(#OpTitleCase " {}", m_lhs_reg);                            \
    }

JS_ENUMERATE_COMMON_BINARY_OPS(JS_DEFINE_COMMON_BINARY_OP)

static ThrowCompletionOr<Value> not_(VM&, Value value)
{
    return Value(!value.to_boolean());
}

static ThrowCompletionOr<Value> typeof_(VM& vm, Value value)
{
    return PrimitiveString::create(vm, value.typeof());
}

#define JS_DEFINE_COMMON_UNARY_OP(OpTitleCase, op_snake_case)                                   \
    ThrowCompletionOr<void> OpTitleCase::execute_impl(Bytecode::Interpreter& interpreter) const \
    {                                                                                           \
        auto& vm = interpreter.vm();                                                            \
        interpreter.accumulator() = TRY(op_snake_case(vm, interpreter.accumulator()));          \
        return {};                                                                              \
    }                                                                                           \
    ByteString OpTitleCase::to_byte_string_impl(Bytecode::Executable const&) const              \
    {                                                                                           \
        return #OpTitleCase;                                                                    \
    }

JS_ENUMERATE_COMMON_UNARY_OPS(JS_DEFINE_COMMON_UNARY_OP)

ThrowCompletionOr<void> NewBigInt::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    interpreter.accumulator() = BigInt::create(vm, m_bigint);
    return {};
}

ThrowCompletionOr<void> NewArray::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto array = MUST(Array::create(interpreter.realm(), 0));
    for (size_t i = 0; i < m_element_count; i++) {
        auto& value = interpreter.reg(Register(m_elements[0].index() + i));
        array->indexed_properties().put(i, value, default_attributes);
    }
    interpreter.accumulator() = array;
    return {};
}

ThrowCompletionOr<void> NewPrimitiveArray::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto array = MUST(Array::create(interpreter.realm(), 0));
    for (size_t i = 0; i < m_values.size(); i++)
        array->indexed_properties().put(i, m_values[i], default_attributes);
    interpreter.accumulator() = array;
    return {};
}

ThrowCompletionOr<void> Append::execute_impl(Bytecode::Interpreter& interpreter) const
{
    return append(interpreter.vm(), interpreter.reg(m_lhs), interpreter.accumulator(), m_is_spread);
}

ThrowCompletionOr<void> ImportCall::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto specifier = interpreter.reg(m_specifier);
    auto options_value = interpreter.reg(m_options);
    interpreter.accumulator() = TRY(perform_import_call(vm, specifier, options_value));
    return {};
}

ThrowCompletionOr<void> IteratorToArray::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.accumulator() = TRY(iterator_to_array(interpreter.vm(), interpreter.accumulator()));
    return {};
}

ThrowCompletionOr<void> NewString::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.accumulator() = PrimitiveString::create(interpreter.vm(), interpreter.current_executable().get_string(m_string));
    return {};
}

ThrowCompletionOr<void> NewObject::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto& realm = *vm.current_realm();

    interpreter.accumulator() = Object::create(realm, realm.intrinsics().object_prototype());
    return {};
}

ThrowCompletionOr<void> NewRegExp::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.accumulator() = new_regexp(
        interpreter.vm(),
        interpreter.current_executable().regex_table->get(m_regex_index),
        interpreter.current_executable().get_string(m_source_index),
        interpreter.current_executable().get_string(m_flags_index));
    return {};
}

#define JS_DEFINE_NEW_BUILTIN_ERROR_OP(ErrorName)                                                                                    \
    ThrowCompletionOr<void> New##ErrorName::execute_impl(Bytecode::Interpreter& interpreter) const                                   \
    {                                                                                                                                \
        auto& vm = interpreter.vm();                                                                                                 \
        auto& realm = *vm.current_realm();                                                                                           \
        interpreter.accumulator() = ErrorName::create(realm, interpreter.current_executable().get_string(m_error_string));           \
        return {};                                                                                                                   \
    }                                                                                                                                \
    ByteString New##ErrorName::to_byte_string_impl(Bytecode::Executable const& executable) const                                     \
    {                                                                                                                                \
        return ByteString::formatted("New" #ErrorName " {} (\"{}\")", m_error_string, executable.string_table->get(m_error_string)); \
    }

JS_ENUMERATE_NEW_BUILTIN_ERROR_OPS(JS_DEFINE_NEW_BUILTIN_ERROR_OP)

ThrowCompletionOr<void> CopyObjectExcludingProperties::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto& realm = *vm.current_realm();

    auto from_object = interpreter.reg(m_from_object);

    auto to_object = Object::create(realm, realm.intrinsics().object_prototype());

    HashTable<PropertyKey> excluded_names;
    for (size_t i = 0; i < m_excluded_names_count; ++i) {
        excluded_names.set(TRY(interpreter.reg(m_excluded_names[i]).to_property_key(vm)));
    }

    TRY(to_object->copy_data_properties(vm, from_object, excluded_names));

    interpreter.accumulator() = to_object;
    return {};
}

ThrowCompletionOr<void> ConcatString::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto string = TRY(interpreter.accumulator().to_primitive_string(vm));
    interpreter.reg(m_lhs) = PrimitiveString::create(vm, interpreter.reg(m_lhs).as_string(), string);
    return {};
}

ThrowCompletionOr<void> GetVariable::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.accumulator() = TRY(get_variable(
        interpreter,
        interpreter.current_executable().get_identifier(m_identifier),
        interpreter.current_executable().environment_variable_caches[m_cache_index]));
    return {};
}

ThrowCompletionOr<void> GetCalleeAndThisFromEnvironment::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto callee_and_this = TRY(get_callee_and_this_from_environment(
        interpreter,
        interpreter.current_executable().get_identifier(m_identifier),
        interpreter.current_executable().environment_variable_caches[m_cache_index]));
    interpreter.reg(m_callee_reg) = callee_and_this.callee;
    interpreter.reg(m_this_reg) = callee_and_this.this_value;
    return {};
}

ThrowCompletionOr<void> GetGlobal::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.accumulator() = TRY(get_global(
        interpreter,
        interpreter.current_executable().get_identifier(m_identifier),
        interpreter.current_executable().global_variable_caches[m_cache_index]));
    return {};
}

ThrowCompletionOr<void> GetLocal::execute_impl(Bytecode::Interpreter&) const
{
    // Handled in the interpreter loop.
    __builtin_unreachable();
}

ThrowCompletionOr<void> DeleteVariable::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto const& string = interpreter.current_executable().get_identifier(m_identifier);
    auto reference = TRY(vm.resolve_binding(string));
    interpreter.accumulator() = Value(TRY(reference.delete_(vm)));
    return {};
}

ThrowCompletionOr<void> CreateLexicalEnvironment::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto make_and_swap_envs = [&](auto& old_environment) {
        GCPtr<Environment> environment = new_declarative_environment(*old_environment).ptr();
        swap(old_environment, environment);
        return environment;
    };
    interpreter.saved_lexical_environment_stack().append(make_and_swap_envs(interpreter.vm().running_execution_context().lexical_environment));
    return {};
}

ThrowCompletionOr<void> EnterObjectEnvironment::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto object = TRY(interpreter.accumulator().to_object(interpreter.vm()));
    interpreter.enter_object_environment(*object);
    return {};
}

ThrowCompletionOr<void> Catch::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.catch_exception();
    return {};
}

ThrowCompletionOr<void> CreateVariable::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto const& name = interpreter.current_executable().get_identifier(m_identifier);
    return create_variable(interpreter.vm(), name, m_mode, m_is_global, m_is_immutable, m_is_strict);
}

ThrowCompletionOr<void> SetVariable::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto const& name = interpreter.current_executable().get_identifier(m_identifier);
    TRY(set_variable(vm,
        name,
        interpreter.accumulator(),
        m_mode,
        m_initialization_mode,
        interpreter.current_executable().environment_variable_caches[m_cache_index]));
    return {};
}

ThrowCompletionOr<void> SetLocal::execute_impl(Bytecode::Interpreter&) const
{
    // Handled in the interpreter loop.
    __builtin_unreachable();
}

ThrowCompletionOr<void> GetById::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto base_value = interpreter.accumulator();
    auto& cache = interpreter.current_executable().property_lookup_caches[m_cache_index];
    interpreter.accumulator() = TRY(get_by_id(interpreter.vm(), interpreter.current_executable().get_identifier(m_property), base_value, base_value, cache));
    return {};
}

ThrowCompletionOr<void> GetByIdWithThis::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto base_value = interpreter.accumulator();
    auto this_value = interpreter.reg(m_this_value);
    auto& cache = interpreter.current_executable().property_lookup_caches[m_cache_index];
    interpreter.accumulator() = TRY(get_by_id(interpreter.vm(), interpreter.current_executable().get_identifier(m_property), base_value, this_value, cache));
    return {};
}

ThrowCompletionOr<void> GetPrivateById::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto const& name = interpreter.current_executable().get_identifier(m_property);
    auto base_value = interpreter.accumulator();
    auto private_reference = make_private_reference(vm, base_value, name);
    interpreter.accumulator() = TRY(private_reference.get_value(vm));
    return {};
}

ThrowCompletionOr<void> HasPrivateId::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();

    if (!interpreter.accumulator().is_object())
        return vm.throw_completion<TypeError>(ErrorType::InOperatorWithObject);

    auto private_environment = vm.running_execution_context().private_environment;
    VERIFY(private_environment);
    auto private_name = private_environment->resolve_private_identifier(interpreter.current_executable().get_identifier(m_property));
    interpreter.accumulator() = Value(interpreter.accumulator().as_object().private_element_find(private_name) != nullptr);
    return {};
}

ThrowCompletionOr<void> PutById::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    // NOTE: Get the value from the accumulator before side effects have a chance to overwrite it.
    auto value = interpreter.accumulator();
    auto base = interpreter.reg(m_base);
    PropertyKey name = interpreter.current_executable().get_identifier(m_property);
    auto& cache = interpreter.current_executable().property_lookup_caches[m_cache_index];
    TRY(put_by_property_key(vm, base, base, value, name, m_kind, &cache));
    interpreter.accumulator() = value;
    return {};
}

ThrowCompletionOr<void> PutByIdWithThis::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    // NOTE: Get the value from the accumulator before side effects have a chance to overwrite it.
    auto value = interpreter.accumulator();
    auto base = interpreter.reg(m_base);
    PropertyKey name = interpreter.current_executable().get_identifier(m_property);
    auto& cache = interpreter.current_executable().property_lookup_caches[m_cache_index];
    TRY(put_by_property_key(vm, base, interpreter.reg(m_this_value), value, name, m_kind, &cache));
    interpreter.accumulator() = value;
    return {};
}

ThrowCompletionOr<void> PutPrivateById::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    // NOTE: Get the value from the accumulator before side effects have a chance to overwrite it.
    auto value = interpreter.accumulator();
    auto object = TRY(interpreter.reg(m_base).to_object(vm));
    auto name = interpreter.current_executable().get_identifier(m_property);
    auto private_reference = make_private_reference(vm, object, name);
    TRY(private_reference.put_value(vm, value));
    interpreter.accumulator() = value;
    return {};
}

ThrowCompletionOr<void> DeleteById::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto base_value = interpreter.accumulator();
    interpreter.accumulator() = TRY(Bytecode::delete_by_id(interpreter, base_value, m_property));
    return {};
}

ThrowCompletionOr<void> DeleteByIdWithThis::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto base_value = interpreter.accumulator();
    auto const& identifier = interpreter.current_executable().get_identifier(m_property);
    bool strict = vm.in_strict_mode();
    auto reference = Reference { base_value, identifier, interpreter.reg(m_this_value), strict };
    interpreter.accumulator() = Value(TRY(reference.delete_(vm)));
    return {};
}

ThrowCompletionOr<void> Jump::execute_impl(Bytecode::Interpreter&) const
{
    // Handled in the interpreter loop.
    __builtin_unreachable();
}

ThrowCompletionOr<void> ResolveThisBinding::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& cached_this_value = interpreter.reg(Register::this_value());
    if (cached_this_value.is_empty()) {
        // OPTIMIZATION: Because the value of 'this' cannot be reassigned during a function execution, it's
        //               resolved once and then saved for subsequent use.
        auto& vm = interpreter.vm();
        cached_this_value = TRY(vm.resolve_this_binding());
    }
    interpreter.accumulator() = cached_this_value;
    return {};
}

// https://tc39.es/ecma262/#sec-makesuperpropertyreference
ThrowCompletionOr<void> ResolveSuperBase::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();

    // 1. Let env be GetThisEnvironment().
    auto& env = verify_cast<FunctionEnvironment>(*get_this_environment(vm));

    // 2. Assert: env.HasSuperBinding() is true.
    VERIFY(env.has_super_binding());

    // 3. Let baseValue be ? env.GetSuperBase().
    interpreter.accumulator() = TRY(env.get_super_base());

    return {};
}

ThrowCompletionOr<void> GetNewTarget::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.accumulator() = interpreter.vm().get_new_target();
    return {};
}

ThrowCompletionOr<void> GetImportMeta::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.accumulator() = interpreter.vm().get_import_meta();
    return {};
}

ThrowCompletionOr<void> JumpConditional::execute_impl(Bytecode::Interpreter&) const
{
    // Handled in the interpreter loop.
    __builtin_unreachable();
}

ThrowCompletionOr<void> JumpNullish::execute_impl(Bytecode::Interpreter&) const
{
    // Handled in the interpreter loop.
    __builtin_unreachable();
}

ThrowCompletionOr<void> JumpUndefined::execute_impl(Bytecode::Interpreter&) const
{
    // Handled in the interpreter loop.
    __builtin_unreachable();
}

static ThrowCompletionOr<Value> dispatch_builtin_call(Bytecode::Interpreter& interpreter, Bytecode::Builtin builtin, Register first_argument)
{
    switch (builtin) {
    case Builtin::MathAbs:
        return TRY(MathObject::abs_impl(interpreter.vm(), interpreter.reg(first_argument)));
    case Builtin::MathLog:
        return TRY(MathObject::log_impl(interpreter.vm(), interpreter.reg(first_argument)));
    case Builtin::MathPow: {
        auto exponent = interpreter.reg(Register { first_argument.index() + 1 });
        return TRY(MathObject::pow_impl(interpreter.vm(), interpreter.reg(first_argument), exponent));
    }
    case Builtin::MathExp:
        return TRY(MathObject::exp_impl(interpreter.vm(), interpreter.reg(first_argument)));
    case Builtin::MathCeil:
        return TRY(MathObject::ceil_impl(interpreter.vm(), interpreter.reg(first_argument)));
    case Builtin::MathFloor:
        return TRY(MathObject::floor_impl(interpreter.vm(), interpreter.reg(first_argument)));
    case Builtin::MathRound:
        return TRY(MathObject::round_impl(interpreter.vm(), interpreter.reg(first_argument)));
    case Builtin::MathSqrt:
        return TRY(MathObject::sqrt_impl(interpreter.vm(), interpreter.reg(first_argument)));
    case Bytecode::Builtin::__Count:
        VERIFY_NOT_REACHED();
    }
    VERIFY_NOT_REACHED();
}

ThrowCompletionOr<void> Call::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto callee = interpreter.reg(m_callee);

    TRY(throw_if_needed_for_call(interpreter, callee, call_type(), expression_string()));

    if (m_builtin.has_value() && m_argument_count == Bytecode::builtin_argument_count(m_builtin.value()) && interpreter.realm().get_builtin_value(m_builtin.value()) == callee) {
        interpreter.accumulator() = TRY(dispatch_builtin_call(interpreter, m_builtin.value(), m_first_argument));
        return {};
    }

    interpreter.accumulator() = TRY(perform_call(interpreter, interpreter.reg(m_this_value), call_type(), callee, interpreter.registers().slice(m_first_argument.index(), m_argument_count)));
    return {};
}

ThrowCompletionOr<void> CallWithArgumentArray::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto callee = interpreter.reg(m_callee);
    TRY(throw_if_needed_for_call(interpreter, callee, call_type(), expression_string()));
    auto argument_values = argument_list_evaluation(interpreter.vm(), interpreter.accumulator());
    interpreter.accumulator() = TRY(perform_call(interpreter, interpreter.reg(m_this_value), call_type(), callee, move(argument_values)));
    return {};
}

// 13.3.7.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-super-keyword-runtime-semantics-evaluation
ThrowCompletionOr<void> SuperCallWithArgumentArray::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.accumulator() = TRY(super_call_with_argument_array(interpreter.vm(), interpreter.accumulator(), m_is_synthetic));
    return {};
}

ThrowCompletionOr<void> NewFunction::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    interpreter.accumulator() = new_function(vm, m_function_node, m_lhs_name, m_home_object);
    return {};
}

ThrowCompletionOr<void> Return::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.do_return(interpreter.accumulator().value_or(js_undefined()));
    return {};
}

ThrowCompletionOr<void> Increment::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto old_value = interpreter.accumulator();

    // OPTIMIZATION: Fast path for Int32 values.
    if (old_value.is_int32()) {
        auto integer_value = old_value.as_i32();
        if (integer_value != NumericLimits<i32>::max()) [[likely]] {
            interpreter.accumulator() = Value { integer_value + 1 };
            return {};
        }
    }

    old_value = TRY(old_value.to_numeric(vm));

    if (old_value.is_number())
        interpreter.accumulator() = Value(old_value.as_double() + 1);
    else
        interpreter.accumulator() = BigInt::create(vm, old_value.as_bigint().big_integer().plus(Crypto::SignedBigInteger { 1 }));
    return {};
}

ThrowCompletionOr<void> Decrement::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto old_value = TRY(interpreter.accumulator().to_numeric(vm));

    if (old_value.is_number())
        interpreter.accumulator() = Value(old_value.as_double() - 1);
    else
        interpreter.accumulator() = BigInt::create(vm, old_value.as_bigint().big_integer().minus(Crypto::SignedBigInteger { 1 }));
    return {};
}

ThrowCompletionOr<void> Throw::execute_impl(Bytecode::Interpreter& interpreter) const
{
    return throw_completion(interpreter.accumulator());
}

ThrowCompletionOr<void> ThrowIfNotObject::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    if (!interpreter.accumulator().is_object())
        return vm.throw_completion<TypeError>(ErrorType::NotAnObject, interpreter.accumulator().to_string_without_side_effects());
    return {};
}

ThrowCompletionOr<void> ThrowIfNullish::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto value = interpreter.accumulator();
    if (value.is_nullish())
        return vm.throw_completion<TypeError>(ErrorType::NotObjectCoercible, value.to_string_without_side_effects());
    return {};
}

ThrowCompletionOr<void> EnterUnwindContext::execute_impl(Bytecode::Interpreter&) const
{
    // Handled in the interpreter loop.
    __builtin_unreachable();
}

ThrowCompletionOr<void> ScheduleJump::execute_impl(Bytecode::Interpreter&) const
{
    // Handled in the interpreter loop.
    __builtin_unreachable();
}

ThrowCompletionOr<void> LeaveLexicalEnvironment::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.vm().running_execution_context().lexical_environment = interpreter.saved_lexical_environment_stack().take_last();
    return {};
}

ThrowCompletionOr<void> LeaveUnwindContext::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.leave_unwind_context();
    return {};
}

ThrowCompletionOr<void> ContinuePendingUnwind::execute_impl(Bytecode::Interpreter&) const
{
    // Handled in the interpreter loop.
    __builtin_unreachable();
}

ThrowCompletionOr<void> Yield::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto yielded_value = interpreter.accumulator().value_or(js_undefined());
    auto object = Object::create(interpreter.realm(), nullptr);
    object->define_direct_property("result", yielded_value, JS::default_attributes);

    if (m_continuation_label.has_value())
        // FIXME: If we get a pointer, which is not accurately representable as a double
        //        will cause this to explode
        object->define_direct_property("continuation", Value(static_cast<double>(reinterpret_cast<u64>(&m_continuation_label->block()))), JS::default_attributes);
    else
        object->define_direct_property("continuation", Value(0), JS::default_attributes);

    object->define_direct_property("isAwait", Value(false), JS::default_attributes);
    interpreter.do_return(object);
    return {};
}

ThrowCompletionOr<void> Await::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto yielded_value = interpreter.accumulator().value_or(js_undefined());
    auto object = Object::create(interpreter.realm(), nullptr);
    object->define_direct_property("result", yielded_value, JS::default_attributes);
    // FIXME: If we get a pointer, which is not accurately representable as a double
    //        will cause this to explode
    object->define_direct_property("continuation", Value(static_cast<double>(reinterpret_cast<u64>(&m_continuation_label.block()))), JS::default_attributes);
    object->define_direct_property("isAwait", Value(true), JS::default_attributes);
    interpreter.do_return(object);
    return {};
}

ThrowCompletionOr<void> GetByValue::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.accumulator() = TRY(get_by_value(interpreter.vm(), interpreter.reg(m_base), interpreter.accumulator()));
    return {};
}

ThrowCompletionOr<void> GetByValueWithThis::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();

    // NOTE: Get the property key from the accumulator before side effects have a chance to overwrite it.
    auto property_key_value = interpreter.accumulator();

    auto object = TRY(interpreter.reg(m_base).to_object(vm));

    auto property_key = TRY(property_key_value.to_property_key(vm));

    interpreter.accumulator() = TRY(object->internal_get(property_key, interpreter.reg(m_this_value)));
    return {};
}

ThrowCompletionOr<void> PutByValue::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();

    auto value = interpreter.accumulator();
    TRY(put_by_value(vm, interpreter.reg(m_base), interpreter.reg(m_property), interpreter.accumulator(), m_kind));
    interpreter.accumulator() = value;

    return {};
}

ThrowCompletionOr<void> PutByValueWithThis::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();

    // NOTE: Get the value from the accumulator before side effects have a chance to overwrite it.
    auto value = interpreter.accumulator();

    auto base = interpreter.reg(m_base);

    auto property_key = m_kind != PropertyKind::Spread ? TRY(interpreter.reg(m_property).to_property_key(vm)) : PropertyKey {};
    TRY(put_by_property_key(vm, base, interpreter.reg(m_this_value), value, property_key, m_kind));
    interpreter.accumulator() = value;
    return {};
}

ThrowCompletionOr<void> DeleteByValue::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto base_value = interpreter.reg(m_base);
    auto property_key_value = interpreter.accumulator();
    interpreter.accumulator() = TRY(delete_by_value(interpreter, base_value, property_key_value));

    return {};
}

ThrowCompletionOr<void> DeleteByValueWithThis::execute_impl(Bytecode::Interpreter& interpreter) const
{
    // NOTE: Get the property key from the accumulator before side effects have a chance to overwrite it.
    auto property_key_value = interpreter.accumulator();
    auto base_value = interpreter.reg(m_base);
    auto this_value = interpreter.reg(m_this_value);
    interpreter.accumulator() = TRY(delete_by_value_with_this(interpreter, base_value, property_key_value, this_value));

    return {};
}

ThrowCompletionOr<void> GetIterator::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    interpreter.accumulator() = TRY(get_iterator(vm, interpreter.accumulator(), m_hint));
    return {};
}

ThrowCompletionOr<void> GetObjectFromIteratorRecord::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& iterator_record = verify_cast<IteratorRecord>(interpreter.reg(m_iterator_record).as_object());
    interpreter.reg(m_object) = iterator_record.iterator;
    return {};
}

ThrowCompletionOr<void> GetNextMethodFromIteratorRecord::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& iterator_record = verify_cast<IteratorRecord>(interpreter.reg(m_iterator_record).as_object());
    interpreter.reg(m_next_method) = iterator_record.next_method;
    return {};
}

ThrowCompletionOr<void> GetMethod::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto identifier = interpreter.current_executable().get_identifier(m_property);
    auto method = TRY(interpreter.accumulator().get_method(vm, identifier));
    interpreter.accumulator() = method ?: js_undefined();
    return {};
}

ThrowCompletionOr<void> GetObjectPropertyIterator::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.accumulator() = TRY(get_object_property_iterator(interpreter.vm(), interpreter.accumulator()));
    return {};
}

ThrowCompletionOr<void> IteratorClose::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto& iterator = verify_cast<IteratorRecord>(interpreter.accumulator().as_object());

    // FIXME: Return the value of the resulting completion. (Note that m_completion_value can be empty!)
    TRY(iterator_close(vm, iterator, Completion { m_completion_type, m_completion_value, {} }));
    return {};
}

ThrowCompletionOr<void> AsyncIteratorClose::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto& iterator = verify_cast<IteratorRecord>(interpreter.accumulator().as_object());

    // FIXME: Return the value of the resulting completion. (Note that m_completion_value can be empty!)
    TRY(async_iterator_close(vm, iterator, Completion { m_completion_type, m_completion_value, {} }));
    return {};
}

ThrowCompletionOr<void> IteratorNext::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto& iterator = verify_cast<IteratorRecord>(interpreter.accumulator().as_object());

    interpreter.accumulator() = TRY(iterator_next(vm, iterator));
    return {};
}

ThrowCompletionOr<void> NewClass::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.accumulator() = TRY(new_class(interpreter.vm(), interpreter.accumulator(), m_class_expression, m_lhs_name));
    return {};
}

// 13.5.3.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-typeof-operator-runtime-semantics-evaluation
ThrowCompletionOr<void> TypeofVariable::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    interpreter.accumulator() = TRY(typeof_variable(vm, interpreter.current_executable().get_identifier(m_identifier)));
    return {};
}

ThrowCompletionOr<void> TypeofLocal::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto const& value = vm.running_execution_context().local(m_index);
    interpreter.accumulator() = PrimitiveString::create(vm, value.typeof());
    return {};
}

ThrowCompletionOr<void> ToNumeric::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.accumulator() = TRY(interpreter.accumulator().to_numeric(interpreter.vm()));
    return {};
}

ThrowCompletionOr<void> BlockDeclarationInstantiation::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto old_environment = vm.running_execution_context().lexical_environment;
    interpreter.saved_lexical_environment_stack().append(old_environment);
    vm.running_execution_context().lexical_environment = new_declarative_environment(*old_environment);
    m_scope_node.block_declaration_instantiation(vm, vm.running_execution_context().lexical_environment);
    return {};
}

ByteString Load::to_byte_string_impl(Bytecode::Executable const&) const
{
    return ByteString::formatted("Load {}", m_src);
}

ByteString LoadImmediate::to_byte_string_impl(Bytecode::Executable const&) const
{
    return ByteString::formatted("LoadImmediate {}", m_value);
}

ByteString Store::to_byte_string_impl(Bytecode::Executable const&) const
{
    return ByteString::formatted("Store {}", m_dst);
}

ByteString NewBigInt::to_byte_string_impl(Bytecode::Executable const&) const
{
    return ByteString::formatted("NewBigInt \"{}\"", m_bigint.to_base_deprecated(10));
}

ByteString NewArray::to_byte_string_impl(Bytecode::Executable const&) const
{
    StringBuilder builder;
    builder.append("NewArray"sv);
    if (m_element_count != 0) {
        builder.appendff(" [{}-{}]", m_elements[0], m_elements[1]);
    }
    return builder.to_byte_string();
}

ByteString NewPrimitiveArray::to_byte_string_impl(Bytecode::Executable const&) const
{
    return ByteString::formatted("NewPrimitiveArray {}"sv, m_values.span());
}

ByteString Append::to_byte_string_impl(Bytecode::Executable const&) const
{
    if (m_is_spread)
        return ByteString::formatted("Append lhs: **{}", m_lhs);
    return ByteString::formatted("Append lhs: {}", m_lhs);
}

ByteString IteratorToArray::to_byte_string_impl(Bytecode::Executable const&) const
{
    return "IteratorToArray";
}

ByteString NewString::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("NewString {} (\"{}\")", m_string, executable.string_table->get(m_string));
}

ByteString NewObject::to_byte_string_impl(Bytecode::Executable const&) const
{
    return "NewObject";
}

ByteString NewRegExp::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("NewRegExp source:{} (\"{}\") flags:{} (\"{}\")", m_source_index, executable.get_string(m_source_index), m_flags_index, executable.get_string(m_flags_index));
}

ByteString CopyObjectExcludingProperties::to_byte_string_impl(Bytecode::Executable const&) const
{
    StringBuilder builder;
    builder.appendff("CopyObjectExcludingProperties from:{}", m_from_object);
    if (m_excluded_names_count != 0) {
        builder.append(" excluding:["sv);
        builder.join(", "sv, ReadonlySpan<Register>(m_excluded_names, m_excluded_names_count));
        builder.append(']');
    }
    return builder.to_byte_string();
}

ByteString ConcatString::to_byte_string_impl(Bytecode::Executable const&) const
{
    return ByteString::formatted("ConcatString {}", m_lhs);
}

ByteString GetCalleeAndThisFromEnvironment::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("GetCalleeAndThisFromEnvironment {} -> callee: {}, this:{} ", executable.identifier_table->get(m_identifier), m_callee_reg, m_this_reg);
}

ByteString GetVariable::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("GetVariable {} ({})", m_identifier, executable.identifier_table->get(m_identifier));
}

ByteString GetGlobal::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("GetGlobal {} ({})", m_identifier, executable.identifier_table->get(m_identifier));
}

ByteString GetLocal::to_byte_string_impl(Bytecode::Executable const&) const
{
    return ByteString::formatted("GetLocal {}", m_index);
}

ByteString DeleteVariable::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("DeleteVariable {} ({})", m_identifier, executable.identifier_table->get(m_identifier));
}

ByteString CreateLexicalEnvironment::to_byte_string_impl(Bytecode::Executable const&) const
{
    return "CreateLexicalEnvironment"sv;
}

ByteString CreateVariable::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    auto mode_string = m_mode == EnvironmentMode::Lexical ? "Lexical" : "Variable";
    return ByteString::formatted("CreateVariable env:{} immutable:{} global:{} {} ({})", mode_string, m_is_immutable, m_is_global, m_identifier, executable.identifier_table->get(m_identifier));
}

ByteString EnterObjectEnvironment::to_byte_string_impl(Executable const&) const
{
    return ByteString::formatted("EnterObjectEnvironment");
}

ByteString SetVariable::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    auto initialization_mode_name = m_initialization_mode == InitializationMode::Initialize ? "Initialize" : "Set";
    auto mode_string = m_mode == EnvironmentMode::Lexical ? "Lexical" : "Variable";
    return ByteString::formatted("SetVariable env:{} init:{} {} ({})", mode_string, initialization_mode_name, m_identifier, executable.identifier_table->get(m_identifier));
}

ByteString SetLocal::to_byte_string_impl(Bytecode::Executable const&) const
{
    return ByteString::formatted("SetLocal {}", m_index);
}

static StringView property_kind_to_string(PropertyKind kind)
{
    switch (kind) {
    case PropertyKind::Getter:
        return "getter"sv;
    case PropertyKind::Setter:
        return "setter"sv;
    case PropertyKind::KeyValue:
        return "key-value"sv;
    case PropertyKind::DirectKeyValue:
        return "direct-key-value"sv;
    case PropertyKind::Spread:
        return "spread"sv;
    case PropertyKind::ProtoSetter:
        return "proto-setter"sv;
    }
    VERIFY_NOT_REACHED();
}

ByteString PutById::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    auto kind = property_kind_to_string(m_kind);
    return ByteString::formatted("PutById kind:{} base:{}, property:{} ({})", kind, m_base, m_property, executable.identifier_table->get(m_property));
}

ByteString PutByIdWithThis::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    auto kind = property_kind_to_string(m_kind);
    return ByteString::formatted("PutByIdWithThis kind:{} base:{}, property:{} ({}) this_value:{}", kind, m_base, m_property, executable.identifier_table->get(m_property), m_this_value);
}

ByteString PutPrivateById::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    auto kind = property_kind_to_string(m_kind);
    return ByteString::formatted("PutPrivateById kind:{} base:{}, property:{} ({})", kind, m_base, m_property, executable.identifier_table->get(m_property));
}

ByteString GetById::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("GetById {} ({})", m_property, executable.identifier_table->get(m_property));
}

ByteString GetByIdWithThis::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("GetByIdWithThis {} ({}) this_value:{}", m_property, executable.identifier_table->get(m_property), m_this_value);
}

ByteString GetPrivateById::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("GetPrivateById {} ({})", m_property, executable.identifier_table->get(m_property));
}

ByteString HasPrivateId::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("HasPrivateId {} ({})", m_property, executable.identifier_table->get(m_property));
}

ByteString DeleteById::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("DeleteById {} ({})", m_property, executable.identifier_table->get(m_property));
}

ByteString DeleteByIdWithThis::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("DeleteByIdWithThis {} ({}) this_value:{}", m_property, executable.identifier_table->get(m_property), m_this_value);
}

ByteString Jump::to_byte_string_impl(Bytecode::Executable const&) const
{
    if (m_true_target.has_value())
        return ByteString::formatted("Jump {}", *m_true_target);
    return ByteString::formatted("Jump <empty>");
}

ByteString JumpConditional::to_byte_string_impl(Bytecode::Executable const&) const
{
    auto true_string = m_true_target.has_value() ? ByteString::formatted("{}", *m_true_target) : "<empty>";
    auto false_string = m_false_target.has_value() ? ByteString::formatted("{}", *m_false_target) : "<empty>";
    return ByteString::formatted("JumpConditional true:{} false:{}", true_string, false_string);
}

ByteString JumpNullish::to_byte_string_impl(Bytecode::Executable const&) const
{
    auto true_string = m_true_target.has_value() ? ByteString::formatted("{}", *m_true_target) : "<empty>";
    auto false_string = m_false_target.has_value() ? ByteString::formatted("{}", *m_false_target) : "<empty>";
    return ByteString::formatted("JumpNullish null:{} nonnull:{}", true_string, false_string);
}

ByteString JumpUndefined::to_byte_string_impl(Bytecode::Executable const&) const
{
    auto true_string = m_true_target.has_value() ? ByteString::formatted("{}", *m_true_target) : "<empty>";
    auto false_string = m_false_target.has_value() ? ByteString::formatted("{}", *m_false_target) : "<empty>";
    return ByteString::formatted("JumpUndefined undefined:{} not undefined:{}", true_string, false_string);
}

static StringView call_type_to_string(CallType type)
{
    switch (type) {
    case CallType::Call:
        return ""sv;
    case CallType::Construct:
        return " (Construct)"sv;
    case CallType::DirectEval:
        return " (DirectEval)"sv;
    }
    VERIFY_NOT_REACHED();
}

ByteString Call::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    auto type = call_type_to_string(m_type);
    if (m_builtin.has_value())
        return ByteString::formatted("Call{} callee:{}, this:{}, first_arg:{} (builtin {})", type, m_callee, m_this_value, m_first_argument, m_builtin.value());
    if (m_expression_string.has_value())
        return ByteString::formatted("Call{} callee:{}, this:{}, first_arg:{} ({})", type, m_callee, m_this_value, m_first_argument, executable.get_string(m_expression_string.value()));
    return ByteString::formatted("Call{} callee:{}, this:{}, first_arg:{}", type, m_callee, m_first_argument, m_this_value);
}

ByteString CallWithArgumentArray::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    auto type = call_type_to_string(m_type);
    if (m_expression_string.has_value())
        return ByteString::formatted("CallWithArgumentArray{} callee:{}, this:{}, arguments:[...acc] ({})", type, m_callee, m_this_value, executable.get_string(m_expression_string.value()));
    return ByteString::formatted("CallWithArgumentArray{} callee:{}, this:{}, arguments:[...acc]", type, m_callee, m_this_value);
}

ByteString SuperCallWithArgumentArray::to_byte_string_impl(Bytecode::Executable const&) const
{
    return "SuperCallWithArgumentArray arguments:[...acc]"sv;
}

ByteString NewFunction::to_byte_string_impl(Bytecode::Executable const&) const
{
    StringBuilder builder;
    builder.append("NewFunction"sv);
    if (m_function_node.has_name())
        builder.appendff(" name:{}"sv, m_function_node.name());
    if (m_lhs_name.has_value())
        builder.appendff(" lhs_name:{}"sv, m_lhs_name.value());
    if (m_home_object.has_value())
        builder.appendff(" home_object:{}"sv, m_home_object.value());
    return builder.to_byte_string();
}

ByteString NewClass::to_byte_string_impl(Bytecode::Executable const&) const
{
    StringBuilder builder;
    auto name = m_class_expression.name();
    builder.appendff("NewClass '{}'"sv, name.is_null() ? ""sv : name);
    if (m_lhs_name.has_value())
        builder.appendff(" lhs_name:{}"sv, m_lhs_name.value());
    return builder.to_byte_string();
}

ByteString Return::to_byte_string_impl(Bytecode::Executable const&) const
{
    return "Return";
}

ByteString Increment::to_byte_string_impl(Bytecode::Executable const&) const
{
    return "Increment";
}

ByteString Decrement::to_byte_string_impl(Bytecode::Executable const&) const
{
    return "Decrement";
}

ByteString Throw::to_byte_string_impl(Bytecode::Executable const&) const
{
    return "Throw";
}

ByteString ThrowIfNotObject::to_byte_string_impl(Bytecode::Executable const&) const
{
    return "ThrowIfNotObject";
}

ByteString ThrowIfNullish::to_byte_string_impl(Bytecode::Executable const&) const
{
    return "ThrowIfNullish";
}

ByteString EnterUnwindContext::to_byte_string_impl(Bytecode::Executable const&) const
{
    return ByteString::formatted("EnterUnwindContext entry:{}", m_entry_point);
}

ByteString ScheduleJump::to_byte_string_impl(Bytecode::Executable const&) const
{
    return ByteString::formatted("ScheduleJump {}", m_target);
}

ByteString LeaveLexicalEnvironment::to_byte_string_impl(Bytecode::Executable const&) const
{
    return "LeaveLexicalEnvironment"sv;
}

ByteString LeaveUnwindContext::to_byte_string_impl(Bytecode::Executable const&) const
{
    return "LeaveUnwindContext";
}

ByteString ContinuePendingUnwind::to_byte_string_impl(Bytecode::Executable const&) const
{
    return ByteString::formatted("ContinuePendingUnwind resume:{}", m_resume_target);
}

ByteString Yield::to_byte_string_impl(Bytecode::Executable const&) const
{
    if (m_continuation_label.has_value())
        return ByteString::formatted("Yield continuation:@{}", m_continuation_label->block().name());
    return ByteString::formatted("Yield return");
}

ByteString Await::to_byte_string_impl(Bytecode::Executable const&) const
{
    return ByteString::formatted("Await continuation:@{}", m_continuation_label.block().name());
}

ByteString GetByValue::to_byte_string_impl(Bytecode::Executable const&) const
{
    return ByteString::formatted("GetByValue base:{}", m_base);
}

ByteString GetByValueWithThis::to_byte_string_impl(Bytecode::Executable const&) const
{
    return ByteString::formatted("GetByValueWithThis base:{} this_value:{}", m_base, m_this_value);
}

ByteString PutByValue::to_byte_string_impl(Bytecode::Executable const&) const
{
    auto kind = property_kind_to_string(m_kind);
    return ByteString::formatted("PutByValue kind:{} base:{}, property:{}", kind, m_base, m_property);
}

ByteString PutByValueWithThis::to_byte_string_impl(Bytecode::Executable const&) const
{
    auto kind = property_kind_to_string(m_kind);
    return ByteString::formatted("PutByValueWithThis kind:{} base:{}, property:{} this_value:{}", kind, m_base, m_property, m_this_value);
}

ByteString DeleteByValue::to_byte_string_impl(Bytecode::Executable const&) const
{
    return ByteString::formatted("DeleteByValue base:{}", m_base);
}

ByteString DeleteByValueWithThis::to_byte_string_impl(Bytecode::Executable const&) const
{
    return ByteString::formatted("DeleteByValueWithThis base:{} this_value:{}", m_base, m_this_value);
}

ByteString GetIterator::to_byte_string_impl(Executable const&) const
{
    auto hint = m_hint == IteratorHint::Sync ? "sync" : "async";
    return ByteString::formatted("GetIterator hint:{}", hint);
}

ByteString GetMethod::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("GetMethod {} ({})", m_property, executable.identifier_table->get(m_property));
}

ByteString GetObjectPropertyIterator::to_byte_string_impl(Bytecode::Executable const&) const
{
    return "GetObjectPropertyIterator";
}

ByteString IteratorClose::to_byte_string_impl(Bytecode::Executable const&) const
{
    if (!m_completion_value.has_value())
        return ByteString::formatted("IteratorClose completion_type={} completion_value=<empty>", to_underlying(m_completion_type));

    auto completion_value_string = m_completion_value->to_string_without_side_effects();
    return ByteString::formatted("IteratorClose completion_type={} completion_value={}", to_underlying(m_completion_type), completion_value_string);
}

ByteString AsyncIteratorClose::to_byte_string_impl(Bytecode::Executable const&) const
{
    if (!m_completion_value.has_value())
        return ByteString::formatted("AsyncIteratorClose completion_type={} completion_value=<empty>", to_underlying(m_completion_type));

    auto completion_value_string = m_completion_value->to_string_without_side_effects();
    return ByteString::formatted("AsyncIteratorClose completion_type={} completion_value={}", to_underlying(m_completion_type), completion_value_string);
}

ByteString IteratorNext::to_byte_string_impl(Executable const&) const
{
    return "IteratorNext";
}

ByteString ResolveThisBinding::to_byte_string_impl(Bytecode::Executable const&) const
{
    return "ResolveThisBinding"sv;
}

ByteString ResolveSuperBase::to_byte_string_impl(Bytecode::Executable const&) const
{
    return "ResolveSuperBase"sv;
}

ByteString GetNewTarget::to_byte_string_impl(Bytecode::Executable const&) const
{
    return "GetNewTarget"sv;
}

ByteString GetImportMeta::to_byte_string_impl(Bytecode::Executable const&) const
{
    return "GetImportMeta"sv;
}

ByteString TypeofVariable::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("TypeofVariable {} ({})", m_identifier, executable.identifier_table->get(m_identifier));
}

ByteString TypeofLocal::to_byte_string_impl(Bytecode::Executable const&) const
{
    return ByteString::formatted("TypeofLocal {}", m_index);
}

ByteString ToNumeric::to_byte_string_impl(Bytecode::Executable const&) const
{
    return "ToNumeric"sv;
}

ByteString BlockDeclarationInstantiation::to_byte_string_impl(Bytecode::Executable const&) const
{
    return "BlockDeclarationInstantiation"sv;
}

ByteString ImportCall::to_byte_string_impl(Bytecode::Executable const&) const
{
    return ByteString::formatted("ImportCall specifier:{} options:{}"sv, m_specifier, m_options);
}

ByteString Catch::to_byte_string_impl(Bytecode::Executable const&) const
{
    return "Catch"sv;
}

ByteString GetObjectFromIteratorRecord::to_byte_string_impl(Bytecode::Executable const&) const
{
    return ByteString::formatted("GetObjectFromIteratorRecord object:{} <- iterator_record:{}", m_object, m_iterator_record);
}

ByteString GetNextMethodFromIteratorRecord::to_byte_string_impl(Bytecode::Executable const&) const
{
    return ByteString::formatted("GetNextMethodFromIteratorRecord next_method:{} <- iterator_record:{}", m_next_method, m_iterator_record);
}

}
