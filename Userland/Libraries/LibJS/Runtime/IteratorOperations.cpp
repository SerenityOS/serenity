/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
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
ThrowCompletionOr<Iterator> get_iterator(GlobalObject& global_object, Value value, IteratorHint hint, Optional<Value> method)
{
    auto& vm = global_object.vm();

    // 1. If hint is not present, set hint to sync.

    // 2. If method is not present, then
    if (!method.has_value()) {
        // a. If hint is async, then
        if (hint == IteratorHint::Async) {
            // i. Set method to ? GetMethod(obj, @@asyncIterator).
            auto* async_method = TRY(value.get_method(global_object, *vm.well_known_symbol_async_iterator()));

            // ii. If method is undefined, then
            if (async_method == nullptr) {
                // 1. Let syncMethod be ? GetMethod(obj, @@iterator).
                auto* sync_method = TRY(value.get_method(global_object, *vm.well_known_symbol_iterator()));

                // 2. Let syncIteratorRecord be ? GetIterator(obj, sync, syncMethod).
                auto sync_iterator_record = TRY(get_iterator(global_object, value, IteratorHint::Sync, sync_method));

                // 3. Return ! CreateAsyncFromSyncIterator(syncIteratorRecord).
                return MUST(create_async_from_sync_iterator(global_object, sync_iterator_record));
            }

            method = Value(async_method);
        }
        // b. Otherwise, set method to ? GetMethod(obj, @@iterator).
        else {
            method = TRY(value.get_method(global_object, *vm.well_known_symbol_iterator()));
        }
    }

    // NOTE: Additional type check to produce a better error message than Call().
    if (!method->is_function())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotIterable, value.to_string_without_side_effects());

    // 3. Let iterator be ? Call(method, obj).
    auto iterator = TRY(call(global_object, *method, value));

    // 4. If Type(iterator) is not Object, throw a TypeError exception.
    if (!iterator.is_object())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotIterable, value.to_string_without_side_effects());

    // 5. Let nextMethod be ? GetV(iterator, "next").
    auto next_method = TRY(iterator.get(global_object, vm.names.next));

    // 6. Let iteratorRecord be the Record { [[Iterator]]: iterator, [[NextMethod]]: nextMethod, [[Done]]: false }.
    auto iterator_record = Iterator { .iterator = &iterator.as_object(), .next_method = next_method, .done = false };

    // 7. Return iteratorRecord.
    return iterator_record;
}

// 7.4.2 IteratorNext ( iteratorRecord [ , value ] ), https://tc39.es/ecma262/#sec-iteratornext
ThrowCompletionOr<Object*> iterator_next(GlobalObject& global_object, Iterator const& iterator_record, Optional<Value> value)
{
    auto& vm = global_object.vm();

    Value result;

    // 1. If value is not present, then
    if (!value.has_value()) {
        // a. Let result be ? Call(iteratorRecord.[[NextMethod]], iteratorRecord.[[Iterator]]).
        result = TRY(call(global_object, iterator_record.next_method, iterator_record.iterator));
    } else {
        // a. Let result be ? Call(iteratorRecord.[[NextMethod]], iteratorRecord.[[Iterator]], « value »).
        result = TRY(call(global_object, iterator_record.next_method, iterator_record.iterator, *value));
    }

    // 3. If Type(result) is not Object, throw a TypeError exception.
    if (!result.is_object())
        return vm.throw_completion<TypeError>(global_object, ErrorType::IterableNextBadReturn);

    // 4. Return result.
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
ThrowCompletionOr<Object*> iterator_step(GlobalObject& global_object, Iterator const& iterator_record)
{
    // 1. Let result be ? IteratorNext(iteratorRecord).
    auto* result = TRY(iterator_next(global_object, iterator_record));

    // 2. Let done be ? IteratorComplete(result).
    auto done = TRY(iterator_complete(global_object, *result));

    // 3. If done is true, return false.
    if (done)
        return nullptr;

    // 4. Return result.
    return result;
}

// 7.4.6 IteratorClose ( iteratorRecord, completion ), https://tc39.es/ecma262/#sec-iteratorclose
// 7.4.8 AsyncIteratorClose ( iteratorRecord, completion ), https://tc39.es/ecma262/#sec-asynciteratorclose
// NOTE: These only differ in that async awaits the inner value after the call.
static Completion iterator_close_impl(GlobalObject& global_object, Iterator const& iterator_record, Completion completion, IteratorHint iterator_hint)
{
    auto& vm = global_object.vm();

    // 1. Assert: Type(iteratorRecord.[[Iterator]]) is Object.

    // 2. Let iterator be iteratorRecord.[[Iterator]].
    auto* iterator = iterator_record.iterator;

    // The callers of iterator_close() are often in an exceptional state.
    // Temporarily clear that exception for invocation(s) to Call.
    TemporaryClearException clear_exception(vm);

    // 3. Let innerResult be GetMethod(iterator, "return").
    auto inner_result = ThrowCompletionOr<Value> { js_undefined() };
    auto get_method_result = Value(iterator).get_method(global_object, vm.names.return_);
    if (get_method_result.is_error())
        inner_result = get_method_result.release_error();

    // 4. If innerResult.[[Type]] is normal, then
    if (!inner_result.is_error()) {
        // a. Let return be innerResult.[[Value]].
        auto* return_method = get_method_result.value();

        // b. If return is undefined, return Completion(completion).
        if (!return_method)
            return completion;

        // c. Set innerResult to Call(return, iterator).
        inner_result = call(global_object, return_method, iterator);

        // Note: If this is AsyncIteratorClose perform one extra step.
        if (iterator_hint == IteratorHint::Async && !inner_result.is_error()) {
            // d. If innerResult.[[Type]] is normal, set innerResult to Await(innerResult.[[Value]]).
            inner_result = await(global_object, inner_result.value());
        }
    }

    // 5. If completion.[[Type]] is throw, return Completion(completion).
    if (completion.is_error())
        return completion;

    // 6. If innerResult.[[Type]] is throw, return Completion(innerResult).
    if (inner_result.is_throw_completion())
        return inner_result;

    // 7. If Type(innerResult.[[Value]]) is not Object, throw a TypeError exception.
    if (!inner_result.value().is_object())
        return vm.throw_completion<TypeError>(global_object, ErrorType::IterableReturnBadReturn);

    // 8. Return Completion(completion).
    return completion;
}

// 7.4.6 IteratorClose ( iteratorRecord, completion ), https://tc39.es/ecma262/#sec-iteratorclose
Completion iterator_close(GlobalObject& global_object, Iterator const& iterator_record, Completion completion)
{
    return iterator_close_impl(global_object, iterator_record, move(completion), IteratorHint::Sync);
}

// 7.4.8 AsyncIteratorClose ( iteratorRecord, completion ), https://tc39.es/ecma262/#sec-asynciteratorclose
Completion async_iterator_close(GlobalObject& global_object, Iterator const& iterator_record, Completion completion)
{
    return iterator_close_impl(global_object, iterator_record, move(completion), IteratorHint::Async);
}

// 7.4.9 CreateIterResultObject ( value, done ), https://tc39.es/ecma262/#sec-createiterresultobject
Object* create_iterator_result_object(GlobalObject& global_object, Value value, bool done)
{
    auto& vm = global_object.vm();

    // 1. Let obj be ! OrdinaryObjectCreate(%Object.prototype%).
    auto* object = Object::create(global_object, global_object.object_prototype());

    // 2. Perform ! CreateDataPropertyOrThrow(obj, "value", value).
    MUST(object->create_data_property_or_throw(vm.names.value, value));

    // 3. Perform ! CreateDataPropertyOrThrow(obj, "done", done).
    MUST(object->create_data_property_or_throw(vm.names.done, Value(done)));

    // 4. Return obj.
    return object;
}

// 7.4.11 IterableToList ( items [ , method ] ), https://tc39.es/ecma262/#sec-iterabletolist
ThrowCompletionOr<MarkedValueList> iterable_to_list(GlobalObject& global_object, Value iterable, Optional<Value> method)
{
    auto& vm = global_object.vm();
    MarkedValueList values(vm.heap());

    (void)TRY(get_iterator_values(
        global_object, iterable, [&](auto value) -> Optional<Completion> {
            values.append(value);
            return {};
        },
        move(method)));

    return { move(values) };
}

// Non-standard
Completion get_iterator_values(GlobalObject& global_object, Value iterable, IteratorValueCallback callback, Optional<Value> method)
{
    auto iterator_record = TRY(get_iterator(global_object, iterable, IteratorHint::Sync, move(method)));

    while (true) {
        auto* next_object = TRY(iterator_step(global_object, iterator_record));
        if (!next_object)
            return {};

        auto next_value = TRY(iterator_value(global_object, *next_object));

        if (auto completion = callback(next_value); completion.has_value())
            return iterator_close(global_object, iterator_record, completion.release_value());
    }
}

}
