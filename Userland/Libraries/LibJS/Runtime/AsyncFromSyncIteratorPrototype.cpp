/*
 * Copyright (c) 2021, David Tuin <davidot@serenityos.org>
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AsyncFromSyncIteratorPrototype.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/IteratorOperations.h>
#include <LibJS/Runtime/Promise.h>
#include <LibJS/Runtime/PromiseConstructor.h>
#include <LibJS/Runtime/PromiseReaction.h>

namespace JS {

AsyncFromSyncIteratorPrototype::AsyncFromSyncIteratorPrototype(GlobalObject& global_object)
    : PrototypeObject(*global_object.async_iterator_prototype())
{
}

void AsyncFromSyncIteratorPrototype::initialize(GlobalObject& global_object)
{
    auto& vm = global_object.vm();
    Object::initialize(global_object);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.next, next, 1, attr);
    define_native_function(vm.names.return_, return_, 1, attr);
    define_native_function(vm.names.throw_, throw_, 1, attr);
}

// 27.1.4.4 AsyncFromSyncIteratorContinuation ( result, promiseCapability ), https://tc39.es/ecma262/#sec-asyncfromsynciteratorcontinuation
static Object* async_from_sync_iterator_continuation(GlobalObject& global_object, Object& result, PromiseCapability& promise_capability)
{
    // 1. NOTE: Because promiseCapability is derived from the intrinsic %Promise%, the calls to promiseCapability.[[Reject]] entailed by the use IfAbruptRejectPromise below are guaranteed not to throw.
    // 2. Let done be Completion(IteratorComplete(result)).
    // 3. IfAbruptRejectPromise(done, promiseCapability).
    auto done = TRY_OR_MUST_REJECT(global_object, promise_capability, iterator_complete(global_object, result));

    // 4. Let value be Completion(IteratorValue(result)).
    // 5. IfAbruptRejectPromise(value, promiseCapability).
    auto value = TRY_OR_MUST_REJECT(global_object, promise_capability, iterator_value(global_object, result));

    // 6. Let valueWrapper be PromiseResolve(%Promise%, value).
    // 7. IfAbruptRejectPromise(valueWrapper, promiseCapability).
    auto value_wrapper = TRY_OR_MUST_REJECT(global_object, promise_capability, promise_resolve(global_object, *global_object.promise_constructor(), value));

    // 8. Let unwrap be a new Abstract Closure with parameters (value) that captures done and performs the following steps when called:
    auto unwrap = [done](VM& vm, GlobalObject& global_object) -> ThrowCompletionOr<Value> {
        // a. Return CreateIterResultObject(value, done).
        return create_iterator_result_object(global_object, vm.argument(0), done);
    };

    // 9. Let onFulfilled be CreateBuiltinFunction(unwrap, 1, "", « »).
    // 10. NOTE: onFulfilled is used when processing the "value" property of an IteratorResult object in order to wait for its value if it is a promise and re-package the result in a new "unwrapped" IteratorResult object.
    auto* on_fulfilled = NativeFunction::create(global_object, move(unwrap), 1, "");

    // 11. Perform PerformPromiseThen(valueWrapper, onFulfilled, undefined, promiseCapability).
    verify_cast<Promise>(value_wrapper)->perform_then(move(on_fulfilled), js_undefined(), promise_capability);

    // 12. Return promiseCapability.[[Promise]].
    return promise_capability.promise;
}

// 27.1.4.2.1 %AsyncFromSyncIteratorPrototype%.next ( [ value ] ), https://tc39.es/ecma262/#sec-%asyncfromsynciteratorprototype%.next
JS_DEFINE_NATIVE_FUNCTION(AsyncFromSyncIteratorPrototype::next)
{
    // 1. Let O be the this value.
    // 2. Assert: O is an Object that has a [[SyncIteratorRecord]] internal slot.
    auto* this_object = MUST(typed_this_object(global_object));

    // 3. Let promiseCapability be ! NewPromiseCapability(%Promise%).
    auto promise_capability = MUST(new_promise_capability(global_object, global_object.promise_constructor()));

    // 4. Let syncIteratorRecord be O.[[SyncIteratorRecord]].
    auto& sync_iterator_record = this_object->sync_iterator_record();

    // 5. If value is present, then
    //     a. Let result be Completion(IteratorNext(syncIteratorRecord, value)).
    // 6. Else,
    //     a. Let result be Completion(IteratorNext(syncIteratorRecord)).
    // 7. IfAbruptRejectPromise(result, promiseCapability).
    auto* result = TRY_OR_REJECT(global_object, promise_capability,
        (vm.argument_count() > 0 ? iterator_next(global_object, sync_iterator_record, vm.argument(0))
                                 : iterator_next(global_object, sync_iterator_record)));

    // 8. Return AsyncFromSyncIteratorContinuation(result, promiseCapability).
    return async_from_sync_iterator_continuation(global_object, *result, promise_capability);
}

// 27.1.4.2.2 %AsyncFromSyncIteratorPrototype%.return ( [ value ] ), https://tc39.es/ecma262/#sec-%asyncfromsynciteratorprototype%.return
JS_DEFINE_NATIVE_FUNCTION(AsyncFromSyncIteratorPrototype::return_)
{
    // 1. Let O be the this value.
    // 2. Assert: O is an Object that has a [[SyncIteratorRecord]] internal slot.
    auto* this_object = MUST(typed_this_object(global_object));

    // 3. Let promiseCapability be ! NewPromiseCapability(%Promise%).
    auto promise_capability = MUST(new_promise_capability(global_object, global_object.promise_constructor()));

    // 4. Let syncIterator be O.[[SyncIteratorRecord]].[[Iterator]].
    auto* sync_iterator = this_object->sync_iterator_record().iterator;

    // 5. Let return be Completion(GetMethod(syncIterator, "return")).
    // 6. IfAbruptRejectPromise(return, promiseCapability).
    auto* return_method = TRY_OR_REJECT(global_object, promise_capability, Value(sync_iterator).get_method(global_object, vm.names.return_));

    // 7. If return is undefined, then
    if (return_method == nullptr) {
        // a. Let iterResult be CreateIterResultObject(value, true).
        auto* iter_result = create_iterator_result_object(global_object, vm.argument(0), true);

        // b. Perform ! Call(promiseCapability.[[Resolve]], undefined, « iterResult »).
        MUST(call(global_object, *promise_capability.reject, js_undefined(), iter_result));

        // c. Return promiseCapability.[[Promise]].
        return promise_capability.promise;
    }

    // 8. If value is present, then
    //     a. Let result be Completion(Call(return, syncIterator, « value »)).
    // 9. Else,
    //     a. Let result be Completion(Call(return, syncIterator)).
    // 10. IfAbruptRejectPromise(result, promiseCapability).
    auto result = TRY_OR_REJECT(global_object, promise_capability,
        (vm.argument_count() > 0 ? call(global_object, return_method, sync_iterator, vm.argument(0))
                                 : call(global_object, return_method, sync_iterator)));

    // 11. If Type(result) is not Object, then
    if (!result.is_object()) {
        auto* error = TypeError::create(global_object, String::formatted(ErrorType::NotAnObject.message(), "SyncIteratorReturnResult"));
        // a. Perform ! Call(promiseCapability.[[Reject]], undefined, « a newly created TypeError object »).
        MUST(call(global_object, *promise_capability.reject, js_undefined(), error));
        // b. Return promiseCapability.[[Promise]].
        return promise_capability.promise;
    }

    // 12. Return AsyncFromSyncIteratorContinuation(result, promiseCapability).
    return async_from_sync_iterator_continuation(global_object, result.as_object(), promise_capability);
}

// 27.1.4.2.3 %AsyncFromSyncIteratorPrototype%.throw ( [ value ] ), https://tc39.es/ecma262/#sec-%asyncfromsynciteratorprototype%.throw
JS_DEFINE_NATIVE_FUNCTION(AsyncFromSyncIteratorPrototype::throw_)
{
    // 1. Let O be the this value.
    // 2. Assert: O is an Object that has a [[SyncIteratorRecord]] internal slot.
    auto* this_object = MUST(typed_this_object(global_object));

    // 3. Let promiseCapability be ! NewPromiseCapability(%Promise%).
    auto promise_capability = MUST(new_promise_capability(global_object, global_object.promise_constructor()));

    // 4. Let syncIterator be O.[[SyncIteratorRecord]].[[Iterator]].
    auto* sync_iterator = this_object->sync_iterator_record().iterator;

    // 5. Let throw be Completion(GetMethod(syncIterator, "throw")).
    // 6. IfAbruptRejectPromise(throw, promiseCapability).
    auto* throw_method = TRY_OR_REJECT(global_object, promise_capability, Value(sync_iterator).get_method(global_object, vm.names.throw_));

    // 7. If throw is undefined, then
    if (throw_method == nullptr) {
        // a. Perform ! Call(promiseCapability.[[Reject]], undefined, « value »).
        MUST(call(global_object, *promise_capability.reject, js_undefined(), vm.argument(0)));
        // b. Return promiseCapability.[[Promise]].
        return promise_capability.promise;
    }
    // 8. If value is present, then
    //     a. Let result be Completion(Call(throw, syncIterator, « value »)).
    // 9. Else,
    //     a. Let result be Completion(Call(throw, syncIterator)).
    // 10. IfAbruptRejectPromise(result, promiseCapability).
    auto result = TRY_OR_REJECT(global_object, promise_capability,
        (vm.argument_count() > 0 ? call(global_object, throw_method, sync_iterator, vm.argument(0))
                                 : call(global_object, throw_method, sync_iterator)));

    // 11. If Type(result) is not Object, then
    if (!result.is_object()) {
        auto* error = TypeError::create(global_object, String::formatted(ErrorType::NotAnObject.message(), "SyncIteratorThrowResult"));
        // a. Perform ! Call(promiseCapability.[[Reject]], undefined, « a newly created TypeError object »).
        MUST(call(global_object, *promise_capability.reject, js_undefined(), error));

        // b. Return promiseCapability.[[Promise]].
        return promise_capability.promise;
    }

    // 12. Return AsyncFromSyncIteratorContinuation(result, promiseCapability).
    return async_from_sync_iterator_continuation(global_object, result.as_object(), promise_capability);
}

// 27.1.4.1 CreateAsyncFromSyncIterator ( syncIteratorRecord ), https://tc39.es/ecma262/#sec-createasyncfromsynciterator
Iterator create_async_from_sync_iterator(GlobalObject& global_object, Iterator sync_iterator_record)
{
    auto& vm = global_object.vm();

    // 1. Let asyncIterator be OrdinaryObjectCreate(%AsyncFromSyncIteratorPrototype%, « [[SyncIteratorRecord]] »).
    // 2. Set asyncIterator.[[SyncIteratorRecord]] to syncIteratorRecord.
    auto* async_iterator = AsyncFromSyncIterator::create(global_object, sync_iterator_record);

    // 3. Let nextMethod be ! Get(asyncIterator, "next").
    auto next_method = MUST(async_iterator->get(vm.names.next));

    // 4. Let iteratorRecord be the Iterator Record { [[Iterator]]: asyncIterator, [[NextMethod]]: nextMethod, [[Done]]: false }.
    auto iterator_record = Iterator { .iterator = async_iterator, .next_method = next_method, .done = false };

    // 5. Return iteratorRecord.
    return iterator_record;
}

}
