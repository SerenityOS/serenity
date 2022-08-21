/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TemporaryChange.h>
#include <LibJS/Bytecode/Generator.h>
#include <LibJS/Bytecode/Interpreter.h>
#include <LibJS/Runtime/GeneratorObject.h>
#include <LibJS/Runtime/GeneratorPrototype.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

ThrowCompletionOr<GeneratorObject*> GeneratorObject::create(Realm& realm, Value initial_value, ECMAScriptFunctionObject* generating_function, ExecutionContext execution_context, Bytecode::RegisterWindow frame)
{
    auto& vm = realm.vm();
    // This is "g1.prototype" in figure-2 (https://tc39.es/ecma262/img/figure-2.png)
    Value generating_function_prototype;
    if (generating_function->kind() == FunctionKind::Async) {
        // We implement async functions by transforming them to generator function in the bytecode
        // interpreter. However an async function does not have a prototype and should not be
        // changed thus we hardcode the prototype.
        generating_function_prototype = realm.global_object().generator_prototype();
    } else {
        generating_function_prototype = TRY(generating_function->get(vm.names.prototype));
    }
    auto* generating_function_prototype_object = TRY(generating_function_prototype.to_object(vm));
    auto object = realm.heap().allocate<GeneratorObject>(realm, realm, *generating_function_prototype_object, move(execution_context));
    object->m_generating_function = generating_function;
    object->m_frame = move(frame);
    object->m_previous_value = initial_value;
    return object;
}

GeneratorObject::GeneratorObject(Realm&, Object& prototype, ExecutionContext context)
    : Object(prototype)
    , m_execution_context(move(context))
{
}

void GeneratorObject::initialize(Realm&)
{
}

void GeneratorObject::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_generating_function);
    visitor.visit(m_previous_value);
}

ThrowCompletionOr<Value> GeneratorObject::next_impl(VM& vm, Optional<Value> next_argument, Optional<Value> value_to_throw)
{
    auto& realm = *vm.current_realm();
    auto bytecode_interpreter = Bytecode::Interpreter::current();
    VERIFY(bytecode_interpreter);

    auto generated_value = [](Value value) -> ThrowCompletionOr<Value> {
        if (value.is_object())
            return TRY(value.as_object().get("result"));
        return value.is_empty() ? js_undefined() : value;
    };

    auto generated_continuation = [&](Value value) -> ThrowCompletionOr<Bytecode::BasicBlock const*> {
        if (value.is_object()) {
            auto number_value = TRY(value.as_object().get("continuation"));
            return reinterpret_cast<Bytecode::BasicBlock const*>(static_cast<u64>(TRY(number_value.to_double(vm))));
        }
        return nullptr;
    };

    auto previous_generated_value = TRY(generated_value(m_previous_value));

    auto result = Object::create(realm, realm.global_object().object_prototype());
    result->define_direct_property("value", previous_generated_value, default_attributes);

    if (m_done) {
        result->define_direct_property("done", Value(true), default_attributes);
        return result;
    }

    // Extract the continuation
    auto next_block = TRY(generated_continuation(m_previous_value));

    if (!next_block) {
        // The generator has terminated, now we can simply return done=true.
        m_done = true;
        result->define_direct_property("done", Value(true), default_attributes);
        return result;
    }

    // Make sure it's an actual block
    VERIFY(!m_generating_function->bytecode_executable()->basic_blocks.find_if([next_block](auto& block) { return block == next_block; }).is_end());

    // Temporarily switch to the captured execution context
    TRY(vm.push_execution_context(m_execution_context, {}));

    // Pretend that 'yield' returned the passed value, or threw
    if (value_to_throw.has_value()) {
        bytecode_interpreter->accumulator() = js_undefined();
        return throw_completion(value_to_throw.release_value());
    }

    Bytecode::RegisterWindow* frame = nullptr;
    if (m_frame.has_value())
        frame = &m_frame.value();

    auto next_value = next_argument.value_or(js_undefined());
    if (frame)
        frame->registers[0] = next_value;
    else
        bytecode_interpreter->accumulator() = next_value;

    auto next_result = bytecode_interpreter->run_and_return_frame(*m_generating_function->bytecode_executable(), next_block, frame);

    vm.pop_execution_context();

    if (!m_frame.has_value())
        m_frame = move(*next_result.frame);

    m_previous_value = TRY(move(next_result.value));
    m_done = TRY(generated_continuation(m_previous_value)) == nullptr;

    result->define_direct_property("value", TRY(generated_value(m_previous_value)), default_attributes);
    result->define_direct_property("done", Value(m_done), default_attributes);

    return result;
}

}
