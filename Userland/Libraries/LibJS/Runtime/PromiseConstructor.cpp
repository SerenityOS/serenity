/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <YAK/Function.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/AggregateError.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/IteratorOperations.h>
#include <LibJS/Runtime/Promise.h>
#include <LibJS/Runtime/PromiseConstructor.h>
#include <LibJS/Runtime/PromiseReaction.h>
#include <LibJS/Runtime/PromiseResolvingElementFunctions.h>
#include <LibJS/Runtime/TemporaryClearException.h>

namespace JS {

// 27.2.4.1.1 GetPromiseResolve ( promiseConstructor ), https://tc39.es/ecma262/#sec-getpromiseresolve
static Value get_promise_resolve(GlobalObject& global_object, Value constructor)
{
    VERIFY(constructor.is_constructor());
    auto& vm = global_object.vm();

    auto promise_resolve = constructor.get(global_object, vm.names.resolve);
    if (vm.exception())
        return {};
    if (!promise_resolve.is_function()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAFunction, promise_resolve.to_string_without_side_effects());
        return {};
    }

    return promise_resolve;
}

// 27.2.1.1.1 IfAbruptRejectPromise ( value, capability ), https://tc39.es/ecma262/#sec-ifabruptrejectpromise
static Optional<Value> if_abrupt_reject_promise(GlobalObject& global_object, Value, PromiseCapability capability)
{
    auto& vm = global_object.vm();

    if (auto* exception = vm.exception()) {
        vm.clear_exception();
        vm.stop_unwind();

        (void)vm.call(*capability.reject, js_undefined(), exception->value());
        return capability.promise;
    }

    return {};
}

static bool iterator_record_is_complete(GlobalObject& global_object, Object& iterator_record)
{
    auto& vm = global_object.vm();

    // FIXME: Create a native iterator structure with the [[Done]] internal slot. For now, temporarily clear
    //        the exception so we can access the "done" property on the iterator object.
    TemporaryClearException clear_exception(vm);
    return iterator_complete(global_object, iterator_record);
}

static void set_iterator_record_complete(GlobalObject& global_object, Object& iterator_record)
{
    auto& vm = global_object.vm();

    // FIXME: Create a native iterator structure with the [[Done]] internal slot. For now, temporarily clear
    //        the exception so we can access the "done" property on the iterator object.
    TemporaryClearException clear_exception(vm);
    iterator_record.set(vm.names.done, Value(true), Object::ShouldThrowExceptions::No);
}

using EndOfElementsCallback = Function<Value(PromiseValueList&)>;
using InvokeElementFunctionCallback = Function<void(PromiseValueList&, RemainingElements&, Value, size_t)>;

static Value perform_promise_common(GlobalObject& global_object, Object& iterator_record, Value constructor, PromiseCapability result_capability, Value promise_resolve, EndOfElementsCallback end_of_list, InvokeElementFunctionCallback invoke_element_function)
{
    auto& vm = global_object.vm();

    VERIFY(constructor.is_constructor());
    VERIFY(promise_resolve.is_function());

    auto* values = vm.heap().allocate_without_global_object<PromiseValueList>();
    auto* remaining_elements_count = vm.heap().allocate_without_global_object<RemainingElements>(1);
    size_t index = 0;

    while (true) {
        auto* next = iterator_step(global_object, iterator_record);
        if (vm.exception()) {
            set_iterator_record_complete(global_object, iterator_record);
            return {};
        }

        if (!next) {
            set_iterator_record_complete(global_object, iterator_record);
            if (vm.exception())
                return {};

            if (--remaining_elements_count->value == 0)
                return end_of_list(*values);
            return result_capability.promise;
        }

        auto next_value = iterator_value(global_object, *next);
        if (vm.exception()) {
            set_iterator_record_complete(global_object, iterator_record);
            return {};
        }

        values->values.append(js_undefined());

        auto next_promise = vm.call(promise_resolve.as_function(), constructor, next_value);
        if (vm.exception())
            return {};

        ++remaining_elements_count->value;

        invoke_element_function(*values, *remaining_elements_count, next_promise, index);
        if (vm.exception())
            return {};

        ++index;
    }
}

// 27.2.4.1.2 PerformPromiseAll ( iteratorRecord, constructor, resultCapability, promiseResolve ), https://tc39.es/ecma262/#sec-performpromiseall
static Value perform_promise_all(GlobalObject& global_object, Object& iterator_record, Value constructor, PromiseCapability result_capability, Value promise_resolve)
{
    auto& vm = global_object.vm();

    return perform_promise_common(
        global_object, iterator_record, constructor, result_capability, promise_resolve,
        [&](PromiseValueList& values) -> Value {
            auto values_array = Array::create_from(global_object, values.values);

            (void)vm.call(*result_capability.resolve, js_undefined(), values_array);
            if (vm.exception())
                return {};

            return result_capability.promise;
        },
        [&](PromiseValueList& values, RemainingElements& remaining_elements_count, Value next_promise, size_t index) {
            auto* on_fulfilled = PromiseAllResolveElementFunction::create(global_object, index, values, result_capability, remaining_elements_count);
            on_fulfilled->define_direct_property(vm.names.name, js_string(vm, String::empty()), Attribute::Configurable);

            (void)next_promise.invoke(global_object, vm.names.then, on_fulfilled, result_capability.reject);
        });
}

// 27.2.4.2.1 PerformPromiseAllSettled ( iteratorRecord, constructor, resultCapability, promiseResolve ), https://tc39.es/ecma262/#sec-performpromiseallsettled
static Value perform_promise_all_settled(GlobalObject& global_object, Object& iterator_record, Value constructor, PromiseCapability result_capability, Value promise_resolve)
{
    auto& vm = global_object.vm();

    return perform_promise_common(
        global_object, iterator_record, constructor, result_capability, promise_resolve,
        [&](PromiseValueList& values) -> Value {
            auto values_array = Array::create_from(global_object, values.values);

            (void)vm.call(*result_capability.resolve, js_undefined(), values_array);
            if (vm.exception())
                return {};

            return result_capability.promise;
        },
        [&](PromiseValueList& values, RemainingElements& remaining_elements_count, Value next_promise, size_t index) {
            auto* on_fulfilled = PromiseAllSettledResolveElementFunction::create(global_object, index, values, result_capability, remaining_elements_count);
            on_fulfilled->define_direct_property(vm.names.name, js_string(vm, String::empty()), Attribute::Configurable);

            auto* on_rejected = PromiseAllSettledRejectElementFunction::create(global_object, index, values, result_capability, remaining_elements_count);
            on_rejected->define_direct_property(vm.names.name, js_string(vm, String::empty()), Attribute::Configurable);

            (void)next_promise.invoke(global_object, vm.names.then, on_fulfilled, on_rejected);
        });
}

// 27.2.4.3.1 PerformPromiseAny ( iteratorRecord, constructor, resultCapability, promiseResolve ), https://tc39.es/ecma262/#sec-performpromiseany
static Value perform_promise_any(GlobalObject& global_object, Object& iterator_record, Value constructor, PromiseCapability result_capability, Value promise_resolve)
{
    auto& vm = global_object.vm();

    return perform_promise_common(
        global_object, iterator_record, constructor, result_capability, promise_resolve,
        [&](PromiseValueList& errors) -> Value {
            auto errors_array = Array::create_from(global_object, errors.values);

            auto* error = AggregateError::create(global_object);
            error->define_property_or_throw(vm.names.errors, { .value = errors_array, .writable = true, .enumerable = false, .configurable = true });

            vm.throw_exception(global_object, error);
            return {};
        },
        [&](PromiseValueList& errors, RemainingElements& remaining_elements_count, Value next_promise, size_t index) {
            auto* on_rejected = PromiseAnyRejectElementFunction::create(global_object, index, errors, result_capability, remaining_elements_count);
            on_rejected->define_direct_property(vm.names.name, js_string(vm, String::empty()), Attribute::Configurable);

            (void)next_promise.invoke(global_object, vm.names.then, result_capability.resolve, on_rejected);
        });
}

// 27.2.4.5.1 PerformPromiseRace ( iteratorRecord, constructor, resultCapability, promiseResolve ), https://tc39.es/ecma262/#sec-performpromiserace
static Value perform_promise_race(GlobalObject& global_object, Object& iterator_record, Value constructor, PromiseCapability result_capability, Value promise_resolve)
{
    auto& vm = global_object.vm();

    return perform_promise_common(
        global_object, iterator_record, constructor, result_capability, promise_resolve,
        [&](PromiseValueList&) -> Value {
            return result_capability.promise;
        },
        [&](PromiseValueList&, RemainingElements&, Value next_promise, size_t) {
            (void)next_promise.invoke(global_object, vm.names.then, result_capability.resolve, result_capability.reject);
        });
}

PromiseConstructor::PromiseConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.Promise.as_string(), *global_object.function_prototype())
{
}

void PromiseConstructor::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    NativeFunction::initialize(global_object);

    // 27.2.4.4 Promise.prototype, https://tc39.es/ecma262/#sec-promise.prototype
    define_direct_property(vm.names.prototype, global_object.promise_prototype(), 0);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.all, all, 1, attr);
    define_native_function(vm.names.allSettled, all_settled, 1, attr);
    define_native_function(vm.names.any, any, 1, attr);
    define_native_function(vm.names.race, race, 1, attr);
    define_native_function(vm.names.reject, reject, 1, attr);
    define_native_function(vm.names.resolve, resolve, 1, attr);

    define_native_accessor(*vm.well_known_symbol_species(), symbol_species_getter, {}, Attribute::Configurable);

    define_direct_property(vm.names.length, Value(1), Attribute::Configurable);
}

// 27.2.3.1 Promise ( executor ), https://tc39.es/ecma262/#sec-promise-executor
Value PromiseConstructor::call()
{
    auto& vm = this->vm();
    vm.throw_exception<TypeError>(global_object(), ErrorType::ConstructorWithoutNew, vm.names.Promise);
    return {};
}

// 27.2.3.1 Promise ( executor ), https://tc39.es/ecma262/#sec-promise-executor
Value PromiseConstructor::construct(FunctionObject& new_target)
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    auto executor = vm.argument(0);
    if (!executor.is_function()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::PromiseExecutorNotAFunction);
        return {};
    }

    auto* promise = ordinary_create_from_constructor<Promise>(global_object, new_target, &GlobalObject::promise_prototype);
    if (vm.exception())
        return {};

    auto [resolve_function, reject_function] = promise->create_resolving_functions();

    (void)vm.call(executor.as_function(), js_undefined(), &resolve_function, &reject_function);
    if (auto* exception = vm.exception()) {
        vm.clear_exception();
        vm.stop_unwind();
        (void)vm.call(reject_function, js_undefined(), exception->value());
    }
    return promise;
}

// 27.2.4.1 Promise.all ( iterable ), https://tc39.es/ecma262/#sec-promise.all
JS_DEFINE_NATIVE_FUNCTION(PromiseConstructor::all)
{
    auto* constructor = vm.this_value(global_object).to_object(global_object);
    if (!constructor)
        return {};

    auto promise_capability = new_promise_capability(global_object, constructor);
    if (vm.exception())
        return {};

    auto promise_resolve = get_promise_resolve(global_object, constructor);
    if (auto abrupt = if_abrupt_reject_promise(global_object, promise_resolve, promise_capability); abrupt.has_value())
        return abrupt.value();

    auto iterator_record = get_iterator(global_object, vm.argument(0));
    if (auto abrupt = if_abrupt_reject_promise(global_object, iterator_record, promise_capability); abrupt.has_value())
        return abrupt.value();

    auto result = perform_promise_all(global_object, *iterator_record, constructor, promise_capability, promise_resolve);
    if (vm.exception()) {
        if (!iterator_record_is_complete(global_object, *iterator_record))
            iterator_close(*iterator_record);

        auto abrupt = if_abrupt_reject_promise(global_object, result, promise_capability);
        return abrupt.value();
    }

    return result;
}

// 27.2.4.2 Promise.allSettled ( iterable ), https://tc39.es/ecma262/#sec-promise.allsettled
JS_DEFINE_NATIVE_FUNCTION(PromiseConstructor::all_settled)
{
    auto* constructor = vm.this_value(global_object).to_object(global_object);
    if (!constructor)
        return {};

    auto promise_capability = new_promise_capability(global_object, constructor);
    if (vm.exception())
        return {};

    auto promise_resolve = get_promise_resolve(global_object, constructor);
    if (auto abrupt = if_abrupt_reject_promise(global_object, promise_resolve, promise_capability); abrupt.has_value())
        return abrupt.value();

    auto iterator_record = get_iterator(global_object, vm.argument(0));
    if (auto abrupt = if_abrupt_reject_promise(global_object, iterator_record, promise_capability); abrupt.has_value())
        return abrupt.value();

    auto result = perform_promise_all_settled(global_object, *iterator_record, constructor, promise_capability, promise_resolve);
    if (vm.exception()) {
        if (!iterator_record_is_complete(global_object, *iterator_record))
            iterator_close(*iterator_record);

        auto abrupt = if_abrupt_reject_promise(global_object, result, promise_capability);
        return abrupt.value();
    }

    return result;
}

// 27.2.4.3 Promise.any ( iterable ), https://tc39.es/ecma262/#sec-promise.any
JS_DEFINE_NATIVE_FUNCTION(PromiseConstructor::any)
{
    auto* constructor = vm.this_value(global_object).to_object(global_object);
    if (!constructor)
        return {};

    auto promise_capability = new_promise_capability(global_object, constructor);
    if (vm.exception())
        return {};

    auto promise_resolve = get_promise_resolve(global_object, constructor);
    if (auto abrupt = if_abrupt_reject_promise(global_object, promise_resolve, promise_capability); abrupt.has_value())
        return abrupt.value();

    auto iterator_record = get_iterator(global_object, vm.argument(0));
    if (auto abrupt = if_abrupt_reject_promise(global_object, iterator_record, promise_capability); abrupt.has_value())
        return abrupt.value();

    auto result = perform_promise_any(global_object, *iterator_record, constructor, promise_capability, promise_resolve);
    if (vm.exception()) {
        if (!iterator_record_is_complete(global_object, *iterator_record))
            iterator_close(*iterator_record);

        auto abrupt = if_abrupt_reject_promise(global_object, result, promise_capability);
        return abrupt.value();
    }

    return result;
}

// 27.2.4.5 Promise.race ( iterable ), https://tc39.es/ecma262/#sec-promise.race
JS_DEFINE_NATIVE_FUNCTION(PromiseConstructor::race)
{
    auto* constructor = vm.this_value(global_object).to_object(global_object);
    if (!constructor)
        return {};

    auto promise_capability = new_promise_capability(global_object, constructor);
    if (vm.exception())
        return {};

    auto promise_resolve = get_promise_resolve(global_object, constructor);
    if (auto abrupt = if_abrupt_reject_promise(global_object, promise_resolve, promise_capability); abrupt.has_value())
        return abrupt.value();

    auto iterator_record = get_iterator(global_object, vm.argument(0));
    if (auto abrupt = if_abrupt_reject_promise(global_object, iterator_record, promise_capability); abrupt.has_value())
        return abrupt.value();

    auto result = perform_promise_race(global_object, *iterator_record, constructor, promise_capability, promise_resolve);
    if (vm.exception()) {
        if (!iterator_record_is_complete(global_object, *iterator_record))
            iterator_close(*iterator_record);

        auto abrupt = if_abrupt_reject_promise(global_object, result, promise_capability);
        return abrupt.value();
    }

    return result;
}

// 27.2.4.6 Promise.reject ( r ), https://tc39.es/ecma262/#sec-promise.reject
JS_DEFINE_NATIVE_FUNCTION(PromiseConstructor::reject)
{
    auto* constructor = vm.this_value(global_object).to_object(global_object);
    if (!constructor)
        return {};
    auto promise_capability = new_promise_capability(global_object, constructor);
    if (vm.exception())
        return {};
    auto reason = vm.argument(0);
    [[maybe_unused]] auto result = vm.call(*promise_capability.reject, js_undefined(), reason);
    return promise_capability.promise;
}

// 27.2.4.7 Promise.resolve ( x ), https://tc39.es/ecma262/#sec-promise.resolve
JS_DEFINE_NATIVE_FUNCTION(PromiseConstructor::resolve)
{
    auto* constructor = vm.this_value(global_object).to_object(global_object);
    if (!constructor)
        return {};
    auto value = vm.argument(0);
    return promise_resolve(global_object, *constructor, value);
}

// 27.2.4.8 get Promise [ @@species ], https://tc39.es/ecma262/#sec-get-promise-@@species
JS_DEFINE_NATIVE_GETTER(PromiseConstructor::symbol_species_getter)
{
    return vm.this_value(global_object);
}

}
