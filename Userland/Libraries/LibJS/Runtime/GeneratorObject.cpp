/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <YAK/TemporaryChange.h>
#include <LibJS/Bytecode/Generator.h>
#include <LibJS/Bytecode/Interpreter.h>
#include <LibJS/Runtime/GeneratorObject.h>
#include <LibJS/Runtime/GeneratorObjectPrototype.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

GeneratorObject* GeneratorObject::create(GlobalObject& global_object, Value initial_value, OrdinaryFunctionObject* generating_function, Environment* generating_scope, Bytecode::RegisterWindow frame)
{
    // This is "g1.prototype" in figure-2 (https://tc39.es/ecma262/img/figure-2.png)
    auto generating_function_proto_property = generating_function->get(global_object.vm().names.prototype).to_object(global_object);
    if (!generating_function_proto_property)
        return {};

    auto object = global_object.heap().allocate<GeneratorObject>(global_object, global_object, *generating_function_proto_property);
    object->m_generating_function = generating_function;
    object->m_environment = generating_scope;
    object->m_frame = move(frame);
    object->m_previous_value = initial_value;
    return object;
}

GeneratorObject::GeneratorObject(GlobalObject&, Object& prototype)
    : Object(prototype)
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
    Object::visit_edges(visitor);
    visitor.visit(m_environment);
    visitor.visit(m_generating_function);
    if (m_previous_value.is_object())
        visitor.visit(&m_previous_value.as_object());
}

Value GeneratorObject::next_impl(VM& vm, GlobalObject& global_object, Optional<Value> value_to_throw)
{
    auto bytecode_interpreter = Bytecode::Interpreter::current();
    VERIFY(bytecode_interpreter);

    auto generated_value = [](Value value) {
        if (value.is_object())
            return value.as_object().get("result");
        return value.is_empty() ? js_undefined() : value;
    };

    auto generated_continuation = [&](Value value) -> Bytecode::BasicBlock const* {
        if (value.is_object())
            return reinterpret_cast<Bytecode::BasicBlock const*>(static_cast<u64>(value.as_object().get("continuation").to_double(global_object)));
        return nullptr;
    };

    Value previous_generated_value { generated_value(m_previous_value) };

    if (vm.exception())
        return {};

    auto result = Object::create(global_object, global_object.object_prototype());
    result->define_direct_property("value", previous_generated_value, JS::default_attributes);

    if (m_done) {
        result->define_direct_property("done", Value(true), JS::default_attributes);
        return result;
    }

    // Extract the continuation
    auto next_block = generated_continuation(m_previous_value);
    if (vm.exception())
        return {};

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

    // Pretend that 'yield' returned the passed value, or threw
    if (value_to_throw.has_value()) {
        vm.throw_exception(global_object, value_to_throw.release_value());
        bytecode_interpreter->accumulator() = js_undefined();
    } else {
        bytecode_interpreter->accumulator() = vm.argument(0);
    }

    // Temporarily switch to the captured environment record
    TemporaryChange change { vm.running_execution_context().lexical_environment, m_environment };

    m_previous_value = bytecode_interpreter->run(*m_generating_function->bytecode_executable(), next_block);

    bytecode_interpreter->leave_frame();

    m_done = generated_continuation(m_previous_value) == nullptr;

    result->define_direct_property("value", generated_value(m_previous_value), JS::default_attributes);
    result->define_direct_property("done", Value(m_done), JS::default_attributes);

    if (vm.exception())
        return {};

    return result;
}

}
