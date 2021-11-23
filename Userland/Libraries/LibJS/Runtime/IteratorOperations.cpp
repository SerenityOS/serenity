/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/AsyncFromSyncIteratorPrototype.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/IteratorOperations.h>
#include <LibJS/Runtime/TemporaryClearException.h>

namespace JS {

// 7.4.1 GetIterator ( obj [ , hint [ , method ] ] ), https://tc39.es/ecma262/#sec-getiterator
ThrowCompletionOr<Object*> get_iterator(GlobalObject& global_object, Value value, IteratorHint hint, Value method)
{
    auto& vm = global_object.vm();
    if (method.is_empty()) {
        if (hint == IteratorHint::Async) {
            auto* async_method = TRY(value.get_method(global_object, *vm.well_known_symbol_async_iterator()));
            if (async_method == nullptr) {
                auto* sync_method = TRY(value.get_method(global_object, *vm.well_known_symbol_iterator()));
                auto* sync_iterator_record = TRY(get_iterator(global_object, value, IteratorHint::Sync, sync_method));
                return TRY(create_async_from_sync_iterator(global_object, *sync_iterator_record));
            }
            method = Value(async_method);
        } else {
            method = TRY(value.get_method(global_object, *vm.well_known_symbol_iterator()));
        }
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
// 7.4.8 AsyncIteratorClose ( iteratorRecord, completion ), https://tc39.es/ecma262/#sec-asynciteratorclose
// NOTE: These only differ in that async awaits the inner value after the call.
static Completion iterator_close_(Object& iterator, Completion completion, IteratorHint iterator_hint)
{
    auto& vm = iterator.vm();
    auto& global_object = iterator.global_object();

    // The callers of iterator_close() are often in an exceptional state.
    // Temporarily clear that exception for invocation(s) to Call.
    TemporaryClearException clear_exception(vm);

    // 3. Let innerResult be GetMethod(iterator, "return").
    auto inner_result_or_error = Value(&iterator).get_method(global_object, vm.names.return_);
    Value inner_result;

    // 4. If innerResult.[[Type]] is normal, then
    if (!inner_result_or_error.is_error()) {
        // a. Let return be innerResult.[[Value]].
        auto* return_method = inner_result_or_error.release_value();

        // b. If return is undefined, return Completion(completion).
        if (!return_method)
            return completion;

        vm.stop_unwind();

        // c. Set innerResult to Call(return, iterator).
        auto result_or_error = vm.call(*return_method, &iterator);
        if (result_or_error.is_error()) {
            inner_result_or_error = result_or_error.release_error();
        } else {
            inner_result = result_or_error.release_value();
            // Note: If this is AsyncIteratorClose perform one extra step.
            if (iterator_hint == IteratorHint::Async) {
                // d. If innerResult.[[Type]] is normal, set innerResult to Await(innerResult.[[Value]]).
                inner_result = TRY(await(global_object, inner_result));
            }
        }
    }

    // 5. If completion.[[Type]] is throw, return Completion(completion).
    if (completion.is_error())
        return completion;

    // 6. If innerResult.[[Type]] is throw, return Completion(innerResult).
    if (inner_result_or_error.is_error())
        return inner_result_or_error.release_error();

    // 7. If Type(innerResult.[[Value]]) is not Object, throw a TypeError exception.
    if (!inner_result.is_object())
        return vm.throw_completion<TypeError>(global_object, ErrorType::IterableReturnBadReturn);

    // 8. Return Completion(completion).
    return completion;
}

// 7.4.6 IteratorClose ( iteratorRecord, completion ), https://tc39.es/ecma262/#sec-iteratorclose
Completion iterator_close(Object& iterator, Completion completion)
{
    return iterator_close_(iterator, move(completion), IteratorHint::Sync);
}

// 7.4.8 AsyncIteratorClose ( iteratorRecord, completion ), https://tc39.es/ecma262/#sec-asynciteratorclose
Completion async_iterator_close(Object& iterator, Completion completion)
{
    return iterator_close_(iterator, move(completion), IteratorHint::Async);
}

// 7.4.9 CreateIterResultObject ( value, done ), https://tc39.es/ecma262/#sec-createiterresultobject
Object* create_iterator_result_object(GlobalObject& global_object, Value value, bool done)
{
    auto& vm = global_object.vm();
    auto* object = Object::create(global_object, global_object.object_prototype());
    MUST(object->create_data_property_or_throw(vm.names.value, value));
    MUST(object->create_data_property_or_throw(vm.names.done, Value(done)));
    return object;
}

// 7.4.11 IterableToList ( items [ , method ] ), https://tc39.es/ecma262/#sec-iterabletolist
ThrowCompletionOr<MarkedValueList> iterable_to_list(GlobalObject& global_object, Value iterable, Value method)
{
    auto& vm = global_object.vm();
    MarkedValueList values(vm.heap());

    TRY(get_iterator_values(
        global_object, iterable, [&](auto value) -> Optional<Completion> {
            values.append(value);
            return {};
        },
        method));

    return { move(values) };
}

Completion get_iterator_values(GlobalObject& global_object, Value iterable, IteratorValueCallback callback, Value method)
{
    auto* iterator = TRY(get_iterator(global_object, iterable, IteratorHint::Sync, method));

    while (true) {
        auto* next_object = TRY(iterator_step(global_object, *iterator));
        if (!next_object)
            return {};

        auto next_value = TRY(iterator_value(global_object, *next_object));

        if (auto completion = callback(next_value); completion.has_value())
            return iterator_close(*iterator, completion.release_value());
    }
}

}
