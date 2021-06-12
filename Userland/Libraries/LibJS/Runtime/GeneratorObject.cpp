/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TemporaryChange.h>
#include <LibJS/Bytecode/Generator.h>
#include <LibJS/Bytecode/Interpreter.h>
#include <LibJS/Runtime/GeneratorObject.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

GeneratorObject* GeneratorObject::create(GlobalObject& global_object, Value initial_value, ScriptFunction* generating_function, ScopeObject* generating_scope, Bytecode::RegisterWindow frame)
{
    auto object = global_object.heap().allocate<GeneratorObject>(global_object, global_object);
    object->m_generating_function = generating_function;
    object->m_scope = generating_scope;
    object->m_frame = move(frame);
    object->m_previous_value = initial_value;
    return object;
}

GeneratorObject::GeneratorObject(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void GeneratorObject::initialize(GlobalObject& global_object)
{
    // FIXME: These should be on a separate Generator prototype object!
    // https://tc39.es/ecma262/#sec-generator-objects

    auto& vm = this->vm();
    Object::initialize(global_object);
    define_native_function(vm.names.next, next);
    define_native_function(vm.names.return_, return_);
    define_native_function(vm.names.throw_, throw_);
}

GeneratorObject::~GeneratorObject()
{
}

void GeneratorObject::visit_edges(Cell::Visitor& visitor)
{
    Object::visit_edges(visitor);
    visitor.visit(m_scope);
    visitor.visit(m_generating_function);
    if (m_previous_value.is_object())
        visitor.visit(&m_previous_value.as_object());
}

GeneratorObject* GeneratorObject::typed_this(VM& vm, GlobalObject& global_object)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};
    if (!is<GeneratorObject>(this_object)) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotA, "Generator");
        return nullptr;
    }
    return static_cast<GeneratorObject*>(this_object);
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

    auto result = Object::create_empty(global_object);
    result->put("value", previous_generated_value);

    if (m_done) {
        result->put("done", Value(true));
        return result;
    }

    // Extract the continuation
    auto next_block = generated_continuation(m_previous_value);
    if (vm.exception())
        return {};

    if (!next_block) {
        // The generator has terminated, now we can simply return done=true.
        m_done = true;
        result->put("done", Value(true));
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

    // Temporarily switch to the captured scope
    TemporaryChange change { vm.call_frame().scope, m_scope };

    m_previous_value = bytecode_interpreter->run(*m_generating_function->bytecode_executable(), next_block);

    bytecode_interpreter->leave_frame();

    m_done = generated_continuation(m_previous_value) == nullptr;

    result->put("value", generated_value(m_previous_value));
    result->put("done", Value(m_done));

    if (vm.exception())
        return {};

    return result;
}

JS_DEFINE_NATIVE_FUNCTION(GeneratorObject::next)
{
    auto object = typed_this(vm, global_object);
    if (!object)
        return {};
    return object->next_impl(vm, global_object, {});
}

JS_DEFINE_NATIVE_FUNCTION(GeneratorObject::return_)
{
    auto object = typed_this(vm, global_object);
    if (!object)
        return {};
    object->m_done = true;
    return object->next_impl(vm, global_object, {});
}

JS_DEFINE_NATIVE_FUNCTION(GeneratorObject::throw_)
{
    auto object = typed_this(vm, global_object);
    if (!object)
        return {};
    return object->next_impl(vm, global_object, vm.argument(0));
}

}
