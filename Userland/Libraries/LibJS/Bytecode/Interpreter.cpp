/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
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
#include <LibJS/Bytecode/Op.h>
#include <LibJS/JIT/Compiler.h>
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
    ExecutionContext script_context(vm.heap());

    // 3. Set the Function of scriptContext to null.
    // NOTE: This was done during execution context construction.

    // 4. Set the Realm of scriptContext to scriptRecord.[[Realm]].
    script_context.realm = &script_record.realm();

    // 5. Set the ScriptOrModule of scriptContext to scriptRecord.
    script_context.script_or_module = NonnullGCPtr<Script>(script_record);

    // 6. Set the VariableEnvironment of scriptContext to globalEnv.
    script_context.variable_environment = &global_environment;

    // 7. Set the LexicalEnvironment of scriptContext to globalEnv.
    script_context.lexical_environment = &global_environment;

    // Non-standard: Override the lexical environment if requested.
    if (lexical_environment_override)
        script_context.lexical_environment = lexical_environment_override;

    // 8. Set the PrivateEnvironment of scriptContext to null.

    // NOTE: This isn't in the spec, but we require it.
    script_context.is_strict_mode = script_record.parse_node().is_strict_mode();

    // FIXME: 9. Suspend the currently running execution context.

    // 10. Push scriptContext onto the execution context stack; scriptContext is now the running execution context.
    TRY(vm.push_execution_context(script_context, {}));

    // 11. Let script be scriptRecord.[[ECMAScriptCode]].
    auto& script = script_record.parse_node();

    // 12. Let result be Completion(GlobalDeclarationInstantiation(script, globalEnv)).
    auto instantiation_result = script.global_declaration_instantiation(vm, global_environment);
    Completion result = instantiation_result.is_throw_completion() ? instantiation_result.throw_completion() : normal_completion({});

    // 13. If result.[[Type]] is normal, then
    if (result.type() == Completion::Type::Normal) {
        auto executable_result = JS::Bytecode::Generator::generate(script);

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
                result = result_or_error.frame->registers[0];
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
    auto* locals = vm().running_execution_context().local_variables.data();
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
                enter_unwind_context(
                    static_cast<Op::EnterUnwindContext const&>(instruction).handler_target(),
                    static_cast<Op::EnterUnwindContext const&>(instruction).finalizer_target());
                m_current_block = &static_cast<Op::EnterUnwindContext const&>(instruction).entry_point().block();
                goto start;
            case Instruction::Type::ContinuePendingUnwind:
                if (auto exception = reg(Register::exception()); !exception.is_empty()) {
                    result = throw_completion(exception);
                    break;
                }
                if (!saved_return_value().is_empty()) {
                    do_return(saved_return_value());
                    break;
                }
                if (m_scheduled_jump) {
                    // FIXME: If we `break` or `continue` in the finally, we need to clear
                    //        this field
                    m_current_block = exchange(m_scheduled_jump, nullptr);
                } else {
                    m_current_block = &static_cast<Op::ContinuePendingUnwind const&>(instruction).resume_target().block();
                }
                goto start;
            case Instruction::Type::ScheduleJump:
                m_scheduled_jump = &static_cast<Op::ScheduleJump const&>(instruction).target().block();
                m_current_block = unwind_contexts().last().finalizer;
                goto start;
            default:
                result = instruction.execute(*this);
                break;
            }

            if (result.is_error()) [[unlikely]] {
                reg(Register::exception()) = *result.throw_completion().value();
                if (unwind_contexts().is_empty())
                    return;
                auto& unwind_context = unwind_contexts().last();
                if (unwind_context.executable != m_current_executable)
                    return;
                if (unwind_context.handler && !unwind_context.handler_called) {
                    vm().running_execution_context().lexical_environment = unwind_context.lexical_environment;
                    m_current_block = unwind_context.handler;
                    unwind_context.handler_called = true;

                    accumulator = reg(Register::exception());
                    reg(Register::exception()) = {};
                    goto start;
                }
                if (unwind_context.finalizer) {
                    m_current_block = unwind_context.finalizer;
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

        if (!unwind_contexts().is_empty() && !will_yield) {
            auto& unwind_context = unwind_contexts().last();
            if (unwind_context.executable == m_current_executable && unwind_context.finalizer) {
                reg(Register::saved_return_value()) = reg(Register::return_value());
                reg(Register::return_value()) = {};
                m_current_block = unwind_context.finalizer;
                // the unwind_context will be pop'ed when entering the finally block
                continue;
            }
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
        push_call_frame(in_frame, executable.number_of_registers);
    else
        push_call_frame(make<CallFrame>(), executable.number_of_registers);

    if (auto native_executable = executable.get_or_create_native_executable()) {
        native_executable->run(vm());

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
        call_frame().registers[0] = return_value;

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

void Interpreter::enter_unwind_context(Optional<Label> handler_target, Optional<Label> finalizer_target)
{
    unwind_contexts().empend(
        m_current_executable,
        handler_target.has_value() ? &handler_target->block() : nullptr,
        finalizer_target.has_value() ? &finalizer_target->block() : nullptr,
        vm().running_execution_context().lexical_environment);
}

void Interpreter::leave_unwind_context()
{
    unwind_contexts().take_last();
}

ThrowCompletionOr<NonnullRefPtr<Bytecode::Executable>> compile(VM& vm, ASTNode const& node, FunctionKind kind, DeprecatedFlyString const& name)
{
    auto executable_result = Bytecode::Generator::generate(node, kind);
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

void Interpreter::push_call_frame(Variant<NonnullOwnPtr<CallFrame>, CallFrame*> frame, size_t register_count)
{
    m_call_frames.append(move(frame));
    this->call_frame().registers.resize(register_count);
    m_current_call_frame = this->call_frame().registers;
    reg(Register::return_value()) = {};
}

Variant<NonnullOwnPtr<CallFrame>, CallFrame*> Interpreter::pop_call_frame()
{
    auto frame = m_call_frames.take_last();
    m_current_call_frame = m_call_frames.is_empty() ? Span<Value> {} : this->call_frame().registers;
    return frame;
}

}

namespace JS::Bytecode {

DeprecatedString Instruction::to_deprecated_string(Bytecode::Executable const& executable) const
{
#define __BYTECODE_OP(op)       \
    case Instruction::Type::op: \
        return static_cast<Bytecode::Op::op const&>(*this).to_deprecated_string_impl(executable);

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

#define JS_DEFINE_COMMON_BINARY_OP(OpTitleCase, op_snake_case)                                  \
    ThrowCompletionOr<void> OpTitleCase::execute_impl(Bytecode::Interpreter& interpreter) const \
    {                                                                                           \
        auto& vm = interpreter.vm();                                                            \
        auto lhs = interpreter.reg(m_lhs_reg);                                                  \
        auto rhs = interpreter.accumulator();                                                   \
        interpreter.accumulator() = TRY(op_snake_case(vm, lhs, rhs));                           \
        return {};                                                                              \
    }                                                                                           \
    DeprecatedString OpTitleCase::to_deprecated_string_impl(Bytecode::Executable const&) const  \
    {                                                                                           \
        return DeprecatedString::formatted(#OpTitleCase " {}", m_lhs_reg);                      \
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
    DeprecatedString OpTitleCase::to_deprecated_string_impl(Bytecode::Executable const&) const  \
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

ThrowCompletionOr<void> Append::execute_impl(Bytecode::Interpreter& interpreter) const
{
    // Note: This OpCode is used to construct array literals and argument arrays for calls,
    //       containing at least one spread element,
    //       Iterating over such a spread element to unpack it has to be visible by
    //       the user courtesy of
    //       (1) https://tc39.es/ecma262/#sec-runtime-semantics-arrayaccumulation
    //          SpreadElement : ... AssignmentExpression
    //              1. Let spreadRef be ? Evaluation of AssignmentExpression.
    //              2. Let spreadObj be ? GetValue(spreadRef).
    //              3. Let iteratorRecord be ? GetIterator(spreadObj).
    //              4. Repeat,
    //                  a. Let next be ? IteratorStep(iteratorRecord).
    //                  b. If next is false, return nextIndex.
    //                  c. Let nextValue be ? IteratorValue(next).
    //                  d. Perform ! CreateDataPropertyOrThrow(array, ! ToString(𝔽(nextIndex)), nextValue).
    //                  e. Set nextIndex to nextIndex + 1.
    //       (2) https://tc39.es/ecma262/#sec-runtime-semantics-argumentlistevaluation
    //          ArgumentList : ... AssignmentExpression
    //              1. Let list be a new empty List.
    //              2. Let spreadRef be ? Evaluation of AssignmentExpression.
    //              3. Let spreadObj be ? GetValue(spreadRef).
    //              4. Let iteratorRecord be ? GetIterator(spreadObj).
    //              5. Repeat,
    //                  a. Let next be ? IteratorStep(iteratorRecord).
    //                  b. If next is false, return list.
    //                  c. Let nextArg be ? IteratorValue(next).
    //                  d. Append nextArg to list.
    //          ArgumentList : ArgumentList , ... AssignmentExpression
    //             1. Let precedingArgs be ? ArgumentListEvaluation of ArgumentList.
    //             2. Let spreadRef be ? Evaluation of AssignmentExpression.
    //             3. Let iteratorRecord be ? GetIterator(? GetValue(spreadRef)).
    //             4. Repeat,
    //                 a. Let next be ? IteratorStep(iteratorRecord).
    //                 b. If next is false, return precedingArgs.
    //                 c. Let nextArg be ? IteratorValue(next).
    //                 d. Append nextArg to precedingArgs.

    auto& vm = interpreter.vm();

    // Note: We know from codegen, that lhs is a plain array with only indexed properties
    auto& lhs = interpreter.reg(m_lhs).as_array();
    auto lhs_size = lhs.indexed_properties().array_like_size();

    auto rhs = interpreter.accumulator();

    if (m_is_spread) {
        // ...rhs
        size_t i = lhs_size;
        TRY(get_iterator_values(vm, rhs, [&i, &lhs](Value iterator_value) -> Optional<Completion> {
            lhs.indexed_properties().put(i, iterator_value, default_attributes);
            ++i;
            return {};
        }));
    } else {
        lhs.indexed_properties().put(lhs_size, rhs, default_attributes);
    }

    return {};
}

ThrowCompletionOr<void> ImportCall::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto specifier = interpreter.reg(m_specifier);
    auto options_value = interpreter.reg(m_options);
    interpreter.accumulator() = TRY(perform_import_call(vm, specifier, options_value));
    return {};
}

// FIXME: Since the accumulator is a Value, we store an object there and have to convert back and forth between that an Iterator records. Not great.
// Make sure to put this into the accumulator before the iterator object disappears from the stack to prevent the members from being GC'd.
static Object* iterator_to_object(VM& vm, IteratorRecord iterator)
{
    auto& realm = *vm.current_realm();
    auto object = Object::create(realm, nullptr);
    object->define_direct_property(vm.names.iterator, iterator.iterator, 0);
    object->define_direct_property(vm.names.next, iterator.next_method, 0);
    object->define_direct_property(vm.names.done, Value(iterator.done), 0);
    return object;
}

static IteratorRecord object_to_iterator(VM& vm, Object& object)
{
    return IteratorRecord {
        .iterator = &MUST(object.get(vm.names.iterator)).as_object(),
        .next_method = MUST(object.get(vm.names.next)),
        .done = MUST(object.get(vm.names.done)).as_bool()
    };
}

ThrowCompletionOr<void> IteratorToArray::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto iterator_object = TRY(interpreter.accumulator().to_object(vm));
    auto iterator = object_to_iterator(vm, iterator_object);

    auto array = MUST(Array::create(interpreter.realm(), 0));
    size_t index = 0;

    while (true) {
        auto iterator_result = TRY(iterator_next(vm, iterator));

        auto complete = TRY(iterator_complete(vm, iterator_result));

        if (complete) {
            interpreter.accumulator() = array;
            return {};
        }

        auto value = TRY(iterator_value(vm, iterator_result));

        MUST(array->create_data_property_or_throw(index, value));
        index++;
    }
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

// 13.2.7.3 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-regular-expression-literals-runtime-semantics-evaluation
ThrowCompletionOr<void> NewRegExp::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();

    auto& realm = *vm.current_realm();

    // 1. Let pattern be CodePointsToString(BodyText of RegularExpressionLiteral).
    auto pattern = interpreter.current_executable().get_string(m_source_index);

    // 2. Let flags be CodePointsToString(FlagText of RegularExpressionLiteral).
    auto flags = interpreter.current_executable().get_string(m_flags_index);

    // 3. Return ! RegExpCreate(pattern, flags).
    auto& parsed_regex = interpreter.current_executable().regex_table->get(m_regex_index);
    Regex<ECMA262> regex(parsed_regex.regex, parsed_regex.pattern, parsed_regex.flags);
    // NOTE: We bypass RegExpCreate and subsequently RegExpAlloc as an optimization to use the already parsed values.
    auto regexp_object = RegExpObject::create(realm, move(regex), move(pattern), move(flags));
    // RegExpAlloc has these two steps from the 'Legacy RegExp features' proposal.
    regexp_object->set_realm(*vm.current_realm());
    // We don't need to check 'If SameValue(newTarget, thisRealm.[[Intrinsics]].[[%RegExp%]]) is true'
    // here as we know RegExpCreate calls RegExpAlloc with %RegExp% for newTarget.
    regexp_object->set_legacy_features_enabled(true);
    interpreter.accumulator() = regexp_object;
    return {};
}

#define JS_DEFINE_NEW_BUILTIN_ERROR_OP(ErrorName)                                                                                          \
    ThrowCompletionOr<void> New##ErrorName::execute_impl(Bytecode::Interpreter& interpreter) const                                         \
    {                                                                                                                                      \
        auto& vm = interpreter.vm();                                                                                                       \
        auto& realm = *vm.current_realm();                                                                                                 \
        interpreter.accumulator() = ErrorName::create(realm, interpreter.current_executable().get_string(m_error_string));                 \
        return {};                                                                                                                         \
    }                                                                                                                                      \
    DeprecatedString New##ErrorName::to_deprecated_string_impl(Bytecode::Executable const& executable) const                               \
    {                                                                                                                                      \
        return DeprecatedString::formatted("New" #ErrorName " {} (\"{}\")", m_error_string, executable.string_table->get(m_error_string)); \
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
    auto& vm = interpreter.vm();

    auto& cached_environment_coordinate = interpreter.current_executable().environment_variable_caches[m_cache_index];
    if (cached_environment_coordinate.has_value()) {
        auto environment = vm.running_execution_context().lexical_environment;
        for (size_t i = 0; i < cached_environment_coordinate->hops; ++i)
            environment = environment->outer_environment();
        VERIFY(environment);
        VERIFY(environment->is_declarative_environment());
        if (!environment->is_permanently_screwed_by_eval()) {
            interpreter.accumulator() = TRY(verify_cast<DeclarativeEnvironment>(*environment).get_binding_value_direct(vm, cached_environment_coordinate.value().index, vm.in_strict_mode()));
            return {};
        }
        cached_environment_coordinate = {};
    }

    auto const& string = interpreter.current_executable().get_identifier(m_identifier);
    auto reference = TRY(vm.resolve_binding(string));
    if (reference.environment_coordinate().has_value())
        cached_environment_coordinate = reference.environment_coordinate();
    interpreter.accumulator() = TRY(reference.get_value(vm));
    return {};
}

ThrowCompletionOr<void> GetCalleeAndThisFromEnvironment::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();

    auto& cached_environment_coordinate = interpreter.current_executable().environment_variable_caches[m_cache_index];
    if (cached_environment_coordinate.has_value()) {
        auto environment = vm.running_execution_context().lexical_environment;
        for (size_t i = 0; i < cached_environment_coordinate->hops; ++i)
            environment = environment->outer_environment();
        VERIFY(environment);
        VERIFY(environment->is_declarative_environment());
        if (!environment->is_permanently_screwed_by_eval()) {
            interpreter.reg(m_callee_reg) = TRY(verify_cast<DeclarativeEnvironment>(*environment).get_binding_value_direct(vm, cached_environment_coordinate.value().index, vm.in_strict_mode()));
            Value this_value = js_undefined();
            if (auto base_object = environment->with_base_object())
                this_value = base_object;
            interpreter.reg(m_this_reg) = this_value;
            return {};
        }
        cached_environment_coordinate = {};
    }

    auto const& string = interpreter.current_executable().get_identifier(m_identifier);
    auto reference = TRY(vm.resolve_binding(string));
    if (reference.environment_coordinate().has_value())
        cached_environment_coordinate = reference.environment_coordinate();

    interpreter.reg(m_callee_reg) = TRY(reference.get_value(vm));

    Value this_value = js_undefined();
    if (reference.is_property_reference()) {
        this_value = reference.get_this_value();
    } else {
        if (reference.is_environment_reference()) {
            if (auto base_object = reference.base_environment().with_base_object(); base_object != nullptr)
                this_value = base_object;
        }
    }
    interpreter.reg(m_this_reg) = this_value;

    return {};
}

ThrowCompletionOr<void> GetGlobal::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.accumulator() = TRY(get_global(interpreter, m_identifier, m_cache_index));
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
    auto& vm = interpreter.vm();
    auto& old_environment = vm.running_execution_context().lexical_environment;
    interpreter.saved_lexical_environment_stack().append(old_environment);
    auto object = TRY(interpreter.accumulator().to_object(vm));
    vm.running_execution_context().lexical_environment = new_object_environment(object, true, old_environment);
    return {};
}

ThrowCompletionOr<void> CreateVariable::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto const& name = interpreter.current_executable().get_identifier(m_identifier);

    if (m_mode == EnvironmentMode::Lexical) {
        VERIFY(!m_is_global);

        // Note: This is papering over an issue where "FunctionDeclarationInstantiation" creates these bindings for us.
        //       Instead of crashing in there, we'll just raise an exception here.
        if (TRY(vm.lexical_environment()->has_binding(name)))
            return vm.throw_completion<InternalError>(TRY_OR_THROW_OOM(vm, String::formatted("Lexical environment already has binding '{}'", name)));

        if (m_is_immutable)
            return vm.lexical_environment()->create_immutable_binding(vm, name, m_is_strict);
        else
            return vm.lexical_environment()->create_mutable_binding(vm, name, m_is_strict);
    } else {
        if (!m_is_global) {
            if (m_is_immutable)
                return vm.variable_environment()->create_immutable_binding(vm, name, m_is_strict);
            else
                return vm.variable_environment()->create_mutable_binding(vm, name, m_is_strict);
        } else {
            // NOTE: CreateVariable with m_is_global set to true is expected to only be used in GlobalDeclarationInstantiation currently, which only uses "false" for "can_be_deleted".
            //       The only area that sets "can_be_deleted" to true is EvalDeclarationInstantiation, which is currently fully implemented in C++ and not in Bytecode.
            return verify_cast<GlobalEnvironment>(vm.variable_environment())->create_global_var_binding(name, false);
        }
    }
    return {};
}

ThrowCompletionOr<void> SetVariable::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto const& name = interpreter.current_executable().get_identifier(m_identifier);
    TRY(set_variable(vm, name, interpreter.accumulator(), m_mode, m_initialization_mode));
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
    interpreter.accumulator() = TRY(get_by_id(interpreter, m_property, base_value, base_value, m_cache_index));
    return {};
}

ThrowCompletionOr<void> GetByIdWithThis::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto base_value = interpreter.accumulator();
    auto this_value = interpreter.reg(m_this_value);
    interpreter.accumulator() = TRY(get_by_id(interpreter, m_property, base_value, this_value, m_cache_index));
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
    TRY(put_by_property_key(vm, base, base, value, name, m_kind));
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
    TRY(put_by_property_key(vm, base, interpreter.reg(m_this_value), value, name, m_kind));
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
    auto& vm = interpreter.vm();
    auto base_value = interpreter.accumulator();
    auto const& identifier = interpreter.current_executable().get_identifier(m_property);
    bool strict = vm.in_strict_mode();
    auto reference = Reference { base_value, identifier, {}, strict };
    interpreter.accumulator() = Value(TRY(reference.delete_(vm)));
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

// 13.3.8.1 https://tc39.es/ecma262/#sec-runtime-semantics-argumentlistevaluation
static MarkedVector<Value> argument_list_evaluation(Bytecode::Interpreter& interpreter)
{
    // Note: Any spreading and actual evaluation is handled in preceding opcodes
    // Note: The spec uses the concept of a list, while we create a temporary array
    //       in the preceding opcodes, so we have to convert in a manner that is not
    //       visible to the user
    auto& vm = interpreter.vm();

    MarkedVector<Value> argument_values { vm.heap() };
    auto arguments = interpreter.accumulator();

    auto& argument_array = arguments.as_array();
    auto array_length = argument_array.indexed_properties().array_like_size();

    argument_values.ensure_capacity(array_length);

    for (size_t i = 0; i < array_length; ++i) {
        if (auto maybe_value = argument_array.indexed_properties().get(i); maybe_value.has_value())
            argument_values.append(maybe_value.release_value().value);
        else
            argument_values.append(js_undefined());
    }

    return argument_values;
}

ThrowCompletionOr<void> Call::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto callee = interpreter.reg(m_callee);

    TRY(throw_if_needed_for_call(interpreter, callee, call_type(), expression_string()));

    MarkedVector<Value> argument_values(vm.heap());
    argument_values.ensure_capacity(m_argument_count);
    for (u32 i = 0; i < m_argument_count; ++i) {
        argument_values.unchecked_append(interpreter.reg(Register { m_first_argument.index() + i }));
    }
    interpreter.accumulator() = TRY(perform_call(interpreter, interpreter.reg(m_this_value), call_type(), callee, move(argument_values)));
    return {};
}

ThrowCompletionOr<void> CallWithArgumentArray::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto callee = interpreter.reg(m_callee);
    TRY(throw_if_needed_for_call(interpreter, callee, call_type(), expression_string()));
    auto argument_values = argument_list_evaluation(interpreter);
    interpreter.accumulator() = TRY(perform_call(interpreter, interpreter.reg(m_this_value), call_type(), callee, move(argument_values)));
    return {};
}

// 13.3.7.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-super-keyword-runtime-semantics-evaluation
ThrowCompletionOr<void> SuperCallWithArgumentArray::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    // 1. Let newTarget be GetNewTarget().
    auto new_target = vm.get_new_target();

    // 2. Assert: Type(newTarget) is Object.
    VERIFY(new_target.is_object());

    // 3. Let func be GetSuperConstructor().
    auto* func = get_super_constructor(vm);

    // 4. Let argList be ? ArgumentListEvaluation of Arguments.
    MarkedVector<Value> arg_list { vm.heap() };
    if (m_is_synthetic) {
        auto const& value = interpreter.accumulator();
        VERIFY(value.is_object() && is<Array>(value.as_object()));
        auto const& array_value = static_cast<Array const&>(value.as_object());
        auto length = MUST(length_of_array_like(vm, array_value));
        for (size_t i = 0; i < length; ++i)
            arg_list.append(array_value.get_without_side_effects(PropertyKey { i }));
    } else {
        arg_list = argument_list_evaluation(interpreter);
    }

    // 5. If IsConstructor(func) is false, throw a TypeError exception.
    if (!Value(func).is_constructor())
        return vm.throw_completion<TypeError>(ErrorType::NotAConstructor, "Super constructor");

    // 6. Let result be ? Construct(func, argList, newTarget).
    auto result = TRY(construct(vm, static_cast<FunctionObject&>(*func), move(arg_list), &new_target.as_function()));

    // 7. Let thisER be GetThisEnvironment().
    auto& this_environment = verify_cast<FunctionEnvironment>(*get_this_environment(vm));

    // 8. Perform ? thisER.BindThisValue(result).
    TRY(this_environment.bind_this_value(vm, result));

    // 9. Let F be thisER.[[FunctionObject]].
    auto& f = this_environment.function_object();

    // 10. Assert: F is an ECMAScript function object.
    // NOTE: This is implied by the strong C++ type.

    // 11. Perform ? InitializeInstanceElements(result, F).
    TRY(result->initialize_instance_elements(f));

    // 12. Return result.
    interpreter.accumulator() = result;
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
    auto old_value = TRY(interpreter.accumulator().to_numeric(vm));

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

ThrowCompletionOr<void> PushDeclarativeEnvironment::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto environment = interpreter.vm().heap().allocate_without_realm<DeclarativeEnvironment>(interpreter.vm().lexical_environment());
    interpreter.vm().running_execution_context().lexical_environment = environment;
    interpreter.vm().running_execution_context().variable_environment = environment;
    return {};
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
    interpreter.accumulator() = TRY(get_by_value(interpreter, interpreter.reg(m_base), interpreter.accumulator()));
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
    auto& vm = interpreter.vm();

    // NOTE: Get the property key from the accumulator before side effects have a chance to overwrite it.
    auto property_key_value = interpreter.accumulator();

    auto base_value = interpreter.reg(m_base);
    auto property_key = TRY(property_key_value.to_property_key(vm));
    bool strict = vm.in_strict_mode();
    auto reference = Reference { base_value, property_key, {}, strict };
    interpreter.accumulator() = Value(TRY(reference.delete_(vm)));
    return {};
}

ThrowCompletionOr<void> DeleteByValueWithThis::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();

    // NOTE: Get the property key from the accumulator before side effects have a chance to overwrite it.
    auto property_key_value = interpreter.accumulator();

    auto base_value = interpreter.reg(m_base);
    auto property_key = TRY(property_key_value.to_property_key(vm));
    bool strict = vm.in_strict_mode();
    auto reference = Reference { base_value, property_key, interpreter.reg(m_this_value), strict };
    interpreter.accumulator() = Value(TRY(reference.delete_(vm)));
    return {};
}

ThrowCompletionOr<void> GetIterator::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto iterator = TRY(get_iterator(vm, interpreter.accumulator(), m_hint));
    interpreter.accumulator() = iterator_to_object(vm, iterator);
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

// 14.7.5.9 EnumerateObjectProperties ( O ), https://tc39.es/ecma262/#sec-enumerate-object-properties
ThrowCompletionOr<void> GetObjectPropertyIterator::execute_impl(Bytecode::Interpreter& interpreter) const
{
    // While the spec does provide an algorithm, it allows us to implement it ourselves so long as we meet the following invariants:
    //    1- Returned property keys do not include keys that are Symbols
    //    2- Properties of the target object may be deleted during enumeration. A property that is deleted before it is processed by the iterator's next method is ignored
    //    3- If new properties are added to the target object during enumeration, the newly added properties are not guaranteed to be processed in the active enumeration
    //    4- A property name will be returned by the iterator's next method at most once in any enumeration.
    //    5- Enumerating the properties of the target object includes enumerating properties of its prototype, and the prototype of the prototype, and so on, recursively;
    //       but a property of a prototype is not processed if it has the same name as a property that has already been processed by the iterator's next method.
    //    6- The values of [[Enumerable]] attributes are not considered when determining if a property of a prototype object has already been processed.
    //    7- The enumerable property names of prototype objects must be obtained by invoking EnumerateObjectProperties passing the prototype object as the argument.
    //    8- EnumerateObjectProperties must obtain the own property keys of the target object by calling its [[OwnPropertyKeys]] internal method.
    //    9- Property attributes of the target object must be obtained by calling its [[GetOwnProperty]] internal method

    // Invariant 3 effectively allows the implementation to ignore newly added keys, and we do so (similar to other implementations).
    auto& vm = interpreter.vm();
    auto object = TRY(interpreter.accumulator().to_object(vm));
    // Note: While the spec doesn't explicitly require these to be ordered, it says that the values should be retrieved via OwnPropertyKeys,
    //       so we just keep the order consistent anyway.
    OrderedHashTable<PropertyKey> properties;
    OrderedHashTable<PropertyKey> non_enumerable_properties;
    HashTable<NonnullGCPtr<Object>> seen_objects;
    // Collect all keys immediately (invariant no. 5)
    for (auto object_to_check = GCPtr { object.ptr() }; object_to_check && !seen_objects.contains(*object_to_check); object_to_check = TRY(object_to_check->internal_get_prototype_of())) {
        seen_objects.set(*object_to_check);
        for (auto& key : TRY(object_to_check->internal_own_property_keys())) {
            if (key.is_symbol())
                continue;
            auto property_key = TRY(PropertyKey::from_value(vm, key));

            // If there is a non-enumerable property higher up the prototype chain with the same key,
            // we mustn't include this property even if it's enumerable (invariant no. 5 and 6)
            if (non_enumerable_properties.contains(property_key))
                continue;
            if (properties.contains(property_key))
                continue;

            auto descriptor = TRY(object_to_check->internal_get_own_property(property_key));
            if (!*descriptor->enumerable)
                non_enumerable_properties.set(move(property_key));
            else
                properties.set(move(property_key));
        }
    }
    IteratorRecord iterator {
        .iterator = object,
        .next_method = NativeFunction::create(
            interpreter.realm(),
            [items = move(properties)](VM& vm) mutable -> ThrowCompletionOr<Value> {
                auto& realm = *vm.current_realm();
                auto iterated_object_value = vm.this_value();
                if (!iterated_object_value.is_object())
                    return vm.throw_completion<InternalError>("Invalid state for GetObjectPropertyIterator.next"sv);

                auto& iterated_object = iterated_object_value.as_object();
                auto result_object = Object::create(realm, nullptr);
                while (true) {
                    if (items.is_empty()) {
                        result_object->define_direct_property(vm.names.done, JS::Value(true), default_attributes);
                        return result_object;
                    }

                    auto key = items.take_first();

                    // If the property is deleted, don't include it (invariant no. 2)
                    if (!TRY(iterated_object.has_property(key)))
                        continue;

                    result_object->define_direct_property(vm.names.done, JS::Value(false), default_attributes);

                    if (key.is_number())
                        result_object->define_direct_property(vm.names.value, PrimitiveString::create(vm, TRY_OR_THROW_OOM(vm, String::number(key.as_number()))), default_attributes);
                    else if (key.is_string())
                        result_object->define_direct_property(vm.names.value, PrimitiveString::create(vm, key.as_string()), default_attributes);
                    else
                        VERIFY_NOT_REACHED(); // We should not have non-string/number keys.

                    return result_object;
                }
            },
            1,
            vm.names.next),
        .done = false,
    };
    interpreter.accumulator() = iterator_to_object(vm, move(iterator));
    return {};
}

ThrowCompletionOr<void> IteratorClose::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto iterator_object = TRY(interpreter.accumulator().to_object(vm));
    auto iterator = object_to_iterator(vm, iterator_object);

    // FIXME: Return the value of the resulting completion. (Note that m_completion_value can be empty!)
    TRY(iterator_close(vm, iterator, Completion { m_completion_type, m_completion_value, {} }));
    return {};
}

ThrowCompletionOr<void> AsyncIteratorClose::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto iterator_object = TRY(interpreter.accumulator().to_object(vm));
    auto iterator = object_to_iterator(vm, iterator_object);

    // FIXME: Return the value of the resulting completion. (Note that m_completion_value can be empty!)
    TRY(async_iterator_close(vm, iterator, Completion { m_completion_type, m_completion_value, {} }));
    return {};
}

ThrowCompletionOr<void> IteratorNext::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto iterator_object = TRY(interpreter.accumulator().to_object(vm));
    auto iterator = object_to_iterator(vm, iterator_object);

    interpreter.accumulator() = TRY(iterator_next(vm, iterator));
    return {};
}

ThrowCompletionOr<void> IteratorResultDone::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto iterator_result = TRY(interpreter.accumulator().to_object(vm));

    auto complete = TRY(iterator_complete(vm, iterator_result));
    interpreter.accumulator() = Value(complete);
    return {};
}

ThrowCompletionOr<void> IteratorResultValue::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto iterator_result = TRY(interpreter.accumulator().to_object(vm));

    interpreter.accumulator() = TRY(iterator_value(vm, iterator_result));
    return {};
}

ThrowCompletionOr<void> NewClass::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto name = m_class_expression.name();
    auto super_class = interpreter.accumulator();

    // NOTE: NewClass expects classEnv to be active lexical environment
    auto class_environment = vm.lexical_environment();
    vm.running_execution_context().lexical_environment = interpreter.saved_lexical_environment_stack().take_last();

    DeprecatedFlyString binding_name;
    DeprecatedFlyString class_name;
    if (!m_class_expression.has_name() && m_lhs_name.has_value()) {
        class_name = interpreter.current_executable().get_identifier(m_lhs_name.value());
    } else {
        binding_name = name;
        class_name = name.is_null() ? ""sv : name;
    }

    interpreter.accumulator() = TRY(m_class_expression.create_class_constructor(vm, class_environment, vm.lexical_environment(), super_class, binding_name, class_name));

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
    auto const& value = vm.running_execution_context().local_variables[m_index];
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

DeprecatedString Load::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return DeprecatedString::formatted("Load {}", m_src);
}

DeprecatedString LoadImmediate::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return DeprecatedString::formatted("LoadImmediate {}", m_value);
}

DeprecatedString Store::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return DeprecatedString::formatted("Store {}", m_dst);
}

DeprecatedString NewBigInt::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return DeprecatedString::formatted("NewBigInt \"{}\"", m_bigint.to_base_deprecated(10));
}

DeprecatedString NewArray::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    StringBuilder builder;
    builder.append("NewArray"sv);
    if (m_element_count != 0) {
        builder.appendff(" [{}-{}]", m_elements[0], m_elements[1]);
    }
    return builder.to_deprecated_string();
}

DeprecatedString Append::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    if (m_is_spread)
        return DeprecatedString::formatted("Append lhs: **{}", m_lhs);
    return DeprecatedString::formatted("Append lhs: {}", m_lhs);
}

DeprecatedString IteratorToArray::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return "IteratorToArray";
}

DeprecatedString NewString::to_deprecated_string_impl(Bytecode::Executable const& executable) const
{
    return DeprecatedString::formatted("NewString {} (\"{}\")", m_string, executable.string_table->get(m_string));
}

DeprecatedString NewObject::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return "NewObject";
}

DeprecatedString NewRegExp::to_deprecated_string_impl(Bytecode::Executable const& executable) const
{
    return DeprecatedString::formatted("NewRegExp source:{} (\"{}\") flags:{} (\"{}\")", m_source_index, executable.get_string(m_source_index), m_flags_index, executable.get_string(m_flags_index));
}

DeprecatedString CopyObjectExcludingProperties::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    StringBuilder builder;
    builder.appendff("CopyObjectExcludingProperties from:{}", m_from_object);
    if (m_excluded_names_count != 0) {
        builder.append(" excluding:["sv);
        builder.join(", "sv, ReadonlySpan<Register>(m_excluded_names, m_excluded_names_count));
        builder.append(']');
    }
    return builder.to_deprecated_string();
}

DeprecatedString ConcatString::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return DeprecatedString::formatted("ConcatString {}", m_lhs);
}

DeprecatedString GetCalleeAndThisFromEnvironment::to_deprecated_string_impl(Bytecode::Executable const& executable) const
{
    return DeprecatedString::formatted("GetCalleeAndThisFromEnvironment {} -> callee: {}, this:{} ", executable.identifier_table->get(m_identifier), m_callee_reg, m_this_reg);
}

DeprecatedString GetVariable::to_deprecated_string_impl(Bytecode::Executable const& executable) const
{
    return DeprecatedString::formatted("GetVariable {} ({})", m_identifier, executable.identifier_table->get(m_identifier));
}

DeprecatedString GetGlobal::to_deprecated_string_impl(Bytecode::Executable const& executable) const
{
    return DeprecatedString::formatted("GetGlobal {} ({})", m_identifier, executable.identifier_table->get(m_identifier));
}

DeprecatedString GetLocal::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return DeprecatedString::formatted("GetLocal {}", m_index);
}

DeprecatedString DeleteVariable::to_deprecated_string_impl(Bytecode::Executable const& executable) const
{
    return DeprecatedString::formatted("DeleteVariable {} ({})", m_identifier, executable.identifier_table->get(m_identifier));
}

DeprecatedString CreateLexicalEnvironment::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return "CreateLexicalEnvironment"sv;
}

DeprecatedString CreateVariable::to_deprecated_string_impl(Bytecode::Executable const& executable) const
{
    auto mode_string = m_mode == EnvironmentMode::Lexical ? "Lexical" : "Variable";
    return DeprecatedString::formatted("CreateVariable env:{} immutable:{} global:{} {} ({})", mode_string, m_is_immutable, m_is_global, m_identifier, executable.identifier_table->get(m_identifier));
}

DeprecatedString EnterObjectEnvironment::to_deprecated_string_impl(Executable const&) const
{
    return DeprecatedString::formatted("EnterObjectEnvironment");
}

DeprecatedString SetVariable::to_deprecated_string_impl(Bytecode::Executable const& executable) const
{
    auto initialization_mode_name = m_initialization_mode == InitializationMode::Initialize ? "Initialize" : "Set";
    auto mode_string = m_mode == EnvironmentMode::Lexical ? "Lexical" : "Variable";
    return DeprecatedString::formatted("SetVariable env:{} init:{} {} ({})", mode_string, initialization_mode_name, m_identifier, executable.identifier_table->get(m_identifier));
}

DeprecatedString SetLocal::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return DeprecatedString::formatted("SetLocal {}", m_index);
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

DeprecatedString PutById::to_deprecated_string_impl(Bytecode::Executable const& executable) const
{
    auto kind = property_kind_to_string(m_kind);
    return DeprecatedString::formatted("PutById kind:{} base:{}, property:{} ({})", kind, m_base, m_property, executable.identifier_table->get(m_property));
}

DeprecatedString PutByIdWithThis::to_deprecated_string_impl(Bytecode::Executable const& executable) const
{
    auto kind = property_kind_to_string(m_kind);
    return DeprecatedString::formatted("PutByIdWithThis kind:{} base:{}, property:{} ({}) this_value:{}", kind, m_base, m_property, executable.identifier_table->get(m_property), m_this_value);
}

DeprecatedString PutPrivateById::to_deprecated_string_impl(Bytecode::Executable const& executable) const
{
    auto kind = property_kind_to_string(m_kind);
    return DeprecatedString::formatted("PutPrivateById kind:{} base:{}, property:{} ({})", kind, m_base, m_property, executable.identifier_table->get(m_property));
}

DeprecatedString GetById::to_deprecated_string_impl(Bytecode::Executable const& executable) const
{
    return DeprecatedString::formatted("GetById {} ({})", m_property, executable.identifier_table->get(m_property));
}

DeprecatedString GetByIdWithThis::to_deprecated_string_impl(Bytecode::Executable const& executable) const
{
    return DeprecatedString::formatted("GetByIdWithThis {} ({}) this_value:{}", m_property, executable.identifier_table->get(m_property), m_this_value);
}

DeprecatedString GetPrivateById::to_deprecated_string_impl(Bytecode::Executable const& executable) const
{
    return DeprecatedString::formatted("GetPrivateById {} ({})", m_property, executable.identifier_table->get(m_property));
}

DeprecatedString HasPrivateId::to_deprecated_string_impl(Bytecode::Executable const& executable) const
{
    return DeprecatedString::formatted("HasPrivateId {} ({})", m_property, executable.identifier_table->get(m_property));
}

DeprecatedString DeleteById::to_deprecated_string_impl(Bytecode::Executable const& executable) const
{
    return DeprecatedString::formatted("DeleteById {} ({})", m_property, executable.identifier_table->get(m_property));
}

DeprecatedString DeleteByIdWithThis::to_deprecated_string_impl(Bytecode::Executable const& executable) const
{
    return DeprecatedString::formatted("DeleteByIdWithThis {} ({}) this_value:{}", m_property, executable.identifier_table->get(m_property), m_this_value);
}

DeprecatedString Jump::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    if (m_true_target.has_value())
        return DeprecatedString::formatted("Jump {}", *m_true_target);
    return DeprecatedString::formatted("Jump <empty>");
}

DeprecatedString JumpConditional::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    auto true_string = m_true_target.has_value() ? DeprecatedString::formatted("{}", *m_true_target) : "<empty>";
    auto false_string = m_false_target.has_value() ? DeprecatedString::formatted("{}", *m_false_target) : "<empty>";
    return DeprecatedString::formatted("JumpConditional true:{} false:{}", true_string, false_string);
}

DeprecatedString JumpNullish::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    auto true_string = m_true_target.has_value() ? DeprecatedString::formatted("{}", *m_true_target) : "<empty>";
    auto false_string = m_false_target.has_value() ? DeprecatedString::formatted("{}", *m_false_target) : "<empty>";
    return DeprecatedString::formatted("JumpNullish null:{} nonnull:{}", true_string, false_string);
}

DeprecatedString JumpUndefined::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    auto true_string = m_true_target.has_value() ? DeprecatedString::formatted("{}", *m_true_target) : "<empty>";
    auto false_string = m_false_target.has_value() ? DeprecatedString::formatted("{}", *m_false_target) : "<empty>";
    return DeprecatedString::formatted("JumpUndefined undefined:{} not undefined:{}", true_string, false_string);
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

DeprecatedString Call::to_deprecated_string_impl(Bytecode::Executable const& executable) const
{
    auto type = call_type_to_string(m_type);
    if (m_expression_string.has_value())
        return DeprecatedString::formatted("Call{} callee:{}, this:{}, first_arg:{} ({})", type, m_callee, m_this_value, m_first_argument, executable.get_string(m_expression_string.value()));
    return DeprecatedString::formatted("Call{} callee:{}, this:{}, first_arg:{}", type, m_callee, m_first_argument, m_this_value);
}

DeprecatedString CallWithArgumentArray::to_deprecated_string_impl(Bytecode::Executable const& executable) const
{
    auto type = call_type_to_string(m_type);
    if (m_expression_string.has_value())
        return DeprecatedString::formatted("CallWithArgumentArray{} callee:{}, this:{}, arguments:[...acc] ({})", type, m_callee, m_this_value, executable.get_string(m_expression_string.value()));
    return DeprecatedString::formatted("CallWithArgumentArray{} callee:{}, this:{}, arguments:[...acc]", type, m_callee, m_this_value);
}

DeprecatedString SuperCallWithArgumentArray::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return "SuperCallWithArgumentArray arguments:[...acc]"sv;
}

DeprecatedString NewFunction::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    StringBuilder builder;
    builder.append("NewFunction"sv);
    if (m_function_node.has_name())
        builder.appendff(" name:{}"sv, m_function_node.name());
    if (m_lhs_name.has_value())
        builder.appendff(" lhs_name:{}"sv, m_lhs_name.value());
    if (m_home_object.has_value())
        builder.appendff(" home_object:{}"sv, m_home_object.value());
    return builder.to_deprecated_string();
}

DeprecatedString NewClass::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    StringBuilder builder;
    auto name = m_class_expression.name();
    builder.appendff("NewClass '{}'"sv, name.is_null() ? ""sv : name);
    if (m_lhs_name.has_value())
        builder.appendff(" lhs_name:{}"sv, m_lhs_name.value());
    return builder.to_deprecated_string();
}

DeprecatedString Return::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return "Return";
}

DeprecatedString Increment::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return "Increment";
}

DeprecatedString Decrement::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return "Decrement";
}

DeprecatedString Throw::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return "Throw";
}

DeprecatedString ThrowIfNotObject::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return "ThrowIfNotObject";
}

DeprecatedString ThrowIfNullish::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return "ThrowIfNullish";
}

DeprecatedString EnterUnwindContext::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    auto handler_string = m_handler_target.has_value() ? DeprecatedString::formatted("{}", *m_handler_target) : "<empty>";
    auto finalizer_string = m_finalizer_target.has_value() ? DeprecatedString::formatted("{}", *m_finalizer_target) : "<empty>";
    return DeprecatedString::formatted("EnterUnwindContext handler:{} finalizer:{} entry:{}", handler_string, finalizer_string, m_entry_point);
}

DeprecatedString ScheduleJump::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return DeprecatedString::formatted("ScheduleJump {}", m_target);
}

DeprecatedString LeaveLexicalEnvironment::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return "LeaveLexicalEnvironment"sv;
}

DeprecatedString LeaveUnwindContext::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return "LeaveUnwindContext";
}

DeprecatedString ContinuePendingUnwind::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return DeprecatedString::formatted("ContinuePendingUnwind resume:{}", m_resume_target);
}

DeprecatedString PushDeclarativeEnvironment::to_deprecated_string_impl(Bytecode::Executable const& executable) const
{
    StringBuilder builder;
    builder.append("PushDeclarativeEnvironment"sv);
    if (!m_variables.is_empty()) {
        builder.append(" {"sv);
        Vector<DeprecatedString> names;
        for (auto& it : m_variables)
            names.append(executable.get_string(it.key));
        builder.append('}');
        builder.join(", "sv, names);
    }
    return builder.to_deprecated_string();
}

DeprecatedString Yield::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    if (m_continuation_label.has_value())
        return DeprecatedString::formatted("Yield continuation:@{}", m_continuation_label->block().name());
    return DeprecatedString::formatted("Yield return");
}

DeprecatedString Await::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return DeprecatedString::formatted("Await continuation:@{}", m_continuation_label.block().name());
}

DeprecatedString GetByValue::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return DeprecatedString::formatted("GetByValue base:{}", m_base);
}

DeprecatedString GetByValueWithThis::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return DeprecatedString::formatted("GetByValueWithThis base:{} this_value:{}", m_base, m_this_value);
}

DeprecatedString PutByValue::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    auto kind = property_kind_to_string(m_kind);
    return DeprecatedString::formatted("PutByValue kind:{} base:{}, property:{}", kind, m_base, m_property);
}

DeprecatedString PutByValueWithThis::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    auto kind = property_kind_to_string(m_kind);
    return DeprecatedString::formatted("PutByValueWithThis kind:{} base:{}, property:{} this_value:{}", kind, m_base, m_property, m_this_value);
}

DeprecatedString DeleteByValue::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return DeprecatedString::formatted("DeleteByValue base:{}", m_base);
}

DeprecatedString DeleteByValueWithThis::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return DeprecatedString::formatted("DeleteByValueWithThis base:{} this_value:{}", m_base, m_this_value);
}

DeprecatedString GetIterator::to_deprecated_string_impl(Executable const&) const
{
    auto hint = m_hint == IteratorHint::Sync ? "sync" : "async";
    return DeprecatedString::formatted("GetIterator hint:{}", hint);
}

DeprecatedString GetMethod::to_deprecated_string_impl(Bytecode::Executable const& executable) const
{
    return DeprecatedString::formatted("GetMethod {} ({})", m_property, executable.identifier_table->get(m_property));
}

DeprecatedString GetObjectPropertyIterator::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return "GetObjectPropertyIterator";
}

DeprecatedString IteratorClose::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    if (!m_completion_value.has_value())
        return DeprecatedString::formatted("IteratorClose completion_type={} completion_value=<empty>", to_underlying(m_completion_type));

    auto completion_value_string = m_completion_value->to_string_without_side_effects();
    return DeprecatedString::formatted("IteratorClose completion_type={} completion_value={}", to_underlying(m_completion_type), completion_value_string);
}

DeprecatedString AsyncIteratorClose::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    if (!m_completion_value.has_value())
        return DeprecatedString::formatted("AsyncIteratorClose completion_type={} completion_value=<empty>", to_underlying(m_completion_type));

    auto completion_value_string = m_completion_value->to_string_without_side_effects();
    return DeprecatedString::formatted("AsyncIteratorClose completion_type={} completion_value={}", to_underlying(m_completion_type), completion_value_string);
}

DeprecatedString IteratorNext::to_deprecated_string_impl(Executable const&) const
{
    return "IteratorNext";
}

DeprecatedString IteratorResultDone::to_deprecated_string_impl(Executable const&) const
{
    return "IteratorResultDone";
}

DeprecatedString IteratorResultValue::to_deprecated_string_impl(Executable const&) const
{
    return "IteratorResultValue";
}

DeprecatedString ResolveThisBinding::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return "ResolveThisBinding"sv;
}

DeprecatedString ResolveSuperBase::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return "ResolveSuperBase"sv;
}

DeprecatedString GetNewTarget::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return "GetNewTarget"sv;
}

DeprecatedString GetImportMeta::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return "GetImportMeta"sv;
}

DeprecatedString TypeofVariable::to_deprecated_string_impl(Bytecode::Executable const& executable) const
{
    return DeprecatedString::formatted("TypeofVariable {} ({})", m_identifier, executable.identifier_table->get(m_identifier));
}

DeprecatedString TypeofLocal::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return DeprecatedString::formatted("TypeofLocal {}", m_index);
}

DeprecatedString ToNumeric::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return "ToNumeric"sv;
}

DeprecatedString BlockDeclarationInstantiation::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return "BlockDeclarationInstantiation"sv;
}

DeprecatedString ImportCall::to_deprecated_string_impl(Bytecode::Executable const&) const
{
    return DeprecatedString::formatted("ImportCall specifier:{} options:{}"sv, m_specifier, m_options);
}

}
