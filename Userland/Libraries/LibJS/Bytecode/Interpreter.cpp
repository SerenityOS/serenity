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

static ByteString format_operand(StringView name, Operand operand, Bytecode::Executable const& executable)
{
    StringBuilder builder;
    if (!name.is_empty())
        builder.appendff("\033[32m{}\033[0m:", name);
    switch (operand.type()) {
    case Operand::Type::Register:
        builder.appendff("\033[33mreg{}\033[0m", operand.index());
        break;
    case Operand::Type::Local:
        // FIXME: Show local name.
        builder.appendff("\033[34mloc{}\033[0m", operand.index());
        break;
    case Operand::Type::Constant: {
        builder.append("\033[36m"sv);
        auto value = executable.constants[operand.index()];
        if (value.is_empty())
            builder.append("<Empty>"sv);
        else if (value.is_boolean())
            builder.appendff("Bool({})", value.as_bool() ? "true"sv : "false"sv);
        else if (value.is_int32())
            builder.appendff("Int32({})", value.as_i32());
        else if (value.is_double())
            builder.appendff("Double({})", value.as_double());
        else if (value.is_bigint())
            builder.appendff("BigInt({})", value.as_bigint().to_byte_string());
        else if (value.is_string())
            builder.appendff("String(\"{}\")", value.as_string().utf8_string_view());
        else if (value.is_undefined())
            builder.append("Undefined"sv);
        else if (value.is_null())
            builder.append("Null"sv);
        else
            builder.appendff("Value: {}", value);
        builder.append("\033[0m"sv);
        break;
    }
    default:
        VERIFY_NOT_REACHED();
    }
    return builder.to_byte_string();
}

static ByteString format_operand_list(StringView name, ReadonlySpan<Operand> operands, Bytecode::Executable const& executable)
{
    StringBuilder builder;
    if (!name.is_empty())
        builder.appendff(", \033[32m{}\033[0m:[", name);
    for (size_t i = 0; i < operands.size(); ++i) {
        if (i != 0)
            builder.append(", "sv);
        builder.appendff("{}", format_operand(""sv, operands[i], executable));
    }
    builder.append("]"sv);
    return builder.to_byte_string();
}

static ByteString format_value_list(StringView name, ReadonlySpan<Value> values)
{
    StringBuilder builder;
    if (!name.is_empty())
        builder.appendff(", \033[32m{}\033[0m:[", name);
    builder.join(", "sv, values);
    builder.append("]"sv);
    return builder.to_byte_string();
}

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

ALWAYS_INLINE static ThrowCompletionOr<Value> loosely_inequals(VM& vm, Value src1, Value src2)
{
    if (src1.tag() == src2.tag()) {
        if (src1.is_int32() || src1.is_object() || src1.is_boolean() || src1.is_nullish())
            return Value(src1.encoded() != src2.encoded());
    }
    return Value(!TRY(is_loosely_equal(vm, src1, src2)));
}

ALWAYS_INLINE static ThrowCompletionOr<Value> loosely_equals(VM& vm, Value src1, Value src2)
{
    if (src1.tag() == src2.tag()) {
        if (src1.is_int32() || src1.is_object() || src1.is_boolean() || src1.is_nullish())
            return Value(src1.encoded() == src2.encoded());
    }
    return Value(TRY(is_loosely_equal(vm, src1, src2)));
}

ALWAYS_INLINE static ThrowCompletionOr<Value> strict_inequals(VM&, Value src1, Value src2)
{
    if (src1.tag() == src2.tag()) {
        if (src1.is_int32() || src1.is_object() || src1.is_boolean() || src1.is_nullish())
            return Value(src1.encoded() != src2.encoded());
    }
    return Value(!is_strictly_equal(src1, src2));
}

ALWAYS_INLINE static ThrowCompletionOr<Value> strict_equals(VM&, Value src1, Value src2)
{
    if (src1.tag() == src2.tag()) {
        if (src1.is_int32() || src1.is_object() || src1.is_boolean() || src1.is_nullish())
            return Value(src1.encoded() == src2.encoded());
    }
    return Value(is_strictly_equal(src1, src2));
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

ALWAYS_INLINE Value Interpreter::get(Operand op) const
{
    switch (op.type()) {
    case Operand::Type::Register:
        return reg(Register { op.index() });
    case Operand::Type::Local:
        return vm().running_execution_context().locals[op.index()];
    case Operand::Type::Constant:
        return current_executable().constants[op.index()];
    }
    __builtin_unreachable();
}

ALWAYS_INLINE void Interpreter::set(Operand op, Value value)
{
    switch (op.type()) {
    case Operand::Type::Register:
        reg(Register { op.index() }) = value;
        return;
    case Operand::Type::Local:
        vm().running_execution_context().locals[op.index()] = value;
        return;
    case Operand::Type::Constant:
        VERIFY_NOT_REACHED();
    }
    __builtin_unreachable();
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
        auto executable_result = JS::Bytecode::Generator::generate(vm, script, {});

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
            case Instruction::Type::SetLocal:
                locals[static_cast<Op::SetLocal const&>(instruction).index()] = get(static_cast<Op::SetLocal const&>(instruction).src());
                break;
            case Instruction::Type::Mov:
                set(static_cast<Op::Mov const&>(instruction).dst(), get(static_cast<Op::Mov const&>(instruction).src()));
                break;
            case Instruction::Type::End:
                accumulator = get(static_cast<Op::End const&>(instruction).value());
                return;
            case Instruction::Type::Jump:
                m_current_block = &static_cast<Op::Jump const&>(instruction).true_target()->block();
                goto start;
            case Instruction::Type::JumpIf:
                if (get(static_cast<Op::JumpIf const&>(instruction).condition()).to_boolean())
                    m_current_block = &static_cast<Op::JumpIf const&>(instruction).true_target()->block();
                else
                    m_current_block = &static_cast<Op::JumpIf const&>(instruction).false_target()->block();
                goto start;
            case Instruction::Type::JumpNullish:
                if (get(static_cast<Op::JumpNullish const&>(instruction).condition()).is_nullish())
                    m_current_block = &static_cast<Op::Jump const&>(instruction).true_target()->block();
                else
                    m_current_block = &static_cast<Op::Jump const&>(instruction).false_target()->block();
                goto start;
            case Instruction::Type::JumpUndefined:
                if (get(static_cast<Op::JumpUndefined const&>(instruction).condition()).is_undefined())
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

    TemporaryChange restore_executable { m_current_executable, GCPtr { executable } };
    TemporaryChange restore_saved_jump { m_scheduled_jump, static_cast<BasicBlock const*>(nullptr) };
    TemporaryChange restore_realm { m_realm, GCPtr { vm().current_realm() } };
    TemporaryChange restore_global_object { m_global_object, GCPtr { m_realm->global_object() } };
    TemporaryChange restore_global_declarative_environment { m_global_declarative_environment, GCPtr { m_realm->global_environment().declarative_record() } };

    VERIFY(!vm().execution_context_stack().is_empty());

    TemporaryChange restore_current_block { m_current_block, entry_point ?: executable.basic_blocks.first() };

    if (in_frame)
        push_call_frame(in_frame);
    else
        push_call_frame(CallFrame::create(executable.number_of_registers));

    vm().execution_context_stack().last()->executable = &executable;

    run_bytecode();

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

void Interpreter::catch_exception(Operand dst)
{
    set(dst, reg(Register::exception()));
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

ThrowCompletionOr<NonnullGCPtr<Bytecode::Executable>> compile(VM& vm, ASTNode const& node, ReadonlySpan<FunctionParameter> parameters, FunctionKind kind, DeprecatedFlyString const& name)
{
    auto executable_result = Bytecode::Generator::generate(vm, node, parameters, kind);
    if (executable_result.is_error())
        return vm.throw_completion<InternalError>(ErrorType::NotImplemented, TRY_OR_THROW_OOM(vm, executable_result.error().to_string()));

    auto bytecode_executable = executable_result.release_value();
    bytecode_executable->name = name;

    if (Bytecode::g_dump_bytecode)
        bytecode_executable->dump();

    return bytecode_executable;
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

static void dump_object(Object& o, HashTable<Object const*>& seen, int indent = 0)
{
    if (seen.contains(&o))
        return;
    seen.set(&o);
    for (auto& it : o.shape().property_table()) {
        auto value = o.get_direct(it.value.offset);
        dbgln("{}  {} -> {}", String::repeated(' ', indent).release_value(), it.key.to_display_string(), value);
        if (value.is_object()) {
            dump_object(value.as_object(), seen, indent + 2);
        }
    }
}

ThrowCompletionOr<void> Dump::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto value = interpreter.get(m_value);
    dbgln("(DUMP) {}: {}", m_text, value);
    if (value.is_object()) {
        HashTable<Object const*> seen;
        dump_object(value.as_object(), seen);
    }
    return {};
}

ThrowCompletionOr<void> End::execute_impl(Bytecode::Interpreter&) const
{
    // Handled in the interpreter loop.
    __builtin_unreachable();
}

#define JS_DEFINE_EXECUTE_FOR_COMMON_BINARY_OP(OpTitleCase, op_snake_case)                      \
    ThrowCompletionOr<void> OpTitleCase::execute_impl(Bytecode::Interpreter& interpreter) const \
    {                                                                                           \
        auto& vm = interpreter.vm();                                                            \
        auto lhs = interpreter.get(m_lhs);                                                      \
        auto rhs = interpreter.get(m_rhs);                                                      \
        interpreter.set(m_dst, TRY(op_snake_case(vm, lhs, rhs)));                               \
        return {};                                                                              \
    }

#define JS_DEFINE_TO_BYTE_STRING_FOR_COMMON_BINARY_OP(OpTitleCase, op_snake_case)             \
    ByteString OpTitleCase::to_byte_string_impl(Bytecode::Executable const& executable) const \
    {                                                                                         \
        return ByteString::formatted(#OpTitleCase " {}, {}, {}",                              \
            format_operand("dst"sv, m_dst, executable),                                       \
            format_operand("lhs"sv, m_lhs, executable),                                       \
            format_operand("rhs"sv, m_rhs, executable));                                      \
    }

JS_ENUMERATE_COMMON_BINARY_OPS_WITHOUT_FAST_PATH(JS_DEFINE_EXECUTE_FOR_COMMON_BINARY_OP)
JS_ENUMERATE_COMMON_BINARY_OPS_WITHOUT_FAST_PATH(JS_DEFINE_TO_BYTE_STRING_FOR_COMMON_BINARY_OP)
JS_ENUMERATE_COMMON_BINARY_OPS_WITH_FAST_PATH(JS_DEFINE_TO_BYTE_STRING_FOR_COMMON_BINARY_OP)

ThrowCompletionOr<void> Add::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto const lhs = interpreter.get(m_lhs);
    auto const rhs = interpreter.get(m_rhs);

    if (lhs.is_number() && rhs.is_number()) {
        if (lhs.is_int32() && rhs.is_int32()) {
            if (!Checked<i32>::addition_would_overflow(lhs.as_i32(), rhs.as_i32())) {
                interpreter.set(m_dst, Value(lhs.as_i32() + rhs.as_i32()));
                return {};
            }
        }
        interpreter.set(m_dst, Value(lhs.as_double() + rhs.as_double()));
        return {};
    }

    interpreter.set(m_dst, TRY(add(vm, lhs, rhs)));
    return {};
}

ThrowCompletionOr<void> Mul::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto const lhs = interpreter.get(m_lhs);
    auto const rhs = interpreter.get(m_rhs);

    if (lhs.is_number() && rhs.is_number()) {
        if (lhs.is_int32() && rhs.is_int32()) {
            if (!Checked<i32>::multiplication_would_overflow(lhs.as_i32(), rhs.as_i32())) {
                interpreter.set(m_dst, Value(lhs.as_i32() * rhs.as_i32()));
                return {};
            }
        }
        interpreter.set(m_dst, Value(lhs.as_double() * rhs.as_double()));
        return {};
    }

    interpreter.set(m_dst, TRY(mul(vm, lhs, rhs)));
    return {};
}

ThrowCompletionOr<void> Sub::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto const lhs = interpreter.get(m_lhs);
    auto const rhs = interpreter.get(m_rhs);

    if (lhs.is_number() && rhs.is_number()) {
        if (lhs.is_int32() && rhs.is_int32()) {
            if (!Checked<i32>::addition_would_overflow(lhs.as_i32(), -rhs.as_i32())) {
                interpreter.set(m_dst, Value(lhs.as_i32() - rhs.as_i32()));
                return {};
            }
        }
        interpreter.set(m_dst, Value(lhs.as_double() - rhs.as_double()));
        return {};
    }

    interpreter.set(m_dst, TRY(sub(vm, lhs, rhs)));
    return {};
}

ThrowCompletionOr<void> BitwiseXor::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto const lhs = interpreter.get(m_lhs);
    auto const rhs = interpreter.get(m_rhs);
    if (lhs.is_int32() && rhs.is_int32()) {
        interpreter.set(m_dst, Value(lhs.as_i32() ^ rhs.as_i32()));
        return {};
    }
    interpreter.set(m_dst, TRY(bitwise_xor(vm, lhs, rhs)));
    return {};
}

ThrowCompletionOr<void> BitwiseAnd::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto const lhs = interpreter.get(m_lhs);
    auto const rhs = interpreter.get(m_rhs);
    if (lhs.is_int32() && rhs.is_int32()) {
        interpreter.set(m_dst, Value(lhs.as_i32() & rhs.as_i32()));
        return {};
    }
    interpreter.set(m_dst, TRY(bitwise_and(vm, lhs, rhs)));
    return {};
}

ThrowCompletionOr<void> BitwiseOr::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto const lhs = interpreter.get(m_lhs);
    auto const rhs = interpreter.get(m_rhs);
    if (lhs.is_int32() && rhs.is_int32()) {
        interpreter.set(m_dst, Value(lhs.as_i32() | rhs.as_i32()));
        return {};
    }
    interpreter.set(m_dst, TRY(bitwise_or(vm, lhs, rhs)));
    return {};
}

ThrowCompletionOr<void> UnsignedRightShift::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto const lhs = interpreter.get(m_lhs);
    auto const rhs = interpreter.get(m_rhs);
    if (lhs.is_int32() && rhs.is_int32()) {
        auto const shift_count = static_cast<u32>(rhs.as_i32()) % 32;
        interpreter.set(m_dst, Value(static_cast<u32>(lhs.as_i32()) >> shift_count));
        return {};
    }
    interpreter.set(m_dst, TRY(unsigned_right_shift(vm, lhs, rhs)));
    return {};
}

ThrowCompletionOr<void> RightShift::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto const lhs = interpreter.get(m_lhs);
    auto const rhs = interpreter.get(m_rhs);
    if (lhs.is_int32() && rhs.is_int32()) {
        auto const shift_count = static_cast<u32>(rhs.as_i32()) % 32;
        interpreter.set(m_dst, Value(lhs.as_i32() >> shift_count));
        return {};
    }
    interpreter.set(m_dst, TRY(right_shift(vm, lhs, rhs)));
    return {};
}

ThrowCompletionOr<void> LeftShift::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto const lhs = interpreter.get(m_lhs);
    auto const rhs = interpreter.get(m_rhs);
    if (lhs.is_int32() && rhs.is_int32()) {
        auto const shift_count = static_cast<u32>(rhs.as_i32()) % 32;
        interpreter.set(m_dst, Value(lhs.as_i32() << shift_count));
        return {};
    }
    interpreter.set(m_dst, TRY(left_shift(vm, lhs, rhs)));
    return {};
}

ThrowCompletionOr<void> LessThan::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto const lhs = interpreter.get(m_lhs);
    auto const rhs = interpreter.get(m_rhs);
    if (lhs.is_int32() && rhs.is_int32()) {
        interpreter.set(m_dst, Value(lhs.as_i32() < rhs.as_i32()));
        return {};
    }
    interpreter.set(m_dst, TRY(less_than(vm, lhs, rhs)));
    return {};
}

ThrowCompletionOr<void> LessThanEquals::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto const lhs = interpreter.get(m_lhs);
    auto const rhs = interpreter.get(m_rhs);
    if (lhs.is_int32() && rhs.is_int32()) {
        interpreter.set(m_dst, Value(lhs.as_i32() <= rhs.as_i32()));
        return {};
    }
    interpreter.set(m_dst, TRY(less_than_equals(vm, lhs, rhs)));
    return {};
}

ThrowCompletionOr<void> GreaterThan::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto const lhs = interpreter.get(m_lhs);
    auto const rhs = interpreter.get(m_rhs);
    if (lhs.is_int32() && rhs.is_int32()) {
        interpreter.set(m_dst, Value(lhs.as_i32() > rhs.as_i32()));
        return {};
    }
    interpreter.set(m_dst, TRY(greater_than(vm, lhs, rhs)));
    return {};
}

ThrowCompletionOr<void> GreaterThanEquals::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto const lhs = interpreter.get(m_lhs);
    auto const rhs = interpreter.get(m_rhs);
    if (lhs.is_int32() && rhs.is_int32()) {
        interpreter.set(m_dst, Value(lhs.as_i32() >= rhs.as_i32()));
        return {};
    }
    interpreter.set(m_dst, TRY(greater_than_equals(vm, lhs, rhs)));
    return {};
}

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
        interpreter.set(dst(), TRY(op_snake_case(vm, interpreter.get(src()))));                 \
        return {};                                                                              \
    }                                                                                           \
    ByteString OpTitleCase::to_byte_string_impl(Bytecode::Executable const& executable) const   \
    {                                                                                           \
        return ByteString::formatted(#OpTitleCase " {}, {}",                                    \
            format_operand("dst"sv, dst(), executable),                                         \
            format_operand("src"sv, src(), executable));                                        \
    }

JS_ENUMERATE_COMMON_UNARY_OPS(JS_DEFINE_COMMON_UNARY_OP)

ThrowCompletionOr<void> NewArray::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto array = MUST(Array::create(interpreter.realm(), 0));
    for (size_t i = 0; i < m_element_count; i++) {
        auto& value = interpreter.reg(Register(m_elements[0].index() + i));
        array->indexed_properties().put(i, value, default_attributes);
    }
    interpreter.set(dst(), array);
    return {};
}

ThrowCompletionOr<void> NewPrimitiveArray::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto array = MUST(Array::create(interpreter.realm(), 0));
    for (size_t i = 0; i < m_element_count; i++)
        array->indexed_properties().put(i, m_elements[i], default_attributes);
    interpreter.set(dst(), array);
    return {};
}

ThrowCompletionOr<void> ArrayAppend::execute_impl(Bytecode::Interpreter& interpreter) const
{
    return append(interpreter.vm(), interpreter.get(dst()), interpreter.get(src()), m_is_spread);
}

ThrowCompletionOr<void> ImportCall::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto specifier = interpreter.get(m_specifier);
    auto options_value = interpreter.get(m_options);
    interpreter.set(dst(), TRY(perform_import_call(vm, specifier, options_value)));
    return {};
}

ThrowCompletionOr<void> IteratorToArray::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.set(dst(), TRY(iterator_to_array(interpreter.vm(), interpreter.get(iterator()))));
    return {};
}

ThrowCompletionOr<void> NewObject::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto& realm = *vm.current_realm();
    interpreter.set(dst(), Object::create(realm, realm.intrinsics().object_prototype()));
    return {};
}

ThrowCompletionOr<void> NewRegExp::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.set(dst(),
        new_regexp(
            interpreter.vm(),
            interpreter.current_executable().regex_table->get(m_regex_index),
            interpreter.current_executable().get_string(m_source_index),
            interpreter.current_executable().get_string(m_flags_index)));
    return {};
}

#define JS_DEFINE_NEW_BUILTIN_ERROR_OP(ErrorName)                                                                      \
    ThrowCompletionOr<void> New##ErrorName::execute_impl(Bytecode::Interpreter& interpreter) const                     \
    {                                                                                                                  \
        auto& vm = interpreter.vm();                                                                                   \
        auto& realm = *vm.current_realm();                                                                             \
        interpreter.set(dst(), ErrorName::create(realm, interpreter.current_executable().get_string(m_error_string))); \
        return {};                                                                                                     \
    }                                                                                                                  \
    ByteString New##ErrorName::to_byte_string_impl(Bytecode::Executable const& executable) const                       \
    {                                                                                                                  \
        return ByteString::formatted("New" #ErrorName " {}, {}",                                                       \
            format_operand("dst"sv, m_dst, executable),                                                                \
            executable.string_table->get(m_error_string));                                                             \
    }

JS_ENUMERATE_NEW_BUILTIN_ERROR_OPS(JS_DEFINE_NEW_BUILTIN_ERROR_OP)

ThrowCompletionOr<void> CopyObjectExcludingProperties::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto& realm = *vm.current_realm();

    auto from_object = interpreter.get(m_from_object);

    auto to_object = Object::create(realm, realm.intrinsics().object_prototype());

    HashTable<PropertyKey> excluded_names;
    for (size_t i = 0; i < m_excluded_names_count; ++i) {
        excluded_names.set(TRY(interpreter.get(m_excluded_names[i]).to_property_key(vm)));
    }

    TRY(to_object->copy_data_properties(vm, from_object, excluded_names));

    interpreter.set(dst(), to_object);
    return {};
}

ThrowCompletionOr<void> ConcatString::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto string = TRY(interpreter.get(src()).to_primitive_string(vm));
    interpreter.set(dst(), PrimitiveString::create(vm, interpreter.get(dst()).as_string(), string));
    return {};
}

ThrowCompletionOr<void> GetVariable::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.set(dst(), TRY(get_variable(interpreter, interpreter.current_executable().get_identifier(m_identifier), interpreter.current_executable().environment_variable_caches[m_cache_index])));
    return {};
}

ThrowCompletionOr<void> GetCalleeAndThisFromEnvironment::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto callee_and_this = TRY(get_callee_and_this_from_environment(
        interpreter,
        interpreter.current_executable().get_identifier(m_identifier),
        interpreter.current_executable().environment_variable_caches[m_cache_index]));
    interpreter.set(m_callee, callee_and_this.callee);
    interpreter.set(m_this_value, callee_and_this.this_value);
    return {};
}

ThrowCompletionOr<void> GetGlobal::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.set(dst(), TRY(get_global(interpreter, interpreter.current_executable().get_identifier(m_identifier), interpreter.current_executable().global_variable_caches[m_cache_index])));
    return {};
}

ThrowCompletionOr<void> DeleteVariable::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto const& string = interpreter.current_executable().get_identifier(m_identifier);
    auto reference = TRY(vm.resolve_binding(string));
    interpreter.set(dst(), Value(TRY(reference.delete_(vm))));
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
    auto object = TRY(interpreter.get(m_object).to_object(interpreter.vm()));
    interpreter.enter_object_environment(*object);
    return {};
}

ThrowCompletionOr<void> Catch::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.catch_exception(dst());
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
        interpreter.get(src()),
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
    auto base_identifier = interpreter.current_executable().get_identifier(m_base_identifier);
    auto const& property_identifier = interpreter.current_executable().get_identifier(m_property);

    auto base_value = interpreter.get(base());
    auto& cache = interpreter.current_executable().property_lookup_caches[m_cache_index];

    interpreter.set(dst(), TRY(get_by_id(interpreter.vm(), base_identifier, property_identifier, base_value, base_value, cache)));
    return {};
}

ThrowCompletionOr<void> GetByIdWithThis::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto base_value = interpreter.get(m_base);
    auto this_value = interpreter.get(m_this_value);
    auto& cache = interpreter.current_executable().property_lookup_caches[m_cache_index];
    interpreter.set(dst(), TRY(get_by_id(interpreter.vm(), {}, interpreter.current_executable().get_identifier(m_property), base_value, this_value, cache)));
    return {};
}

ThrowCompletionOr<void> GetPrivateById::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto const& name = interpreter.current_executable().get_identifier(m_property);
    auto base_value = interpreter.get(m_base);
    auto private_reference = make_private_reference(vm, base_value, name);
    interpreter.set(dst(), TRY(private_reference.get_value(vm)));
    return {};
}

ThrowCompletionOr<void> HasPrivateId::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();

    auto base = interpreter.get(m_base);
    if (!base.is_object())
        return vm.throw_completion<TypeError>(ErrorType::InOperatorWithObject);

    auto private_environment = vm.running_execution_context().private_environment;
    VERIFY(private_environment);
    auto private_name = private_environment->resolve_private_identifier(interpreter.current_executable().get_identifier(m_property));
    interpreter.set(dst(), Value(base.as_object().private_element_find(private_name) != nullptr));
    return {};
}

ThrowCompletionOr<void> PutById::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto value = interpreter.get(m_src);
    auto base = interpreter.get(m_base);
    auto base_identifier = interpreter.current_executable().get_identifier(m_base_identifier);
    PropertyKey name = interpreter.current_executable().get_identifier(m_property);
    auto& cache = interpreter.current_executable().property_lookup_caches[m_cache_index];
    TRY(put_by_property_key(vm, base, base, value, base_identifier, name, m_kind, &cache));
    return {};
}

ThrowCompletionOr<void> PutByIdWithThis::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto value = interpreter.get(m_src);
    auto base = interpreter.get(m_base);
    PropertyKey name = interpreter.current_executable().get_identifier(m_property);
    auto& cache = interpreter.current_executable().property_lookup_caches[m_cache_index];
    TRY(put_by_property_key(vm, base, interpreter.get(m_this_value), value, {}, name, m_kind, &cache));
    return {};
}

ThrowCompletionOr<void> PutPrivateById::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto value = interpreter.get(m_src);
    auto object = TRY(interpreter.get(m_base).to_object(vm));
    auto name = interpreter.current_executable().get_identifier(m_property);
    auto private_reference = make_private_reference(vm, object, name);
    TRY(private_reference.put_value(vm, value));
    return {};
}

ThrowCompletionOr<void> DeleteById::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto base_value = interpreter.get(m_base);
    interpreter.set(dst(), TRY(Bytecode::delete_by_id(interpreter, base_value, m_property)));
    return {};
}

ThrowCompletionOr<void> DeleteByIdWithThis::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto base_value = interpreter.get(m_base);
    auto const& identifier = interpreter.current_executable().get_identifier(m_property);
    bool strict = vm.in_strict_mode();
    auto reference = Reference { base_value, identifier, interpreter.get(m_this_value), strict };
    interpreter.set(dst(), Value(TRY(reference.delete_(vm))));
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
    interpreter.set(dst(), cached_this_value);
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
    interpreter.set(dst(), TRY(env.get_super_base()));

    return {};
}

ThrowCompletionOr<void> GetNewTarget::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.set(dst(), interpreter.vm().get_new_target());
    return {};
}

ThrowCompletionOr<void> GetImportMeta::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.set(dst(), interpreter.vm().get_import_meta());
    return {};
}

ThrowCompletionOr<void> JumpIf::execute_impl(Bytecode::Interpreter&) const
{
    // Handled in the interpreter loop.
    __builtin_unreachable();
}

ThrowCompletionOr<void> JumpUndefined::execute_impl(Bytecode::Interpreter&) const
{
    // Handled in the interpreter loop.
    __builtin_unreachable();
}

ThrowCompletionOr<void> JumpNullish::execute_impl(Bytecode::Interpreter&) const
{
    // Handled in the interpreter loop.
    __builtin_unreachable();
}

ThrowCompletionOr<void> Mov::execute_impl(Bytecode::Interpreter&) const
{
    // Handled in the interpreter loop.
    __builtin_unreachable();
}

static ThrowCompletionOr<Value> dispatch_builtin_call(Bytecode::Interpreter& interpreter, Bytecode::Builtin builtin, ReadonlySpan<Operand> arguments)
{
    switch (builtin) {
    case Builtin::MathAbs:
        return TRY(MathObject::abs_impl(interpreter.vm(), interpreter.get(arguments[0])));
    case Builtin::MathLog:
        return TRY(MathObject::log_impl(interpreter.vm(), interpreter.get(arguments[0])));
    case Builtin::MathPow:
        return TRY(MathObject::pow_impl(interpreter.vm(), interpreter.get(arguments[0]), interpreter.get(arguments[1])));
    case Builtin::MathExp:
        return TRY(MathObject::exp_impl(interpreter.vm(), interpreter.get(arguments[0])));
    case Builtin::MathCeil:
        return TRY(MathObject::ceil_impl(interpreter.vm(), interpreter.get(arguments[0])));
    case Builtin::MathFloor:
        return TRY(MathObject::floor_impl(interpreter.vm(), interpreter.get(arguments[0])));
    case Builtin::MathRound:
        return TRY(MathObject::round_impl(interpreter.vm(), interpreter.get(arguments[0])));
    case Builtin::MathSqrt:
        return TRY(MathObject::sqrt_impl(interpreter.vm(), interpreter.get(arguments[0])));
    case Bytecode::Builtin::__Count:
        VERIFY_NOT_REACHED();
    }
    VERIFY_NOT_REACHED();
}

ThrowCompletionOr<void> Call::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto callee = interpreter.get(m_callee);

    TRY(throw_if_needed_for_call(interpreter, callee, call_type(), expression_string()));

    if (m_builtin.has_value()
        && m_argument_count == Bytecode::builtin_argument_count(m_builtin.value())
        && callee.is_object()
        && interpreter.realm().get_builtin_value(m_builtin.value()) == &callee.as_object()) {
        interpreter.set(dst(), TRY(dispatch_builtin_call(interpreter, m_builtin.value(), { m_arguments, m_argument_count })));
        return {};
    }

    Vector<Value> argument_values;
    argument_values.ensure_capacity(m_argument_count);
    for (size_t i = 0; i < m_argument_count; ++i)
        argument_values.unchecked_append(interpreter.get(m_arguments[i]));
    interpreter.set(dst(), TRY(perform_call(interpreter, interpreter.get(m_this_value), call_type(), callee, argument_values)));
    return {};
}

ThrowCompletionOr<void> CallWithArgumentArray::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto callee = interpreter.get(m_callee);
    TRY(throw_if_needed_for_call(interpreter, callee, call_type(), expression_string()));
    auto argument_values = argument_list_evaluation(interpreter.vm(), interpreter.get(arguments()));
    interpreter.set(dst(), TRY(perform_call(interpreter, interpreter.get(m_this_value), call_type(), callee, move(argument_values))));
    return {};
}

// 13.3.7.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-super-keyword-runtime-semantics-evaluation
ThrowCompletionOr<void> SuperCallWithArgumentArray::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.set(dst(), TRY(super_call_with_argument_array(interpreter.vm(), interpreter.get(arguments()), m_is_synthetic)));
    return {};
}

ThrowCompletionOr<void> NewFunction::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    interpreter.set(dst(), new_function(vm, m_function_node, m_lhs_name, m_home_object));
    return {};
}

ThrowCompletionOr<void> Return::execute_impl(Bytecode::Interpreter& interpreter) const
{
    if (m_value.has_value())
        interpreter.do_return(interpreter.get(*m_value));
    else
        interpreter.do_return(js_undefined());
    return {};
}

ThrowCompletionOr<void> Increment::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto old_value = interpreter.get(dst());

    // OPTIMIZATION: Fast path for Int32 values.
    if (old_value.is_int32()) {
        auto integer_value = old_value.as_i32();
        if (integer_value != NumericLimits<i32>::max()) [[likely]] {
            interpreter.set(dst(), Value { integer_value + 1 });
            return {};
        }
    }

    old_value = TRY(old_value.to_numeric(vm));

    if (old_value.is_number())
        interpreter.set(dst(), Value(old_value.as_double() + 1));
    else
        interpreter.set(dst(), BigInt::create(vm, old_value.as_bigint().big_integer().plus(Crypto::SignedBigInteger { 1 })));
    return {};
}

ThrowCompletionOr<void> PostfixIncrement::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto old_value = interpreter.get(m_src);

    // OPTIMIZATION: Fast path for Int32 values.
    if (old_value.is_int32()) {
        auto integer_value = old_value.as_i32();
        if (integer_value != NumericLimits<i32>::max()) [[likely]] {
            interpreter.set(m_dst, old_value);
            interpreter.set(m_src, Value { integer_value + 1 });
            return {};
        }
    }

    old_value = TRY(old_value.to_numeric(vm));
    interpreter.set(m_dst, old_value);

    if (old_value.is_number())
        interpreter.set(m_src, Value(old_value.as_double() + 1));
    else
        interpreter.set(m_src, BigInt::create(vm, old_value.as_bigint().big_integer().plus(Crypto::SignedBigInteger { 1 })));
    return {};
}

ThrowCompletionOr<void> Decrement::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto old_value = interpreter.get(dst());

    old_value = TRY(old_value.to_numeric(vm));

    if (old_value.is_number())
        interpreter.set(dst(), Value(old_value.as_double() - 1));
    else
        interpreter.set(dst(), BigInt::create(vm, old_value.as_bigint().big_integer().minus(Crypto::SignedBigInteger { 1 })));
    return {};
}

ThrowCompletionOr<void> PostfixDecrement::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto old_value = interpreter.get(m_src);

    old_value = TRY(old_value.to_numeric(vm));
    interpreter.set(m_dst, old_value);

    if (old_value.is_number())
        interpreter.set(m_src, Value(old_value.as_double() - 1));
    else
        interpreter.set(m_src, BigInt::create(vm, old_value.as_bigint().big_integer().minus(Crypto::SignedBigInteger { 1 })));
    return {};
}

ThrowCompletionOr<void> Throw::execute_impl(Bytecode::Interpreter& interpreter) const
{
    return throw_completion(interpreter.get(src()));
}

ThrowCompletionOr<void> ThrowIfNotObject::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto src = interpreter.get(m_src);
    if (!src.is_object())
        return vm.throw_completion<TypeError>(ErrorType::NotAnObject, src.to_string_without_side_effects());
    return {};
}

ThrowCompletionOr<void> ThrowIfNullish::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto value = interpreter.get(m_src);
    if (value.is_nullish())
        return vm.throw_completion<TypeError>(ErrorType::NotObjectCoercible, value.to_string_without_side_effects());
    return {};
}

ThrowCompletionOr<void> ThrowIfTDZ::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto value = interpreter.get(m_src);
    if (value.is_empty())
        return vm.throw_completion<ReferenceError>(ErrorType::BindingNotInitialized, value.to_string_without_side_effects());
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
    auto yielded_value = interpreter.get(m_value).value_or(js_undefined());

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
    auto yielded_value = interpreter.get(m_argument).value_or(js_undefined());
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
    auto base_identifier = interpreter.current_executable().get_identifier(m_base_identifier);

    interpreter.set(dst(), TRY(get_by_value(interpreter.vm(), base_identifier, interpreter.get(m_base), interpreter.get(m_property))));
    return {};
}

ThrowCompletionOr<void> GetByValueWithThis::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto property_key_value = interpreter.get(m_property);
    auto object = TRY(interpreter.get(m_base).to_object(vm));
    auto property_key = TRY(property_key_value.to_property_key(vm));
    interpreter.set(dst(), TRY(object->internal_get(property_key, interpreter.get(m_this_value))));
    return {};
}

ThrowCompletionOr<void> PutByValue::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto value = interpreter.get(m_src);
    auto base_identifier = interpreter.current_executable().get_identifier(m_base_identifier);
    TRY(put_by_value(vm, interpreter.get(m_base), base_identifier, interpreter.get(m_property), value, m_kind));
    return {};
}

ThrowCompletionOr<void> PutByValueWithThis::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto value = interpreter.get(m_src);
    auto base = interpreter.get(m_base);
    auto property_key = m_kind != PropertyKind::Spread ? TRY(interpreter.get(m_property).to_property_key(vm)) : PropertyKey {};
    TRY(put_by_property_key(vm, base, interpreter.get(m_this_value), value, {}, property_key, m_kind));
    return {};
}

ThrowCompletionOr<void> DeleteByValue::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto base_value = interpreter.get(m_base);
    auto property_key_value = interpreter.get(m_property);
    interpreter.set(dst(), TRY(delete_by_value(interpreter, base_value, property_key_value)));

    return {};
}

ThrowCompletionOr<void> DeleteByValueWithThis::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto property_key_value = interpreter.get(m_property);
    auto base_value = interpreter.get(m_base);
    auto this_value = interpreter.get(m_this_value);
    interpreter.set(dst(), TRY(delete_by_value_with_this(interpreter, base_value, property_key_value, this_value)));

    return {};
}

ThrowCompletionOr<void> GetIterator::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    interpreter.set(dst(), TRY(get_iterator(vm, interpreter.get(iterable()), m_hint)));
    return {};
}

ThrowCompletionOr<void> GetObjectFromIteratorRecord::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& iterator_record = verify_cast<IteratorRecord>(interpreter.get(m_iterator_record).as_object());
    interpreter.set(m_object, iterator_record.iterator);
    return {};
}

ThrowCompletionOr<void> GetNextMethodFromIteratorRecord::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& iterator_record = verify_cast<IteratorRecord>(interpreter.get(m_iterator_record).as_object());
    interpreter.set(m_next_method, iterator_record.next_method);
    return {};
}

ThrowCompletionOr<void> GetMethod::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto identifier = interpreter.current_executable().get_identifier(m_property);
    auto method = TRY(interpreter.get(m_object).get_method(vm, identifier));
    interpreter.set(dst(), method ?: js_undefined());
    return {};
}

ThrowCompletionOr<void> GetObjectPropertyIterator::execute_impl(Bytecode::Interpreter& interpreter) const
{
    interpreter.set(dst(), TRY(get_object_property_iterator(interpreter.vm(), interpreter.get(object()))));
    return {};
}

ThrowCompletionOr<void> IteratorClose::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto& iterator = verify_cast<IteratorRecord>(interpreter.get(m_iterator_record).as_object());

    // FIXME: Return the value of the resulting completion. (Note that m_completion_value can be empty!)
    TRY(iterator_close(vm, iterator, Completion { m_completion_type, m_completion_value, {} }));
    return {};
}

ThrowCompletionOr<void> AsyncIteratorClose::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto& iterator = verify_cast<IteratorRecord>(interpreter.get(m_iterator_record).as_object());

    // FIXME: Return the value of the resulting completion. (Note that m_completion_value can be empty!)
    TRY(async_iterator_close(vm, iterator, Completion { m_completion_type, m_completion_value, {} }));
    return {};
}

ThrowCompletionOr<void> IteratorNext::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto& iterator_record = verify_cast<IteratorRecord>(interpreter.get(m_iterator_record).as_object());
    interpreter.set(dst(), TRY(iterator_next(vm, iterator_record)));
    return {};
}

ThrowCompletionOr<void> NewClass::execute_impl(Bytecode::Interpreter& interpreter) const
{
    Value super_class;
    if (m_super_class.has_value())
        super_class = interpreter.get(m_super_class.value());
    interpreter.set(dst(), TRY(new_class(interpreter.vm(), super_class, m_class_expression, m_lhs_name)));
    return {};
}

// 13.5.3.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-typeof-operator-runtime-semantics-evaluation
ThrowCompletionOr<void> TypeofVariable::execute_impl(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    interpreter.set(dst(), TRY(typeof_variable(vm, interpreter.current_executable().get_identifier(m_identifier))));
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

ByteString Mov::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("Mov {}, {}",
        format_operand("dst"sv, m_dst, executable),
        format_operand("src"sv, m_src, executable));
}

ByteString NewArray::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    StringBuilder builder;
    builder.appendff("NewArray {}", format_operand("dst"sv, dst(), executable));
    if (m_element_count != 0) {
        builder.appendff(", [{}-{}]", format_operand("from"sv, m_elements[0], executable), format_operand("to"sv, m_elements[1], executable));
    }
    return builder.to_byte_string();
}

ByteString NewPrimitiveArray::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("NewPrimitiveArray {}, {}"sv,
        format_operand("dst"sv, dst(), executable),
        format_value_list("elements"sv, elements()));
}

ByteString ArrayAppend::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("Append {}, {}{}",
        format_operand("dst"sv, dst(), executable),
        format_operand("src"sv, src(), executable),
        m_is_spread ? " **"sv : ""sv);
}

ByteString IteratorToArray::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("IteratorToArray {}, {}",
        format_operand("dst"sv, dst(), executable),
        format_operand("iterator"sv, iterator(), executable));
}

ByteString NewObject::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("NewObject {}", format_operand("dst"sv, dst(), executable));
}

ByteString NewRegExp::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("NewRegExp {}, source:{} (\"{}\") flags:{} (\"{}\")",
        format_operand("dst"sv, dst(), executable),
        m_source_index, executable.get_string(m_source_index), m_flags_index, executable.get_string(m_flags_index));
}

ByteString CopyObjectExcludingProperties::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    StringBuilder builder;
    builder.appendff("CopyObjectExcludingProperties {}, {}",
        format_operand("dst"sv, dst(), executable),
        format_operand("from"sv, m_from_object, executable));
    if (m_excluded_names_count != 0) {
        builder.append(" excluding:["sv);
        for (size_t i = 0; i < m_excluded_names_count; ++i) {
            if (i != 0)
                builder.append(", "sv);
            builder.append(format_operand("#"sv, m_excluded_names[i], executable));
        }
        builder.append(']');
    }
    return builder.to_byte_string();
}

ByteString ConcatString::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("ConcatString {}, {}",
        format_operand("dst"sv, dst(), executable),
        format_operand("src"sv, src(), executable));
}

ByteString GetCalleeAndThisFromEnvironment::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("GetCalleeAndThisFromEnvironment {}, {} <- {}",
        format_operand("callee"sv, m_callee, executable),
        format_operand("this"sv, m_this_value, executable),
        executable.identifier_table->get(m_identifier));
}

ByteString GetVariable::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("GetVariable {}, {}",
        format_operand("dst"sv, dst(), executable),
        executable.identifier_table->get(m_identifier));
}

ByteString GetGlobal::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("GetGlobal {}, {}", format_operand("dst"sv, dst(), executable),
        executable.identifier_table->get(m_identifier));
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

ByteString EnterObjectEnvironment::to_byte_string_impl(Executable const& executable) const
{
    return ByteString::formatted("EnterObjectEnvironment {}",
        format_operand("object"sv, m_object, executable));
}

ByteString SetVariable::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    auto initialization_mode_name = m_initialization_mode == InitializationMode::Initialize ? "Initialize" : "Set";
    auto mode_string = m_mode == EnvironmentMode::Lexical ? "Lexical" : "Variable";
    return ByteString::formatted("SetVariable {}, {}, env:{} init:{}",
        executable.identifier_table->get(m_identifier),
        format_operand("src"sv, src(), executable),
        mode_string, initialization_mode_name);
}

ByteString SetLocal::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("SetLocal {}, {}",
        format_operand("dst"sv, dst(), executable),
        format_operand("src"sv, src(), executable));
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
    return ByteString::formatted("PutById {}, {}, {}, kind:{}",
        format_operand("base"sv, m_base, executable),
        executable.identifier_table->get(m_property),
        format_operand("src"sv, m_src, executable),
        kind);
}

ByteString PutByIdWithThis::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    auto kind = property_kind_to_string(m_kind);
    return ByteString::formatted("PutByIdWithThis {}, {}, {}, {}, kind:{}",
        format_operand("base"sv, m_base, executable),
        executable.identifier_table->get(m_property),
        format_operand("src"sv, m_src, executable),
        format_operand("this"sv, m_this_value, executable),
        kind);
}

ByteString PutPrivateById::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    auto kind = property_kind_to_string(m_kind);
    return ByteString::formatted(
        "PutPrivateById {}, {}, {}, kind:{} ",
        format_operand("base"sv, m_base, executable),
        executable.identifier_table->get(m_property),
        format_operand("src"sv, m_src, executable),
        kind);
}

ByteString GetById::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("GetById {}, {}, {}",
        format_operand("dst"sv, m_dst, executable),
        format_operand("base"sv, m_base, executable),
        executable.identifier_table->get(m_property));
}

ByteString GetByIdWithThis::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("GetByIdWithThis {}, {}, {}, {}",
        format_operand("dst"sv, m_dst, executable),
        format_operand("base"sv, m_base, executable),
        executable.identifier_table->get(m_property),
        format_operand("this"sv, m_this_value, executable));
}

ByteString GetPrivateById::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("GetPrivateById {}, {}, {}",
        format_operand("dst"sv, m_dst, executable),
        format_operand("base"sv, m_base, executable),
        executable.identifier_table->get(m_property));
}

ByteString HasPrivateId::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("HasPrivateId {}, {}, {}",
        format_operand("dst"sv, m_dst, executable),
        format_operand("base"sv, m_base, executable),
        executable.identifier_table->get(m_property));
}

ByteString DeleteById::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("DeleteById {}, {}, {}",
        format_operand("dst"sv, m_dst, executable),
        format_operand("base"sv, m_base, executable),
        executable.identifier_table->get(m_property));
}

ByteString DeleteByIdWithThis::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("DeleteByIdWithThis {}, {}, {}, {}",
        format_operand("dst"sv, m_dst, executable),
        format_operand("base"sv, m_base, executable),
        executable.identifier_table->get(m_property),
        format_operand("this"sv, m_this_value, executable));
}

ByteString Jump::to_byte_string_impl(Bytecode::Executable const&) const
{
    if (m_true_target.has_value())
        return ByteString::formatted("Jump {}", *m_true_target);
    return ByteString::formatted("Jump <empty>");
}

ByteString JumpIf::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    auto true_string = m_true_target.has_value() ? ByteString::formatted("{}", *m_true_target) : "<empty>";
    auto false_string = m_false_target.has_value() ? ByteString::formatted("{}", *m_false_target) : "<empty>";
    return ByteString::formatted("JumpIf {}, \033[32mtrue\033[0m:{} \033[32mfalse\033[0m:{}",
        format_operand("condition"sv, m_condition, executable),
        true_string, false_string);
}

ByteString JumpNullish::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    auto true_string = m_true_target.has_value() ? ByteString::formatted("{}", *m_true_target) : "<empty>";
    auto false_string = m_false_target.has_value() ? ByteString::formatted("{}", *m_false_target) : "<empty>";
    return ByteString::formatted("JumpNullish {}, null:{} nonnull:{}",
        format_operand("condition"sv, m_condition, executable),
        true_string, false_string);
}

ByteString JumpUndefined::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    auto true_string = m_true_target.has_value() ? ByteString::formatted("{}", *m_true_target) : "<empty>";
    auto false_string = m_false_target.has_value() ? ByteString::formatted("{}", *m_false_target) : "<empty>";
    return ByteString::formatted("JumpUndefined {}, undefined:{} defined:{}",
        format_operand("condition"sv, m_condition, executable),
        true_string, false_string);
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

    StringBuilder builder;
    builder.appendff("Call{} {}, {}, {}"sv,
        type,
        format_operand("dst"sv, m_dst, executable),
        format_operand("callee"sv, m_callee, executable),
        format_operand("this"sv, m_this_value, executable));

    builder.append(format_operand_list("args"sv, { m_arguments, m_argument_count }, executable));

    if (m_builtin.has_value()) {
        builder.appendff(", (builtin:{})", m_builtin.value());
    }

    if (m_expression_string.has_value()) {
        builder.appendff(", `{}`", executable.get_string(m_expression_string.value()));
    }

    return builder.to_byte_string();
}

ByteString CallWithArgumentArray::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    auto type = call_type_to_string(m_type);
    StringBuilder builder;
    builder.appendff("CallWithArgumentArray{} {}, {}, {}, {}",
        type,
        format_operand("dst"sv, m_dst, executable),
        format_operand("callee"sv, m_callee, executable),
        format_operand("this"sv, m_this_value, executable),
        format_operand("arguments"sv, m_arguments, executable));

    if (m_expression_string.has_value())
        builder.appendff(" ({})", executable.get_string(m_expression_string.value()));
    return builder.to_byte_string();
}

ByteString SuperCallWithArgumentArray::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("SuperCallWithArgumentArray {}, {}",
        format_operand("dst"sv, m_dst, executable),
        format_operand("arguments"sv, m_arguments, executable));
}

ByteString NewFunction::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    StringBuilder builder;
    builder.appendff("NewFunction {}",
        format_operand("dst"sv, m_dst, executable));
    if (m_function_node.has_name())
        builder.appendff(" name:{}"sv, m_function_node.name());
    if (m_lhs_name.has_value())
        builder.appendff(" lhs_name:{}"sv, executable.get_identifier(m_lhs_name.value()));
    if (m_home_object.has_value())
        builder.appendff(", {}"sv, format_operand("home_object"sv, m_home_object.value(), executable));
    return builder.to_byte_string();
}

ByteString NewClass::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    StringBuilder builder;
    auto name = m_class_expression.name();
    builder.appendff("NewClass {}",
        format_operand("dst"sv, m_dst, executable));
    if (m_super_class.has_value())
        builder.appendff(", {}", format_operand("super_class"sv, *m_super_class, executable));
    if (!name.is_empty())
        builder.appendff(", {}", name);
    if (m_lhs_name.has_value())
        builder.appendff(", lhs_name:{}"sv, m_lhs_name.value());
    return builder.to_byte_string();
}

ByteString Return::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    if (m_value.has_value())
        return ByteString::formatted("Return {}", format_operand("value"sv, m_value.value(), executable));
    return "Return";
}

ByteString Increment::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("Increment {}", format_operand("dst"sv, m_dst, executable));
}

ByteString PostfixIncrement::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("PostfixIncrement {}, {}",
        format_operand("dst"sv, m_dst, executable),
        format_operand("src"sv, m_src, executable));
}

ByteString Decrement::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("Decrement {}", format_operand("dst"sv, m_dst, executable));
}

ByteString PostfixDecrement::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("PostfixDecrement {}, {}",
        format_operand("dst"sv, m_dst, executable),
        format_operand("src"sv, m_src, executable));
}

ByteString Throw::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("Throw {}",
        format_operand("src"sv, m_src, executable));
}

ByteString ThrowIfNotObject::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("ThrowIfNotObject {}",
        format_operand("src"sv, m_src, executable));
}

ByteString ThrowIfNullish::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("ThrowIfNullish {}",
        format_operand("src"sv, m_src, executable));
}

ByteString ThrowIfTDZ::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("ThrowIfTDZ {}",
        format_operand("src"sv, m_src, executable));
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

ByteString Yield::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    if (m_continuation_label.has_value()) {
        return ByteString::formatted("Yield continuation:@{}, {}",
            m_continuation_label->block().name(),
            format_operand("value"sv, m_value, executable));
    }
    return ByteString::formatted("Yield return {}",
        format_operand("value"sv, m_value, executable));
}

ByteString Await::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("Await {}, continuation:@{}",
        format_operand("argument"sv, m_argument, executable),
        m_continuation_label.block().name());
}

ByteString GetByValue::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("GetByValue {}, {}, {}",
        format_operand("dst"sv, m_dst, executable),
        format_operand("base"sv, m_base, executable),
        format_operand("property"sv, m_property, executable));
}

ByteString GetByValueWithThis::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("GetByValueWithThis {}, {}, {}",
        format_operand("dst"sv, m_dst, executable),
        format_operand("base"sv, m_base, executable),
        format_operand("property"sv, m_property, executable));
}

ByteString PutByValue::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    auto kind = property_kind_to_string(m_kind);
    return ByteString::formatted("PutByValue {}, {}, {}, kind:{}",
        format_operand("base"sv, m_base, executable),
        format_operand("property"sv, m_property, executable),
        format_operand("src"sv, m_src, executable),
        kind);
}

ByteString PutByValueWithThis::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    auto kind = property_kind_to_string(m_kind);
    return ByteString::formatted("PutByValueWithThis {}, {}, {}, {}, kind:{}",
        format_operand("base"sv, m_base, executable),
        format_operand("property"sv, m_property, executable),
        format_operand("src"sv, m_src, executable),
        format_operand("this"sv, m_this_value, executable),
        kind);
}

ByteString DeleteByValue::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("DeleteByValue {}, {}, {}",
        format_operand("dst"sv, dst(), executable),
        format_operand("base"sv, m_base, executable),
        format_operand("property"sv, m_property, executable));
}

ByteString DeleteByValueWithThis::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("DeleteByValueWithThis {}, {}, {}, {}",
        format_operand("dst"sv, dst(), executable),
        format_operand("base"sv, m_base, executable),
        format_operand("property"sv, m_property, executable),
        format_operand("this"sv, m_this_value, executable));
}

ByteString GetIterator::to_byte_string_impl(Executable const& executable) const
{
    auto hint = m_hint == IteratorHint::Sync ? "sync" : "async";
    return ByteString::formatted("GetIterator {}, {}, hint:{}",
        format_operand("dst"sv, m_dst, executable),
        format_operand("iterable"sv, m_iterable, executable),
        hint);
}

ByteString GetMethod::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("GetMethod {}, {}, {}",
        format_operand("dst"sv, m_dst, executable),
        format_operand("object"sv, m_object, executable),
        executable.identifier_table->get(m_property));
}

ByteString GetObjectPropertyIterator::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("GetObjectPropertyIterator {}, {}",
        format_operand("dst"sv, dst(), executable),
        format_operand("object"sv, object(), executable));
}

ByteString IteratorClose::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    if (!m_completion_value.has_value())
        return ByteString::formatted("IteratorClose {}, completion_type={} completion_value=<empty>",
            format_operand("iterator_record"sv, m_iterator_record, executable),
            to_underlying(m_completion_type));

    auto completion_value_string = m_completion_value->to_string_without_side_effects();
    return ByteString::formatted("IteratorClose {}, completion_type={} completion_value={}",
        format_operand("iterator_record"sv, m_iterator_record, executable),
        to_underlying(m_completion_type), completion_value_string);
}

ByteString AsyncIteratorClose::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    if (!m_completion_value.has_value()) {
        return ByteString::formatted("AsyncIteratorClose {}, completion_type:{} completion_value:<empty>",
            format_operand("iterator_record"sv, m_iterator_record, executable),
            to_underlying(m_completion_type));
    }

    return ByteString::formatted("AsyncIteratorClose {}, completion_type:{}, completion_value:{}",
        format_operand("iterator_record"sv, m_iterator_record, executable),
        to_underlying(m_completion_type), m_completion_value);
}

ByteString IteratorNext::to_byte_string_impl(Executable const& executable) const
{
    return ByteString::formatted("IteratorNext {}, {}",
        format_operand("dst"sv, m_dst, executable),
        format_operand("iterator_record"sv, m_iterator_record, executable));
}

ByteString ResolveThisBinding::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("ResolveThisBinding {}", format_operand("dst"sv, m_dst, executable));
}

ByteString ResolveSuperBase::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("ResolveSuperBase {}",
        format_operand("dst"sv, m_dst, executable));
}

ByteString GetNewTarget::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("GetNewTarget {}", format_operand("dst"sv, m_dst, executable));
}

ByteString GetImportMeta::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("GetImportMeta {}", format_operand("dst"sv, m_dst, executable));
}

ByteString TypeofVariable::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("TypeofVariable {}, {}",
        format_operand("dst"sv, m_dst, executable),
        executable.identifier_table->get(m_identifier));
}

ByteString BlockDeclarationInstantiation::to_byte_string_impl(Bytecode::Executable const&) const
{
    return "BlockDeclarationInstantiation"sv;
}

ByteString ImportCall::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("ImportCall {}, {}, {}",
        format_operand("dst"sv, m_dst, executable),
        format_operand("specifier"sv, m_specifier, executable),
        format_operand("options"sv, m_options, executable));
}

ByteString Catch::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("Catch {}",
        format_operand("dst"sv, m_dst, executable));
}

ByteString GetObjectFromIteratorRecord::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("GetObjectFromIteratorRecord {}, {}",
        format_operand("object"sv, m_object, executable),
        format_operand("iterator_record"sv, m_iterator_record, executable));
}

ByteString GetNextMethodFromIteratorRecord::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("GetNextMethodFromIteratorRecord {}, {}",
        format_operand("next_method"sv, m_next_method, executable),
        format_operand("iterator_record"sv, m_iterator_record, executable));
}

ByteString End::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("End {}", format_operand("value"sv, m_value, executable));
}

ByteString Dump::to_byte_string_impl(Bytecode::Executable const& executable) const
{
    return ByteString::formatted("Dump '{}', {}", m_text,
        format_operand("value"sv, m_value, executable));
}

}
