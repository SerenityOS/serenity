/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2020, Marcin Gasperowicz <xnooga@gmail.com>
 * Copyright (c) 2021, David Tuin <david.tuin@gmail.com>
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
#include <LibJS/Runtime/ObjectPrototype.h>
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

    // Use define_property here instead of define_native_function so that
    // Object.is(Array.prototype[Symbol.iterator], Array.prototype.values)
    // evaluates to true
    // 23.1.3.33 Array.prototype [ @@iterator ] ( ), https://tc39.es/ecma262/#sec-array.prototype-@@iterator
    define_property(*vm.well_known_symbol_iterator(), get(vm.names.values), attr);

    // 23.1.3.34 Array.prototype [ @@unscopables ], https://tc39.es/ecma262/#sec-array.prototype-@@unscopables
    auto* unscopable_list = Object::create(global_object, nullptr);
    unscopable_list->create_data_property_or_throw(vm.names.copyWithin, Value(true));
    unscopable_list->create_data_property_or_throw(vm.names.entries, Value(true));
    unscopable_list->create_data_property_or_throw(vm.names.fill, Value(true));
    unscopable_list->create_data_property_or_throw(vm.names.find, Value(true));
    unscopable_list->create_data_property_or_throw(vm.names.findIndex, Value(true));
    unscopable_list->create_data_property_or_throw(vm.names.flat, Value(true));
    unscopable_list->create_data_property_or_throw(vm.names.flatMap, Value(true));
    unscopable_list->create_data_property_or_throw(vm.names.includes, Value(true));
    unscopable_list->create_data_property_or_throw(vm.names.keys, Value(true));
    unscopable_list->create_data_property_or_throw(vm.names.values, Value(true));

    define_property(*vm.well_known_symbol_unscopables(), unscopable_list, Attribute::Configurable);
}

ArrayPrototype::~ArrayPrototype()
{
}

// 10.4.2.3 ArraySpeciesCreate ( originalArray, length ), https://tc39.es/ecma262/#sec-arrayspeciescreate
static Object* array_species_create(GlobalObject& global_object, Object& original_array, size_t length)
{
    auto& vm = global_object.vm();

    if (!Value(&original_array).is_array(global_object)) {
        auto array = Array::create(global_object, length);
        if (vm.exception())
            return {};
        return array;
    }

    auto constructor = original_array.get(vm.names.constructor);
    if (vm.exception())
        return {};
    if (constructor.is_constructor()) {
        auto& constructor_function = constructor.as_function();
        auto* constructor_realm = get_function_realm(global_object, constructor_function);
        if (vm.exception())
            return {};
        if (constructor_realm != &global_object) {
            if (&constructor_function == constructor_realm->array_constructor())
                constructor = js_undefined();
        }
    }

    if (constructor.is_object()) {
        constructor = constructor.as_object().get(*vm.well_known_symbol_species());
        if (vm.exception())
            return {};
        if (constructor.is_null())
            constructor = js_undefined();
    }

    if (constructor.is_undefined()) {
        auto array = Array::create(global_object, length);
        if (vm.exception())
            return {};
        return array;
    }

    if (!constructor.is_constructor()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAConstructor, constructor.to_string_without_side_effects());
        return {};
    }

    MarkedValueList arguments(vm.heap());
    arguments.append(Value(length));
    auto result = vm.construct(constructor.as_function(), constructor.as_function(), move(arguments));
    if (vm.exception())
        return {};
    return &result.as_object();
}

// 23.1.3.7 Array.prototype.filter ( callbackfn [ , thisArg ] ), https://tc39.es/ecma262/#sec-array.prototype.filter
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::filter)
{
    auto callback_function = vm.argument(0);
    auto this_arg = vm.argument(1);

    // 1. Let O be ? ToObject(this value).
    auto* object = vm.this_value(global_object).to_object(global_object);
    if (vm.exception())
        return {};

    // 2. Let len be ? LengthOfArrayLike(O).
    auto length = length_of_array_like(global_object, *object);
    if (vm.exception())
        return {};

    // 3. If IsCallable(callbackfn) is false, throw a TypeError exception.
    if (!callback_function.is_function()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAFunction, callback_function.to_string_without_side_effects());
        return {};
    }

    // 4. Let A be ? ArraySpeciesCreate(O, 0).
    auto* array = array_species_create(global_object, *object, 0);
    if (vm.exception())
        return {};

    // 5. Let k be 0.
    size_t k = 0;

    // 6. Let to be 0.
    size_t to = 0;

    // 7. Repeat, while k < len,
    for (; k < length; ++k) {
        // a. Let Pk be ! ToString(𝔽(k)).
        auto property_name = PropertyName { k };

        // b. Let kPresent be ? HasProperty(O, Pk).
        auto k_present = object->has_property(property_name);
        if (vm.exception())
            return {};

        // c. If kPresent is true, then
        if (k_present) {
            // i. Let kValue be ? Get(O, Pk).
            auto k_value = object->get(k);
            if (vm.exception())
                return {};

            // ii. Let selected be ! ToBoolean(? Call(callbackfn, thisArg, « kValue, 𝔽(k), O »)).
            auto selected = vm.call(callback_function.as_function(), this_arg, k_value, Value(k), object);
            if (vm.exception())
                return {};

            // iii. If selected is true, then
            if (selected.to_boolean()) {
                // 1. Perform ? CreateDataPropertyOrThrow(A, ! ToString(𝔽(to)), kValue).
                array->create_data_property_or_throw(to, k_value);

                // 2. Set to to to + 1.
                ++to;
            }
        }

        // d. Set k to k + 1.
    }

    // 8. Return A.
    return array;
}

// 23.1.3.12 Array.prototype.forEach ( callbackfn [ , thisArg ] ), https://tc39.es/ecma262/#sec-array.prototype.foreach
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::for_each)
{
    auto callback_function = vm.argument(0);
    auto this_arg = vm.argument(1);

    // 1. Let O be ? ToObject(this value).
    auto object = vm.this_value(global_object).to_object(global_object);
    if (vm.exception())
        return {};

    // 2. Let len be ? LengthOfArrayLike(O).
    auto length = length_of_array_like(global_object, *object);
    if (vm.exception())
        return {};

    // 3. If IsCallable(callbackfn) is false, throw a TypeError exception.
    if (!callback_function.is_function()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAFunction, callback_function.to_string_without_side_effects());
        return {};
    }

    // 4. Let k be 0.
    // 5. Repeat, while k < len,
    for (size_t k = 0; k < length; ++k) {
        // a. Let Pk be ! ToString(𝔽(k)).
        auto property_name = PropertyName { k };

        // b. Let kPresent be ? HasProperty(O, Pk).
        auto k_present = object->has_property(property_name);
        if (vm.exception())
            return {};

        // c. If kPresent is true, then
        if (k_present) {
            // i. Let kValue be ? Get(O, Pk).
            auto k_value = object->get(property_name);
            if (vm.exception())
                return {};

            // ii. Perform ? Call(callbackfn, thisArg, « kValue, 𝔽(k), O »).
            (void)vm.call(callback_function.as_function(), this_arg, k_value, Value(k), object);
            if (vm.exception())
                return {};
        }

        // d. Set k to k + 1.
    }

    // 6. Return undefined.
    return js_undefined();
}

// 23.1.3.18 Array.prototype.map ( callbackfn [ , thisArg ] ), https://tc39.es/ecma262/#sec-array.prototype.map
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::map)
{
    auto callback_function = vm.argument(0);
    auto this_arg = vm.argument(1);

    // 1. Let O be ? ToObject(this value).
    auto* object = vm.this_value(global_object).to_object(global_object);
    if (vm.exception())
        return {};

    // 2. Let len be ? LengthOfArrayLike(O).
    auto length = length_of_array_like(global_object, *object);
    if (vm.exception())
        return {};

    // 3. If IsCallable(callbackfn) is false, throw a TypeError exception.
    if (!callback_function.is_function()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAFunction, callback_function.to_string_without_side_effects());
        return {};
    }

    // 4. Let A be ? ArraySpeciesCreate(O, len).
    auto* array = array_species_create(global_object, *object, length);
    if (vm.exception())
        return {};

    // 5. Let k be 0.
    // 6. Repeat, while k < len,
    for (size_t k = 0; k < length; ++k) {
        // a. Let Pk be ! ToString(𝔽(k)).
        auto property_name = PropertyName { k };

        // b. Let kPresent be ? HasProperty(O, Pk).
        auto k_present = object->has_property(property_name);
        if (vm.exception())
            return {};

        // c. If kPresent is true, then
        if (k_present) {
            // i. Let kValue be ? Get(O, Pk).
            auto k_value = object->get(property_name);
            if (vm.exception())
                return {};

            // ii. Let mappedValue be ? Call(callbackfn, thisArg, « kValue, 𝔽(k), O »).
            auto mapped_value = vm.call(callback_function.as_function(), this_arg, k_value, Value(k), object);
            if (vm.exception())
                return {};

            // iii. Perform ? CreateDataPropertyOrThrow(A, Pk, mappedValue).
            array->create_data_property_or_throw(property_name, mapped_value);
            if (vm.exception())
                return {};
        }

        // d. Set k to k + 1.
    }

    // 7. Return A.
    return array;
}

// 23.1.3.20 Array.prototype.push ( ...items ), https://tc39.es/ecma262/#sec-array.prototype.push
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::push)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};
    if (this_object->is_array()) {
        auto* array = static_cast<Array*>(this_object);
        for (size_t i = 0; i < vm.argument_count(); ++i)
            array->indexed_properties().append(vm.argument(i));
        return Value(static_cast<i32>(array->indexed_properties().array_like_size()));
    }
    auto length = length_of_array_like(global_object, *this_object);
    if (vm.exception())
        return {};
    auto argument_count = vm.argument_count();
    auto new_length = length + argument_count;
    if (new_length > MAX_ARRAY_LIKE_INDEX) {
        vm.throw_exception<TypeError>(global_object, ErrorType::ArrayMaxSize);
        return {};
    }
    for (size_t i = 0; i < argument_count; ++i) {
        this_object->define_property(length + i, vm.argument(i));
        if (vm.exception())
            return {};
    }
    auto new_length_value = Value((i32)new_length);
    this_object->put(vm.names.length, new_length_value);
    if (vm.exception())
        return {};
    return new_length_value;
}

// 23.1.3.31 Array.prototype.unshift ( ...items ), https://tc39.es/ecma262/#sec-array.prototype.unshift
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::unshift)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};
    auto length = length_of_array_like(global_object, *this_object);
    if (vm.exception())
        return {};
    auto arg_count = vm.argument_count();
    size_t new_length = length + arg_count;
    if (arg_count > 0) {
        if (new_length > MAX_ARRAY_LIKE_INDEX) {
            vm.throw_exception<TypeError>(global_object, ErrorType::ArrayMaxSize);
            return {};
        }

        for (size_t k = length; k > 0; --k) {
            auto from = k - 1;
            auto to = k + arg_count - 1;

            bool from_present = this_object->has_property(from);
            if (vm.exception())
                return {};
            if (from_present) {
                auto from_value = this_object->get(from);
                if (vm.exception())
                    return {};
                this_object->define_property(to, from_value);
                if (vm.exception())
                    return {};
            } else {
                this_object->delete_property_or_throw(to);
                if (vm.exception())
                    return {};
            }
        }

        for (size_t j = 0; j < arg_count; j++) {
            this_object->define_property(j, vm.argument(j));
            if (vm.exception())
                return {};
        }
    }

    this_object->put(vm.names.length, Value(new_length));
    if (vm.exception())
        return {};
    return Value(new_length);
}

// 23.1.3.19 Array.prototype.pop ( ), https://tc39.es/ecma262/#sec-array.prototype.pop
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::pop)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};
    if (this_object->is_array()) {
        auto* array = static_cast<Array*>(this_object);
        if (array->indexed_properties().is_empty())
            return js_undefined();
        return array->indexed_properties().take_last(array).value.value_or(js_undefined());
    }
    auto length = length_of_array_like(global_object, *this_object);
    if (vm.exception())
        return {};
    if (length == 0) {
        this_object->put(vm.names.length, Value(0));
        return js_undefined();
    }
    auto index = length - 1;
    auto element = this_object->get(index);
    if (vm.exception())
        return {};
    this_object->delete_property_or_throw(index);
    if (vm.exception())
        return {};
    this_object->put(vm.names.length, Value((i32)index));
    if (vm.exception())
        return {};
    return element;
}

// 23.1.3.24 Array.prototype.shift ( ), https://tc39.es/ecma262/#sec-array.prototype.shift
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::shift)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};
    auto length = length_of_array_like(global_object, *this_object);
    if (vm.exception())
        return {};
    if (length == 0) {
        this_object->put(vm.names.length, Value(0));
        if (vm.exception())
            return {};
        return js_undefined();
    }
    auto first = this_object->get(0);
    if (vm.exception())
        return {};

    for (size_t k = 1; k < length; ++k) {
        size_t from = k;
        size_t to = k - 1;
        bool from_present = this_object->has_property(from);
        if (vm.exception())
            return {};
        if (from_present) {
            auto from_value = this_object->get(from);
            if (vm.exception())
                return {};
            this_object->define_property(to, from_value);
            if (vm.exception())
                return {};
        } else {
            this_object->delete_property_or_throw(to);
            if (vm.exception())
                return {};
        }
    }

    this_object->delete_property_or_throw(length - 1);
    if (vm.exception())
        return {};

    this_object->put(vm.names.length, Value(length - 1));
    if (vm.exception())
        return {};
    return first;
}

// 23.1.3.30 Array.prototype.toString ( ), https://tc39.es/ecma262/#sec-array.prototype.tostring
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::to_string)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};
    auto join_function = this_object->get(vm.names.join);
    if (vm.exception())
        return {};
    if (!join_function.is_function())
        return ObjectPrototype::to_string(vm, global_object);
    return vm.call(join_function.as_function(), this_object);
}

// 23.1.3.29 Array.prototype.toLocaleString ( [ reserved1 [ , reserved2 ] ] ), https://tc39.es/ecma262/#sec-array.prototype.tolocalestring
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::to_locale_string)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};

    if (s_array_join_seen_objects.contains(this_object))
        return js_string(vm, "");
    s_array_join_seen_objects.set(this_object);
    ArmedScopeGuard unsee_object_guard = [&] {
        s_array_join_seen_objects.remove(this_object);
    };

    auto length = length_of_array_like(global_object, *this_object);
    if (vm.exception())
        return {};

    String separator = ","; // NOTE: This is implementation-specific.
    StringBuilder builder;
    for (size_t i = 0; i < length; ++i) {
        if (i > 0)
            builder.append(separator);
        auto value = this_object->get(i);
        if (vm.exception())
            return {};
        if (value.is_nullish())
            continue;
        auto* value_object = value.to_object(global_object);
        if (!value_object)
            return {};
        auto locale_string_result = value_object->invoke(vm.names.toLocaleString.as_string());
        if (vm.exception())
            return {};
        auto string = locale_string_result.to_string(global_object);
        if (vm.exception())
            return {};
        builder.append(string);
    }
    return js_string(vm, builder.to_string());
}

// 23.1.3.15 Array.prototype.join ( separator ), https://tc39.es/ecma262/#sec-array.prototype.join
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::join)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};

    // This is not part of the spec, but all major engines do some kind of circular reference checks.
    // FWIW: engine262, a "100% spec compliant" ECMA-262 impl, aborts with "too much recursion".
    // Same applies to Array.prototype.toLocaleString().
    if (s_array_join_seen_objects.contains(this_object))
        return js_string(vm, "");
    s_array_join_seen_objects.set(this_object);
    ArmedScopeGuard unsee_object_guard = [&] {
        s_array_join_seen_objects.remove(this_object);
    };

    auto length = length_of_array_like(global_object, *this_object);
    if (vm.exception())
        return {};
    String separator = ",";
    if (!vm.argument(0).is_undefined()) {
        separator = vm.argument(0).to_string(global_object);
        if (vm.exception())
            return {};
    }
    StringBuilder builder;
    for (size_t i = 0; i < length; ++i) {
        if (i > 0)
            builder.append(separator);
        auto value = this_object->get(i);
        if (vm.exception())
            return {};
        if (value.is_nullish())
            continue;
        auto string = value.to_string(global_object);
        if (vm.exception())
            return {};
        builder.append(string);
    }

    return js_string(vm, builder.to_string());
}

// 23.1.3.1 Array.prototype.concat ( ...items ), https://tc39.es/ecma262/#sec-array.prototype.concat
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::concat)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};

    auto* new_array = array_species_create(global_object, *this_object, 0);
    if (vm.exception())
        return {};

    size_t n = 0;

    // 23.1.3.1.1 IsConcatSpreadable ( O ), https://tc39.es/ecma262/#sec-isconcatspreadable
    auto is_concat_spreadable = [&vm, &global_object](Value const& val) {
        if (!val.is_object()) {
            return false;
        }
        auto* object = val.to_object(global_object);
        if (vm.exception())
            return false;

        auto spreadable = object->get(*vm.well_known_symbol_is_concat_spreadable());
        if (vm.exception())
            return false;

        if (!spreadable.is_undefined())
            return spreadable.to_boolean();

        return val.is_array(global_object);
    };

    auto append_to_new_array = [&vm, &is_concat_spreadable, &new_array, &global_object, &n](Value arg) {
        auto spreadable = is_concat_spreadable(arg);
        if (vm.exception())
            return;
        if (spreadable) {
            VERIFY(arg.is_object());
            Object& obj = arg.as_object();
            size_t k = 0;
            auto length = length_of_array_like(global_object, obj);
            if (vm.exception())
                return;

            if (n + length > MAX_ARRAY_LIKE_INDEX) {
                vm.throw_exception<TypeError>(global_object, ErrorType::ArrayMaxSize);
                return;
            }
            while (k < length) {
                auto k_exists = obj.has_property(k);
                if (vm.exception())
                    return;
                if (k_exists) {
                    auto k_value = obj.get(k);
                    if (vm.exception())
                        return;
                    new_array->create_data_property_or_throw(n, k_value);
                    if (vm.exception())
                        return;
                }
                ++n;
                ++k;
            }
        } else {
            if (n >= MAX_ARRAY_LIKE_INDEX) {
                vm.throw_exception<TypeError>(global_object, ErrorType::ArrayMaxSize);
                return;
            }
            new_array->create_data_property_or_throw(n, arg);
            if (vm.exception())
                return;
            ++n;
        }
    };

    append_to_new_array(this_object);
    if (vm.exception())
        return {};

    for (size_t i = 0; i < vm.argument_count(); ++i) {
        auto argument = vm.argument(i);
        append_to_new_array(argument);
        if (vm.exception())
            return {};
    }

    new_array->put(vm.names.length, Value(n));
    if (vm.exception())
        return {};
    return Value(new_array);
}

// 23.1.3.25 Array.prototype.slice ( start, end ), https://tc39.es/ecma262/#sec-array.prototype.slice
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::slice)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};

    auto initial_length = length_of_array_like(global_object, *this_object);
    if (vm.exception())
        return {};

    auto relative_start = vm.argument(0).to_integer_or_infinity(global_object);
    if (vm.exception())
        return {};

    double actual_start;

    if (Value(relative_start).is_negative_infinity())
        actual_start = 0.0;
    else if (relative_start < 0.0)
        actual_start = max((double)initial_length + relative_start, 0.0);
    else
        actual_start = min(relative_start, (double)initial_length);

    double relative_end;

    if (vm.argument(1).is_undefined() || vm.argument(1).is_empty()) {
        relative_end = (double)initial_length;
    } else {
        relative_end = vm.argument(1).to_integer_or_infinity(global_object);
        if (vm.exception())
            return {};
    }

    double final;

    if (Value(relative_end).is_negative_infinity())
        final = 0.0;
    else if (relative_end < 0.0)
        final = max((double)initial_length + relative_end, 0.0);
    else
        final = min(relative_end, (double)initial_length);

    auto count = max(final - actual_start, 0.0);

    auto* new_array = array_species_create(global_object, *this_object, count);
    if (vm.exception())
        return {};

    size_t index = 0;

    while (actual_start < final) {
        bool present = this_object->has_property((u32)actual_start);
        if (vm.exception())
            return {};

        if (present) {
            auto value = this_object->get((u32)actual_start);
            if (vm.exception())
                return {};

            new_array->create_data_property_or_throw(index, value);
            if (vm.exception())
                return {};
        }

        ++actual_start;
        ++index;
    }

    new_array->put(vm.names.length, Value(index));
    if (vm.exception())
        return {};

    return new_array;
}

// 23.1.3.14 Array.prototype.indexOf ( searchElement [ , fromIndex ] ), https://tc39.es/ecma262/#sec-array.prototype.indexof
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::index_of)
{
    auto search_element = vm.argument(0);
    auto from_index = vm.argument(1);

    // 1. Let O be ? ToObject(this value).
    auto* object = vm.this_value(global_object).to_object(global_object);
    if (vm.exception())
        return {};

    // 2. Let len be ? LengthOfArrayLike(O).
    auto length = length_of_array_like(global_object, *object);
    if (vm.exception())
        return {};

    // 3. If len is 0, return -1𝔽.
    if (length == 0)
        return Value(-1);

    // 4. Let n be ? ToIntegerOrInfinity(fromIndex).
    auto n = from_index.to_integer_or_infinity(global_object);
    if (vm.exception())
        return {};

    // 5. Assert: If fromIndex is undefined, then n is 0.
    if (from_index.is_undefined())
        VERIFY(n == 0);

    // 6. If n is +∞, return -1𝔽.
    if (Value(n).is_positive_infinity())
        return Value(-1);

    // 7. Else if n is -∞, set n to 0.
    if (Value(n).is_negative_infinity())
        n = 0;

    u32 k;

    // 8. If n ≥ 0, then
    if (n >= 0) {
        // a. Let k be n.
        k = (u32)n;
    }
    // 9. Else,
    else {
        // a. Let k be len + n.
        // b. If k < 0, set k to 0.
        k = max((i32)length + (i32)n, 0);
    }

    // 10. Repeat, while k < len,
    for (; k < length; ++k) {
        auto property_name = PropertyName { k };

        // a. Let kPresent be ? HasProperty(O, ! ToString(𝔽(k))).
        auto k_present = object->has_property(property_name);
        if (vm.exception())
            return {};

        // b. If kPresent is true, then
        if (k_present) {
            // i. Let elementK be ? Get(O, ! ToString(𝔽(k))).
            auto element_k = object->get(property_name);
            if (vm.exception())
                return {};

            // ii. Let same be IsStrictlyEqual(searchElement, elementK).
            auto same = strict_eq(search_element, element_k);

            // iii. If same is true, return 𝔽(k).
            if (same)
                return Value(k);
        }

        // c. Set k to k + 1.
    }

    // 11. Return -1𝔽.
    return Value(-1);
}

// 23.1.3.21 Array.prototype.reduce ( callbackfn [ , initialValue ] ), https://tc39.es/ecma262/#sec-array.prototype.reduce
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::reduce)
{
    auto callback_function = vm.argument(0);
    auto initial_value = vm.argument(1);

    // 1. Let O be ? ToObject(this value).
    auto* object = vm.this_value(global_object).to_object(global_object);
    if (vm.exception())
        return {};

    // 2. Let len be ? LengthOfArrayLike(O).
    auto length = length_of_array_like(global_object, *object);
    if (vm.exception())
        return {};

    // 3. If IsCallable(callbackfn) is false, throw a TypeError exception.
    if (!callback_function.is_function()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAFunction, callback_function.to_string_without_side_effects());
        return {};
    }

    // 4. If len = 0 and initialValue is not present, throw a TypeError exception.
    if (length == 0 && vm.argument_count() <= 1) {
        vm.throw_exception<TypeError>(global_object, ErrorType::ReduceNoInitial);
        return {};
    }

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
            auto property_name = PropertyName { k };

            // ii. Set kPresent to ? HasProperty(O, Pk).
            k_present = object->has_property(property_name);
            if (vm.exception())
                return {};

            // iii. If kPresent is true, then
            if (k_present) {
                // 1. Set accumulator to ? Get(O, Pk).
                accumulator = object->get(property_name);
                if (vm.exception())
                    return {};
            }

            // iv. Set k to k + 1.
        }

        // c. If kPresent is false, throw a TypeError exception.
        if (!k_present) {
            vm.throw_exception<TypeError>(global_object, ErrorType::ReduceNoInitial);
            return {};
        }
    }

    // 9. Repeat, while k < len,
    for (; k < length; ++k) {
        // a. Let Pk be ! ToString(𝔽(k)).
        auto property_name = PropertyName { k };

        // b. Let kPresent be ? HasProperty(O, Pk).
        auto k_present = object->has_property(property_name);

        // c. If kPresent is true, then
        if (k_present) {
            // i. Let kValue be ? Get(O, Pk).
            auto k_value = object->get(property_name);
            if (vm.exception())
                return {};

            // ii. Set accumulator to ? Call(callbackfn, undefined, « accumulator, kValue, 𝔽(k), O »).
            accumulator = vm.call(callback_function.as_function(), js_undefined(), accumulator, k_value, Value(k), object);
            if (vm.exception())
                return {};
        }

        // d. Set k to k + 1.
    }

    // 10. Return accumulator.
    return accumulator;
}

// 23.1.3.22 Array.prototype.reduceRight ( callbackfn [ , initialValue ] ), https://tc39.es/ecma262/#sec-array.prototype.reduceright
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::reduce_right)
{
    auto callback_function = vm.argument(0);
    auto initial_value = vm.argument(1);

    // 1. Let O be ? ToObject(this value).
    auto* object = vm.this_value(global_object).to_object(global_object);
    if (vm.exception())
        return {};

    // 2. Let len be ? LengthOfArrayLike(O).
    auto length = length_of_array_like(global_object, *object);
    if (vm.exception())
        return {};

    // 3. If IsCallable(callbackfn) is false, throw a TypeError exception.
    if (!callback_function.is_function()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAFunction, callback_function.to_string_without_side_effects());
        return {};
    }

    // 4. If len = 0 and initialValue is not present, throw a TypeError exception.
    if (length == 0 && vm.argument_count() <= 1) {
        vm.throw_exception<TypeError>(global_object, ErrorType::ReduceNoInitial);
        return {};
    }

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
            auto property_name = PropertyName { k };

            // ii. Set kPresent to ? HasProperty(O, Pk).
            k_present = object->has_property(property_name);
            if (vm.exception())
                return {};

            // iii. If kPresent is true, then
            if (k_present) {
                // 1. Set accumulator to ? Get(O, Pk).
                accumulator = object->get(property_name);
                if (vm.exception())
                    return {};
            }

            // iv. Set k to k - 1.
        }

        // c. If kPresent is false, throw a TypeError exception.
        if (!k_present) {
            vm.throw_exception<TypeError>(global_object, ErrorType::ReduceNoInitial);
            return {};
        }
    }

    // 9. Repeat, while k ≥ 0,
    for (; k >= 0; --k) {
        // a. Let Pk be ! ToString(𝔽(k)).
        auto property_name = PropertyName { k };

        // b. Let kPresent be ? HasProperty(O, Pk).
        auto k_present = object->has_property(property_name);
        if (vm.exception())
            return {};

        // c. If kPresent is true, then
        if (k_present) {
            // i. Let kValue be ? Get(O, Pk).
            auto k_value = object->get(property_name);
            if (vm.exception())
                return {};

            // ii. Set accumulator to ? Call(callbackfn, undefined, « accumulator, kValue, 𝔽(k), O »).
            accumulator = vm.call(callback_function.as_function(), js_undefined(), accumulator, k_value, Value((i32)k), object);
            if (vm.exception())
                return {};
        }

        // d. Set k to k - 1.
    }

    // 10. Return accumulator.
    return accumulator;
}

// 23.1.3.23 Array.prototype.reverse ( ), https://tc39.es/ecma262/#sec-array.prototype.reverse
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::reverse)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};
    auto length = length_of_array_like(global_object, *this_object);
    if (vm.exception())
        return {};

    auto middle = length / 2;
    for (size_t lower = 0; lower < middle; ++lower) {
        auto upper = length - lower - 1;

        auto lower_exists = this_object->has_property(lower);
        if (vm.exception())
            return {};
        Value lower_value;
        if (lower_exists) {
            lower_value = this_object->get(lower);
            if (vm.exception())
                return {};
        }

        auto upper_exists = this_object->has_property(upper);
        if (vm.exception())
            return {};
        Value upper_value;
        if (upper_exists) {
            upper_value = this_object->get(upper);
            if (vm.exception())
                return {};
        }

        if (lower_exists && upper_exists) {
            this_object->define_property(lower, upper_value);
            if (vm.exception())
                return {};
            this_object->define_property(upper, lower_value);
            if (vm.exception())
                return {};
        } else if (!lower_exists && upper_exists) {
            this_object->define_property(lower, upper_value);
            if (vm.exception())
                return {};
            this_object->delete_property_or_throw(upper);
            if (vm.exception())
                return {};
        } else if (lower_exists && !upper_exists) {
            this_object->delete_property_or_throw(lower);
            if (vm.exception())
                return {};
            this_object->define_property(upper, lower_value);
            if (vm.exception())
                return {};
        }
    }

    return this_object;
}

static void array_merge_sort(VM& vm, GlobalObject& global_object, FunctionObject* compare_func, MarkedValueList& arr_to_sort)
{
    // FIXME: it would probably be better to switch to insertion sort for small arrays for
    // better performance
    if (arr_to_sort.size() <= 1)
        return;

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

    array_merge_sort(vm, global_object, compare_func, left);
    if (vm.exception())
        return;
    array_merge_sort(vm, global_object, compare_func, right);
    if (vm.exception())
        return;

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
            auto call_result = vm.call(*compare_func, js_undefined(), left[left_index], right[right_index]);
            if (vm.exception())
                return;

            auto number = call_result.to_number(global_object);
            if (vm.exception())
                return;

            if (number.is_nan())
                comparison_result = 0;
            else
                comparison_result = number.as_double();
        } else {
            // FIXME: It would probably be much better to be smarter about this and implement
            // the Abstract Relational Comparison in line once iterating over code points, rather
            // than calling it twice after creating two primitive strings.

            auto x_string = x.to_primitive_string(global_object);
            if (vm.exception())
                return;
            auto y_string = y.to_primitive_string(global_object);
            if (vm.exception())
                return;

            auto x_string_value = Value(x_string);
            auto y_string_value = Value(y_string);

            // Because they are called with primitive strings, these abstract_relation calls
            // should never result in a VM exception.
            auto x_lt_y_relation = abstract_relation(global_object, true, x_string_value, y_string_value);
            VERIFY(x_lt_y_relation != TriState::Unknown);
            auto y_lt_x_relation = abstract_relation(global_object, true, y_string_value, x_string_value);
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
}

// 23.1.3.27 Array.prototype.sort ( comparefn ), https://tc39.es/ecma262/#sec-array.prototype.sort
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::sort)
{
    auto callback = vm.argument(0);
    if (!callback.is_undefined() && !callback.is_function()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAFunction, callback.to_string_without_side_effects());
        return {};
    }

    auto* object = vm.this_value(global_object).to_object(global_object);
    if (vm.exception())
        return {};

    auto length = length_of_array_like(global_object, *object);
    if (vm.exception())
        return {};

    MarkedValueList items(vm.heap());
    for (size_t k = 0; k < length; ++k) {
        auto k_present = object->has_property(k);
        if (vm.exception())
            return {};

        if (k_present) {
            auto k_value = object->get(k);
            if (vm.exception())
                return {};

            items.append(k_value);
        }
    }

    // Perform sorting by merge sort. This isn't as efficient compared to quick sort, but
    // quicksort can't be used in all cases because the spec requires Array.prototype.sort()
    // to be stable. FIXME: when initially scanning through the array, maintain a flag
    // for if an unstable sort would be indistinguishable from a stable sort (such as just
    // just strings or numbers), and in that case use quick sort instead for better performance.
    array_merge_sort(vm, global_object, callback.is_undefined() ? nullptr : &callback.as_function(), items);
    if (vm.exception())
        return {};

    for (size_t j = 0; j < items.size(); ++j) {
        object->set(j, items[j], true);
        if (vm.exception())
            return {};
    }

    // The empty parts of the array are always sorted to the end, regardless of the
    // compare function. FIXME: For performance, a similar process could be used
    // for undefined, which are sorted to right before the empty values.
    for (size_t j = items.size(); j < length; ++j) {
        object->delete_property_or_throw(j);
        if (vm.exception())
            return {};
    }

    return object;
}

// 23.1.3.17 Array.prototype.lastIndexOf ( searchElement [ , fromIndex ] ), https://tc39.es/ecma262/#sec-array.prototype.lastindexof
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::last_index_of)
{
    auto search_element = vm.argument(0);
    auto from_index = vm.argument(1);

    // 1. Let O be ? ToObject(this value).
    auto* object = vm.this_value(global_object).to_object(global_object);
    if (vm.exception())
        return {};

    // 2. Let len be ? LengthOfArrayLike(O).
    auto length = length_of_array_like(global_object, *object);
    if (vm.exception())
        return {};

    // 3. If len is 0, return -1𝔽.
    if (length == 0)
        return Value(-1);

    double n;

    // 4. If fromIndex is present, let n be ? ToIntegerOrInfinity(fromIndex); else let n be len - 1.
    if (vm.argument_count() >= 2) {
        n = from_index.to_integer_or_infinity(global_object);
        if (vm.exception())
            return {};
    } else {
        n = (double)length - 1;
    }

    // 5. If n is -∞, return -1𝔽.
    if (Value(n).is_negative_infinity())
        return Value(-1);

    i32 k;

    // 6. If n ≥ 0, then
    if (n >= 0) {
        // a. Let k be min(n, len - 1).
        k = min((i32)n, (i32)length - 1);
    }
    // 7. Else,
    else {
        //  a. Let k be len + n.
        k = (i32)length + (i32)n;
    }

    // 8. Repeat, while k ≥ 0,
    for (; k >= 0; --k) {
        auto property_name = PropertyName { k };

        // a. Let kPresent be ? HasProperty(O, ! ToString(𝔽(k))).
        auto k_present = object->has_property(property_name);
        if (vm.exception())
            return {};

        // b. If kPresent is true, then
        if (k_present) {
            // i. Let elementK be ? Get(O, ! ToString(𝔽(k))).
            auto element_k = object->get(property_name);
            if (vm.exception())
                return {};

            // ii. Let same be IsStrictlyEqual(searchElement, elementK).
            auto same = strict_eq(search_element, element_k);

            // iii. If same is true, return 𝔽(k).
            if (same)
                return Value(k);
        }

        // c. Set k to k - 1.
    }

    // 9. Return -1𝔽.
    return Value(-1);
}

// 23.1.3.13 Array.prototype.includes ( searchElement [ , fromIndex ] ), https://tc39.es/ecma262/#sec-array.prototype.includes
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::includes)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};
    i32 length = length_of_array_like(global_object, *this_object);
    if (vm.exception())
        return {};
    if (length == 0)
        return Value(false);
    i32 from_index = 0;
    if (vm.argument_count() >= 2) {
        from_index = vm.argument(1).to_i32(global_object);
        if (vm.exception())
            return {};
        if (from_index >= length)
            return Value(false);
        if (from_index < 0)
            from_index = max(length + from_index, 0);
    }
    auto value_to_find = vm.argument(0);
    for (i32 i = from_index; i < length; ++i) {
        auto element = this_object->get(i);
        if (vm.exception())
            return {};
        if (same_value_zero(element, value_to_find))
            return Value(true);
    }
    return Value(false);
}

// 23.1.3.8 Array.prototype.find ( predicate [ , thisArg ] ), https://tc39.es/ecma262/#sec-array.prototype.find
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::find)
{
    auto predicate = vm.argument(0);
    auto this_arg = vm.argument(1);

    // 1. Let O be ? ToObject(this value).
    auto* object = vm.this_value(global_object).to_object(global_object);
    if (vm.exception())
        return {};

    // 2. Let len be ? LengthOfArrayLike(O).
    auto length = length_of_array_like(global_object, *object);
    if (vm.exception())
        return {};

    // 3. If IsCallable(predicate) is false, throw a TypeError exception.
    if (!predicate.is_function()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAFunction, predicate.to_string_without_side_effects());
        return {};
    }

    // 4. Let k be 0.
    // 5. Repeat, while k < len,
    for (size_t k = 0; k < length; ++k) {
        // a. Let Pk be ! ToString(𝔽(k)).
        auto property_name = PropertyName { k };

        // b. Let kValue be ? Get(O, Pk).
        auto k_value = object->get(property_name);
        if (vm.exception())
            return {};

        // c. Let testResult be ! ToBoolean(? Call(predicate, thisArg, « kValue, 𝔽(k), O »)).
        auto test_result = vm.call(predicate.as_function(), this_arg, k_value, Value(k), object);
        if (vm.exception())
            return {};

        // d. If testResult is true, return kValue.
        if (test_result.to_boolean())
            return k_value;

        // e. Set k to k + 1.
    }

    // 6. Return undefined.
    return js_undefined();
}

// 23.1.3.9 Array.prototype.findIndex ( predicate [ , thisArg ] ), https://tc39.es/ecma262/#sec-array.prototype.findindex
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::find_index)
{
    auto predicate = vm.argument(0);
    auto this_arg = vm.argument(1);

    // 1. Let O be ? ToObject(this value).
    auto* object = vm.this_value(global_object).to_object(global_object);
    if (vm.exception())
        return {};

    // 2. Let len be ? LengthOfArrayLike(O).
    auto length = length_of_array_like(global_object, *object);
    if (vm.exception())
        return {};

    // 3. If IsCallable(predicate) is false, throw a TypeError exception.
    if (!predicate.is_function()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAFunction, predicate.to_string_without_side_effects());
        return {};
    }

    // 4. Let k be 0.
    // 5. Repeat, while k < len,
    for (size_t k = 0; k < length; ++k) {
        // a. Let Pk be ! ToString(𝔽(k)).
        auto property_name = PropertyName { k };

        // b. Let kValue be ? Get(O, Pk).
        auto k_value = object->get(property_name);
        if (vm.exception())
            return {};

        // c. Let testResult be ! ToBoolean(? Call(predicate, thisArg, « kValue, 𝔽(k), O »)).
        auto test_result = vm.call(predicate.as_function(), this_arg, k_value, Value(k), object);
        if (vm.exception())
            return {};

        // d. If testResult is true, return 𝔽(k).
        if (test_result.to_boolean())
            return Value(k);

        // e. Set k to k + 1.
    }

    // 6. Return -1𝔽.
    return Value(-1);
}

// 23.1.3.26 Array.prototype.some ( callbackfn [ , thisArg ] ), https://tc39.es/ecma262/#sec-array.prototype.some
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::some)
{
    auto callback_function = vm.argument(0);
    auto this_arg = vm.argument(1);

    // 1. Let O be ? ToObject(this value).
    auto* object = vm.this_value(global_object).to_object(global_object);
    if (vm.exception())
        return {};

    // 2. Let len be ? LengthOfArrayLike(O).
    auto length = length_of_array_like(global_object, *object);
    if (vm.exception())
        return {};

    // 3. If IsCallable(callbackfn) is false, throw a TypeError exception.
    if (!callback_function.is_function()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAFunction, callback_function.to_string_without_side_effects());
        return {};
    }

    // 4. Let k be 0.
    // 5. Repeat, while k < len,
    for (size_t k = 0; k < length; ++k) {
        // a. Let Pk be ! ToString(𝔽(k)).
        auto property_name = PropertyName { k };

        // b. Let kPresent be ? HasProperty(O, Pk).
        auto k_present = object->has_property(property_name);
        if (vm.exception())
            return {};

        // c. If kPresent is true, then
        if (k_present) {
            // i. Let kValue be ? Get(O, Pk).
            auto k_value = object->get(property_name);
            if (vm.exception())
                return {};

            // ii. Let testResult be ! ToBoolean(? Call(callbackfn, thisArg, « kValue, 𝔽(k), O »)).
            auto test_result = vm.call(callback_function.as_function(), this_arg, k_value, Value(k), object);
            if (vm.exception())
                return {};

            // iii. If testResult is true, return true.
            if (test_result.to_boolean())
                return Value(true);
        }

        // d. Set k to k + 1.
    }

    // 6. Return false.
    return Value(false);
}

// 23.1.3.5 Array.prototype.every ( callbackfn [ , thisArg ] ), https://tc39.es/ecma262/#sec-array.prototype.every
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::every)
{
    auto callback_function = vm.argument(0);
    auto this_arg = vm.argument(1);

    // 1. Let O be ? ToObject(this value).
    auto object = vm.this_value(global_object).to_object(global_object);
    if (vm.exception())
        return {};

    // 2. Let len be ? LengthOfArrayLike(O).
    auto length = length_of_array_like(global_object, *object);
    if (vm.exception())
        return {};

    // 3. If IsCallable(callbackfn) is false, throw a TypeError exception.
    if (!callback_function.is_function()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAFunction, callback_function.to_string_without_side_effects());
        return {};
    }

    // 4. Let k be 0.
    // 5. Repeat, while k < len,
    for (size_t k = 0; k < length; ++k) {
        // a. Let Pk be ! ToString(𝔽(k)).
        auto property_name = PropertyName { k };

        // b. Let kPresent be ? HasProperty(O, Pk).
        auto k_present = object->has_property(property_name);
        if (vm.exception())
            return {};

        // c. If kPresent is true, then
        if (k_present) {
            // i. Let kValue be ? Get(O, Pk).
            auto k_value = object->get(property_name);
            if (vm.exception())
                return {};

            // ii. Let testResult be ! ToBoolean(? Call(callbackfn, thisArg, « kValue, 𝔽(k), O »)).
            auto test_result = vm.call(callback_function.as_function(), this_arg, k_value, Value(k), object);
            if (vm.exception())
                return {};

            // iii. If testResult is false, return false.
            if (!test_result.to_boolean())
                return Value(false);
        }

        // d. Set k to k + 1.
    }

    // 6. Return true.
    return Value(true);
}

// 23.1.3.28 Array.prototype.splice ( start, deleteCount, ...items ), https://tc39.es/ecma262#sec-array.prototype.splice
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::splice)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};

    auto initial_length = length_of_array_like(global_object, *this_object);
    if (vm.exception())
        return {};

    auto relative_start = vm.argument(0).to_i32(global_object);
    if (vm.exception())
        return {};

    size_t actual_start;

    if (relative_start < 0)
        actual_start = max((ssize_t)initial_length + relative_start, (ssize_t)0);
    else
        actual_start = min((size_t)relative_start, initial_length);

    size_t insert_count = 0;
    size_t actual_delete_count = 0;

    if (vm.argument_count() == 1) {
        actual_delete_count = initial_length - actual_start;
    } else if (vm.argument_count() >= 2) {
        insert_count = vm.argument_count() - 2;
        i32 delete_count = vm.argument(1).to_i32(global_object);
        if (vm.exception())
            return {};

        actual_delete_count = min((size_t)max(delete_count, 0), initial_length - actual_start);
    }

    size_t new_length = initial_length + insert_count - actual_delete_count;

    if (new_length > MAX_ARRAY_LIKE_INDEX) {
        vm.throw_exception<TypeError>(global_object, ErrorType::ArrayMaxSize);
        return {};
    }

    auto* removed_elements = array_species_create(global_object, *this_object, actual_delete_count);
    if (vm.exception())
        return {};

    for (size_t i = 0; i < actual_delete_count; ++i) {
        auto from = actual_start + i;
        bool from_present = this_object->has_property(from);
        if (vm.exception())
            return {};

        if (from_present) {
            auto from_value = this_object->get(actual_start + i);
            if (vm.exception())
                return {};

            removed_elements->create_data_property_or_throw(i, from_value);
            if (vm.exception())
                return {};
        }
    }

    removed_elements->put(vm.names.length, Value(actual_delete_count));
    if (vm.exception())
        return {};

    if (insert_count < actual_delete_count) {
        for (size_t i = actual_start; i < initial_length - actual_delete_count; ++i) {
            auto from = this_object->get(i + actual_delete_count);
            if (vm.exception())
                return {};

            auto to = i + insert_count;

            if (!from.is_empty()) {
                this_object->define_property(to, from);
            } else {
                this_object->delete_property_or_throw(to);
            }
            if (vm.exception())
                return {};
        }

        for (size_t i = initial_length; i > new_length; --i) {
            this_object->delete_property_or_throw(i - 1);
            if (vm.exception())
                return {};
        }
    } else if (insert_count > actual_delete_count) {
        for (size_t i = initial_length - actual_delete_count; i > actual_start; --i) {
            auto from = this_object->get(i + actual_delete_count - 1);
            if (vm.exception())
                return {};

            auto to = i + insert_count - 1;

            if (!from.is_empty()) {
                this_object->define_property(to, from);
            } else {
                this_object->delete_property_or_throw(to);
            }
            if (vm.exception())
                return {};
        }
    }

    for (size_t i = 0; i < insert_count; ++i) {
        this_object->define_property(actual_start + i, vm.argument(i + 2));
        if (vm.exception())
            return {};
    }

    this_object->put(vm.names.length, Value((i32)new_length));
    if (vm.exception())
        return {};

    return removed_elements;
}

// 23.1.3.6 Array.prototype.fill ( value [ , start [ , end ] ] ), https://tc39.es/ecma262/#sec-array.prototype.fill
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::fill)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};

    ssize_t length = length_of_array_like(global_object, *this_object);
    if (vm.exception())
        return {};

    ssize_t relative_start = 0;
    ssize_t relative_end = length;

    if (vm.argument_count() >= 2) {
        relative_start = vm.argument(1).to_i32(global_object);
        if (vm.exception())
            return {};
    }

    if (vm.argument_count() >= 3) {
        relative_end = vm.argument(2).to_i32(global_object);
        if (vm.exception())
            return {};
    }

    size_t from, to;

    if (relative_start < 0)
        from = max(length + relative_start, 0L);
    else
        from = min(relative_start, length);

    if (relative_end < 0)
        to = max(length + relative_end, 0L);
    else
        to = min(relative_end, length);

    for (size_t i = from; i < to; i++) {
        this_object->put(i, vm.argument(0));
        if (vm.exception())
            return {};
    }

    return this_object;
}

// 23.1.3.32 Array.prototype.values ( ), https://tc39.es/ecma262/#sec-array.prototype.values
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::values)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};

    return ArrayIterator::create(global_object, this_object, Object::PropertyKind::Value);
}

// 23.1.3.16 Array.prototype.entries ( ), https://tc39.es/ecma262/#sec-array.prototype.entries
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::entries)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};

    return ArrayIterator::create(global_object, this_object, Object::PropertyKind::KeyAndValue);
}

// 23.1.3.16 Array.prototype.keys ( ), https://tc39.es/ecma262/#sec-array.prototype.keys
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::keys)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};

    return ArrayIterator::create(global_object, this_object, Object::PropertyKind::Key);
}

// 23.1.3.10.1 FlattenIntoArray ( target, source, sourceLen, start, depth [ , mapperFunction [ , thisArg ] ] ), https://tc39.es/ecma262/#sec-flattenintoarray
static size_t flatten_into_array(GlobalObject& global_object, Object& new_array, Object& array, size_t array_length, size_t target_index, double depth, FunctionObject* mapper_func = {}, Value this_arg = {})
{
    VERIFY(!mapper_func || (!this_arg.is_empty() && depth == 1));
    auto& vm = global_object.vm();

    for (size_t j = 0; j < array_length; ++j) {
        auto value_exists = array.has_property(j);
        if (vm.exception())
            return {};

        if (!value_exists)
            continue;
        auto value = array.get(j);
        if (vm.exception())
            return {};

        if (mapper_func) {
            value = vm.call(*mapper_func, this_arg, value, Value(j), &array);
            if (vm.exception())
                return {};
        }

        if (depth > 0 && value.is_array(global_object)) {
            auto length = length_of_array_like(global_object, value.as_array());
            if (vm.exception())
                return {};
            target_index = flatten_into_array(global_object, new_array, value.as_array(), length, target_index, depth - 1);
            if (vm.exception())
                return {};
            continue;
        }

        if (target_index >= MAX_ARRAY_LIKE_INDEX) {
            vm.throw_exception<TypeError>(global_object, ErrorType::InvalidIndex);
            return {};
        }

        new_array.create_data_property_or_throw(target_index, value);
        if (vm.exception())
            return {};

        ++target_index;
    }
    return target_index;
}

// 23.1.3.10 Array.prototype.flat ( [ depth ] ), https://tc39.es/ecma262/#sec-array.prototype.flat
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::flat)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};

    auto length = length_of_array_like(global_object, *this_object);
    if (vm.exception())
        return {};

    double depth = 1;
    if (!vm.argument(0).is_undefined()) {
        auto depth_num = vm.argument(0).to_integer_or_infinity(global_object);
        if (vm.exception())
            return {};
        depth = max(depth_num, 0.0);
    }

    auto* new_array = array_species_create(global_object, *this_object, 0);
    if (vm.exception())
        return {};

    flatten_into_array(global_object, *new_array, *this_object, length, 0, depth);
    if (vm.exception())
        return {};
    return new_array;
}

// 23.1.3.11 Array.prototype.flatMap ( mapperFunction [ , thisArg ] ), https://tc39.es/ecma262/#sec-array.prototype.flatmap
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::flat_map)
{
    auto mapper_function = vm.argument(0);
    auto this_arg = vm.argument(1);

    // 1. Let O be ? ToObject(this value).
    auto* object = vm.this_value(global_object).to_object(global_object);
    if (vm.exception())
        return {};

    // 2. Let sourceLen be ? LengthOfArrayLike(O).
    auto source_length = length_of_array_like(global_object, *object);
    if (vm.exception())
        return {};

    // 3. If ! IsCallable(mapperFunction) is false, throw a TypeError exception.
    if (!mapper_function.is_function()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAFunction, mapper_function.to_string_without_side_effects());
        return {};
    }

    // 4. Let A be ? ArraySpeciesCreate(O, 0).
    auto* array = array_species_create(global_object, *object, 0);
    if (vm.exception())
        return {};

    // 5. Perform ? FlattenIntoArray(A, O, sourceLen, 0, 1, mapperFunction, thisArg).
    flatten_into_array(global_object, *array, *object, source_length, 0, 1, &mapper_function.as_function(), this_arg);
    if (vm.exception())
        return {};

    // 6. Return A.
    return array;
}

// 23.1.3.3 Array.prototype.copyWithin ( target, start [ , end ] ), https://tc39.es/ecma262/#sec-array.prototype.copywithin
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::copy_within)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};
    auto length = length_of_array_like(global_object, *this_object);
    if (vm.exception())
        return {};

    auto relative_target = vm.argument(0).to_integer_or_infinity(global_object);
    if (vm.exception())
        return {};

    double to;
    if (relative_target < 0)
        to = max(length + relative_target, 0.0);
    else
        to = min(relative_target, (double)length);

    auto relative_start = vm.argument(1).to_integer_or_infinity(global_object);
    if (vm.exception())
        return {};

    double from;
    if (relative_start < 0)
        from = max(length + relative_start, 0.0);
    else
        from = min(relative_start, (double)length);

    auto relative_end = vm.argument(2).is_undefined() ? length : vm.argument(2).to_integer_or_infinity(global_object);
    if (vm.exception())
        return {};

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
        auto from_present = this_object->has_property(from_i);
        if (vm.exception())
            return {};

        if (from_present) {
            auto from_value = this_object->get(from_i);
            if (vm.exception())
                return {};
            this_object->put(to_i, from_value);
            if (vm.exception())
                return {};
        } else {
            this_object->delete_property_or_throw(to_i);
            if (vm.exception())
                return {};
        }

        from_i += direction;
        to_i += direction;
        --count_i;
    }

    return this_object;
}

// 1.1 Array.prototype.at ( index ), https://tc39.es/proposal-relative-indexing-method/#sec-array.prototype.at
JS_DEFINE_NATIVE_FUNCTION(ArrayPrototype::at)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};
    auto length = length_of_array_like(global_object, *this_object);
    if (vm.exception())
        return {};
    auto relative_index = vm.argument(0).to_integer_or_infinity(global_object);
    if (vm.exception())
        return {};
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
    return this_object->get(index.value());
}

}
