/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2022-2023, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/AsyncFromSyncIteratorPrototype.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibJS/Runtime/Iterator.h>
#include <LibJS/Runtime/VM.h>

namespace JS {

NonnullGCPtr<Iterator> Iterator::create(Realm& realm, Object& prototype, IteratorRecord iterated)
{
    return realm.heap().allocate<Iterator>(realm, prototype, move(iterated));
}

Iterator::Iterator(Object& prototype, IteratorRecord iterated)
    : Object(ConstructWithPrototypeTag::Tag, prototype)
    , m_iterated(move(iterated))
{
}

Iterator::Iterator(Object& prototype)
    : Iterator(prototype, {})
{
}

// 7.4.2 GetIteratorFromMethod ( obj, method ), https://tc39.es/ecma262/#sec-getiteratorfrommethod
ThrowCompletionOr<IteratorRecord> get_iterator_from_method(VM& vm, Value object, NonnullGCPtr<FunctionObject> method)
{
    // 1. Let iterator be ? Call(method, obj).
    auto iterator = TRY(call(vm, *method, object));

    // 2. If iterator is not an Object, throw a TypeError exception.
    if (!iterator.is_object())
        return vm.throw_completion<TypeError>(ErrorType::NotIterable, object.to_string_without_side_effects());

    // 3. Let nextMethod be ? Get(iterator, "next").
    auto next_method = TRY(iterator.get(vm, vm.names.next));

    // 4. Let iteratorRecord be the Iterator Record { [[Iterator]]: iterator, [[NextMethod]]: nextMethod, [[Done]]: false }.
    auto iterator_record = IteratorRecord { .iterator = &iterator.as_object(), .next_method = next_method, .done = false };

    // 5. Return iteratorRecord.
    return iterator_record;
}

// 7.4.3 GetIterator ( obj, kind ), https://tc39.es/ecma262/#sec-getiterator
ThrowCompletionOr<IteratorRecord> get_iterator(VM& vm, Value object, IteratorHint kind)
{
    JS::GCPtr<FunctionObject> method;

    // 1. If kind is async, then
    if (kind == IteratorHint::Async) {
        // a. Let method be ? GetMethod(obj, @@asyncIterator).
        method = TRY(object.get_method(vm, vm.well_known_symbol_async_iterator()));

        // b. If method is undefined, then
        if (!method) {
            // i. Let syncMethod be ? GetMethod(obj, @@iterator).
            auto sync_method = TRY(object.get_method(vm, vm.well_known_symbol_iterator()));

            // ii. If syncMethod is undefined, throw a TypeError exception.
            if (!sync_method)
                return vm.throw_completion<TypeError>(ErrorType::NotIterable, object.to_string_without_side_effects());

            // iii. Let syncIteratorRecord be ? GetIteratorFromMethod(obj, syncMethod).
            auto sync_iterator_record = TRY(get_iterator_from_method(vm, object, *sync_method));

            // iv. Return CreateAsyncFromSyncIterator(syncIteratorRecord).
            return create_async_from_sync_iterator(vm, sync_iterator_record);
        }
    }
    // 2. Else,
    else {
        // a. Let method be ? GetMethod(obj, @@iterator).
        method = TRY(object.get_method(vm, vm.well_known_symbol_iterator()));
    }

    // 3. If method is undefined, throw a TypeError exception.
    if (!method)
        return vm.throw_completion<TypeError>(ErrorType::NotIterable, object.to_string_without_side_effects());

    // 4. Return ? GetIteratorFromMethod(obj, method).
    return TRY(get_iterator_from_method(vm, object, *method));
}

// 2.1.1 GetIteratorDirect ( obj ), https://tc39.es/proposal-iterator-helpers/#sec-getiteratorflattenable
ThrowCompletionOr<IteratorRecord> get_iterator_direct(VM& vm, Object& object)
{
    // 1. Let nextMethod be ? Get(obj, "next").
    auto next_method = TRY(object.get(vm.names.next));

    // 2. Let iteratorRecord be Record { [[Iterator]]: obj, [[NextMethod]]: nextMethod, [[Done]]: false }.
    IteratorRecord iterator_record { .iterator = object, .next_method = next_method, .done = false };

    // 3. Return iteratorRecord.
    return iterator_record;
}

// 2.1.2 GetIteratorFlattenable ( obj, stringHandling ), https://tc39.es/proposal-iterator-helpers/#sec-getiteratorflattenable
ThrowCompletionOr<IteratorRecord> get_iterator_flattenable(VM& vm, Value object, StringHandling string_handling)
{
    // 1. If obj is not an Object, then
    if (!object.is_object()) {
        // a. If stringHandling is reject-strings or obj is not a String, throw a TypeError exception.
        if (string_handling == StringHandling::RejectStrings || !object.is_string())
            return vm.throw_completion<TypeError>(ErrorType::NotAnObject, object.to_string_without_side_effects());
    }

    // 2. Let method be ? GetMethod(obj, @@iterator).
    auto method = TRY(object.get_method(vm, vm.well_known_symbol_iterator()));

    Value iterator;

    // 3. If method is undefined, then
    if (!method) {
        // a. Let iterator be obj.
        iterator = object;
    }
    // 4. Else,
    else {
        // a. Let iterator be ? Call(method, obj).
        iterator = TRY(call(vm, method, object));
    }

    // 5. If iterator is not an Object, throw a TypeError exception.
    if (!iterator.is_object())
        return vm.throw_completion<TypeError>(ErrorType::NotAnObject, iterator.to_string_without_side_effects());

    // 6. Return ? GetIteratorDirect(iterator).
    return TRY(get_iterator_direct(vm, iterator.as_object()));
}

// 7.4.4 IteratorNext ( iteratorRecord [ , value ] ), https://tc39.es/ecma262/#sec-iteratornext
ThrowCompletionOr<NonnullGCPtr<Object>> iterator_next(VM& vm, IteratorRecord const& iterator_record, Optional<Value> value)
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
    return result.as_object();
}

// 7.4.5 IteratorComplete ( iterResult ), https://tc39.es/ecma262/#sec-iteratorcomplete
ThrowCompletionOr<bool> iterator_complete(VM& vm, Object& iterator_result)
{
    // 1. Return ToBoolean(? Get(iterResult, "done")).
    return TRY(iterator_result.get(vm.names.done)).to_boolean();
}

// 7.4.6 IteratorValue ( iterResult ), https://tc39.es/ecma262/#sec-iteratorvalue
ThrowCompletionOr<Value> iterator_value(VM& vm, Object& iterator_result)
{
    // 1. Return ? Get(iterResult, "value").
    return TRY(iterator_result.get(vm.names.value));
}

// 7.4.7 IteratorStep ( iteratorRecord ), https://tc39.es/ecma262/#sec-iteratorstep
ThrowCompletionOr<GCPtr<Object>> iterator_step(VM& vm, IteratorRecord const& iterator_record)
{
    // 1. Let result be ? IteratorNext(iteratorRecord).
    auto result = TRY(iterator_next(vm, iterator_record));

    // 2. Let done be ? IteratorComplete(result).
    auto done = TRY(iterator_complete(vm, result));

    // 3. If done is true, return false.
    if (done)
        return nullptr;

    // 4. Return result.
    return result;
}

// 7.4.8 IteratorClose ( iteratorRecord, completion ), https://tc39.es/ecma262/#sec-iteratorclose
// 7.4.10 AsyncIteratorClose ( iteratorRecord, completion ), https://tc39.es/ecma262/#sec-asynciteratorclose
// NOTE: These only differ in that async awaits the inner value after the call.
static Completion iterator_close_impl(VM& vm, IteratorRecord const& iterator_record, Completion completion, IteratorHint iterator_hint)
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
        auto return_method = get_method_result.value();

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

// 7.4.8 IteratorClose ( iteratorRecord, completion ), https://tc39.es/ecma262/#sec-iteratorclose
Completion iterator_close(VM& vm, IteratorRecord const& iterator_record, Completion completion)
{
    return iterator_close_impl(vm, iterator_record, move(completion), IteratorHint::Sync);
}

// 7.4.10 AsyncIteratorClose ( iteratorRecord, completion ), https://tc39.es/ecma262/#sec-asynciteratorclose
Completion async_iterator_close(VM& vm, IteratorRecord const& iterator_record, Completion completion)
{
    return iterator_close_impl(vm, iterator_record, move(completion), IteratorHint::Async);
}

// 7.4.11 CreateIterResultObject ( value, done ), https://tc39.es/ecma262/#sec-createiterresultobject
NonnullGCPtr<Object> create_iterator_result_object(VM& vm, Value value, bool done)
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

// 7.4.13 IteratorToList ( iteratorRecord ), https://tc39.es/ecma262/#sec-iteratortolist
ThrowCompletionOr<MarkedVector<Value>> iterator_to_list(VM& vm, IteratorRecord const& iterator_record)
{
    // 1. Let values be a new empty List.
    MarkedVector<Value> values(vm.heap());

    // 2. Let next be true.
    GCPtr<Object> next;

    // 3. Repeat, while next is not false,
    do {
        // a. Set next to ? IteratorStep(iteratorRecord).
        next = TRY(iterator_step(vm, iterator_record));

        // b. If next is not false, then
        if (next) {
            // i. Let nextValue be ? IteratorValue(next).
            auto next_value = TRY(iterator_value(vm, *next));

            // ii. Append nextValue to values.
            TRY_OR_THROW_OOM(vm, values.try_append(next_value));
        }
    } while (next);

    // 4. Return values.
    return values;
}

// Non-standard
Completion get_iterator_values(VM& vm, Value iterable, IteratorValueCallback callback)
{
    auto iterator_record = TRY(get_iterator(vm, iterable, IteratorHint::Sync));

    while (true) {
        auto next_object = TRY(iterator_step(vm, iterator_record));
        if (!next_object)
            return {};

        auto next_value = TRY(iterator_value(vm, *next_object));

        if (auto completion = callback(next_value); completion.has_value())
            return iterator_close(vm, iterator_record, completion.release_value());
    }
}

}
