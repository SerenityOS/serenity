/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibWeb/Bindings/ExceptionOrUtils.h>
#include <LibWeb/Bindings/LegacyPlatformObject.h>

namespace Web::Bindings {

LegacyPlatformObject::LegacyPlatformObject(JS::Object& prototype)
    : PlatformObject(prototype)
{
}

LegacyPlatformObject::~LegacyPlatformObject() = default;

JS::ThrowCompletionOr<bool> LegacyPlatformObject::is_named_property_exposed_on_object(JS::PropertyKey const& property_key) const
{
    [[maybe_unused]] auto& vm = this->vm();

    // The spec doesn't say anything about the type of the property name here.
    // Numbers can be converted to a string, which is fine and what other engines do.
    // However, since a symbol cannot be converted to a string, it cannot be a supported property name. Return early if it's a symbol.
    if (property_key.is_symbol())
        return false;

    // 1. If P is not a supported property name of O, then return false.
    // NOTE: This is in it's own variable to enforce the type.
    // FIXME: Can this throw?
    Vector<String> supported_property_names = this->supported_property_names();
    auto property_key_string = property_key.to_string();
    if (!supported_property_names.contains_slow(property_key_string))
        return false;

    // 2. If O has an own property named P, then return false.
    // NOTE: This has to be done manually instead of using Object::has_own_property, as that would use the overridden internal_get_own_property.
    auto own_property_named_p = MUST(Object::internal_get_own_property(property_key));

    if (own_property_named_p.has_value())
        return false;

    // NOTE: Step 3 is not here as the interface doesn't have the LegacyOverrideBuiltIns extended attribute.
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

JS::ThrowCompletionOr<Optional<JS::PropertyDescriptor>> LegacyPlatformObject::legacy_platform_object_get_own_property_for_get_own_property_slot(JS::PropertyKey const& property_name) const
{

    [[maybe_unused]] auto& global_object = this->global_object();

    if (property_name.is_number()) {
        // 1. Let index be the result of calling ToUint32(P).
        u32 index = property_name.as_number();

        // 2. If index is a supported property index, then:
        // FIXME: Can this throw?
        if (is_supported_property_index(index)) {

            auto value = TRY(throw_dom_exception_if_needed(global_object, [&] { return item_value(index); }));

            // 5. Let desc be a newly created Property Descriptor with no fields.
            JS::PropertyDescriptor descriptor;

            // 6. Set desc.[[Value]] to the result of converting value to an ECMAScript value.
            descriptor.value = value;

            descriptor.writable = false;

            // 8. Set desc.[[Enumerable]] and desc.[[Configurable]] to true.
            descriptor.enumerable = true;
            descriptor.configurable = true;

            // 9. Return desc.
            return descriptor;
        }

        // 3. Set ignoreNamedProps to true.
        return TRY(Object::internal_get_own_property(property_name));
    }

    // 1. If the result of running the named property visibility algorithm with property name P and object O is true, then:
    if (TRY(is_named_property_exposed_on_object(property_name))) {
        // FIXME: It's unfortunate that this is done twice, once in is_named_property_exposed_on_object and here.
        auto property_name_string = property_name.to_string();

        auto value = TRY(throw_dom_exception_if_needed(global_object, [&] { return named_item_value(property_name_string); }));

        // 5. Let desc be a newly created Property Descriptor with no fields.
        JS::PropertyDescriptor descriptor;

        // 6. Set desc.[[Value]] to the result of converting value to an ECMAScript value.
        descriptor.value = value;

        descriptor.writable = false;

        descriptor.enumerable = false;

        // 9. Set desc.[[Configurable]] to true.
        descriptor.configurable = true;

        // 10. Return desc.
        return descriptor;
    }

    return TRY(Object::internal_get_own_property(property_name));
}

JS::ThrowCompletionOr<Optional<JS::PropertyDescriptor>> LegacyPlatformObject::legacy_platform_object_get_own_property_for_set_slot(JS::PropertyKey const& property_name) const
{
    if (!property_name.is_number())
        return TRY(Object::internal_get_own_property(property_name));

    // 1. Let index be the result of calling ToUint32(P).
    u32 index = property_name.as_number();

    // 2. If index is a supported property index, then:
    // FIXME: Can this throw?
    if (is_supported_property_index(index)) {

        auto value = TRY(throw_dom_exception_if_needed(vm(), [&] { return item_value(index); }));

        // 5. Let desc be a newly created Property Descriptor with no fields.
        JS::PropertyDescriptor descriptor;

        // 6. Set desc.[[Value]] to the result of converting value to an ECMAScript value.
        descriptor.value = value;

        descriptor.writable = false;

        // 8. Set desc.[[Enumerable]] and desc.[[Configurable]] to true.
        descriptor.enumerable = true;
        descriptor.configurable = true;

        // 9. Return desc.
        return descriptor;
    }

    // 3. Set ignoreNamedProps to true.
    return TRY(Object::internal_get_own_property(property_name));
}

JS::ThrowCompletionOr<Optional<JS::PropertyDescriptor>> LegacyPlatformObject::internal_get_own_property(JS::PropertyKey const& property_name) const
{
    // 1. Return LegacyPlatformObjectGetOwnProperty(O, P, false).
    return TRY(legacy_platform_object_get_own_property_for_get_own_property_slot(property_name));
}

JS::ThrowCompletionOr<bool> LegacyPlatformObject::internal_set(JS::PropertyKey const& property_name, JS::Value value, JS::Value receiver)
{
    [[maybe_unused]] auto& global_object = this->global_object();

    // 2. Let ownDesc be LegacyPlatformObjectGetOwnProperty(O, P, true).
    auto own_descriptor = TRY(legacy_platform_object_get_own_property_for_set_slot(property_name));

    // 3. Perform ? OrdinarySetWithOwnDescriptor(O, P, V, Receiver, ownDesc).
    // NOTE: The spec says "perform" instead of "return", meaning nothing will be returned on this path according to the spec, which isn't possible to do.
    //       Let's treat it as though it says "return" instead of "perform".
    return ordinary_set_with_own_descriptor(property_name, value, receiver, own_descriptor);
}

JS::ThrowCompletionOr<bool> LegacyPlatformObject::internal_define_own_property(JS::PropertyKey const& property_name, JS::PropertyDescriptor const& property_descriptor)
{
    [[maybe_unused]] auto& vm = this->vm();
    [[maybe_unused]] auto& global_object = this->global_object();

    if (property_name.is_number()) {
        // 1. If the result of calling IsDataDescriptor(Desc) is false, then return false.
        if (!property_descriptor.is_data_descriptor())
            return false;

        return false;
    }

    if (property_name.is_string()) {
        auto& property_name_as_string = property_name.as_string();

        // 1. Let creating be true if P is not a supported property name, and false otherwise.
        // NOTE: This is in it's own variable to enforce the type.
        // FIXME: Can this throw?
        Vector<String> supported_property_names = this->supported_property_names();
        [[maybe_unused]] bool creating = !supported_property_names.contains_slow(property_name_as_string);

        // NOTE: This has to be done manually instead of using Object::has_own_property, as that would use the overridden internal_get_own_property.
        auto own_property_named_p = TRY(Object::internal_get_own_property(property_name));

        if (!own_property_named_p.has_value()) {

            if (!creating)
                return false;
        }
    }

    // property_descriptor is a const&, thus we need to create a copy here to set [[Configurable]]
    JS::PropertyDescriptor descriptor_copy(property_descriptor);
    descriptor_copy.configurable = true;

    // 4. Return OrdinaryDefineOwnProperty(O, P, Desc).
    return Object::internal_define_own_property(property_name, descriptor_copy);
}

JS::ThrowCompletionOr<bool> LegacyPlatformObject::internal_delete(JS::PropertyKey const& property_name)
{
    [[maybe_unused]] auto& global_object = this->global_object();

    if (property_name.is_number()) {
        // 1. Let index be the result of calling ToUint32(P).
        u32 index = property_name.as_number();

        // 2. If index is not a supported property index, then return true.
        // FIXME: Can this throw?
        if (!is_supported_property_index(index))
            return true;

        // 3. Return false.
        return false;
    }

    if (TRY(is_named_property_exposed_on_object(property_name))) {

        return false;
    }

    // 3. If O has an own property with name P, then:
    auto own_property_named_p_descriptor = TRY(Object::internal_get_own_property(property_name));

    if (own_property_named_p_descriptor.has_value()) {
        // 1. If the property is not configurable, then return false.
        // 2. Otherwise, remove the property from O.
        if (*own_property_named_p_descriptor->configurable)
            storage_delete(property_name);
        else
            return false;
    }

    // 4. Return true.
    return true;
}

JS::ThrowCompletionOr<bool> LegacyPlatformObject::internal_prevent_extensions()
{
    // 1. Return false.
    return false;
}

JS::ThrowCompletionOr<JS::MarkedVector<JS::Value>> LegacyPlatformObject::internal_own_property_keys() const
{
    auto& vm = this->vm();

    // 1. Let keys be a new empty list of ECMAScript String and Symbol values.
    JS::MarkedVector<JS::Value> keys { heap() };

    for (u64 index = 0; index <= NumericLimits<u32>::max(); ++index) {
        if (is_supported_property_index(index))
            keys.append(js_string(vm, String::number(index)));
        else
            break;
    }

    for (auto& named_property : supported_property_names()) {
        if (TRY(is_named_property_exposed_on_object(named_property)))
            keys.append(js_string(vm, named_property));
    }

    // 4. For each P of O’s own property keys that is a String, in ascending chronological order of property creation, append P to keys.
    for (auto& it : shape().property_table_ordered()) {
        if (it.key.is_string())
            keys.append(it.key.to_value(vm));
    }

    // 5. For each P of O’s own property keys that is a Symbol, in ascending chronological order of property creation, append P to keys.
    for (auto& it : shape().property_table_ordered()) {
        if (it.key.is_symbol())
            keys.append(it.key.to_value(vm));
    }

    // FIXME: 6. Assert: keys has no duplicate items.

    // 7. Return keys.
    return { move(keys) };
}

JS::Value LegacyPlatformObject::item_value(size_t) const
{
    return JS::js_undefined();
}

JS::Value LegacyPlatformObject::named_item_value(FlyString const&) const
{
    return JS::js_undefined();
}

Vector<String> LegacyPlatformObject::supported_property_names() const
{
    return {};
}

bool LegacyPlatformObject::is_supported_property_index(u32) const
{
    return false;
}

}
