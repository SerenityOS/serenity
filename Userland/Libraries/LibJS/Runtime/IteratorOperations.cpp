/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/IteratorOperations.h>

namespace JS {

// 7.4.1 GetIterator ( obj [ , hint [ , method ] ] ), https://tc39.es/ecma262/#sec-getiterator
ThrowCompletionOr<Object*> get_iterator(GlobalObject& global_object, Value value, IteratorHint hint, Value method)
{
    auto& vm = global_object.vm();
    if (method.is_empty()) {
        if (hint == IteratorHint::Async)
            TODO();

        method = TRY(value.get_method(global_object, *vm.well_known_symbol_iterator()));
    }

    if (!method.is_function())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotIterable, value.to_string_without_side_effects());

    auto iterator = TRY(vm.call(method.as_function(), value));
    if (!iterator.is_object())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotIterable, value.to_string_without_side_effects());

    return &iterator.as_object();
}

// 7.4.2 IteratorNext ( iteratorRecord [ , value ] ), https://tc39.es/ecma262/#sec-iteratornext
ThrowCompletionOr<Object*> iterator_next(Object& iterator, Value value)
{
    // FIXME: Implement using iterator records, not ordinary objects
    auto& vm = iterator.vm();
    auto& global_object = iterator.global_object();

    auto next_method = TRY(iterator.get(vm.names.next));
    if (!next_method.is_function())
        return vm.throw_completion<TypeError>(global_object, ErrorType::IterableNextNotAFunction);

    Value result;
    if (value.is_empty())
        result = TRY(vm.call(next_method.as_function(), &iterator));
    else
        result = TRY(vm.call(next_method.as_function(), &iterator, value));

    if (!result.is_object())
        return vm.throw_completion<TypeError>(global_object, ErrorType::IterableNextBadReturn);

    return &result.as_object();
}

// 7.4.3 IteratorComplete ( iterResult ), https://tc39.es/ecma262/#sec-iteratorcomplete
ThrowCompletionOr<bool> iterator_complete(GlobalObject& global_object, Object& iterator_result)
{
    auto& vm = global_object.vm();

    // 1. Return ! ToBoolean(? Get(iterResult, "done")).
    return TRY(iterator_result.get(vm.names.done)).to_boolean();
}

// 7.4.4 IteratorValue ( iterResult ), https://tc39.es/ecma262/#sec-iteratorvalue
ThrowCompletionOr<Value> iterator_value(GlobalObject& global_object, Object& iterator_result)
{
    auto& vm = global_object.vm();

    // 1. Return ? Get(iterResult, "value").
    return TRY(iterator_result.get(vm.names.value));
}

// 7.4.5 IteratorStep ( iteratorRecord ), https://tc39.es/ecma262/#sec-iteratorstep
ThrowCompletionOr<Object*> iterator_step(GlobalObject& global_object, Object& iterator)
{
    auto* result = TRY(iterator_next(iterator));
    auto done = TRY(iterator_complete(global_object, *result));

    if (done)
        return nullptr;

    return result;
}

// 7.4.6 IteratorClose ( iteratorRecord, completion ), https://tc39.es/ecma262/#sec-iteratorclose
void iterator_close(Object& iterator)
{
    auto& vm = iterator.vm();
    auto& global_object = iterator.global_object();

    // Emulates `completion` behavior
    auto* completion_exception = vm.exception();
    vm.clear_exception();
    auto unwind_until = vm.unwind_until();
    auto unwind_until_label = vm.unwind_until_label();
    vm.stop_unwind();
    auto restore_completion = [&]() {
        if (completion_exception)
            vm.set_exception(*completion_exception);
        if (unwind_until != ScopeType::None)
            vm.unwind(unwind_until, unwind_until_label);
    };

    auto return_method_or_error = Value(&iterator).get_method(global_object, vm.names.return_);
    Value result;
    if (!return_method_or_error.is_error()) { // If innerResult.[[Type]] is normal, then
        auto return_method = return_method_or_error.release_value();
        if (!return_method)
            return restore_completion(); // If return is undefined, return Completion(completion).
        auto result_or_error = vm.call(*return_method, &iterator);
        if (result_or_error.is_error())
            return_method_or_error = result_or_error.release_error();
        else
            result = result_or_error.release_value();
    }
    if (completion_exception)
        return restore_completion(); // If completion.[[Type]] is throw, return Completion(completion).
    if (return_method_or_error.is_error())
        return; // If innerResult.[[Type]] is throw, return Completion(innerResult).
    if (!result.is_object()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::IterableReturnBadReturn);
        return; // If Type(innerResult.[[Value]]) is not Object, throw a TypeError exception.
    }
    restore_completion(); // Return Completion(completion).
}

// 7.4.8 CreateIterResultObject ( value, done ), https://tc39.es/ecma262/#sec-createiterresultobject
Object* create_iterator_result_object(GlobalObject& global_object, Value value, bool done)
{
    auto& vm = global_object.vm();
    auto* object = Object::create(global_object, global_object.object_prototype());
    MUST(object->create_data_property_or_throw(vm.names.value, value));
    MUST(object->create_data_property_or_throw(vm.names.done, Value(done)));
    return object;
}

// 7.4.10 IterableToList ( items [ , method ] ), https://tc39.es/ecma262/#sec-iterabletolist
MarkedValueList iterable_to_list(GlobalObject& global_object, Value iterable, Value method)
{
    auto& vm = global_object.vm();
    MarkedValueList values(vm.heap());

    (void)get_iterator_values(
        global_object, iterable, [&](auto value) -> Optional<Completion> {
            values.append(value);
            return {};
        },
        method);

    return values;
}

Completion get_iterator_values(GlobalObject& global_object, Value iterable, IteratorValueCallback callback, Value method)
{
    auto* iterator = TRY(get_iterator(global_object, iterable, IteratorHint::Sync, method));

    while (true) {
        auto* next_object = TRY(iterator_step(global_object, *iterator));
        if (!next_object)
            return {};

        auto next_value = TRY(iterator_value(global_object, *next_object));

        if (auto completion = callback(next_value); completion.has_value()) {
            iterator_close(*iterator);
            return completion.release_value();
        }
    }
}

}
