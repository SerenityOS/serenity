/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Iterator.h>
#include <LibJS/Runtime/IteratorHelper.h>
#include <LibJS/Runtime/IteratorPrototype.h>
#include <LibJS/Runtime/ValueInlines.h>

namespace JS {

// 27.1.2 The %IteratorPrototype% Object, https://tc39.es/ecma262/#sec-%iteratorprototype%-object
IteratorPrototype::IteratorPrototype(Realm& realm)
    : PrototypeObject(realm.intrinsics().object_prototype())
{
}

void IteratorPrototype::initialize(Realm& realm)
{
    auto& vm = this->vm();
    Base::initialize(realm);

    // 3.1.3.13 Iterator.prototype [ @@toStringTag ], https://tc39.es/proposal-iterator-helpers/#sec-iteratorprototype-@@tostringtag
    define_direct_property(vm.well_known_symbol_to_string_tag(), PrimitiveString::create(vm, "Iterator"_string), Attribute::Configurable | Attribute::Writable);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(realm, vm.well_known_symbol_iterator(), symbol_iterator, 0, attr);
    define_native_function(realm, vm.names.map, map, 1, attr);
    define_native_function(realm, vm.names.filter, filter, 1, attr);
    define_native_function(realm, vm.names.take, take, 1, attr);
    define_native_function(realm, vm.names.drop, drop, 1, attr);
    define_native_function(realm, vm.names.flatMap, flat_map, 1, attr);
    define_native_function(realm, vm.names.reduce, reduce, 1, attr);
    define_native_function(realm, vm.names.toArray, to_array, 0, attr);
    define_native_function(realm, vm.names.forEach, for_each, 1, attr);
    define_native_function(realm, vm.names.some, some, 1, attr);
    define_native_function(realm, vm.names.every, every, 1, attr);
    define_native_function(realm, vm.names.find, find, 1, attr);
}

// 27.1.2.1 %IteratorPrototype% [ @@iterator ] ( ), https://tc39.es/ecma262/#sec-%iteratorprototype%-@@iterator
JS_DEFINE_NATIVE_FUNCTION(IteratorPrototype::symbol_iterator)
{
    // 1. Return the this value.
    return vm.this_value();
}

// 3.1.3.2 Iterator.prototype.map ( mapper ), https://tc39.es/proposal-iterator-helpers/#sec-iteratorprototype.map
JS_DEFINE_NATIVE_FUNCTION(IteratorPrototype::map)
{
    auto& realm = *vm.current_realm();

    auto mapper = vm.argument(0);

    // 1. Let O be the this value.
    // 2. If O is not an Object, throw a TypeError exception.
    auto object = TRY(this_object(vm));

    // 3. If IsCallable(mapper) is false, throw a TypeError exception.
    if (!mapper.is_function())
        return vm.throw_completion<TypeError>(ErrorType::NotAFunction, "mapper"sv);

    // 4. Let iterated be ? GetIteratorDirect(O).
    auto iterated = TRY(get_iterator_direct(vm, object));

    // 5. Let closure be a new Abstract Closure with no parameters that captures iterated and mapper and performs the following steps when called:
    IteratorHelper::Closure closure = [mapper = NonnullGCPtr { mapper.as_function() }](auto& vm, auto& iterator) -> ThrowCompletionOr<Value> {
        auto const& iterated = iterator.underlying_iterator();

        // a. Let counter be 0.
        // b. Repeat,

        // i. Let next be ? IteratorStep(iterated).
        auto next = TRY(iterator_step(vm, iterated));

        // ii. If next is false, return undefined.
        if (!next)
            return iterator.result(js_undefined());

        // iii. Let value be ? IteratorValue(next).
        auto value = TRY(iterator_value(vm, *next));

        // iv. Let mapped be Completion(Call(mapper, undefined, « value, 𝔽(counter) »)).
        auto mapped = call(vm, *mapper, js_undefined(), value, Value { iterator.counter() });

        // v. IfAbruptCloseIterator(mapped, iterated).
        if (mapped.is_error())
            return iterator.close_result(vm, mapped.release_error());

        // viii. Set counter to counter + 1.
        // NOTE: We do this step early to ensure it occurs before returning.
        iterator.increment_counter();

        // vi. Let completion be Completion(Yield(mapped)).
        // vii. IfAbruptCloseIterator(completion, iterated).
        return iterator.result(mapped.release_value());
    };

    // 6. Let result be CreateIteratorFromClosure(closure, "Iterator Helper", %IteratorHelperPrototype%, « [[UnderlyingIterator]] »).
    // 7. Set result.[[UnderlyingIterator]] to iterated.
    auto result = TRY(IteratorHelper::create(realm, move(iterated), move(closure)));

    // 8. Return result.
    return result;
}

// 3.1.3.3 Iterator.prototype.filter ( predicate ), https://tc39.es/proposal-iterator-helpers/#sec-iteratorprototype.filter
JS_DEFINE_NATIVE_FUNCTION(IteratorPrototype::filter)
{
    auto& realm = *vm.current_realm();

    auto predicate = vm.argument(0);

    // 1. Let O be the this value.
    // 2. If O is not an Object, throw a TypeError exception.
    auto object = TRY(this_object(vm));

    // 3. If IsCallable(predicate) is false, throw a TypeError exception.
    if (!predicate.is_function())
        return vm.throw_completion<TypeError>(ErrorType::NotAFunction, "predicate"sv);

    // 4. Let iterated be ? GetIteratorDirect(O).
    auto iterated = TRY(get_iterator_direct(vm, object));

    // 5. Let closure be a new Abstract Closure with no parameters that captures iterated and predicate and performs the following steps when called:
    IteratorHelper::Closure closure = [predicate = NonnullGCPtr { predicate.as_function() }](auto& vm, auto& iterator) -> ThrowCompletionOr<Value> {
        auto const& iterated = iterator.underlying_iterator();

        // a. Let counter be 0.

        // b. Repeat,
        while (true) {
            // i. Let next be ? IteratorStep(iterated).
            auto next = TRY(iterator_step(vm, iterated));

            // ii. If next is false, return undefined.
            if (!next)
                return iterator.result(js_undefined());

            // iii. Let value be ? IteratorValue(next).
            auto value = TRY(iterator_value(vm, *next));

            // iv. Let selected be Completion(Call(predicate, undefined, « value, 𝔽(counter) »)).
            auto selected = call(vm, *predicate, js_undefined(), value, Value { iterator.counter() });

            // v. IfAbruptCloseIterator(selected, iterated).
            if (selected.is_error())
                return iterator.close_result(vm, selected.release_error());

            // vii. Set counter to counter + 1.
            // NOTE: We do this step early to ensure it occurs before returning.
            iterator.increment_counter();

            // vi. If ToBoolean(selected) is true, then
            if (selected.value().to_boolean()) {
                // 1. Let completion be Completion(Yield(value)).
                // 2. IfAbruptCloseIterator(completion, iterated).
                return iterator.result(value);
            }
        }
    };

    // 6. Let result be CreateIteratorFromClosure(closure, "Iterator Helper", %IteratorHelperPrototype%, « [[UnderlyingIterator]] »).
    // 7. Set result.[[UnderlyingIterator]] to iterated.
    auto result = TRY(IteratorHelper::create(realm, move(iterated), move(closure)));

    // 8. Return result.
    return result;
}

// 3.1.3.4 Iterator.prototype.take ( limit ), https://tc39.es/proposal-iterator-helpers/#sec-iteratorprototype.take
JS_DEFINE_NATIVE_FUNCTION(IteratorPrototype::take)
{
    auto& realm = *vm.current_realm();

    auto limit = vm.argument(0);

    // 1. Let O be the this value.
    // 2. If O is not an Object, throw a TypeError exception.
    auto object = TRY(this_object(vm));

    // 3. Let numLimit be ? ToNumber(limit).
    auto numeric_limit = TRY(limit.to_number(vm));

    // 4. If numLimit is NaN, throw a RangeError exception.
    if (numeric_limit.is_nan())
        return vm.throw_completion<RangeError>(ErrorType::NumberIsNaN, "limit"sv);

    // 5. Let integerLimit be ! ToIntegerOrInfinity(numLimit).
    auto integer_limit = MUST(numeric_limit.to_integer_or_infinity(vm));

    // 6. If integerLimit < 0, throw a RangeError exception.
    if (integer_limit < 0)
        return vm.throw_completion<RangeError>(ErrorType::NumberIsNegative, "limit"sv);

    // 7. Let iterated be ? GetIteratorDirect(O).
    auto iterated = TRY(get_iterator_direct(vm, object));

    // 8. Let closure be a new Abstract Closure with no parameters that captures iterated and integerLimit and performs the following steps when called:
    IteratorHelper::Closure closure = [integer_limit](auto& vm, auto& iterator) -> ThrowCompletionOr<Value> {
        auto const& iterated = iterator.underlying_iterator();

        // a. Let remaining be integerLimit.
        // b. Repeat,

        // i. If remaining is 0, then
        if (iterator.counter() >= integer_limit) {
            // 1. Return ? IteratorClose(iterated, NormalCompletion(undefined)).
            return iterator.close_result(vm, normal_completion(js_undefined()));
        }

        // ii. If remaining is not +∞, then
        //     1. Set remaining to remaining - 1.
        iterator.increment_counter();

        // iii. Let next be ? IteratorStep(iterated).
        auto next = TRY(iterator_step(vm, iterated));

        // iv. If next is false, return undefined.
        if (!next)
            return iterator.result(js_undefined());

        // v. Let completion be Completion(Yield(? IteratorValue(next))).
        // vi. IfAbruptCloseIterator(completion, iterated).
        return iterator.result(TRY(iterator_value(vm, *next)));
    };

    // 9. Let result be CreateIteratorFromClosure(closure, "Iterator Helper", %IteratorHelperPrototype%, « [[UnderlyingIterator]] »).
    // 10. Set result.[[UnderlyingIterator]] to iterated.
    auto result = TRY(IteratorHelper::create(realm, move(iterated), move(closure)));

    // 11. Return result.
    return result;
}

// 3.1.3.5 Iterator.prototype.drop ( limit ), https://tc39.es/proposal-iterator-helpers/#sec-iteratorprototype.drop
JS_DEFINE_NATIVE_FUNCTION(IteratorPrototype::drop)
{
    auto& realm = *vm.current_realm();

    auto limit = vm.argument(0);

    // 1. Let O be the this value.
    // 2. If O is not an Object, throw a TypeError exception.
    auto object = TRY(this_object(vm));

    // 3. Let numLimit be ? ToNumber(limit).
    auto numeric_limit = TRY(limit.to_number(vm));

    // 4. If numLimit is NaN, throw a RangeError exception.
    if (numeric_limit.is_nan())
        return vm.throw_completion<RangeError>(ErrorType::NumberIsNaN, "limit"sv);

    // 5. Let integerLimit be ! ToIntegerOrInfinity(numLimit).
    auto integer_limit = MUST(numeric_limit.to_integer_or_infinity(vm));

    // 6. If integerLimit < 0, throw a RangeError exception.
    if (integer_limit < 0)
        return vm.throw_completion<RangeError>(ErrorType::NumberIsNegative, "limit"sv);

    // 7. Let iterated be ? GetIteratorDirect(O).
    auto iterated = TRY(get_iterator_direct(vm, object));

    // 8. Let closure be a new Abstract Closure with no parameters that captures iterated and integerLimit and performs the following steps when called:
    IteratorHelper::Closure closure = [integer_limit](auto& vm, auto& iterator) -> ThrowCompletionOr<Value> {
        auto const& iterated = iterator.underlying_iterator();

        // a. Let remaining be integerLimit.
        // b. Repeat, while remaining > 0,
        while (iterator.counter() < integer_limit) {
            // i. If remaining is not +∞, then
            //     1. Set remaining to remaining - 1.
            iterator.increment_counter();

            // ii. Let next be ? IteratorStep(iterated).
            auto next = TRY(iterator_step(vm, iterated));

            // iii. If next is false, return undefined.
            if (!next)
                return iterator.result(js_undefined());
        }

        // c. Repeat,

        // i. Let next be ? IteratorStep(iterated).
        auto next = TRY(iterator_step(vm, iterated));

        // ii. If next is false, return undefined.
        if (!next)
            return iterator.result(js_undefined());

        // iii. Let completion be Completion(Yield(? IteratorValue(next))).
        // iv. IfAbruptCloseIterator(completion, iterated).
        return iterator.result(TRY(iterator_value(vm, *next)));
    };

    // 9. Let result be CreateIteratorFromClosure(closure, "Iterator Helper", %IteratorHelperPrototype%, « [[UnderlyingIterator]] »).
    // 10. Set result.[[UnderlyingIterator]] to iterated.
    auto result = TRY(IteratorHelper::create(realm, move(iterated), move(closure)));

    // 11. Return result.
    return result;
}

class FlatMapIterator : public Cell {
    JS_CELL(FlatMapIterator, Cell);

public:
    ThrowCompletionOr<Value> next(VM& vm, IteratorRecord const& iterated, IteratorHelper& iterator, FunctionObject& mapper)
    {
        if (m_inner_iterator.has_value())
            return next_inner_iterator(vm, iterated, iterator, mapper);
        return next_outer_iterator(vm, iterated, iterator, mapper);
    }

    // NOTE: This implements step 5.b.ix.4.d of Iterator.prototype.flatMap.
    ThrowCompletionOr<Value> on_abrupt_completion(VM& vm, IteratorHelper& iterator, Completion const& completion)
    {
        VERIFY(m_inner_iterator.has_value());

        // d. If completion is an abrupt completion, then

        // i. Let backupCompletion be Completion(IteratorClose(innerIterator, completion)).
        auto backup_completion = iterator_close(vm, *m_inner_iterator, completion);

        // ii. IfAbruptCloseIterator(backupCompletion, iterated).
        if (backup_completion.is_error())
            return iterator.close_result(vm, backup_completion.release_error());

        // iii. Return ? IteratorClose(completion, iterated).
        return iterator.close_result(vm, completion);
    }

private:
    FlatMapIterator() = default;

    virtual void visit_edges(Visitor& visitor) override
    {
        Base::visit_edges(visitor);

        if (m_inner_iterator.has_value())
            visitor.visit(m_inner_iterator->iterator);
    }

    ThrowCompletionOr<Value> next_outer_iterator(VM& vm, IteratorRecord const& iterated, IteratorHelper& iterator, FunctionObject& mapper)
    {
        // i. Let next be ? IteratorStep(iterated).
        auto next = TRY(iterator_step(vm, iterated));

        // ii. If next is false, return undefined.
        if (!next)
            return iterator.result(js_undefined());

        // iii. Let value be ? IteratorValue(next).
        auto value = TRY(iterator_value(vm, *next));

        // iv. Let mapped be Completion(Call(mapper, undefined, « value, 𝔽(counter) »)).
        auto mapped = call(vm, mapper, js_undefined(), value, Value { iterator.counter() });

        // v. IfAbruptCloseIterator(mapped, iterated).
        if (mapped.is_error())
            return iterator.close_result(vm, mapped.release_error());

        // vi. Let innerIterator be Completion(GetIteratorFlattenable(mapped, reject-strings)).
        auto inner_iterator = get_iterator_flattenable(vm, mapped.release_value(), StringHandling::RejectStrings);

        // vii. IfAbruptCloseIterator(innerIterator, iterated).
        if (inner_iterator.is_error())
            return iterator.close_result(vm, inner_iterator.release_error());

        // viii. Let innerAlive be true.
        m_inner_iterator = inner_iterator.release_value();

        // x. Set counter to counter + 1.
        // NOTE: We do this step early to ensure it occurs before returning.
        iterator.increment_counter();

        // ix. Repeat, while innerAlive is true,
        return next_inner_iterator(vm, iterated, iterator, mapper);
    }

    ThrowCompletionOr<Value> next_inner_iterator(VM& vm, IteratorRecord const& iterated, IteratorHelper& iterator, FunctionObject& mapper)
    {
        VERIFY(m_inner_iterator.has_value());

        // 1. Let innerNext be Completion(IteratorStep(innerIterator)).
        auto inner_next = iterator_step(vm, *m_inner_iterator);

        // 2. IfAbruptCloseIterator(innerNext, iterated).
        if (inner_next.is_error())
            return iterator.close_result(vm, inner_next.release_error());

        // 3. If innerNext is false, then
        if (!inner_next.value()) {
            // a. Set innerAlive to false.
            m_inner_iterator.clear();

            return next_outer_iterator(vm, iterated, iterator, mapper);
        }
        // 4. Else,
        else {
            // a. Let innerValue be Completion(IteratorValue(innerNext)).
            auto inner_value = iterator_value(vm, *inner_next.release_value());

            // b. IfAbruptCloseIterator(innerValue, iterated).
            if (inner_value.is_error())
                return iterator.close_result(vm, inner_value.release_error());

            // c. Let completion be Completion(Yield(innerValue)).
            // NOTE: Step d is implemented via on_abrupt_completion.
            return inner_value.release_value();
        }
    }

    Optional<IteratorRecord> m_inner_iterator;
};

// 3.1.3.6 Iterator.prototype.flatMap ( mapper ), https://tc39.es/proposal-iterator-helpers/#sec-iteratorprototype.flatmap
JS_DEFINE_NATIVE_FUNCTION(IteratorPrototype::flat_map)
{
    auto& realm = *vm.current_realm();

    auto mapper = vm.argument(0);

    // 1. Let O be the this value.
    // 2. If O is not an Object, throw a TypeError exception.
    auto object = TRY(this_object(vm));

    // 3. If IsCallable(mapper) is false, throw a TypeError exception.
    if (!mapper.is_function())
        return vm.throw_completion<TypeError>(ErrorType::NotAFunction, "mapper"sv);

    // 4. Let iterated be ? GetIteratorDirect(O).
    auto iterated = TRY(get_iterator_direct(vm, object));

    auto flat_map_iterator = vm.heap().allocate<FlatMapIterator>(realm);

    // 5. Let closure be a new Abstract Closure with no parameters that captures iterated and mapper and performs the following steps when called:
    IteratorHelper::Closure closure = [flat_map_iterator, mapper = NonnullGCPtr { mapper.as_function() }](auto& vm, auto& iterator) mutable -> ThrowCompletionOr<Value> {
        auto const& iterated = iterator.underlying_iterator();
        return flat_map_iterator->next(vm, iterated, iterator, *mapper);
    };

    IteratorHelper::AbruptClosure abrupt_closure = [flat_map_iterator](auto& vm, auto& iterator, auto const& completion) -> ThrowCompletionOr<Value> {
        return flat_map_iterator->on_abrupt_completion(vm, iterator, completion);
    };

    // 6. Let result be CreateIteratorFromClosure(closure, "Iterator Helper", %IteratorHelperPrototype%, « [[UnderlyingIterator]] »).
    // 7. Set result.[[UnderlyingIterator]] to iterated.
    auto result = TRY(IteratorHelper::create(realm, move(iterated), move(closure), move(abrupt_closure)));

    // 8. Return result.
    return result;
}

// 3.1.3.7 Iterator.prototype.reduce ( reducer [ , initialValue ] ), https://tc39.es/proposal-iterator-helpers/#sec-iteratorprototype.reduce
JS_DEFINE_NATIVE_FUNCTION(IteratorPrototype::reduce)
{
    auto reducer = vm.argument(0);

    // 1. Let O be the this value.
    // 2. If O is not an Object, throw a TypeError exception.
    auto object = TRY(this_object(vm));

    // 3. If IsCallable(reducer) is false, throw a TypeError exception.
    if (!reducer.is_function())
        return vm.throw_completion<TypeError>(ErrorType::NotAFunction, "reducer"sv);

    // 4. Let iterated be ? GetIteratorDirect(O).
    auto iterated = TRY(get_iterator_direct(vm, object));

    Value accumulator;
    size_t counter = 0;

    // 5. If initialValue is not present, then
    if (vm.argument_count() < 2) {
        // a. Let next be ? IteratorStep(iterated).
        auto next = TRY(iterator_step(vm, iterated));

        // b. If next is false, throw a TypeError exception.
        if (!next)
            return vm.throw_completion<TypeError>(ErrorType::ReduceNoInitial);

        // c. Let accumulator be ? IteratorValue(next).
        accumulator = TRY(iterator_value(vm, *next));

        // d. Let counter be 1.
        counter = 1;
    }
    // 6. Else,
    else {
        // a. Let accumulator be initialValue.
        accumulator = vm.argument(1);

        // b. Let counter be 0.
        counter = 0;
    }

    // 7. Repeat,
    while (true) {
        // a. Let next be ? IteratorStep(iterated).
        auto next = TRY(iterator_step(vm, iterated));

        // b. If next is false, return accumulator.
        if (!next)
            return accumulator;

        // c. Let value be ? IteratorValue(next).
        auto value = TRY(iterator_value(vm, *next));

        // d. Let result be Completion(Call(reducer, undefined, « accumulator, value, 𝔽(counter) »)).
        auto result = call(vm, reducer.as_function(), js_undefined(), accumulator, value, Value { counter });

        // e. IfAbruptCloseIterator(result, iterated).
        if (result.is_error())
            return *TRY(iterator_close(vm, iterated, result.release_error()));

        // f. Set accumulator to result.[[Value]].
        accumulator = result.release_value();

        // g. Set counter to counter + 1.
        ++counter;
    }
}

// 3.1.3.8 Iterator.prototype.toArray ( ), https://tc39.es/proposal-iterator-helpers/#sec-iteratorprototype.toarray
JS_DEFINE_NATIVE_FUNCTION(IteratorPrototype::to_array)
{
    auto& realm = *vm.current_realm();

    // 1. Let O be the this value.
    // 2. If O is not an Object, throw a TypeError exception.
    auto object = TRY(this_object(vm));

    // 3. Let iterated be ? GetIteratorDirect(O).
    auto iterated = TRY(get_iterator_direct(vm, object));

    // 4. Let items be a new empty List.
    Vector<Value> items;

    // 5. Repeat,
    while (true) {
        // a. Let next be ? IteratorStep(iterated).
        auto next = TRY(iterator_step(vm, iterated));

        // b. If next is false, return CreateArrayFromList(items).
        if (!next)
            return Array::create_from(realm, items);

        // c. Let value be ? IteratorValue(next).
        auto value = TRY(iterator_value(vm, *next));

        // d. Append value to items.
        TRY_OR_THROW_OOM(vm, items.try_append(value));
    }
}

// 3.1.3.9 Iterator.prototype.forEach ( fn ), https://tc39.es/proposal-iterator-helpers/#sec-iteratorprototype.foreach
JS_DEFINE_NATIVE_FUNCTION(IteratorPrototype::for_each)
{
    auto function = vm.argument(0);

    // 1. Let O be the this value.
    // 2. If O is not an Object, throw a TypeError exception.
    auto object = TRY(this_object(vm));

    // 3. If IsCallable(fn) is false, throw a TypeError exception.
    if (!function.is_function())
        return vm.throw_completion<TypeError>(ErrorType::NotAFunction, "fn"sv);

    // 4. Let iterated be ? GetIteratorDirect(O).
    auto iterated = TRY(get_iterator_direct(vm, object));

    // 5. Let counter be 0.
    size_t counter = 0;

    // 6. Repeat,
    while (true) {
        // a. Let next be ? IteratorStep(iterated).
        auto next = TRY(iterator_step(vm, iterated));

        // b. If next is false, return undefined.
        if (!next)
            return js_undefined();

        // c. Let value be ? IteratorValue(next).
        auto value = TRY(iterator_value(vm, *next));

        // d. Let result be Completion(Call(fn, undefined, « value, 𝔽(counter) »)).
        auto result = call(vm, function.as_function(), js_undefined(), value, Value { counter });

        // e. IfAbruptCloseIterator(result, iterated).
        if (result.is_error())
            return *TRY(iterator_close(vm, iterated, result.release_error()));

        // f. Set counter to counter + 1.
        ++counter;
    }
}

// 3.1.3.10 Iterator.prototype.some ( predicate ), https://tc39.es/proposal-iterator-helpers/#sec-iteratorprototype.some
JS_DEFINE_NATIVE_FUNCTION(IteratorPrototype::some)
{
    auto predicate = vm.argument(0);

    // 1. Let O be the this value.
    // 2. If O is not an Object, throw a TypeError exception.
    auto object = TRY(this_object(vm));

    // 3. If IsCallable(predicate) is false, throw a TypeError exception.
    if (!predicate.is_function())
        return vm.throw_completion<TypeError>(ErrorType::NotAFunction, "predicate"sv);

    // 4. Let iterated be ? GetIteratorDirect(O).
    auto iterated = TRY(get_iterator_direct(vm, object));

    // 5. Let counter be 0.
    size_t counter = 0;

    // 6. Repeat,
    while (true) {
        // a. Let next be ? IteratorStep(iterated).
        auto next = TRY(iterator_step(vm, iterated));

        // b. If next is false, return false.
        if (!next)
            return Value { false };

        // c. Let value be ? IteratorValue(next).
        auto value = TRY(iterator_value(vm, *next));

        // d. Let result be Completion(Call(predicate, undefined, « value, 𝔽(counter) »)).
        auto result = call(vm, predicate.as_function(), js_undefined(), value, Value { counter });

        // e. IfAbruptCloseIterator(result, iterated).
        if (result.is_error())
            return *TRY(iterator_close(vm, iterated, result.release_error()));

        // f. If ToBoolean(result) is true, return ? IteratorClose(iterated, NormalCompletion(true)).
        if (result.value().to_boolean())
            return *TRY(iterator_close(vm, iterated, normal_completion(Value { true })));

        // g. Set counter to counter + 1.
        ++counter;
    }
}

// 3.1.3.11 Iterator.prototype.every ( predicate ), https://tc39.es/proposal-iterator-helpers/#sec-iteratorprototype.every
JS_DEFINE_NATIVE_FUNCTION(IteratorPrototype::every)
{
    auto predicate = vm.argument(0);

    // 1. Let O be the this value.
    // 2. If O is not an Object, throw a TypeError exception.
    auto object = TRY(this_object(vm));

    // 3. If IsCallable(predicate) is false, throw a TypeError exception.
    if (!predicate.is_function())
        return vm.throw_completion<TypeError>(ErrorType::NotAFunction, "predicate"sv);

    // 4. Let iterated be ? GetIteratorDirect(O).
    auto iterated = TRY(get_iterator_direct(vm, object));

    // 5. Let counter be 0.
    size_t counter = 0;

    // 6. Repeat,
    while (true) {
        // a. Let next be ? IteratorStep(iterated).
        auto next = TRY(iterator_step(vm, iterated));

        // b. If next is false, return true.
        if (!next)
            return Value { true };

        // c. Let value be ? IteratorValue(next).
        auto value = TRY(iterator_value(vm, *next));

        // d. Let result be Completion(Call(predicate, undefined, « value, 𝔽(counter) »)).
        auto result = call(vm, predicate.as_function(), js_undefined(), value, Value { counter });

        // e. IfAbruptCloseIterator(result, iterated).
        if (result.is_error())
            return *TRY(iterator_close(vm, iterated, result.release_error()));

        // f. If ToBoolean(result) is false, return ? IteratorClose(iterated, NormalCompletion(false)).
        if (!result.value().to_boolean())
            return *TRY(iterator_close(vm, iterated, normal_completion(Value { false })));

        // g. Set counter to counter + 1.
        ++counter;
    }
}

// 3.1.3.12 Iterator.prototype.find ( predicate ), https://tc39.es/proposal-iterator-helpers/#sec-iteratorprototype.find
JS_DEFINE_NATIVE_FUNCTION(IteratorPrototype::find)
{
    auto predicate = vm.argument(0);

    // 1. Let O be the this value.
    // 2. If O is not an Object, throw a TypeError exception.
    auto object = TRY(this_object(vm));

    // 3. If IsCallable(predicate) is false, throw a TypeError exception.
    if (!predicate.is_function())
        return vm.throw_completion<TypeError>(ErrorType::NotAFunction, "predicate"sv);

    // 4. Let iterated be ? GetIteratorDirect(O).
    auto iterated = TRY(get_iterator_direct(vm, object));

    // 5. Let counter be 0.
    size_t counter = 0;

    // 6. Repeat,
    while (true) {
        // a. Let next be ? IteratorStep(iterated).
        auto next = TRY(iterator_step(vm, iterated));

        // b. If next is false, return undefined.
        if (!next)
            return js_undefined();

        // c. Let value be ? IteratorValue(next).
        auto value = TRY(iterator_value(vm, *next));

        // d. Let result be Completion(Call(predicate, undefined, « value, 𝔽(counter) »)).
        auto result = call(vm, predicate.as_function(), js_undefined(), value, Value { counter });

        // e. IfAbruptCloseIterator(result, iterated).
        if (result.is_error())
            return *TRY(iterator_close(vm, iterated, result.release_error()));

        // f. If ToBoolean(result) is true, return ? IteratorClose(iterated, NormalCompletion(value)).
        if (result.value().to_boolean())
            return *TRY(iterator_close(vm, iterated, normal_completion(value)));

        // g. Set counter to counter + 1.
        ++counter;
    }
}

}
