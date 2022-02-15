/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Utf16View.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/PrimitiveString.h>
#include <LibJS/Runtime/PropertyDescriptor.h>
#include <LibJS/Runtime/StringObject.h>

namespace JS {

// 10.4.3.4 StringCreate ( value, prototype ), https://tc39.es/ecma262/#sec-stringcreate
StringObject* StringObject::create(GlobalObject& global_object, PrimitiveString& primitive_string, Object& prototype)
{
    return global_object.heap().allocate<StringObject>(global_object, primitive_string, prototype);
}

StringObject::StringObject(PrimitiveString& string, Object& prototype)
    : Object(prototype)
    , m_string(string)
{
}

StringObject::~StringObject()
{
}

void StringObject::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    Object::initialize(global_object);
    define_direct_property(vm.names.length, Value(m_string.utf16_string_view().length_in_code_units()), 0);
}

void StringObject::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(&m_string);
}

// 10.4.3.5 StringGetOwnProperty ( S, P ), https://tc39.es/ecma262/#sec-stringgetownproperty
static Optional<PropertyDescriptor> string_get_own_property(StringObject const& string, PropertyKey const& property_key)
{
    // 1. Assert: S is an Object that has a [[StringData]] internal slot.
    // 2. Assert: IsPropertyKey(P) is true.
    VERIFY(property_key.is_valid());

    // 3. If Type(P) is not String, return undefined.
    // NOTE: The spec only uses string and symbol keys, and later coerces to numbers -
    // this is not the case for PropertyKey, so '!property_key.is_string()' would be wrong.
    if (property_key.is_symbol())
        return {};

    // 4. Let index be ! CanonicalNumericIndexString(P).
    auto index = canonical_numeric_index_string(property_key, CanonicalIndexMode::IgnoreNumericRoundtrip);

    // 5. If index is undefined, return undefined.
    // 6. If IsIntegralNumber(index) is false, return undefined.
    // 7. If index is -0𝔽, return undefined.
    if (!index.is_index())
        return {};

    // 8. Let str be S.[[StringData]].
    // 9. Assert: Type(str) is String.
    auto str = string.primitive_string().utf16_string_view();

    // 10. Let len be the length of str.
    auto length = str.length_in_code_units();

    // 11. If ℝ(index) < 0 or len ≤ ℝ(index), return undefined.
    if (length <= index.as_index())
        return {};

    // 12. Let resultStr be the String value of length 1, containing one code unit from str, specifically the code unit at index ℝ(index).
    auto result_str = js_string(string.vm(), str.substring_view(index.as_index(), 1));

    // 13. Return the PropertyDescriptor { [[Value]]: resultStr, [[Writable]]: false, [[Enumerable]]: true, [[Configurable]]: false }.
    return PropertyDescriptor {
        .value = result_str,
        .writable = false,
        .enumerable = true,
        .configurable = false,
    };
}

// 10.4.3.1 [[GetOwnProperty]] ( P ), https://tc39.es/ecma262/#sec-string-exotic-objects-getownproperty-p
ThrowCompletionOr<Optional<PropertyDescriptor>> StringObject::internal_get_own_property(PropertyKey const& property_key) const
{
    // Assert: IsPropertyKey(P) is true.

    // 2. Let desc be OrdinaryGetOwnProperty(S, P).
    auto descriptor = MUST(Object::internal_get_own_property(property_key));

    // 3. If desc is not undefined, return desc.
    if (descriptor.has_value())
        return descriptor;

    // 4. Return ! StringGetOwnProperty(S, P).
    return string_get_own_property(*this, property_key);
}

// 10.4.3.2 [[DefineOwnProperty]] ( P, Desc ), https://tc39.es/ecma262/#sec-string-exotic-objects-defineownproperty-p-desc
ThrowCompletionOr<bool> StringObject::internal_define_own_property(PropertyKey const& property_key, PropertyDescriptor const& property_descriptor)
{
    // 1. Assert: IsPropertyKey(P) is true.
    VERIFY(property_key.is_valid());

    // 2. Let stringDesc be ! StringGetOwnProperty(S, P).
    auto string_descriptor = string_get_own_property(*this, property_key);

    // 3. If stringDesc is not undefined, then
    if (string_descriptor.has_value()) {
        // a. Let extensible be S.[[Extensible]].
        auto extensible = m_is_extensible;

        // b. Return ! IsCompatiblePropertyDescriptor(extensible, Desc, stringDesc).
        return is_compatible_property_descriptor(extensible, property_descriptor, string_descriptor);
    }

    // 4. Return ! OrdinaryDefineOwnProperty(S, P, Desc).
    return Object::internal_define_own_property(property_key, property_descriptor);
}

// 10.4.3.3 [[OwnPropertyKeys]] ( ), https://tc39.es/ecma262/#sec-string-exotic-objects-ownpropertykeys
ThrowCompletionOr<MarkedVector<Value>> StringObject::internal_own_property_keys() const
{
    auto& vm = this->vm();

    // 1. Let keys be a new empty List.
    auto keys = MarkedVector<Value> { heap() };

    // 2. Let str be O.[[StringData]].
    auto str = m_string.utf16_string_view();

    // 3. Assert: Type(str) is String.

    // 4. Let len be the length of str.
    auto length = str.length_in_code_units();

    // 5. For each integer i starting with 0 such that i < len, in ascending order, do
    for (size_t i = 0; i < length; ++i) {
        // a. Add ! ToString(𝔽(i)) as the last element of keys.
        keys.append(js_string(vm, String::number(i)));
    }

    // 6. For each own property key P of O such that P is an array index and ! ToIntegerOrInfinity(P) ≥ len, in ascending numeric index order, do
    for (auto& entry : indexed_properties()) {
        if (entry.index() >= length) {
            // a. Add P as the last element of keys.
            keys.append(js_string(vm, String::number(entry.index())));
        }
    }

    // 7. For each own property key P of O such that Type(P) is String and P is not an array index, in ascending chronological order of property creation, do
    for (auto& it : shape().property_table_ordered()) {
        if (it.key.is_string()) {
            // a. Add P as the last element of keys.
            keys.append(it.key.to_value(vm));
        }
    }

    // 8. For each own property key P of O such that Type(P) is Symbol, in ascending chronological order of property creation, do
    for (auto& it : shape().property_table_ordered()) {
        if (it.key.is_symbol()) {
            // a. Add P as the last element of keys.
            keys.append(it.key.to_value(vm));
        }
    }

    // 9. Return keys.
    return { move(keys) };
}

}
