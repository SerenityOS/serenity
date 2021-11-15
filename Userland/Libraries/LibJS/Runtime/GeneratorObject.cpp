/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TemporaryChange.h>
#include <LibJS/Bytecode/Generator.h>
#include <LibJS/Bytecode/Interpreter.h>
#include <LibJS/Runtime/GeneratorObject.h>
#include <LibJS/Runtime/GeneratorObjectPrototype.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

ThrowCompletionOr<GeneratorObject*> GeneratorObject::create(GlobalObject& global_object, Value initial_value, ECMAScriptFunctionObject* generating_function, ExecutionContext execution_context, Bytecode::RegisterWindow frame)
{
    // This is "g1.prototype" in figure-2 (https://tc39.es/ecma262/img/figure-2.png)
    Value generating_function_prototype;
    if (generating_function->kind() == FunctionKind::Async) {
        // We implement async functions by transforming them to generator function in the bytecode
        // interpreter. However an async function does not have a prototype and should not be
        // changed thus we hardcode the prototype.
        generating_function_prototype = global_object.generator_object_prototype();
    } else {
        generating_function_prototype = TRY(generating_function->get(global_object.vm().names.prototype));
    }
    auto* generating_function_prototype_object = TRY(generating_function_prototype.to_object(global_object));
    auto object = global_object.heap().allocate<GeneratorObject>(global_object, global_object, *generating_function_prototype_object, move(execution_context));
    object->m_generating_function = generating_function;
    object->m_frame = move(frame);
    object->m_previous_value = initial_value;
    return object;
}

GeneratorObject::GeneratorObject(GlobalObject&, Object& prototype, ExecutionContext context)
    : Object(prototype)
    , m_execution_context(move(context))
{
}

void GeneratorObject::initialize(GlobalObject&)
{
}

GeneratorObject::~GeneratorObject()
{
}

void GeneratorObject::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_generating_function);
    visitor.visit(m_previous_value);
}

ThrowCompletionOr<Value> GeneratorObject::next_impl(VM& vm, GlobalObject& global_object, Optional<Value> next_argument, Optional<Value> value_to_throw)
{
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
            return reinterpret_cast<Bytecode::BasicBlock const*>(static_cast<u64>(TRY(number_value.to_double(global_object))));
        }
        return nullptr;
    };

    auto previous_generated_value = TRY(generated_value(m_previous_value));

    auto result = Object::create(global_object, global_object.object_prototype());
    result->define_direct_property("value", previous_generated_value, JS::default_attributes);

    if (m_done) {
        result->define_direct_property("done", Value(true), JS::default_attributes);
        return result;
    }

    // Extract the continuation
    auto next_block = TRY(generated_continuation(m_previous_value));

    if (!next_block) {
        // The generator has terminated, now we can simply return done=true.
        m_done = true;
        result->define_direct_property("done", Value(true), JS::default_attributes);
        return result;
    }

    // Make sure it's an actual block
    VERIFY(!m_generating_function->bytecode_executable()->basic_blocks.find_if([next_block](auto& block) { return block == next_block; }).is_end());

    // Restore the snapshot registers
    bytecode_interpreter->enter_frame(m_frame);

    // Temporarily switch to the captured execution context
    TRY(vm.push_execution_context(m_execution_context, global_object));

    // Pretend that 'yield' returned the passed value, or threw
    if (value_to_throw.has_value()) {
        vm.throw_exception(global_object, value_to_throw.release_value());
        bytecode_interpreter->accumulator() = js_undefined();
    } else {
        bytecode_interpreter->accumulator() = next_argument.value_or(js_undefined());
    }

    auto next_result = bytecode_interpreter->run(*m_generating_function->bytecode_executable(), next_block);

    m_frame = move(*bytecode_interpreter->pop_frame());

    vm.pop_execution_context();

    m_done = TRY(generated_continuation(m_previous_value)) == nullptr;

    m_previous_value = TRY(next_result);

    result->define_direct_property("value", TRY(generated_value(m_previous_value)), JS::default_attributes);
    result->define_direct_property("done", Value(m_done), JS::default_attributes);

    return result;
}

}
