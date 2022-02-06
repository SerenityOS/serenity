/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

// 10.4.2.2 ArrayCreate ( length [ , proto ] ), https://tc39.es/ecma262/#sec-arraycreate
ThrowCompletionOr<Array*> Array::create(GlobalObject& global_object, size_t length, Object* prototype)
{
    auto& vm = global_object.vm();
    if (length > NumericLimits<u32>::max())
        return vm.throw_completion<RangeError>(global_object, ErrorType::InvalidLength, "array");
    if (!prototype)
        prototype = global_object.array_prototype();
    auto* array = global_object.heap().allocate<Array>(global_object, *prototype);
    MUST(array->internal_define_own_property(vm.names.length, { .value = Value(length), .writable = true, .enumerable = false, .configurable = false }));
    return array;
}

// 7.3.18 CreateArrayFromList ( elements ), https://tc39.es/ecma262/#sec-createarrayfromlist
Array* Array::create_from(GlobalObject& global_object, Vector<Value> const& elements)
{
    // 1. Assert: elements is a List whose elements are all ECMAScript language values.

    // 2. Let array be ! ArrayCreate(0).
    auto* array = MUST(Array::create(global_object, 0));

    // 3. Let n be 0.
    // 4. For each element e of elements, do
    for (u32 n = 0; n < elements.size(); ++n) {
        // a. Perform ! CreateDataPropertyOrThrow(array, ! ToString(𝔽(n)), e).
        MUST(array->create_data_property_or_throw(n, elements[n]));

        // b. Set n to n + 1.
    }

    // 5. Return array.
    return array;
}

Array::Array(Object& prototype)
    : Object(prototype)
{
}

Array::~Array()
{
}

// 10.4.2.4 ArraySetLength ( A, Desc ), https://tc39.es/ecma262/#sec-arraysetlength
ThrowCompletionOr<bool> Array::set_length(PropertyDescriptor const& property_descriptor)
{
    auto& global_object = this->global_object();
    auto& vm = this->vm();

    // 1. If Desc.[[Value]] is absent, then
    // a. Return OrdinaryDefineOwnProperty(A, "length", Desc).
    // 2. Let newLenDesc be a copy of Desc.
    // NOTE: Handled by step 16

    size_t new_length = indexed_properties().array_like_size();
    if (property_descriptor.value.has_value()) {
        // 3. Let newLen be ? ToUint32(Desc.[[Value]]).
        new_length = TRY(property_descriptor.value->to_u32(global_object));
        // 4. Let numberLen be ? ToNumber(Desc.[[Value]]).
        auto number_length = TRY(property_descriptor.value->to_number(global_object));
        // 5. If newLen is not the same value as numberLen, throw a RangeError exception.
        if (new_length != number_length.as_double())
            return vm.throw_completion<RangeError>(global_object, ErrorType::InvalidLength, "array");
    }

    // 6. Set newLenDesc.[[Value]] to newLen.
    // 7. Let oldLenDesc be OrdinaryGetOwnProperty(A, "length").
    // 8. Assert: ! IsDataDescriptor(oldLenDesc) is true.
    // 9. Assert: oldLenDesc.[[Configurable]] is false.
    // 10. Let oldLen be oldLenDesc.[[Value]].
    // 11. If newLen ≥ oldLen, then
    // a. Return OrdinaryDefineOwnProperty(A, "length", newLenDesc).
    // 12. If oldLenDesc.[[Writable]] is false, return false.
    // NOTE: Handled by step 16

    // 13. If newLenDesc.[[Writable]] is absent or has the value true, let newWritable be true.
    // 14. Else,
    // a. NOTE: Setting the [[Writable]] attribute to false is deferred in case any elements cannot be deleted.
    // b. Let newWritable be false.
    auto new_writable = property_descriptor.writable.value_or(true);

    // c. Set newLenDesc.[[Writable]] to true.
    // 15. Let succeeded be ! OrdinaryDefineOwnProperty(A, "length", newLenDesc).
    // 16. If succeeded is false, return false.
    // NOTE: Because the length property does not actually exist calling OrdinaryDefineOwnProperty
    // will result in unintended behavior, so instead we only implement here the small subset of
    // checks performed inside of it that would have mattered to us:

    // 10.1.6.3 ValidateAndApplyPropertyDescriptor ( O, P, extensible, Desc, current ), https://tc39.es/ecma262/#sec-validateandapplypropertydescriptor
    // 4. If current.[[Configurable]] is false, then
    // a. If Desc.[[Configurable]] is present and its value is true, return false.
    if (property_descriptor.configurable.has_value() && *property_descriptor.configurable)
        return false;
    // b. If Desc.[[Enumerable]] is present and ! SameValue(Desc.[[Enumerable]], current.[[Enumerable]]) is false, return false.
    if (property_descriptor.enumerable.has_value() && *property_descriptor.enumerable)
        return false;
    // 6. Else if ! SameValue(! IsDataDescriptor(current), ! IsDataDescriptor(Desc)) is false, then
    if (property_descriptor.is_accessor_descriptor()) {
        // a. If current.[[Configurable]] is false, return false.
        return false;
    }
    // 7. Else if IsDataDescriptor(current) and IsDataDescriptor(Desc) are both true, then
    // a. If current.[[Configurable]] is false and current.[[Writable]] is false, then
    if (!m_length_writable) {
        // i. If Desc.[[Writable]] is present and Desc.[[Writable]] is true, return false.
        if (property_descriptor.writable.has_value() && *property_descriptor.writable)
            return false;
        // ii. If Desc.[[Value]] is present and SameValue(Desc.[[Value]], current.[[Value]]) is false, return false.
        if (new_length != indexed_properties().array_like_size())
            return false;
    }

    // 17. For each own property key P of A that is an array index, whose numeric value is greater than or equal to newLen, in descending numeric index order, do
    // a. Let deleteSucceeded be ! A.[[Delete]](P).
    // b. If deleteSucceeded is false, then
    // i. Set newLenDesc.[[Value]] to ! ToUint32(P) + 1𝔽.
    bool success = indexed_properties().set_array_like_size(new_length);

    // ii. If newWritable is false, set newLenDesc.[[Writable]] to false.
    // iii. Perform ! OrdinaryDefineOwnProperty(A, "length", newLenDesc).
    // NOTE: Handled by step 18

    // 18. If newWritable is false, then
    // a. Set succeeded to ! OrdinaryDefineOwnProperty(A, "length", PropertyDescriptor { [[Writable]]: false }).
    // b. Assert: succeeded is true.
    if (!new_writable)
        m_length_writable = false;

    // NOTE: Continuation of step #17
    // iv. Return false.
    if (!success)
        return false;

    // 19. Return true.
    return true;
}

// NON-STANDARD: Used to return the value of the ephemeral length property
ThrowCompletionOr<Optional<PropertyDescriptor>> Array::internal_get_own_property(PropertyKey const& property_key) const
{
    auto& vm = this->vm();
    if (property_key.is_string() && property_key.as_string() == vm.names.length.as_string())
        return PropertyDescriptor { .value = Value(indexed_properties().array_like_size()), .writable = m_length_writable, .enumerable = false, .configurable = false };

    return Object::internal_get_own_property(property_key);
}

// 10.4.2.1 [[DefineOwnProperty]] ( P, Desc ), https://tc39.es/ecma262/#sec-array-exotic-objects-defineownproperty-p-desc
ThrowCompletionOr<bool> Array::internal_define_own_property(PropertyKey const& property_key, PropertyDescriptor const& property_descriptor)
{
    auto& vm = this->vm();

    // 1. Assert: IsPropertyKey(P) is true.
    VERIFY(property_key.is_valid());

    // 2. If P is "length", then
    if (property_key.is_string() && property_key.as_string() == vm.names.length.as_string()) {
        // a. Return ? ArraySetLength(A, Desc).
        return set_length(property_descriptor);
    }

    // 3. Else if P is an array index, then
    if (property_key.is_number()) {
        // a. Let oldLenDesc be OrdinaryGetOwnProperty(A, "length").
        // b. Assert: ! IsDataDescriptor(oldLenDesc) is true.
        // c. Assert: oldLenDesc.[[Configurable]] is false.
        // d. Let oldLen be oldLenDesc.[[Value]].
        // e. Assert: oldLen is a non-negative integral Number.
        // f. Let index be ! ToUint32(P).

        // g. If index ≥ oldLen and oldLenDesc.[[Writable]] is false, return false.
        if (property_key.as_number() >= indexed_properties().array_like_size() && !m_length_writable)
            return false;

        // h. Let succeeded be ! OrdinaryDefineOwnProperty(A, P, Desc).
        auto succeeded = MUST(Object::internal_define_own_property(property_key, property_descriptor));

        // i. If succeeded is false, return false.
        if (!succeeded)
            return false;

        // j. If index ≥ oldLen, then
        // i. Set oldLenDesc.[[Value]] to index + 1𝔽.
        // ii. Set succeeded to OrdinaryDefineOwnProperty(A, "length", oldLenDesc).
        // iii. Assert: succeeded is true.

        // k. Return true.
        return true;
    }

    // 4. Return OrdinaryDefineOwnProperty(A, P, Desc).
    return Object::internal_define_own_property(property_key, property_descriptor);
}

// NON-STANDARD: Used to reject deletes to ephemeral (non-configurable) length property
ThrowCompletionOr<bool> Array::internal_delete(PropertyKey const& property_key)
{
    auto& vm = this->vm();
    if (property_key.is_string() && property_key.as_string() == vm.names.length.as_string())
        return false;
    return Object::internal_delete(property_key);
}

// NON-STANDARD: Used to inject the ephemeral length property's key
ThrowCompletionOr<MarkedValueList> Array::internal_own_property_keys() const
{
    auto& vm = this->vm();
    auto keys = TRY(Object::internal_own_property_keys());
    // FIXME: This is pretty expensive, find a better way to do this
    keys.insert(indexed_properties().real_size(), js_string(vm, vm.names.length.as_string()));
    return { move(keys) };
}

}
