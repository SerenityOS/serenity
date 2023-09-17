/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibWeb/Bindings/ExceptionOrUtils.h>
#include <LibWeb/Bindings/LegacyPlatformObject.h>

namespace Web::Bindings {

LegacyPlatformObject::LegacyPlatformObject(JS::Realm& realm)
    : PlatformObject(realm)
{
}

LegacyPlatformObject::~LegacyPlatformObject() = default;

// https://webidl.spec.whatwg.org/#dfn-named-property-visibility
JS::ThrowCompletionOr<bool> LegacyPlatformObject::is_named_property_exposed_on_object(JS::PropertyKey const& property_key) const
{
    // The spec doesn't say anything about the type of the property name here.
    // Numbers can be converted to a string, which is fine and what other engines do.
    // However, since a symbol cannot be converted to a string, it cannot be a supported property name. Return early if it's a symbol.
    if (property_key.is_symbol())
        return false;

    // 1. If P is not a supported property name of O, then return false.
    // NOTE: This is in it's own variable to enforce the type.
    Vector<DeprecatedString> supported_property_names = this->supported_property_names();
    auto property_key_string = property_key.to_string();
    if (!supported_property_names.contains_slow(property_key_string))
        return false;

    // 2. If O has an own property named P, then return false.
    // NOTE: This has to be done manually instead of using Object::has_own_property, as that would use the overridden internal_get_own_property.
    auto own_property_named_p = MUST(Object::internal_get_own_property(property_key));

    if (own_property_named_p.has_value())
        return false;

    // 3. If O implements an interface that has the [LegacyOverrideBuiltIns] extended attribute, then return true.
    if (has_legacy_override_built_ins_interface_extended_attribute())
        return true;

    // 4. Let prototype be O.[[GetPrototypeOf]]().
    auto* prototype = TRY(internal_get_prototype_of());

    // 5. While prototype is not null:
    while (prototype) {
        // FIXME: 1. If prototype is not a named properties object, and prototype has an own property named P, then return false.
        //           (It currently does not check for named property objects)
        bool prototype_has_own_property_named_p = TRY(prototype->has_own_property(property_key));
        if (prototype_has_own_property_named_p)
            return false;

        // 2. Set prototype to prototype.[[GetPrototypeOf]]().
        prototype = TRY(prototype->internal_get_prototype_of());
    }

    // 6. Return true.
    return true;
}

// https://webidl.spec.whatwg.org/#LegacyPlatformObjectGetOwnProperty
JS::ThrowCompletionOr<Optional<JS::PropertyDescriptor>> LegacyPlatformObject::legacy_platform_object_get_own_property(JS::PropertyKey const& property_name, IgnoreNamedProps ignore_named_props) const
{
    auto& vm = this->vm();

    // 1. If O supports indexed properties and P is an array index, then:
    if (supports_indexed_properties() && property_name.is_number()) {
        // 1. Let index be the result of calling ToUint32(P).
        u32 index = property_name.as_number();

        // 2. If index is a supported property index, then:
        if (is_supported_property_index(index)) {
            // 1. Let operation be the operation used to declare the indexed property getter.
            // 2. Let value be an uninitialized variable.
            // 3. If operation was defined without an identifier, then set value to the result of performing the steps listed in the interface description to determine the value of an indexed property with index as the index.
            // 4. Otherwise, operation was defined with an identifier. Set value to the result of performing the method steps of operation with O as this and « index » as the argument values.
            auto value = TRY(throw_dom_exception_if_needed(vm, [&] { return item_value(index); }));

            // 5. Let desc be a newly created Property Descriptor with no fields.
            JS::PropertyDescriptor descriptor;

            // 6. Set desc.[[Value]] to the result of converting value to an ECMAScript value.
            descriptor.value = value;

            // 7. If O implements an interface with an indexed property setter, then set desc.[[Writable]] to true, otherwise set it to false.
            descriptor.writable = has_indexed_property_setter();

            // 8. Set desc.[[Enumerable]] and desc.[[Configurable]] to true.
            descriptor.enumerable = true;
            descriptor.configurable = true;

            // 9. Return desc.
            return descriptor;
        }

        // 3. Set ignoreNamedProps to true.
        ignore_named_props = IgnoreNamedProps::Yes;
    }

    // 2. If O supports named properties and ignoreNamedProps is false, then:
    if (supports_named_properties() && ignore_named_props == IgnoreNamedProps::No) {
        // 1. If the result of running the named property visibility algorithm with property name P and object O is true, then:
        if (TRY(is_named_property_exposed_on_object(property_name))) {
            // FIXME: It's unfortunate that this is done twice, once in is_named_property_exposed_on_object and here.
            auto property_name_string = property_name.to_string();

            // 1. Let operation be the operation used to declare the named property getter.
            // 2. Let value be an uninitialized variable.
            // 3. If operation was defined without an identifier, then set value to the result of performing the steps listed in the interface description to determine the value of a named property with P as the name.
            // 4. Otherwise, operation was defined with an identifier. Set value to the result of performing the method steps of operation with O as this and « P » as the argument values.
            auto value = TRY(throw_dom_exception_if_needed(vm, [&] { return named_item_value(property_name_string); }));

            // 5. Let desc be a newly created Property Descriptor with no fields.
            JS::PropertyDescriptor descriptor;

            // 6. Set desc.[[Value]] to the result of converting value to an ECMAScript value.
            descriptor.value = value;

            // 7. If O implements an interface with a named property setter, then set desc.[[Writable]] to true, otherwise set it to false.
            descriptor.writable = has_named_property_setter();

            // 8. If O implements an interface with the [LegacyUnenumerableNamedProperties] extended attribute, then set desc.[[Enumerable]] to false, otherwise set it to true.
            descriptor.enumerable = !has_legacy_unenumerable_named_properties_interface_extended_attribute();

            // 9. Set desc.[[Configurable]] to true.
            descriptor.configurable = true;

            // 10. Return desc.
            return descriptor;
        }
    }

    // 3. Return OrdinaryGetOwnProperty(O, P).
    return TRY(Object::internal_get_own_property(property_name));
}

// https://webidl.spec.whatwg.org/#invoke-indexed-setter
WebIDL::ExceptionOr<void> LegacyPlatformObject::invoke_indexed_property_setter(JS::PropertyKey const& property_name, JS::Value value)
{
    // 1. Let index be the result of calling ? ToUint32(P).
    auto index = property_name.as_number();

    // 2. Let creating be true if index is not a supported property index, and false otherwise.
    bool creating = !is_supported_property_index(index);

    // FIXME: We do not have this information at this point, so converting the value is left as an exercise to the inheritor of LegacyPlatformObject.
    // 3. Let operation be the operation used to declare the indexed property setter.
    // 4. Let T be the type of the second argument of operation.
    // 5. Let value be the result of converting V to an IDL value of type T.

    // 6. If operation was defined without an identifier, then:
    if (!indexed_property_setter_has_identifier()) {
        // 1. If creating is true, then perform the steps listed in the interface description to set the value of a new indexed property with index as the index and value as the value.
        if (creating)
            return set_value_of_new_indexed_property(index, value);

        // 2. Otherwise, creating is false. Perform the steps listed in the interface description to set the value of an existing indexed property with index as the index and value as the value.
        return set_value_of_existing_indexed_property(index, value);
    }

    // 7. Otherwise, operation was defined with an identifier. Perform the method steps of operation with O as this and « index, value » as the argument values.
    return set_value_of_indexed_property(index, value);
}

// https://webidl.spec.whatwg.org/#invoke-named-setter
WebIDL::ExceptionOr<void> LegacyPlatformObject::invoke_named_property_setter(DeprecatedString const& property_name, JS::Value value)
{
    // 1. Let creating be true if P is not a supported property name, and false otherwise.
    Vector<DeprecatedString> supported_property_names = this->supported_property_names();
    bool creating = !supported_property_names.contains_slow(property_name);

    // FIXME: We do not have this information at this point, so converting the value is left as an exercise to the inheritor of LegacyPlatformObject.
    // 2. Let operation be the operation used to declare the indexed property setter.
    // 3. Let T be the type of the second argument of operation.
    // 4. Let value be the result of converting V to an IDL value of type T.

    // 5. If operation was defined without an identifier, then:
    if (!named_property_setter_has_identifier()) {
        // 1. If creating is true, then perform the steps listed in the interface description to set the value of a new named property with P as the name and value as the value.
        if (creating)
            return set_value_of_new_named_property(property_name, value);

        // 2. Otherwise, creating is false. Perform the steps listed in the interface description to set the value of an existing named property with P as the name and value as the value.
        return set_value_of_existing_named_property(property_name, value);
    }

    // 6. Otherwise, operation was defined with an identifier. Perform the method steps of operation with O as this and « P, value » as the argument values.
    return set_value_of_named_property(property_name, value);
}

// https://webidl.spec.whatwg.org/#legacy-platform-object-getownproperty
JS::ThrowCompletionOr<Optional<JS::PropertyDescriptor>> LegacyPlatformObject::internal_get_own_property(JS::PropertyKey const& property_name) const
{
    // 1. Return ? LegacyPlatformObjectGetOwnProperty(O, P, false).
    return TRY(legacy_platform_object_get_own_property(property_name, IgnoreNamedProps::No));
}

// https://webidl.spec.whatwg.org/#legacy-platform-object-set
JS::ThrowCompletionOr<bool> LegacyPlatformObject::internal_set(JS::PropertyKey const& property_name, JS::Value value, JS::Value receiver)
{
    auto& vm = this->vm();

    // 1. If O and Receiver are the same object, then:
    if (receiver.is_object() && &receiver.as_object() == this) {
        // 1. If O implements an interface with an indexed property setter and P is an array index, then:
        if (has_indexed_property_setter() && property_name.is_number()) {
            // 1. Invoke the indexed property setter on O with P and V.
            TRY(throw_dom_exception_if_needed(vm, [&] { return invoke_indexed_property_setter(property_name, value); }));

            // 2. Return true.
            return true;
        }

        // 2. If O implements an interface with a named property setter and Type(P) is String, then:
        if (has_named_property_setter() && property_name.is_string()) {
            // 1. Invoke the named property setter on O with P and V.
            TRY(throw_dom_exception_if_needed(vm, [&] { return invoke_named_property_setter(property_name.as_string(), value); }));

            // 2. Return true.
            return true;
        }
    }

    // 2. Let ownDesc be ? LegacyPlatformObjectGetOwnProperty(O, P, true).
    auto own_descriptor = TRY(legacy_platform_object_get_own_property(property_name, IgnoreNamedProps::Yes));

    // 3. Perform ? OrdinarySetWithOwnDescriptor(O, P, V, Receiver, ownDesc).
    // NOTE: The spec says "perform" instead of "return", meaning nothing will be returned on this path according to the spec, which isn't possible to do.
    //       Let's treat it as though it says "return" instead of "perform".
    return ordinary_set_with_own_descriptor(property_name, value, receiver, own_descriptor);
}

// https://webidl.spec.whatwg.org/#legacy-platform-object-defineownproperty
JS::ThrowCompletionOr<bool> LegacyPlatformObject::internal_define_own_property(JS::PropertyKey const& property_name, JS::PropertyDescriptor const& property_descriptor)
{
    auto& vm = this->vm();

    // 1. If O supports indexed properties and P is an array index, then:
    if (supports_indexed_properties() && property_name.is_number()) {
        // 1. If the result of calling IsDataDescriptor(Desc) is false, then return false.
        if (!property_descriptor.is_data_descriptor())
            return false;

        // 2. If O does not implement an interface with an indexed property setter, then return false.
        if (!has_indexed_property_setter())
            return false;

        // 3. Invoke the indexed property setter on O with P and Desc.[[Value]].
        TRY(throw_dom_exception_if_needed(vm, [&] { return invoke_indexed_property_setter(property_name, property_descriptor.value.value()); }));

        // 4. Return true.
        return true;
    }

    // 2. If O supports named properties, O does not implement an interface with the [Global] extended attribute, Type(P) is String, and P is not an unforgeable property name of O, then:
    // FIXME: Check if P is not an unforgeable property name of O
    if (supports_named_properties() && !has_global_interface_extended_attribute() && property_name.is_string()) {
        auto const& property_name_as_string = property_name.as_string();

        // 1. Let creating be true if P is not a supported property name, and false otherwise.
        // NOTE: This is in it's own variable to enforce the type.
        Vector<DeprecatedString> supported_property_names = this->supported_property_names();
        bool creating = !supported_property_names.contains_slow(property_name_as_string);

        // 2. If O implements an interface with the [LegacyOverrideBuiltIns] extended attribute or O does not have an own property named P, then:
        // NOTE: Own property lookup has to be done manually instead of using Object::has_own_property, as that would use the overridden internal_get_own_property.
        if (has_legacy_override_built_ins_interface_extended_attribute() || !TRY(Object::internal_get_own_property(property_name)).has_value()) {
            // 1. If creating is false and O does not implement an interface with a named property setter, then return false.
            if (!creating && !has_named_property_setter())
                return false;

            // 2. If O implements an interface with a named property setter, then:
            if (has_named_property_setter()) {
                // 1. If the result of calling IsDataDescriptor(Desc) is false, then return false.
                if (!property_descriptor.is_data_descriptor())
                    return false;

                // 2. Invoke the named property setter on O with P and Desc.[[Value]].
                TRY(throw_dom_exception_if_needed(vm, [&] { return invoke_named_property_setter(property_name_as_string, property_descriptor.value.value()); }));

                // 3. Return true.
                return true;
            }
        }
    }

    // 3. If O does not implement an interface with the [Global] extended attribute, then set Desc.[[Configurable]] to true.
    // 4. Return ! OrdinaryDefineOwnProperty(O, P, Desc).
    if (!has_global_interface_extended_attribute()) {
        // property_descriptor is a const&, thus we need to create a copy here to set [[Configurable]]
        JS::PropertyDescriptor descriptor_copy(property_descriptor);
        descriptor_copy.configurable = true;
        return Object::internal_define_own_property(property_name, descriptor_copy);
    }

    return Object::internal_define_own_property(property_name, property_descriptor);
}

// https://webidl.spec.whatwg.org/#legacy-platform-object-delete
JS::ThrowCompletionOr<bool> LegacyPlatformObject::internal_delete(JS::PropertyKey const& property_name)
{
    auto& vm = this->vm();

    // 1. If O supports indexed properties and P is an array index, then:
    if (supports_indexed_properties() && property_name.is_number()) {
        // 1. Let index be the result of calling ! ToUint32(P).
        u32 index = property_name.as_number();

        // 2. If index is not a supported property index, then return true.
        if (!is_supported_property_index(index))
            return true;

        // 3. Return false.
        return false;
    }

    // 2. If O supports named properties, O does not implement an interface with the [Global] extended attribute and
    //    the result of calling the named property visibility algorithm with property name P and object O is true, then:
    if (supports_named_properties() && !has_global_interface_extended_attribute() && TRY(is_named_property_exposed_on_object(property_name))) {
        // 1. If O does not implement an interface with a named property deleter, then return false.
        if (!has_named_property_deleter())
            return false;

        // FIXME: It's unfortunate that this is done twice, once in is_named_property_exposed_on_object and here.
        auto property_name_string = property_name.to_string();

        // 2. Let operation be the operation used to declare the named property deleter.
        // 3. If operation was defined without an identifier, then:
        //    1. Perform the steps listed in the interface description to delete an existing named property with P as the name.
        //    2. If the steps indicated that the deletion failed, then return false.
        // 4. Otherwise, operation was defined with an identifier:
        //    1. Perform method steps of operation with O as this and « P » as the argument values.
        //    2. If operation was declared with a return type of boolean and the steps returned false, then return false.
        auto did_deletion_fail = TRY(throw_dom_exception_if_needed(vm, [&] { return delete_value(property_name_string); }));
        if (!named_property_deleter_has_identifier())
            VERIFY(did_deletion_fail != DidDeletionFail::NotRelevant);

        if (did_deletion_fail == DidDeletionFail::Yes)
            return false;

        // 5. Return true.
        return true;
    }

    // 3. If O has an own property with name P, then:
    // NOTE: This has to be done manually instead of using Object::has_own_property, as that would use the overridden internal_get_own_property.
    auto own_property_named_p_descriptor = TRY(Object::internal_get_own_property(property_name));

    if (own_property_named_p_descriptor.has_value()) {
        // 1. If the property is not configurable, then return false.
        if (!own_property_named_p_descriptor->configurable.value())
            return false;

        // 2. Otherwise, remove the property from O.
        storage_delete(property_name);
    }

    // 4. Return true.
    return true;
}

// https://webidl.spec.whatwg.org/#legacy-platform-object-preventextensions
JS::ThrowCompletionOr<bool> LegacyPlatformObject::internal_prevent_extensions()
{
    // 1. Return false.
    // Spec Note: Note: this keeps legacy platform objects extensible by making [[PreventExtensions]] fail for them.
    return false;
}

// https://webidl.spec.whatwg.org/#legacy-platform-object-ownpropertykeys
JS::ThrowCompletionOr<JS::MarkedVector<JS::Value>> LegacyPlatformObject::internal_own_property_keys() const
{
    auto& vm = this->vm();

    // 1. Let keys be a new empty list of ECMAScript String and Symbol values.
    JS::MarkedVector<JS::Value> keys { heap() };

    // 2. If O supports indexed properties, then for each index of O’s supported property indices, in ascending numerical order, append ! ToString(index) to keys.
    if (supports_indexed_properties()) {
        for (u64 index = 0; index <= NumericLimits<u32>::max(); ++index) {
            if (is_supported_property_index(index))
                keys.append(JS::PrimitiveString::create(vm, DeprecatedString::number(index)));
            else
                break;
        }
    }

    // 3. If O supports named properties, then for each P of O’s supported property names that is visible according to the named property visibility algorithm, append P to keys.
    if (supports_named_properties()) {
        for (auto& named_property : supported_property_names()) {
            if (TRY(is_named_property_exposed_on_object(named_property)))
                keys.append(JS::PrimitiveString::create(vm, named_property));
        }
    }

    // 4. For each P of O’s own property keys that is a String, in ascending chronological order of property creation, append P to keys.
    for (auto& it : shape().property_table()) {
        if (it.key.is_string())
            keys.append(it.key.to_value(vm));
    }

    // 5. For each P of O’s own property keys that is a Symbol, in ascending chronological order of property creation, append P to keys.
    for (auto& it : shape().property_table()) {
        if (it.key.is_symbol())
            keys.append(it.key.to_value(vm));
    }

    // FIXME: 6. Assert: keys has no duplicate items.

    // 7. Return keys.
    return { move(keys) };
}

WebIDL::ExceptionOr<JS::Value> LegacyPlatformObject::item_value(size_t) const
{
    return JS::js_undefined();
}

WebIDL::ExceptionOr<JS::Value> LegacyPlatformObject::named_item_value(DeprecatedFlyString const&) const
{
    return JS::js_undefined();
}

Vector<DeprecatedString> LegacyPlatformObject::supported_property_names() const
{
    return {};
}

bool LegacyPlatformObject::is_supported_property_index(u32) const
{
    return false;
}

}
