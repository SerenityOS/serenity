/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2020, Marcin Gasperowicz <xnooga@gmail.com>
 * Copyright (c) 2021, David Tuin <davidot@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <AK/HashTable.h>
#include <AK/ScopeGuard.h>
#include <AK/StringBuilder.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/ArrayConstructor.h>
#include <LibJS/Runtime/ArrayIterator.h>
#include <LibJS/Runtime/ArrayPrototype.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Map.h>
#include <LibJS/Runtime/ObjectPrototype.h>
#include <LibJS/Runtime/Realm.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

static HashTable<Object*> s_array_join_seen_objects;

ArrayPrototype::ArrayPrototype(GlobalObject& global_object)
    : Array(*global_object.object_prototype())
{
}

void ArrayPrototype::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    Array::initialize(global_object);
    u8 attr = Attribute::Writable | Attribute::Configurable;

    define_native_function(vm.names.filter, filter, 1, attr);
    define_native_function(vm.names.forEach, for_each, 1, attr);
    define_native_function(vm.names.map, map, 1, attr);
    define_native_function(vm.names.pop, pop, 0, attr);
    define_native_function(vm.names.push, push, 1, attr);
    define_native_function(vm.names.shift, shift, 0, attr);
    define_native_function(vm.names.toString, to_string, 0, attr);
    define_native_function(vm.names.toLocaleString, to_locale_string, 0, attr);
    define_native_function(vm.names.unshift, unshift, 1, attr);
    define_native_function(vm.names.join, join, 1, attr);
    define_native_function(vm.names.concat, concat, 1, attr);
    define_native_function(vm.names.slice, slice, 2, attr);
    define_native_function(vm.names.indexOf, index_of, 1, attr);
    define_native_function(vm.names.reduce, reduce, 1, attr);
    define_native_function(vm.names.reduceRight, reduce_right, 1, attr);
    define_native_function(vm.names.reverse, reverse, 0, attr);
    define_native_function(vm.names.sort, sort, 1, attr);
    define_native_function(vm.names.lastIndexOf, last_index_of, 1, attr);
    define_native_function(vm.names.includes, includes, 1, attr);
    define_native_function(vm.names.find, find, 1, attr);
    define_native_function(vm.names.findIndex, find_index, 1, attr);
    define_native_function(vm.names.findLast, find_last, 1, attr);
    define_native_function(vm.names.findLastIndex, find_last_index, 1, attr);
    define_native_function(vm.names.some, some, 1, attr);
    define_native_function(vm.names.every, every, 1, attr);
    define_native_function(vm.names.splice, splice, 2, attr);
    define_native_function(vm.names.fill, fill, 1, attr);
    define_native_function(vm.names.values, values, 0, attr);
    define_native_function(vm.names.flat, flat, 0, attr);
    define_native_function(vm.names.flatMap, flat_map, 1, attr);
    define_native_function(vm.names.at, at, 1, attr);
    define_native_function(vm.names.keys, keys, 0, attr);
    define_native_function(vm.names.entries, entries, 0, attr);
    define_native_function(vm.names.copyWithin, copy_within, 2, attr);
    define_native_function(vm.names.groupBy, group_by, 1, attr);
    define_native_function(vm.names.groupByToMap, group_by_to_map, 1, attr);

    // Use define_direct_property here instead of define_native_function so that
    // Object.is(Array.prototype[Symbol.iterator], Array.prototype.values)
    // evaluates to true
    // 23.1.3.34 Array.prototype [ @@iterator ] ( ), https://tc39.es/ecma262/#sec-array.prototype-@@iterator
    define_direct_property(*vm.well_known_symbol_iterator(), get_without_side_effects(vm.names.values), attr);

    // 23.1.3.35 Array.prototype [ @@unscopables ], https://tc39.es/ecma262/#sec-array.prototype-@@unscopables
    // With find from last proposal, https://tc39.es/proposal-array-find-from-last/#sec-array.prototype-@@unscopables
    // With array grouping proposal, https://tc39.es/proposal-array-grouping/#sec-array.prototype-@@unscopables
    auto* unscopable_list = Object::create(global_object, nullptr);
    MUST(unscopable_list->create_data_property_or_throw(vm.names.at, Value(true)));
    MUST(unscopable_list->create_data_property_or_throw(vm.names.copyWithin, Value(true)));
    MUST(unscopable_list->create_data_property_or_throw(vm.names.entries, Value(true)));
    MUST(unscopable_list->create_data_property_or_throw(vm.names.fill, Value(true)));
    MUST(unscopable_list->create_data_property_or_throw(vm.names.find, Value(true)));
    MUST(unscopable_list->create_data_property_or_throw(vm.names.findIndex, Value(true)));
    MUST(unscopable_list->create_data_property_or_throw(vm.names.findLast, Value(true)));
    MUST(unscopable_list->create_data_property_or_throw(vm.names.findLastIndex, Value(true)));
    MUST(unscopable_list->create_data_property_or_throw(vm.names.flat, Value(true)));
    MUST(unscopable_list->create_data_property_or_throw(vm.names.flatMap, Value(true)));
    MUST(unscopable_list->create_data_property_or_throw(vm.names.groupBy, Value(true)));
    MUST(unscopable_list->create_data_property_or_throw(vm.names.groupByToMap, Value(true)));
    MUST(unscopable_list->create_data_property_or_throw(vm.names.includes, Value(true)));
    MUST(unscopable_list->create_data_property_or_throw(vm.names.keys, Value(true)));
    MUST(unscopable_list->create_data_property_or_throw(vm.names.values, Value(true)));

    define_direct_property(*vm.well_known_symbol_unscopables(), unscopable_list, Attribute::Configurable);
}

ArrayPrototype::~ArrayPrototype()
{
}

// 10.4.2.3 ArraySpeciesCreate ( originalArray, length ), https://tc39.es/ecma262/#sec-arrayspeciescreate
static ThrowCompletionOr<Object*> array_species_create(GlobalObject& global_object, Object& original_array, size_t length)
{
    auto& vm = global_object.vm();

    auto is_array = TRY(Value(&original_array).is_array(global_object));

    if (!is_array)
        return TRY(Array::create(global_object, length));

    auto constructor = TRY(original_array.get(vm.names.constructor));
    if (constructor.is_constructor()) {
        auto& constructor_function = constructor.as_function();
        auto* this_realm = vm.current_realm();
        auto* constructor_realm = TRY(get_function_realm(global_object, constructor_function));
        if (constructor_realm != this_realm) {
            if (&constructor_function == constructor_realm->global_object().array_constructor())
                constructor = js_undefined();
        }
    }

    if (constructor.is_object()) {
        constructor = TRY(constructor.as_object().get(*vm.well_known_symbol_species()));
        if (constructor.is_null())
            constructor = js_undefined();
    }

    if (constructor.is_undefined())
        return TRY(Array::create(global_object, length));

    if (!constructor.is_constructor())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAConstructor, constructor.to_string_without_side_effects());

    return TRY(construct(global_object, constructor.as_function(), Value(length)));
}

// 23.1.3.8 Array.prototype.filter ( callbackfn [ , thisArg ] ), https://tc39.es/ecma262/#sec-array.prototype.filter
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::filter)
{
    auto callback_function = vm.argument(0);
    auto this_arg = vm.argument(1);

    // 1. Let O be ? ToObject(this value).
    auto* object = TRY(vm.this_value(global_object).to_object(global_object));

    // 2. Let len be ? LengthOfArrayLike(O).
    auto length = TRY(length_of_array_like(global_object, *object));

    // 3. If IsCallable(callbackfn) is false, throw a TypeError exception.
    if (!callback_function.is_function())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAFunction, callback_function.to_string_without_side_effects());

    // 4. Let A be ? ArraySpeciesCreate(O, 0).
    auto* array = TRY(array_species_create(global_object, *object, 0));

    // 5. Let k be 0.
    size_t k = 0;

    // 6. Let to be 0.
    size_t to = 0;

    // 7. Repeat, while k < len,
    for (; k < length; ++k) {
        // a. Let Pk be ! ToString(𝔽(k)).
        auto property_key = PropertyKey { k };

        // b. Let kPresent be ? HasProperty(O, Pk).
        auto k_present = TRY(object->has_property(property_key));

        // c. If kPresent is true, then
        if (k_present) {
            // i. Let kValue be ? Get(O, Pk).
            auto k_value = TRY(object->get(k));

            // ii. Let selected be ! ToBoolean(? Call(callbackfn, thisArg, « kValue, 𝔽(k), O »)).
            auto selected = TRY(call(global_object, callback_function.as_function(), this_arg, k_value, Value(k), object)).to_boolean();

            // iii. If selected is true, then
            if (selected) {
                // 1. Perform ? CreateDataPropertyOrThrow(A, ! ToString(𝔽(to)), kValue).
                TRY(array->create_data_property_or_throw(to, k_value));

                // 2. Set to to to + 1.
                ++to;
            }
        }

        // d. Set k to k + 1.
    }

    // 8. Return A.
    return array;
}

// 23.1.3.13 Array.prototype.forEach ( callbackfn [ , thisArg ] ), https://tc39.es/ecma262/#sec-array.prototype.foreach
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::for_each)
{
    auto callback_function = vm.argument(0);
    auto this_arg = vm.argument(1);

    // 1. Let O be ? ToObject(this value).
    auto* object = TRY(vm.this_value(global_object).to_object(global_object));

    // 2. Let len be ? LengthOfArrayLike(O).
    auto length = TRY(length_of_array_like(global_object, *object));

    // 3. If IsCallable(callbackfn) is false, throw a TypeError exception.
    if (!callback_function.is_function())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAFunction, callback_function.to_string_without_side_effects());

    // 4. Let k be 0.
    // 5. Repeat, while k < len,
    for (size_t k = 0; k < length; ++k) {
        // a. Let Pk be ! ToString(𝔽(k)).
        auto property_key = PropertyKey { k };

        // b. Let kPresent be ? HasProperty(O, Pk).
        auto k_present = TRY(object->has_property(property_key));

        // c. If kPresent is true, then
        if (k_present) {
            // i. Let kValue be ? Get(O, Pk).
            auto k_value = TRY(object->get(property_key));

            // ii. Perform ? Call(callbackfn, thisArg, « kValue, 𝔽(k), O »).
            TRY(call(global_object, callback_function.as_function(), this_arg, k_value, Value(k), object));
        }

        // d. Set k to k + 1.
    }

    // 6. Return undefined.
    return js_undefined();
}

// 23.1.3.19 Array.prototype.map ( callbackfn [ , thisArg ] ), https://tc39.es/ecma262/#sec-array.prototype.map
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::map)
{
    auto callback_function = vm.argument(0);
    auto this_arg = vm.argument(1);

    // 1. Let O be ? ToObject(this value).
    auto* object = TRY(vm.this_value(global_object).to_object(global_object));

    // 2. Let len be ? LengthOfArrayLike(O).
    auto length = TRY(length_of_array_like(global_object, *object));

    // 3. If IsCallable(callbackfn) is false, throw a TypeError exception.
    if (!callback_function.is_function())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAFunction, callback_function.to_string_without_side_effects());

    // 4. Let A be ? ArraySpeciesCreate(O, len).
    auto* array = TRY(array_species_create(global_object, *object, length));

    // 5. Let k be 0.
    // 6. Repeat, while k < len,
    for (size_t k = 0; k < length; ++k) {
        // a. Let Pk be ! ToString(𝔽(k)).
        auto property_key = PropertyKey { k };

        // b. Let kPresent be ? HasProperty(O, Pk).
        auto k_present = TRY(object->has_property(property_key));

        // c. If kPresent is true, then
        if (k_present) {
            // i. Let kValue be ? Get(O, Pk).
            auto k_value = TRY(object->get(property_key));

            // ii. Let mappedValue be ? Call(callbackfn, thisArg, « kValue, 𝔽(k), O »).
            auto mapped_value = TRY(call(global_object, callback_function.as_function(), this_arg, k_value, Value(k), object));

            // iii. Perform ? CreateDataPropertyOrThrow(A, Pk, mappedValue).
            TRY(array->create_data_property_or_throw(property_key, mapped_value));
        }

        // d. Set k to k + 1.
    }

    // 7. Return A.
    return array;
}

// 23.1.3.21 Array.prototype.push ( ...items ), https://tc39.es/ecma262/#sec-array.prototype.push
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::push)
{
    auto* this_object = TRY(vm.this_value(global_object).to_object(global_object));
    auto length = TRY(length_of_array_like(global_object, *this_object));
    auto argument_count = vm.argument_count();
    auto new_length = length + argument_count;
    if (new_length > MAX_ARRAY_LIKE_INDEX)
        return vm.throw_completion<TypeError>(global_object, ErrorType::ArrayMaxSize);
    for (size_t i = 0; i < argument_count; ++i)
        TRY(this_object->set(length + i, vm.argument(i), Object::ShouldThrowExceptions::Yes));
    auto new_length_value = Value(new_length);
    TRY(this_object->set(vm.names.length, new_length_value, Object::ShouldThrowExceptions::Yes));
    return new_length_value;
}

// 23.1.3.32 Array.prototype.unshift ( ...items ), https://tc39.es/ecma262/#sec-array.prototype.unshift
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::unshift)
{
    auto* this_object = TRY(vm.this_value(global_object).to_object(global_object));
    auto length = TRY(length_of_array_like(global_object, *this_object));
    auto arg_count = vm.argument_count();
    size_t new_length = length + arg_count;
    if (arg_count > 0) {
        if (new_length > MAX_ARRAY_LIKE_INDEX)
            return vm.throw_completion<TypeError>(global_object, ErrorType::ArrayMaxSize);

        for (size_t k = length; k > 0; --k) {
            auto from = k - 1;
            auto to = k + arg_count - 1;

            bool from_present = TRY(this_object->has_property(from));
            if (from_present) {
                auto from_value = TRY(this_object->get(from));
                TRY(this_object->set(to, from_value, Object::ShouldThrowExceptions::Yes));
            } else {
                TRY(this_object->delete_property_or_throw(to));
            }
        }

        for (size_t j = 0; j < arg_count; j++)
            TRY(this_object->set(j, vm.argument(j), Object::ShouldThrowExceptions::Yes));
    }

    TRY(this_object->set(vm.names.length, Value(new_length), Object::ShouldThrowExceptions::Yes));
    return Value(new_length);
}

// 23.1.3.20 Array.prototype.pop ( ), https://tc39.es/ecma262/#sec-array.prototype.pop
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::pop)
{
    auto* this_object = TRY(vm.this_value(global_object).to_object(global_object));
    auto length = TRY(length_of_array_like(global_object, *this_object));
    if (length == 0) {
        TRY(this_object->set(vm.names.length, Value(0), Object::ShouldThrowExceptions::Yes));
        return js_undefined();
    }
    auto index = length - 1;
    auto element = TRY(this_object->get(index));
    TRY(this_object->delete_property_or_throw(index));
    TRY(this_object->set(vm.names.length, Value(index), Object::ShouldThrowExceptions::Yes));
    return element;
}

// 23.1.3.25 Array.prototype.shift ( ), https://tc39.es/ecma262/#sec-array.prototype.shift
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::shift)
{
    auto* this_object = TRY(vm.this_value(global_object).to_object(global_object));
    auto length = TRY(length_of_array_like(global_object, *this_object));
    if (length == 0) {
        TRY(this_object->set(vm.names.length, Value(0), Object::ShouldThrowExceptions::Yes));
        return js_undefined();
    }
    auto first = TRY(this_object->get(0));
    for (size_t k = 1; k < length; ++k) {
        size_t from = k;
        size_t to = k - 1;
        bool from_present = TRY(this_object->has_property(from));
        if (from_present) {
            auto from_value = TRY(this_object->get(from));
            TRY(this_object->set(to, from_value, Object::ShouldThrowExceptions::Yes));
        } else {
            TRY(this_object->delete_property_or_throw(to));
        }
    }

    TRY(this_object->delete_property_or_throw(length - 1));
    TRY(this_object->set(vm.names.length, Value(length - 1), Object::ShouldThrowExceptions::Yes));
    return first;
}

// 23.1.3.31 Array.prototype.toString ( ), https://tc39.es/ecma262/#sec-array.prototype.tostring
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::to_string)
{
    auto* this_object = TRY(vm.this_value(global_object).to_object(global_object));
    auto join_function = TRY(this_object->get(vm.names.join));
    if (!join_function.is_function())
        return ObjectPrototype::to_string(vm, global_object);
    return TRY(call(global_object, join_function.as_function(), this_object));
}

// 19.5.1 Array.prototype.toLocaleString ( [ locales [ , options ] ] ), https://tc39.es/ecma402/#sup-array.prototype.tolocalestring
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::to_locale_string)
{
    auto locales = vm.argument(0);
    auto options = vm.argument(1);

    // 1. Let array be ? ToObject(this value).
    auto* this_object = TRY(vm.this_value(global_object).to_object(global_object));

    if (s_array_join_seen_objects.contains(this_object))
        return js_string(vm, "");
    s_array_join_seen_objects.set(this_object);
    ArmedScopeGuard unsee_object_guard = [&] {
        s_array_join_seen_objects.remove(this_object);
    };

    // 2. Let len be ? ToLength(? Get(array, "length")).
    auto length = TRY(length_of_array_like(global_object, *this_object));

    // 3. Let separator be the String value for the list-separator String appropriate for the host environment's current locale (this is derived in an implementation-defined way).
    constexpr auto separator = ","sv;

    // 4. Let R be the empty String.
    StringBuilder builder;

    // 5. Let k be 0.
    // 6. Repeat, while k < len,
    for (size_t i = 0; i < length; ++i) {
        // a. If k > 0, then
        if (i > 0) {
            // i. Set R to the string-concatenation of R and separator.
            builder.append(separator);
        }

        // b. Let nextElement be ? Get(array, ! ToString(k)).
        auto value = TRY(this_object->get(i));

        // c. If nextElement is not undefined or null, then
        if (!value.is_nullish()) {
            // i. Let S be ? ToString(? Invoke(nextElement, "toLocaleString", « locales, options »)).
            auto locale_string_result = TRY(value.invoke(global_object, vm.names.toLocaleString, locales, options));

            // ii. Set R to the string-concatenation of R and S.
            auto string = TRY(locale_string_result.to_string(global_object));
            builder.append(string);
        }

        // d. Increase k by 1.
    }

    // 7. Return R.
    return js_string(vm, builder.to_string());
}

// 23.1.3.16 Array.prototype.join ( separator ), https://tc39.es/ecma262/#sec-array.prototype.join
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::join)
{
    auto* this_object = TRY(vm.this_value(global_object).to_object(global_object));

    // This is not part of the spec, but all major engines do some kind of circular reference checks.
    // FWIW: engine262, a "100% spec compliant" ECMA-262 impl, aborts with "too much recursion".
    // Same applies to Array.prototype.toLocaleString().
    if (s_array_join_seen_objects.contains(this_object))
        return js_string(vm, "");
    s_array_join_seen_objects.set(this_object);
    ArmedScopeGuard unsee_object_guard = [&] {
        s_array_join_seen_objects.remove(this_object);
    };

    auto length = TRY(length_of_array_like(global_object, *this_object));
    String separator = ",";
    if (!vm.argument(0).is_undefined())
        separator = TRY(vm.argument(0).to_string(global_object));
    StringBuilder builder;
    for (size_t i = 0; i < length; ++i) {
        if (i > 0)
            builder.append(separator);
        auto value = TRY(this_object->get(i));
        if (value.is_nullish())
            continue;
        auto string = TRY(value.to_string(global_object));
        builder.append(string);
    }

    return js_string(vm, builder.to_string());
}

// 23.1.3.2 Array.prototype.concat ( ...items ), https://tc39.es/ecma262/#sec-array.prototype.concat
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::concat)
{
    auto* this_object = TRY(vm.this_value(global_object).to_object(global_object));

    auto* new_array = TRY(array_species_create(global_object, *this_object, 0));

    size_t n = 0;

    // 23.1.3.2.1 IsConcatSpreadable ( O ), https://tc39.es/ecma262/#sec-isconcatspreadable
    auto is_concat_spreadable = [&vm, &global_object](Value const& val) -> ThrowCompletionOr<bool> {
        if (!val.is_object())
            return false;
        auto& object = val.as_object();
        auto spreadable = TRY(object.get(*vm.well_known_symbol_is_concat_spreadable()));
        if (!spreadable.is_undefined())
            return spreadable.to_boolean();

        return TRY(val.is_array(global_object));
    };

    auto append_to_new_array = [&vm, &is_concat_spreadable, &new_array, &global_object, &n](Value arg) -> ThrowCompletionOr<void> {
        auto spreadable = TRY(is_concat_spreadable(arg));
        if (spreadable) {
            VERIFY(arg.is_object());
            Object& obj = arg.as_object();
            size_t k = 0;
            auto length = TRY(length_of_array_like(global_object, obj));

            if (n + length > MAX_ARRAY_LIKE_INDEX)
                return vm.throw_completion<TypeError>(global_object, ErrorType::ArrayMaxSize);
            while (k < length) {
                auto k_exists = TRY(obj.has_property(k));
                if (k_exists) {
                    auto k_value = TRY(obj.get(k));
                    TRY(new_array->create_data_property_or_throw(n, k_value));
                }
                ++n;
                ++k;
            }
        } else {
            if (n >= MAX_ARRAY_LIKE_INDEX)
                return vm.throw_completion<TypeError>(global_object, ErrorType::ArrayMaxSize);
            TRY(new_array->create_data_property_or_throw(n, arg));
            ++n;
        }
        return {};
    };

    TRY(append_to_new_array(this_object));

    for (size_t i = 0; i < vm.argument_count(); ++i)
        TRY(append_to_new_array(vm.argument(i)));

    TRY(new_array->set(vm.names.length, Value(n), Object::ShouldThrowExceptions::Yes));
    return Value(new_array);
}

// 23.1.3.26 Array.prototype.slice ( start, end ), https://tc39.es/ecma262/#sec-array.prototype.slice
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::slice)
{
    auto* this_object = TRY(vm.this_value(global_object).to_object(global_object));

    auto initial_length = TRY(length_of_array_like(global_object, *this_object));

    auto relative_start = TRY(vm.argument(0).to_integer_or_infinity(global_object));

    double actual_start;

    if (Value(relative_start).is_negative_infinity())
        actual_start = 0.0;
    else if (relative_start < 0.0)
        actual_start = max((double)initial_length + relative_start, 0.0);
    else
        actual_start = min(relative_start, (double)initial_length);

    double relative_end;

    if (vm.argument(1).is_undefined() || vm.argument(1).is_empty())
        relative_end = (double)initial_length;
    else
        relative_end = TRY(vm.argument(1).to_integer_or_infinity(global_object));

    double final;

    if (Value(relative_end).is_negative_infinity())
        final = 0.0;
    else if (relative_end < 0.0)
        final = max((double)initial_length + relative_end, 0.0);
    else
        final = min(relative_end, (double)initial_length);

    auto count = max(final - actual_start, 0.0);

    auto* new_array = TRY(array_species_create(global_object, *this_object, count));

    size_t index = 0;
    size_t k = actual_start;

    while (k < final) {
        bool present = TRY(this_object->has_property(k));
        if (present) {
            auto value = TRY(this_object->get(k));
            TRY(new_array->create_data_property_or_throw(index, value));
        }

        ++k;
        ++index;
    }

    TRY(new_array->set(vm.names.length, Value(index), Object::ShouldThrowExceptions::Yes));
    return new_array;
}

// 23.1.3.15 Array.prototype.indexOf ( searchElement [ , fromIndex ] ), https://tc39.es/ecma262/#sec-array.prototype.indexof
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::index_of)
{
    auto search_element = vm.argument(0);
    auto from_index = vm.argument(1);

    // 1. Let O be ? ToObject(this value).
    auto* object = TRY(vm.this_value(global_object).to_object(global_object));

    // 2. Let len be ? LengthOfArrayLike(O).
    auto length = TRY(length_of_array_like(global_object, *object));

    // 3. If len is 0, return -1𝔽.
    if (length == 0)
        return Value(-1);

    // 4. Let n be ? ToIntegerOrInfinity(fromIndex).
    auto n = TRY(from_index.to_integer_or_infinity(global_object));

    // 5. Assert: If fromIndex is undefined, then n is 0.
    if (from_index.is_undefined())
        VERIFY(n == 0);

    // 6. If n is +∞, return -1𝔽.
    if (Value(n).is_positive_infinity())
        return Value(-1);

    // 7. Else if n is -∞, set n to 0.
    if (Value(n).is_negative_infinity())
        n = 0;

    size_t k;

    // 8. If n ≥ 0, then
    if (n >= 0) {
        // a. Let k be n.
        k = (size_t)n;
    }
    // 9. Else,
    else {
        // a. Let k be len + n.
        // b. If k < 0, set k to 0.
        k = max(length + n, 0);
    }

    // 10. Repeat, while k < len,
    for (; k < length; ++k) {
        auto property_key = PropertyKey { k };

        // a. Let kPresent be ? HasProperty(O, ! ToString(𝔽(k))).
        auto k_present = TRY(object->has_property(property_key));

        // b. If kPresent is true, then
        if (k_present) {
            // i. Let elementK be ? Get(O, ! ToString(𝔽(k))).
            auto element_k = TRY(object->get(property_key));

            // ii. Let same be IsStrictlyEqual(searchElement, elementK).
            auto same = is_strictly_equal(search_element, element_k);

            // iii. If same is true, return 𝔽(k).
            if (same)
                return Value(k);
        }

        // c. Set k to k + 1.
    }

    // 11. Return -1𝔽.
    return Value(-1);
}

// 23.1.3.22 Array.prototype.reduce ( callbackfn [ , initialValue ] ), https://tc39.es/ecma262/#sec-array.prototype.reduce
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::reduce)
{
    auto callback_function = vm.argument(0);
    auto initial_value = vm.argument(1);

    // 1. Let O be ? ToObject(this value).
    auto* object = TRY(vm.this_value(global_object).to_object(global_object));

    // 2. Let len be ? LengthOfArrayLike(O).
    auto length = TRY(length_of_array_like(global_object, *object));

    // 3. If IsCallable(callbackfn) is false, throw a TypeError exception.
    if (!callback_function.is_function())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAFunction, callback_function.to_string_without_side_effects());

    // 4. If len = 0 and initialValue is not present, throw a TypeError exception.
    if (length == 0 && vm.argument_count() <= 1)
        return vm.throw_completion<TypeError>(global_object, ErrorType::ReduceNoInitial);

    // 5. Let k be 0.
    size_t k = 0;

    // 6. Let accumulator be undefined.
    auto accumulator = js_undefined();

    // 7. If initialValue is present, then
    if (vm.argument_count() > 1) {
        // a. Set accumulator to initialValue.
        accumulator = initial_value;
    }
    // 8. Else,
    else {
        // a. Let kPresent be false.
        bool k_present = false;

        // b. Repeat, while kPresent is false and k < len,
        for (; !k_present && k < length; ++k) {
            // i. Let Pk be ! ToString(𝔽(k)).
            auto property_key = PropertyKey { k };

            // ii. Set kPresent to ? HasProperty(O, Pk).
            k_present = TRY(object->has_property(property_key));

            // iii. If kPresent is true, then
            if (k_present) {
                // 1. Set accumulator to ? Get(O, Pk).
                accumulator = TRY(object->get(property_key));
            }

            // iv. Set k to k + 1.
        }

        // c. If kPresent is false, throw a TypeError exception.
        if (!k_present)
            return vm.throw_completion<TypeError>(global_object, ErrorType::ReduceNoInitial);
    }

    // 9. Repeat, while k < len,
    for (; k < length; ++k) {
        // a. Let Pk be ! ToString(𝔽(k)).
        auto property_key = PropertyKey { k };

        // b. Let kPresent be ? HasProperty(O, Pk).
        auto k_present = TRY(object->has_property(property_key));

        // c. If kPresent is true, then
        if (k_present) {
            // i. Let kValue be ? Get(O, Pk).
            auto k_value = TRY(object->get(property_key));

            // ii. Set accumulator to ? Call(callbackfn, undefined, « accumulator, kValue, 𝔽(k), O »).
            accumulator = TRY(call(global_object, callback_function.as_function(), js_undefined(), accumulator, k_value, Value(k), object));
        }

        // d. Set k to k + 1.
    }

    // 10. Return accumulator.
    return accumulator;
}

// 23.1.3.23 Array.prototype.reduceRight ( callbackfn [ , initialValue ] ), https://tc39.es/ecma262/#sec-array.prototype.reduceright
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::reduce_right)
{
    auto callback_function = vm.argument(0);
    auto initial_value = vm.argument(1);

    // 1. Let O be ? ToObject(this value).
    auto* object = TRY(vm.this_value(global_object).to_object(global_object));

    // 2. Let len be ? LengthOfArrayLike(O).
    auto length = TRY(length_of_array_like(global_object, *object));

    // 3. If IsCallable(callbackfn) is false, throw a TypeError exception.
    if (!callback_function.is_function())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAFunction, callback_function.to_string_without_side_effects());

    // 4. If len = 0 and initialValue is not present, throw a TypeError exception.
    if (length == 0 && vm.argument_count() <= 1)
        return vm.throw_completion<TypeError>(global_object, ErrorType::ReduceNoInitial);

    // 5. Let k be len - 1.
    ssize_t k = length - 1;

    // 6. Let accumulator be undefined.
    auto accumulator = js_undefined();

    // 7. If initialValue is present, then
    if (vm.argument_count() > 1) {
        // a. Set accumulator to initialValue.
        accumulator = initial_value;
    }
    // 8. Else,
    else {
        // a. Let kPresent be false.
        bool k_present = false;

        // b. Repeat, while kPresent is false and k ≥ 0,
        for (; !k_present && k >= 0; --k) {
            // i. Let Pk be ! ToString(𝔽(k)).
            auto property_key = PropertyKey { k };

            // ii. Set kPresent to ? HasProperty(O, Pk).
            k_present = TRY(object->has_property(property_key));

            // iii. If kPresent is true, then
            if (k_present) {
                // 1. Set accumulator to ? Get(O, Pk).
                accumulator = TRY(object->get(property_key));
            }

            // iv. Set k to k - 1.
        }

        // c. If kPresent is false, throw a TypeError exception.
        if (!k_present)
            return vm.throw_completion<TypeError>(global_object, ErrorType::ReduceNoInitial);
    }

    // 9. Repeat, while k ≥ 0,
    for (; k >= 0; --k) {
        // a. Let Pk be ! ToString(𝔽(k)).
        auto property_key = PropertyKey { k };

        // b. Let kPresent be ? HasProperty(O, Pk).
        auto k_present = TRY(object->has_property(property_key));

        // c. If kPresent is true, then
        if (k_present) {
            // i. Let kValue be ? Get(O, Pk).
            auto k_value = TRY(object->get(property_key));

            // ii. Set accumulator to ? Call(callbackfn, undefined, « accumulator, kValue, 𝔽(k), O »).
            accumulator = TRY(call(global_object, callback_function.as_function(), js_undefined(), accumulator, k_value, Value((size_t)k), object));
        }

        // d. Set k to k - 1.
    }

    // 10. Return accumulator.
    return accumulator;
}

// 23.1.3.24 Array.prototype.reverse ( ), https://tc39.es/ecma262/#sec-array.prototype.reverse
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::reverse)
{
    auto* this_object = TRY(vm.this_value(global_object).to_object(global_object));
    auto length = TRY(length_of_array_like(global_object, *this_object));

    auto middle = length / 2;
    for (size_t lower = 0; lower < middle; ++lower) {
        auto upper = length - lower - 1;

        auto lower_exists = TRY(this_object->has_property(lower));
        Value lower_value;
        if (lower_exists)
            lower_value = TRY(this_object->get(lower));

        auto upper_exists = TRY(this_object->has_property(upper));
        Value upper_value;
        if (upper_exists)
            upper_value = TRY(this_object->get(upper));

        if (lower_exists && upper_exists) {
            TRY(this_object->set(lower, upper_value, Object::ShouldThrowExceptions::Yes));
            TRY(this_object->set(upper, lower_value, Object::ShouldThrowExceptions::Yes));
        } else if (!lower_exists && upper_exists) {
            TRY(this_object->set(lower, upper_value, Object::ShouldThrowExceptions::Yes));
            TRY(this_object->delete_property_or_throw(upper));
        } else if (lower_exists && !upper_exists) {
            TRY(this_object->delete_property_or_throw(lower));
            TRY(this_object->set(upper, lower_value, Object::ShouldThrowExceptions::Yes));
        }
    }

    return this_object;
}

static ThrowCompletionOr<void> array_merge_sort(VM& vm, GlobalObject& global_object, FunctionObject* compare_func, MarkedValueList& arr_to_sort)
{
    // FIXME: it would probably be better to switch to insertion sort for small arrays for
    // better performance
    if (arr_to_sort.size() <= 1)
        return {};

    MarkedValueList left(vm.heap());
    MarkedValueList right(vm.heap());

    left.ensure_capacity(arr_to_sort.size() / 2);
    right.ensure_capacity(arr_to_sort.size() / 2 + (arr_to_sort.size() & 1));

    for (size_t i = 0; i < arr_to_sort.size(); ++i) {
        if (i < arr_to_sort.size() / 2) {
            left.append(arr_to_sort[i]);
        } else {
            right.append(arr_to_sort[i]);
        }
    }

    TRY(array_merge_sort(vm, global_object, compare_func, left));
    TRY(array_merge_sort(vm, global_object, compare_func, right));

    arr_to_sort.clear();

    size_t left_index = 0, right_index = 0;

    while (left_index < left.size() && right_index < right.size()) {
        auto x = left[left_index];
        auto y = right[right_index];

        double comparison_result;

        if (x.is_undefined() && y.is_undefined()) {
            comparison_result = 0;
        } else if (x.is_undefined()) {
            comparison_result = 1;
        } else if (y.is_undefined()) {
            comparison_result = -1;
        } else if (compare_func) {
            auto call_result = TRY(call(global_object, *compare_func, js_undefined(), left[left_index], right[right_index]));
            auto number = TRY(call_result.to_number(global_object));
            if (number.is_nan())
                comparison_result = 0;
            else
                comparison_result = number.as_double();
        } else {
            // FIXME: It would probably be much better to be smarter about this and implement
            // the Abstract Relational Comparison in line once iterating over code points, rather
            // than calling it twice after creating two primitive strings.

            auto x_string = TRY(x.to_primitive_string(global_object));

            auto y_string = TRY(y.to_primitive_string(global_object));

            auto x_string_value = Value(x_string);
            auto y_string_value = Value(y_string);

            // Because they are called with primitive strings, these is_less_than calls
            // should never result in a VM exception.
            auto x_lt_y_relation = MUST(is_less_than(global_object, true, x_string_value, y_string_value));
            VERIFY(x_lt_y_relation != TriState::Unknown);
            auto y_lt_x_relation = MUST(is_less_than(global_object, true, y_string_value, x_string_value));
            VERIFY(y_lt_x_relation != TriState::Unknown);

            if (x_lt_y_relation == TriState::True) {
                comparison_result = -1;
            } else if (y_lt_x_relation == TriState::True) {
                comparison_result = 1;
            } else {
                comparison_result = 0;
            }
        }

        if (comparison_result <= 0) {
            arr_to_sort.append(left[left_index]);
            left_index++;
        } else {
            arr_to_sort.append(right[right_index]);
            right_index++;
        }
    }

    while (left_index < left.size()) {
        arr_to_sort.append(left[left_index]);
        left_index++;
    }

    while (right_index < right.size()) {
        arr_to_sort.append(right[right_index]);
        right_index++;
    }

    return {};
}

// 23.1.3.28 Array.prototype.sort ( comparefn ), https://tc39.es/ecma262/#sec-array.prototype.sort
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::sort)
{
    auto callback = vm.argument(0);
    if (!callback.is_undefined() && !callback.is_function())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAFunction, callback.to_string_without_side_effects());

    auto* object = TRY(vm.this_value(global_object).to_object(global_object));

    auto length = TRY(length_of_array_like(global_object, *object));

    MarkedValueList items(vm.heap());
    for (size_t k = 0; k < length; ++k) {
        auto k_present = TRY(object->has_property(k));

        if (k_present) {
            auto k_value = TRY(object->get(k));
            items.append(k_value);
        }
    }

    // Perform sorting by merge sort. This isn't as efficient compared to quick sort, but
    // quicksort can't be used in all cases because the spec requires Array.prototype.sort()
    // to be stable. FIXME: when initially scanning through the array, maintain a flag
    // for if an unstable sort would be indistinguishable from a stable sort (such as just
    // just strings or numbers), and in that case use quick sort instead for better performance.
    TRY(array_merge_sort(vm, global_object, callback.is_undefined() ? nullptr : &callback.as_function(), items));

    for (size_t j = 0; j < items.size(); ++j)
        TRY(object->set(j, items[j], Object::ShouldThrowExceptions::Yes));

    // The empty parts of the array are always sorted to the end, regardless of the
    // compare function. FIXME: For performance, a similar process could be used
    // for undefined, which are sorted to right before the empty values.
    for (size_t j = items.size(); j < length; ++j)
        TRY(object->delete_property_or_throw(j));

    return object;
}

// 23.1.3.18 Array.prototype.lastIndexOf ( searchElement [ , fromIndex ] ), https://tc39.es/ecma262/#sec-array.prototype.lastindexof
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::last_index_of)
{
    auto search_element = vm.argument(0);
    auto from_index = vm.argument(1);

    // 1. Let O be ? ToObject(this value).
    auto* object = TRY(vm.this_value(global_object).to_object(global_object));

    // 2. Let len be ? LengthOfArrayLike(O).
    auto length = TRY(length_of_array_like(global_object, *object));

    // 3. If len is 0, return -1𝔽.
    if (length == 0)
        return Value(-1);

    double n;

    // 4. If fromIndex is present, let n be ? ToIntegerOrInfinity(fromIndex); else let n be len - 1.
    if (vm.argument_count() >= 2)
        n = TRY(from_index.to_integer_or_infinity(global_object));
    else
        n = (double)length - 1;

    // 5. If n is -∞, return -1𝔽.
    if (Value(n).is_negative_infinity())
        return Value(-1);

    ssize_t k;

    // 6. If n ≥ 0, then
    if (n >= 0) {
        // a. Let k be min(n, len - 1).
        k = min(n, (double)length - 1);
    }
    // 7. Else,
    else {
        //  a. Let k be len + n.
        k = (double)length + n;
    }

    // 8. Repeat, while k ≥ 0,
    for (; k >= 0; --k) {
        auto property_key = PropertyKey { k };

        // a. Let kPresent be ? HasProperty(O, ! ToString(𝔽(k))).
        auto k_present = TRY(object->has_property(property_key));

        // b. If kPresent is true, then
        if (k_present) {
            // i. Let elementK be ? Get(O, ! ToString(𝔽(k))).
            auto element_k = TRY(object->get(property_key));

            // ii. Let same be IsStrictlyEqual(searchElement, elementK).
            auto same = is_strictly_equal(search_element, element_k);

            // iii. If same is true, return 𝔽(k).
            if (same)
                return Value((size_t)k);
        }

        // c. Set k to k - 1.
    }

    // 9. Return -1𝔽.
    return Value(-1);
}

// 23.1.3.14 Array.prototype.includes ( searchElement [ , fromIndex ] ), https://tc39.es/ecma262/#sec-array.prototype.includes
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::includes)
{
    auto* this_object = TRY(vm.this_value(global_object).to_object(global_object));
    auto length = TRY(length_of_array_like(global_object, *this_object));
    if (length == 0)
        return Value(false);
    u64 from_index = 0;
    if (vm.argument_count() >= 2) {
        auto from_argument = TRY(vm.argument(1).to_integer_or_infinity(global_object));

        if (Value(from_argument).is_positive_infinity() || from_argument >= length)
            return Value(false);

        if (Value(from_argument).is_negative_infinity())
            from_argument = 0;

        if (from_argument < 0)
            from_index = max(length + from_argument, 0);
        else
            from_index = from_argument;
    }
    auto value_to_find = vm.argument(0);
    for (u64 i = from_index; i < length; ++i) {
        auto element = TRY(this_object->get(i));
        if (same_value_zero(element, value_to_find))
            return Value(true);
    }
    return Value(false);
}

// 23.1.3.9 Array.prototype.find ( predicate [ , thisArg ] ), https://tc39.es/ecma262/#sec-array.prototype.find
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::find)
{
    auto predicate = vm.argument(0);
    auto this_arg = vm.argument(1);

    // 1. Let O be ? ToObject(this value).
    auto* object = TRY(vm.this_value(global_object).to_object(global_object));

    // 2. Let len be ? LengthOfArrayLike(O).
    auto length = TRY(length_of_array_like(global_object, *object));

    // 3. If IsCallable(predicate) is false, throw a TypeError exception.
    if (!predicate.is_function())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAFunction, predicate.to_string_without_side_effects());

    // 4. Let k be 0.
    // 5. Repeat, while k < len,
    for (size_t k = 0; k < length; ++k) {
        // a. Let Pk be ! ToString(𝔽(k)).
        auto property_key = PropertyKey { k };

        // b. Let kValue be ? Get(O, Pk).
        auto k_value = TRY(object->get(property_key));

        // c. Let testResult be ! ToBoolean(? Call(predicate, thisArg, « kValue, 𝔽(k), O »)).
        auto test_result = TRY(call(global_object, predicate.as_function(), this_arg, k_value, Value(k), object)).to_boolean();

        // d. If testResult is true, return kValue.
        if (test_result)
            return k_value;

        // e. Set k to k + 1.
    }

    // 6. Return undefined.
    return js_undefined();
}

// 23.1.3.10 Array.prototype.findIndex ( predicate [ , thisArg ] ), https://tc39.es/ecma262/#sec-array.prototype.findindex
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::find_index)
{
    auto predicate = vm.argument(0);
    auto this_arg = vm.argument(1);

    // 1. Let O be ? ToObject(this value).
    auto* object = TRY(vm.this_value(global_object).to_object(global_object));

    // 2. Let len be ? LengthOfArrayLike(O).
    auto length = TRY(length_of_array_like(global_object, *object));

    // 3. If IsCallable(predicate) is false, throw a TypeError exception.
    if (!predicate.is_function())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAFunction, predicate.to_string_without_side_effects());

    // 4. Let k be 0.
    // 5. Repeat, while k < len,
    for (size_t k = 0; k < length; ++k) {
        // a. Let Pk be ! ToString(𝔽(k)).
        auto property_key = PropertyKey { k };

        // b. Let kValue be ? Get(O, Pk).
        auto k_value = TRY(object->get(property_key));

        // c. Let testResult be ! ToBoolean(? Call(predicate, thisArg, « kValue, 𝔽(k), O »)).
        auto test_result = TRY(call(global_object, predicate.as_function(), this_arg, k_value, Value(k), object)).to_boolean();

        // d. If testResult is true, return 𝔽(k).
        if (test_result)
            return Value(k);

        // e. Set k to k + 1.
    }

    // 6. Return -1𝔽.
    return Value(-1);
}

// 1 Array.prototype.findLast ( predicate [ , thisArg ] ), https://tc39.es/proposal-array-find-from-last/#sec-array.prototype.findlast
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::find_last)
{
    auto predicate = vm.argument(0);
    auto this_arg = vm.argument(1);

    // 1. Let O be ? ToObject(this value).
    auto* object = TRY(vm.this_value(global_object).to_object(global_object));

    // 2. Let len be ? LengthOfArrayLike(O).
    auto length = TRY(length_of_array_like(global_object, *object));

    // 3. If IsCallable(predicate) is false, throw a TypeError exception.
    if (!predicate.is_function())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAFunction, predicate.to_string_without_side_effects());

    // 4. Let k be len - 1.
    // 5. Repeat, while k ≥ 0,
    for (i64 k = static_cast<i64>(length) - 1; k >= 0; --k) {
        // a. Let Pk be ! ToString(𝔽(k)).
        auto property_key = PropertyKey { k };

        // b. Let kValue be ? Get(O, Pk).
        auto k_value = TRY(object->get(property_key));

        // c. Let testResult be ! ToBoolean(? Call(predicate, thisArg, « kValue, 𝔽(k), O »)).
        auto test_result = TRY(call(global_object, predicate.as_function(), this_arg, k_value, Value((double)k), object)).to_boolean();

        // d. If testResult is true, return kValue.
        if (test_result)
            return k_value;

        // e. Set k to k - 1.
    }

    // 6. Return undefined.
    return js_undefined();
}

// 2 Array.prototype.findLastIndex ( predicate [ , thisArg ] ), https://tc39.es/proposal-array-find-from-last/#sec-array.prototype.findlastindex
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::find_last_index)
{
    auto predicate = vm.argument(0);
    auto this_arg = vm.argument(1);

    // 1. Let O be ? ToObject(this value).
    auto* object = TRY(vm.this_value(global_object).to_object(global_object));

    // 2. Let len be ? LengthOfArrayLike(O).
    auto length = TRY(length_of_array_like(global_object, *object));

    // 3. If IsCallable(predicate) is false, throw a TypeError exception.
    if (!predicate.is_function())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAFunction, predicate.to_string_without_side_effects());

    // 4. Let k be len - 1.
    // 5. Repeat, while k ≥ 0,
    for (i64 k = static_cast<i64>(length) - 1; k >= 0; --k) {
        // a. Let Pk be ! ToString(𝔽(k)).
        auto property_key = PropertyKey { k };

        // b. Let kValue be ? Get(O, Pk).
        auto k_value = TRY(object->get(property_key));

        // c. Let testResult be ! ToBoolean(? Call(predicate, thisArg, « kValue, 𝔽(k), O »)).
        auto test_result = TRY(call(global_object, predicate.as_function(), this_arg, k_value, Value((double)k), object)).to_boolean();

        // d. If testResult is true, return 𝔽(k).
        if (test_result)
            return Value((double)k);

        // e. Set k to k - 1.
    }

    // 6. Return -1𝔽.
    return Value(-1);
}

// 23.1.3.27 Array.prototype.some ( callbackfn [ , thisArg ] ), https://tc39.es/ecma262/#sec-array.prototype.some
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::some)
{
    auto callback_function = vm.argument(0);
    auto this_arg = vm.argument(1);

    // 1. Let O be ? ToObject(this value).
    auto* object = TRY(vm.this_value(global_object).to_object(global_object));

    // 2. Let len be ? LengthOfArrayLike(O).
    auto length = TRY(length_of_array_like(global_object, *object));

    // 3. If IsCallable(callbackfn) is false, throw a TypeError exception.
    if (!callback_function.is_function())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAFunction, callback_function.to_string_without_side_effects());

    // 4. Let k be 0.
    // 5. Repeat, while k < len,
    for (size_t k = 0; k < length; ++k) {
        // a. Let Pk be ! ToString(𝔽(k)).
        auto property_key = PropertyKey { k };

        // b. Let kPresent be ? HasProperty(O, Pk).
        auto k_present = TRY(object->has_property(property_key));

        // c. If kPresent is true, then
        if (k_present) {
            // i. Let kValue be ? Get(O, Pk).
            auto k_value = TRY(object->get(property_key));

            // ii. Let testResult be ! ToBoolean(? Call(callbackfn, thisArg, « kValue, 𝔽(k), O »)).
            auto test_result = TRY(call(global_object, callback_function.as_function(), this_arg, k_value, Value(k), object)).to_boolean();

            // iii. If testResult is true, return true.
            if (test_result)
                return Value(true);
        }

        // d. Set k to k + 1.
    }

    // 6. Return false.
    return Value(false);
}

// 23.1.3.6 Array.prototype.every ( callbackfn [ , thisArg ] ), https://tc39.es/ecma262/#sec-array.prototype.every
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::every)
{
    auto callback_function = vm.argument(0);
    auto this_arg = vm.argument(1);

    // 1. Let O be ? ToObject(this value).
    auto* object = TRY(vm.this_value(global_object).to_object(global_object));

    // 2. Let len be ? LengthOfArrayLike(O).
    auto length = TRY(length_of_array_like(global_object, *object));

    // 3. If IsCallable(callbackfn) is false, throw a TypeError exception.
    if (!callback_function.is_function())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAFunction, callback_function.to_string_without_side_effects());

    // 4. Let k be 0.
    // 5. Repeat, while k < len,
    for (size_t k = 0; k < length; ++k) {
        // a. Let Pk be ! ToString(𝔽(k)).
        auto property_key = PropertyKey { k };

        // b. Let kPresent be ? HasProperty(O, Pk).
        auto k_present = TRY(object->has_property(property_key));

        // c. If kPresent is true, then
        if (k_present) {
            // i. Let kValue be ? Get(O, Pk).
            auto k_value = TRY(object->get(property_key));

            // ii. Let testResult be ! ToBoolean(? Call(callbackfn, thisArg, « kValue, 𝔽(k), O »)).
            auto test_result = TRY(call(global_object, callback_function.as_function(), this_arg, k_value, Value(k), object)).to_boolean();

            // iii. If testResult is false, return false.
            if (!test_result)
                return Value(false);
        }

        // d. Set k to k + 1.
    }

    // 6. Return true.
    return Value(true);
}

// 23.1.3.29 Array.prototype.splice ( start, deleteCount, ...items ), https://tc39.es/ecma262/#sec-array.prototype.splice
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::splice)
{
    auto* this_object = TRY(vm.this_value(global_object).to_object(global_object));

    auto initial_length = TRY(length_of_array_like(global_object, *this_object));

    auto relative_start = TRY(vm.argument(0).to_integer_or_infinity(global_object));

    if (Value(relative_start).is_negative_infinity())
        relative_start = 0;

    u64 actual_start;

    if (relative_start < 0)
        actual_start = max((ssize_t)initial_length + relative_start, (ssize_t)0);
    else
        actual_start = min(relative_start, initial_length);

    u64 insert_count = 0;
    double actual_delete_count = 0;

    if (vm.argument_count() == 1) {
        actual_delete_count = initial_length - actual_start;
    } else if (vm.argument_count() >= 2) {
        insert_count = vm.argument_count() - 2;
        auto delete_count = TRY(vm.argument(1).to_integer_or_infinity(global_object));
        auto temp = max(delete_count, 0);
        actual_delete_count = min(temp, initial_length - actual_start);
    }

    double new_length = initial_length + insert_count - actual_delete_count;

    if (new_length > MAX_ARRAY_LIKE_INDEX)
        return vm.throw_completion<TypeError>(global_object, ErrorType::ArrayMaxSize);

    auto* removed_elements = TRY(array_species_create(global_object, *this_object, actual_delete_count));

    for (u64 i = 0; i < actual_delete_count; ++i) {
        auto from = actual_start + i;
        bool from_present = TRY(this_object->has_property(from));

        if (from_present) {
            auto from_value = TRY(this_object->get(from));

            TRY(removed_elements->create_data_property_or_throw(i, from_value));
        }
    }

    TRY(removed_elements->set(vm.names.length, Value(actual_delete_count), Object::ShouldThrowExceptions::Yes));

    if (insert_count < actual_delete_count) {
        for (u64 i = actual_start; i < initial_length - actual_delete_count; ++i) {
            auto to = i + insert_count;
            u64 from = i + actual_delete_count;

            auto from_present = TRY(this_object->has_property(from));

            if (from_present) {
                auto from_value = TRY(this_object->get(from));
                TRY(this_object->set(to, from_value, Object::ShouldThrowExceptions::Yes));
            } else {
                TRY(this_object->delete_property_or_throw(to));
            }
        }

        for (u64 i = initial_length; i > new_length; --i)
            TRY(this_object->delete_property_or_throw(i - 1));
    } else if (insert_count > actual_delete_count) {
        for (u64 i = initial_length - actual_delete_count; i > actual_start; --i) {
            u64 from_index = i + actual_delete_count - 1;
            auto from_present = TRY(this_object->has_property(from_index));

            auto to = i + insert_count - 1;

            if (from_present) {
                auto from_value = TRY(this_object->get(from_index));
                TRY(this_object->set(to, from_value, Object::ShouldThrowExceptions::Yes));
            } else {
                TRY(this_object->delete_property_or_throw(to));
            }
        }
    }

    for (u64 i = 0; i < insert_count; ++i)
        TRY(this_object->set(actual_start + i, vm.argument(i + 2), Object::ShouldThrowExceptions::Yes));

    TRY(this_object->set(vm.names.length, Value(new_length), Object::ShouldThrowExceptions::Yes));

    return removed_elements;
}

// 23.1.3.7 Array.prototype.fill ( value [ , start [ , end ] ] ), https://tc39.es/ecma262/#sec-array.prototype.fill
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::fill)
{
    auto* this_object = TRY(vm.this_value(global_object).to_object(global_object));

    auto length = TRY(length_of_array_like(global_object, *this_object));

    double relative_start = 0;
    double relative_end = length;

    if (vm.argument_count() >= 2) {
        relative_start = TRY(vm.argument(1).to_integer_or_infinity(global_object));
        if (Value(relative_start).is_negative_infinity())
            relative_start = 0;
    }

    // If end is undefined, let relativeEnd be len; else let relativeEnd be ? ToIntegerOrInfinity(end).
    if (vm.argument_count() >= 3 && !vm.argument(2).is_undefined()) {
        relative_end = TRY(vm.argument(2).to_integer_or_infinity(global_object));
        if (Value(relative_end).is_negative_infinity())
            relative_end = 0;
    }

    u64 from, to;

    if (relative_start < 0)
        from = max(length + relative_start, 0L);
    else
        from = min(relative_start, length);

    if (relative_end < 0)
        to = max(length + relative_end, 0L);
    else
        to = min(relative_end, length);

    for (u64 i = from; i < to; i++)
        TRY(this_object->set(i, vm.argument(0), Object::ShouldThrowExceptions::Yes));

    return this_object;
}

// 23.1.3.33 Array.prototype.values ( ), https://tc39.es/ecma262/#sec-array.prototype.values
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::values)
{
    auto* this_object = TRY(vm.this_value(global_object).to_object(global_object));

    return ArrayIterator::create(global_object, this_object, Object::PropertyKind::Value);
}

// 23.1.3.5 Array.prototype.entries ( ), https://tc39.es/ecma262/#sec-array.prototype.entries
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::entries)
{
    auto* this_object = TRY(vm.this_value(global_object).to_object(global_object));

    return ArrayIterator::create(global_object, this_object, Object::PropertyKind::KeyAndValue);
}

// 23.1.3.17 Array.prototype.keys ( ), https://tc39.es/ecma262/#sec-array.prototype.keys
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::keys)
{
    auto* this_object = TRY(vm.this_value(global_object).to_object(global_object));

    return ArrayIterator::create(global_object, this_object, Object::PropertyKind::Key);
}

// 23.1.3.11.1 FlattenIntoArray ( target, source, sourceLen, start, depth [ , mapperFunction [ , thisArg ] ] ), https://tc39.es/ecma262/#sec-flattenintoarray
static ThrowCompletionOr<size_t> flatten_into_array(GlobalObject& global_object, Object& new_array, Object& array, size_t array_length, size_t target_index, double depth, FunctionObject* mapper_func = {}, Value this_arg = {})
{
    VERIFY(!mapper_func || (!this_arg.is_empty() && depth == 1));
    auto& vm = global_object.vm();

    for (size_t j = 0; j < array_length; ++j) {
        auto value_exists = TRY(array.has_property(j));

        if (!value_exists)
            continue;
        auto value = TRY(array.get(j));

        if (mapper_func)
            value = TRY(call(global_object, *mapper_func, this_arg, value, Value(j), &array));

        if (depth > 0 && TRY(value.is_array(global_object))) {
            if (vm.did_reach_stack_space_limit())
                return vm.throw_completion<InternalError>(global_object, ErrorType::CallStackSizeExceeded);

            auto length = TRY(length_of_array_like(global_object, value.as_object()));
            target_index = TRY(flatten_into_array(global_object, new_array, value.as_object(), length, target_index, depth - 1));
            continue;
        }

        if (target_index >= MAX_ARRAY_LIKE_INDEX)
            return vm.throw_completion<TypeError>(global_object, ErrorType::InvalidIndex);

        TRY(new_array.create_data_property_or_throw(target_index, value));

        ++target_index;
    }
    return target_index;
}

// 23.1.3.11 Array.prototype.flat ( [ depth ] ), https://tc39.es/ecma262/#sec-array.prototype.flat
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::flat)
{
    auto* this_object = TRY(vm.this_value(global_object).to_object(global_object));

    auto length = TRY(length_of_array_like(global_object, *this_object));

    double depth = 1;
    if (!vm.argument(0).is_undefined()) {
        auto depth_num = TRY(vm.argument(0).to_integer_or_infinity(global_object));
        depth = max(depth_num, 0.0);
    }

    auto* new_array = TRY(array_species_create(global_object, *this_object, 0));

    TRY(flatten_into_array(global_object, *new_array, *this_object, length, 0, depth));
    return new_array;
}

// 23.1.3.12 Array.prototype.flatMap ( mapperFunction [ , thisArg ] ), https://tc39.es/ecma262/#sec-array.prototype.flatmap
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::flat_map)
{
    auto mapper_function = vm.argument(0);
    auto this_arg = vm.argument(1);

    // 1. Let O be ? ToObject(this value).
    auto* object = TRY(vm.this_value(global_object).to_object(global_object));

    // 2. Let sourceLen be ? LengthOfArrayLike(O).
    auto source_length = TRY(length_of_array_like(global_object, *object));

    // 3. If ! IsCallable(mapperFunction) is false, throw a TypeError exception.
    if (!mapper_function.is_function())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAFunction, mapper_function.to_string_without_side_effects());

    // 4. Let A be ? ArraySpeciesCreate(O, 0).
    auto* array = TRY(array_species_create(global_object, *object, 0));

    // 5. Perform ? FlattenIntoArray(A, O, sourceLen, 0, 1, mapperFunction, thisArg).
    TRY(flatten_into_array(global_object, *array, *object, source_length, 0, 1, &mapper_function.as_function(), this_arg));

    // 6. Return A.
    return array;
}

// 23.1.3.3 Array.prototype.copyWithin ( target, start [ , end ] ), https://tc39.es/ecma262/#sec-array.prototype.copywithin
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::copy_within)
{
    auto* this_object = TRY(vm.this_value(global_object).to_object(global_object));
    auto length = TRY(length_of_array_like(global_object, *this_object));

    auto relative_target = TRY(vm.argument(0).to_integer_or_infinity(global_object));

    double to;
    if (relative_target < 0)
        to = max(length + relative_target, 0.0);
    else
        to = min(relative_target, (double)length);

    auto relative_start = TRY(vm.argument(1).to_integer_or_infinity(global_object));

    double from;
    if (relative_start < 0)
        from = max(length + relative_start, 0.0);
    else
        from = min(relative_start, (double)length);

    auto relative_end = vm.argument(2).is_undefined() ? length : TRY(vm.argument(2).to_integer_or_infinity(global_object));

    double final;
    if (relative_end < 0)
        final = max(length + relative_end, 0.0);
    else
        final = min(relative_end, (double)length);

    double count = min(final - from, length - to);

    i32 direction = 1;

    if (from < to && to < from + count) {
        direction = -1;
        from = from + count - 1;
        to = to + count - 1;
    }

    if (count < 0) {
        return this_object;
    }

    size_t from_i = from;
    size_t to_i = to;
    size_t count_i = count;

    while (count_i > 0) {
        auto from_present = TRY(this_object->has_property(from_i));

        if (from_present) {
            auto from_value = TRY(this_object->get(from_i));
            TRY(this_object->set(to_i, from_value, Object::ShouldThrowExceptions::Yes));
        } else {
            TRY(this_object->delete_property_or_throw(to_i));
        }

        from_i += direction;
        to_i += direction;
        --count_i;
    }

    return this_object;
}

// 23.1.3.1 Array.prototype.at ( index ), https://tc39.es/ecma262/#sec-array.prototype.at
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::at)
{
    auto* this_object = TRY(vm.this_value(global_object).to_object(global_object));
    auto length = TRY(length_of_array_like(global_object, *this_object));
    auto relative_index = TRY(vm.argument(0).to_integer_or_infinity(global_object));
    if (Value(relative_index).is_infinity())
        return js_undefined();
    Checked<size_t> index { 0 };
    if (relative_index >= 0) {
        index += relative_index;
    } else {
        index += length;
        index -= -relative_index;
    }
    if (index.has_overflow() || index.value() >= length)
        return js_undefined();
    return TRY(this_object->get(index.value()));
}

// 2.3 AddValueToKeyedGroup ( groups, key, value ), https://tc39.es/proposal-array-grouping/#sec-add-value-to-keyed-group
template<typename GroupsType, typename KeyType>
static void add_value_to_keyed_group(GlobalObject& global_object, GroupsType& groups, KeyType key, Value value)
{
    // 1. For each Record { [[Key]], [[Elements]] } g of groups, do
    //      a. If ! SameValue(g.[[Key]], key) is true, then
    //      NOTE: This is performed in KeyedGroupTraits::equals for groupByToMap and Traits<JS::PropertyKey>::equals for groupBy.
    auto existing_elements_iterator = groups.find(key);
    if (existing_elements_iterator != groups.end()) {
        // i. Assert: exactly one element of groups meets this criteria.
        // NOTE: This is done on insertion into the hash map, as only `set` tells us if we overrode an entry.

        // ii. Append value as the last element of g.[[Elements]].
        existing_elements_iterator->value.append(value);

        // iii. Return.
        return;
    }

    // 2. Let group be the Record { [[Key]]: key, [[Elements]]: « value » }.
    MarkedValueList new_elements { global_object.heap() };
    new_elements.append(value);

    // 3. Append group as the last element of groups.
    auto result = groups.set(key, move(new_elements));
    VERIFY(result == AK::HashSetResult::InsertedNewEntry);
}

// 2.1 Array.prototype.groupBy ( callbackfn [ , thisArg ] ), https://tc39.es/proposal-array-grouping/#sec-array.prototype.groupby
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::group_by)
{
    auto callback_function = vm.argument(0);
    auto this_arg = vm.argument(1);

    // 1. Let O be ? ToObject(this value).
    auto* this_object = TRY(vm.this_value(global_object).to_object(global_object));

    // 2. Let len be ? LengthOfArrayLike(O).
    auto length = TRY(length_of_array_like(global_object, *this_object));

    // 3. If IsCallable(callbackfn) is false, throw a TypeError exception.
    if (!callback_function.is_function())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAFunction, callback_function.to_string_without_side_effects());

    // 5. Let groups be a new empty List.
    OrderedHashMap<PropertyKey, MarkedValueList> groups;

    // 4. Let k be 0.
    // 6. Repeat, while k < len
    for (size_t index = 0; index < length; ++index) {
        // a. Let Pk be ! ToString(𝔽(k)).
        auto index_property = PropertyKey { index };

        // b. Let kValue be ? Get(O, Pk).
        auto k_value = TRY(this_object->get(index_property));

        // c. Let propertyKey be ? ToPropertyKey(? Call(callbackfn, thisArg, « kValue, 𝔽(k), O »)).
        auto property_key_value = TRY(call(global_object, callback_function.as_function(), this_arg, k_value, Value(index), this_object));
        auto property_key = TRY(property_key_value.to_property_key(global_object));

        // d. Perform ! AddValueToKeyedGroup(groups, propertyKey, kValue).
        add_value_to_keyed_group(global_object, groups, property_key, k_value);

        // e. Set k to k + 1.
    }

    // 7. Let obj be ! OrdinaryObjectCreate(null).
    auto* object = Object::create(global_object, nullptr);

    // 8. For each Record { [[Key]], [[Elements]] } g of groups, do
    for (auto& group : groups) {
        // a. Let elements be ! CreateArrayFromList(g.[[Elements]]).
        auto* elements = Array::create_from(global_object, group.value);

        // b. Perform ! CreateDataPropertyOrThrow(obj, g.[[Key]], elements).
        MUST(object->create_data_property_or_throw(group.key, elements));
    }

    // 9. Return obj.
    return object;
}

// 2.2 Array.prototype.groupByToMap ( callbackfn [ , thisArg ] ), https://tc39.es/proposal-array-grouping/#sec-array.prototype.groupbymap
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::group_by_to_map)
{
    auto callback_function = vm.argument(0);
    auto this_arg = vm.argument(1);

    // 1. Let O be ? ToObject(this value).
    auto* this_object = TRY(vm.this_value(global_object).to_object(global_object));

    // 2. Let len be ? LengthOfArrayLike(O).
    auto length = TRY(length_of_array_like(global_object, *this_object));

    // 3. If IsCallable(callbackfn) is false, throw a TypeError exception.
    if (!callback_function.is_function())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAFunction, callback_function.to_string_without_side_effects());

    struct KeyedGroupTraits : public Traits<Handle<Value>> {
        static unsigned hash(Handle<Value> const& value_handle)
        {
            return ValueTraits::hash(value_handle.value());
        }

        static bool equals(Handle<Value> const& a, Handle<Value> const& b)
        {
            // AddValueToKeyedGroup uses SameValue on the keys on Step 1.a.
            return same_value(a.value(), b.value());
        }
    };

    // 5. Let groups be a new empty List.
    OrderedHashMap<Handle<Value>, MarkedValueList, KeyedGroupTraits> groups;

    // 4. Let k be 0.
    // 6. Repeat, while k < len
    for (size_t index = 0; index < length; ++index) {
        // a. Let Pk be ! ToString(𝔽(k)).
        auto index_property = PropertyKey { index };

        // b. Let kValue be ? Get(O, Pk).
        auto k_value = TRY(this_object->get(index_property));

        // c. Let key be ? Call(callbackfn, thisArg, « kValue, 𝔽(k), O »).
        auto key = TRY(call(global_object, callback_function.as_function(), this_arg, k_value, Value(index), this_object));

        // d. If key is -0𝔽, set key to +0𝔽.
        if (key.is_negative_zero())
            key = Value(0);

        // e. Perform ! AddValueToKeyedGroup(groups, key, kValue).
        add_value_to_keyed_group(global_object, groups, make_handle(key), k_value);

        // f. Set k to k + 1.
    }

    // 7. Let map be ! Construct(%Map%).
    auto* map = Map::create(global_object);

    // 8. For each Record { [[Key]], [[Elements]] } g of groups, do
    for (auto& group : groups) {
        // a. Let elements be ! CreateArrayFromList(g.[[Elements]]).
        auto* elements = Array::create_from(global_object, group.value);

        // b. Let entry be the Record { [[Key]]: g.[[Key]], [[Value]]: elements }.
        // c. Append entry as the last element of map.[[MapData]].
        map->entries().set(group.key.value(), elements);
    }

    // 9. Return map.
    return map;
}

}
