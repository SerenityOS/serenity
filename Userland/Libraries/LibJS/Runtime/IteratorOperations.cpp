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

namespace JS {

// 7.4.2 GetIterator ( obj [ , hint [ , method ] ] ), https://tc39.es/ecma262/#sec-getiterator
ThrowCompletionOr<Iterator> get_iterator(VM& vm, Value value, IteratorHint hint, Optional<Value> method)
{
    // 1. If hint is not present, set hint to sync.

    // 2. If method is not present, then
    if (!method.has_value()) {
        // a. If hint is async, then
        if (hint == IteratorHint::Async) {
            // i. Set method to ? GetMethod(obj, @@asyncIterator).
            auto* async_method = TRY(value.get_method(vm, *vm.well_known_symbol_async_iterator()));

            // ii. If method is undefined, then
            if (async_method == nullptr) {
                // 1. Let syncMethod be ? GetMethod(obj, @@iterator).
                auto* sync_method = TRY(value.get_method(vm, *vm.well_known_symbol_iterator()));

                // 2. Let syncIteratorRecord be ? GetIterator(obj, sync, syncMethod).
                auto sync_iterator_record = TRY(get_iterator(vm, value, IteratorHint::Sync, sync_method));

                // 3. Return CreateAsyncFromSyncIterator(syncIteratorRecord).
                return create_async_from_sync_iterator(vm, sync_iterator_record);
            }

            method = Value(async_method);
        }
        // b. Otherwise, set method to ? GetMethod(obj, @@iterator).
        else {
            method = TRY(value.get_method(vm, *vm.well_known_symbol_iterator()));
        }
    }

    // NOTE: Additional type check to produce a better error message than Call().
    if (!method->is_function())
        return vm.throw_completion<TypeError>(ErrorType::NotIterable, TRY_OR_THROW_OOM(vm, value.to_string_without_side_effects()));

    // 3. Let iterator be ? Call(method, obj).
    auto iterator = TRY(call(vm, *method, value));

    // 4. If Type(iterator) is not Object, throw a TypeError exception.
    if (!iterator.is_object())
        return vm.throw_completion<TypeError>(ErrorType::NotIterable, TRY_OR_THROW_OOM(vm, value.to_string_without_side_effects()));

    // 5. Let nextMethod be ? GetV(iterator, "next").
    auto next_method = TRY(iterator.get(vm, vm.names.next));

    // 6. Let iteratorRecord be the Iterator Record { [[Iterator]]: iterator, [[NextMethod]]: nextMethod, [[Done]]: false }.
    auto iterator_record = Iterator { .iterator = &iterator.as_object(), .next_method = next_method, .done = false };

    // 7. Return iteratorRecord.
    return iterator_record;
}

// 7.4.3 IteratorNext ( iteratorRecord [ , value ] ), https://tc39.es/ecma262/#sec-iteratornext
ThrowCompletionOr<Object*> iterator_next(VM& vm, Iterator const& iterator_record, Optional<Value> value)
{
    Value result;

    // 1. If value is not present, then
    if (!value.has_value()) {
        // a. Let result be ? Call(iteratorRecord.[[NextMethod]], iteratorRecord.[[Iterator]]).
        result = TRY(call(vm, iterator_record.next_method, iterator_record.iterator));
    } else {
        // a. Let result be ? Call(iteratorRecord.[[NextMethod]], iteratorRecord.[[Iterator]], « value »).
        result = TRY(call(vm, iterator_record.next_method, iterator_record.iterator, *value));
    }

    // 3. If Type(result) is not Object, throw a TypeError exception.
    if (!result.is_object())
        return vm.throw_completion<TypeError>(ErrorType::IterableNextBadReturn);

    // 4. Return result.
    return &result.as_object();
}

// 7.4.4 IteratorComplete ( iterResult ), https://tc39.es/ecma262/#sec-iteratorcomplete
ThrowCompletionOr<bool> iterator_complete(VM& vm, Object& iterator_result)
{
    // 1. Return ToBoolean(? Get(iterResult, "done")).
    return TRY(iterator_result.get(vm.names.done)).to_boolean();
}

// 7.4.5 IteratorValue ( iterResult ), https://tc39.es/ecma262/#sec-iteratorvalue
ThrowCompletionOr<Value> iterator_value(VM& vm, Object& iterator_result)
{
    // 1. Return ? Get(iterResult, "value").
    return TRY(iterator_result.get(vm.names.value));
}

// 7.4.6 IteratorStep ( iteratorRecord ), https://tc39.es/ecma262/#sec-iteratorstep
ThrowCompletionOr<Object*> iterator_step(VM& vm, Iterator const& iterator_record)
{
    // 1. Let result be ? IteratorNext(iteratorRecord).
    auto* result = TRY(iterator_next(vm, iterator_record));

    // 2. Let done be ? IteratorComplete(result).
    auto done = TRY(iterator_complete(vm, *result));

    // 3. If done is true, return false.
    if (done)
        return nullptr;

    // 4. Return result.
    return result;
}

// 7.4.7 IteratorClose ( iteratorRecord, completion ), https://tc39.es/ecma262/#sec-iteratorclose
// 7.4.9 AsyncIteratorClose ( iteratorRecord, completion ), https://tc39.es/ecma262/#sec-asynciteratorclose
// NOTE: These only differ in that async awaits the inner value after the call.
static Completion iterator_close_impl(VM& vm, Iterator const& iterator_record, Completion completion, IteratorHint iterator_hint)
{
    // 1. Assert: Type(iteratorRecord.[[Iterator]]) is Object.

    // 2. Let iterator be iteratorRecord.[[Iterator]].
    auto iterator = iterator_record.iterator;

    // 3. Let innerResult be Completion(GetMethod(iterator, "return")).
    auto inner_result = ThrowCompletionOr<Value> { js_undefined() };
    auto get_method_result = Value(iterator).get_method(vm, vm.names.return_);
    if (get_method_result.is_error())
        inner_result = get_method_result.release_error();

    // 4. If innerResult.[[Type]] is normal, then
    if (!inner_result.is_error()) {
        // a. Let return be innerResult.[[Value]].
        auto* return_method = get_method_result.value();

        // b. If return is undefined, return ? completion.
        if (!return_method)
            return completion;

        // c. Set innerResult to Completion(Call(return, iterator)).
        inner_result = call(vm, return_method, iterator);

        // Note: If this is AsyncIteratorClose perform one extra step.
        if (iterator_hint == IteratorHint::Async && !inner_result.is_error()) {
            // d. If innerResult.[[Type]] is normal, set innerResult to Completion(Await(innerResult.[[Value]])).
            inner_result = await(vm, inner_result.value());
        }
    }

    // 5. If completion.[[Type]] is throw, return ? completion.
    if (completion.is_error())
        return completion;

    // 6. If innerResult.[[Type]] is throw, return ? innerResult.
    if (inner_result.is_throw_completion())
        return inner_result;

    // 7. If Type(innerResult.[[Value]]) is not Object, throw a TypeError exception.
    if (!inner_result.value().is_object())
        return vm.throw_completion<TypeError>(ErrorType::IterableReturnBadReturn);

    // 8. Return ? completion.
    return completion;
}

// 7.4.7 IteratorClose ( iteratorRecord, completion ), https://tc39.es/ecma262/#sec-iteratorclose
Completion iterator_close(VM& vm, Iterator const& iterator_record, Completion completion)
{
    return iterator_close_impl(vm, iterator_record, move(completion), IteratorHint::Sync);
}

// 7.4.9 AsyncIteratorClose ( iteratorRecord, completion ), https://tc39.es/ecma262/#sec-asynciteratorclose
Completion async_iterator_close(VM& vm, Iterator const& iterator_record, Completion completion)
{
    return iterator_close_impl(vm, iterator_record, move(completion), IteratorHint::Async);
}

// 7.4.10 CreateIterResultObject ( value, done ), https://tc39.es/ecma262/#sec-createiterresultobject
Object* create_iterator_result_object(VM& vm, Value value, bool done)
{
    auto& realm = *vm.current_realm();

    // 1. Let obj be OrdinaryObjectCreate(%Object.prototype%).
    auto object = Object::create(realm, realm.intrinsics().object_prototype());

    // 2. Perform ! CreateDataPropertyOrThrow(obj, "value", value).
    MUST(object->create_data_property_or_throw(vm.names.value, value));

    // 3. Perform ! CreateDataPropertyOrThrow(obj, "done", done).
    MUST(object->create_data_property_or_throw(vm.names.done, Value(done)));

    // 4. Return obj.
    return object;
}

// 7.4.12 IterableToList ( items [ , method ] ), https://tc39.es/ecma262/#sec-iterabletolist
ThrowCompletionOr<MarkedVector<Value>> iterable_to_list(VM& vm, Value iterable, Optional<Value> method)
{
    MarkedVector<Value> values(vm.heap());

    (void)TRY(get_iterator_values(
        vm, iterable, [&](auto value) -> Optional<Completion> {
            values.append(value);
            return {};
        },
        move(method)));

    return { move(values) };
}

// Non-standard
Completion get_iterator_values(VM& vm, Value iterable, IteratorValueCallback callback, Optional<Value> method)
{
    auto iterator_record = TRY(get_iterator(vm, iterable, IteratorHint::Sync, move(method)));

    while (true) {
        auto* next_object = TRY(iterator_step(vm, iterator_record));
        if (!next_object)
            return {};

        auto next_value = TRY(iterator_value(vm, *next_object));

        if (auto completion = callback(next_value); completion.has_value())
            return iterator_close(vm, iterator_record, completion.release_value());
    }
}

}
