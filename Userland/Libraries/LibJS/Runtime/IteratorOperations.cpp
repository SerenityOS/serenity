/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/IteratorOperations.h>

namespace JS {

// 7.4.1 GetIterator ( obj [ , hint [ , method ] ] ), https://tc39.es/ecma262/#sec-getiterator
Object* get_iterator(GlobalObject& global_object, Value value, IteratorHint hint, Value method)
{
    auto& vm = global_object.vm();
    if (method.is_empty()) {
        if (hint == IteratorHint::Async)
            TODO();
        auto object = value.to_object(global_object);
        if (!object)
            return {};
        method = object->get(global_object.vm().well_known_symbol_iterator());
        if (vm.exception())
            return {};
    }
    if (!method.is_function()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotIterable, value.to_string_without_side_effects());
        return nullptr;
    }
    auto iterator = vm.call(method.as_function(), value);
    if (vm.exception())
        return {};
    if (!iterator.is_object()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotIterable, value.to_string_without_side_effects());
        return nullptr;
    }
    return &iterator.as_object();
}

// 7.4.2 IteratorNext ( iteratorRecord [ , value ] ), https://tc39.es/ecma262/#sec-iteratornext
Object* iterator_next(Object& iterator, Value value)
{
    auto& vm = iterator.vm();
    auto& global_object = iterator.global_object();
    auto next_method = iterator.get(vm.names.next);
    if (vm.exception())
        return {};

    if (!next_method.is_function()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::IterableNextNotAFunction);
        return nullptr;
    }

    Value result;
    if (value.is_empty())
        result = vm.call(next_method.as_function(), &iterator);
    else
        result = vm.call(next_method.as_function(), &iterator, value);

    if (vm.exception())
        return {};
    if (!result.is_object()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::IterableNextBadReturn);
        return nullptr;
    }

    return &result.as_object();
}

// 7.4.6 IteratorClose ( iteratorRecord, completion ), https://tc39.es/ecma262/#sec-iteratorclose
void iterator_close([[maybe_unused]] Object& iterator)
{
    TODO();
}

// 7.4.10 IterableToList ( items [ , method ] ), https://tc39.es/ecma262/#sec-iterabletolist
MarkedValueList iterable_to_list(GlobalObject& global_object, Value iterable, Value method)
{
    auto& vm = global_object.vm();
    MarkedValueList values(vm.heap());
    get_iterator_values(
        global_object, iterable, [&](auto value) {
            if (vm.exception())
                return IterationDecision::Break;
            values.append(value);
            return IterationDecision::Continue;
        },
        method);
    return values;
}

// 7.4.8 CreateIterResultObject ( value, done ), https://tc39.es/ecma262/#sec-createiterresultobject
Value create_iterator_result_object(GlobalObject& global_object, Value value, bool done)
{
    auto& vm = global_object.vm();
    auto* object = Object::create_empty(global_object);
    object->define_property(vm.names.value, value);
    object->define_property(vm.names.done, Value(done));
    return object;
}

void get_iterator_values(GlobalObject& global_object, Value value, AK::Function<IterationDecision(Value)> callback, Value method)
{
    auto& vm = global_object.vm();

    auto iterator = get_iterator(global_object, value, IteratorHint::Sync, method);
    if (!iterator)
        return;

    while (true) {
        auto next_object = iterator_next(*iterator);
        if (!next_object)
            return;

        auto done_property = next_object->get(vm.names.done);
        if (vm.exception())
            return;

        if (!done_property.is_empty() && done_property.to_boolean())
            return;

        auto next_value = next_object->get(vm.names.value);
        if (vm.exception())
            return;

        auto result = callback(next_value);
        if (result == IterationDecision::Break)
            return;
        VERIFY(result == IterationDecision::Continue);
    }
}

}
