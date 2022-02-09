/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Accessor.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/ECMAScriptFunctionObject.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/PropertyDescriptor.h>
#include <LibJS/Runtime/ProxyObject.h>
#include <LibJS/Runtime/Shape.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

// 10.1.12 OrdinaryObjectCreate ( proto [ , additionalInternalSlotsList ] ), https://tc39.es/ecma262/#sec-ordinaryobjectcreate
Object* Object::create(GlobalObject& global_object, Object* prototype)
{
    if (!prototype)
        return global_object.heap().allocate<Object>(global_object, *global_object.empty_object_shape());
    else if (prototype == global_object.object_prototype())
        return global_object.heap().allocate<Object>(global_object, *global_object.new_object_shape());
    else
        return global_object.heap().allocate<Object>(global_object, *prototype);
}

Object::Object(GlobalObjectTag)
{
    // This is the global object
    m_shape = heap().allocate_without_global_object<Shape>(*this);
}

Object::Object(ConstructWithoutPrototypeTag, GlobalObject& global_object)
{
    m_shape = heap().allocate_without_global_object<Shape>(global_object);
}

Object::Object(GlobalObject& global_object, Object* prototype)
{
    m_shape = global_object.empty_object_shape();
    if (prototype != nullptr)
        set_prototype(prototype);
}

Object::Object(Object& prototype)
{
    m_shape = prototype.global_object().empty_object_shape();
    set_prototype(&prototype);
}

Object::Object(Shape& shape)
    : m_shape(&shape)
{
    m_storage.resize(shape.property_count());
}

void Object::initialize(GlobalObject&)
{
}

Object::~Object()
{
}

// 7.2 Testing and Comparison Operations, https://tc39.es/ecma262/#sec-testing-and-comparison-operations

// 7.2.5 IsExtensible ( O ), https://tc39.es/ecma262/#sec-isextensible-o
ThrowCompletionOr<bool> Object::is_extensible() const
{
    // 1. Return ? O.[[IsExtensible]]().
    return internal_is_extensible();
}

// 7.3 Operations on Objects, https://tc39.es/ecma262/#sec-operations-on-objects

// 7.3.2 Get ( O, P ), https://tc39.es/ecma262/#sec-get-o-p
ThrowCompletionOr<Value> Object::get(PropertyKey const& property_key) const
{
    // 1. Assert: Type(O) is Object.

    // 2. Assert: IsPropertyKey(P) is true.
    VERIFY(property_key.is_valid());

    // 3. Return ? O.[[Get]](P, O).
    return TRY(internal_get(property_key, this));
}

// NOTE: 7.3.3 GetV ( V, P ) is implemented as Value::get().

// 7.3.4 Set ( O, P, V, Throw ), https://tc39.es/ecma262/#sec-set-o-p-v-throw
ThrowCompletionOr<bool> Object::set(PropertyKey const& property_key, Value value, ShouldThrowExceptions throw_exceptions)
{
    VERIFY(!value.is_empty());
    auto& vm = this->vm();

    // 1. Assert: Type(O) is Object.

    // 2. Assert: IsPropertyKey(P) is true.
    VERIFY(property_key.is_valid());

    // 3. Assert: Type(Throw) is Boolean.

    // 4. Let success be ? O.[[Set]](P, V, O).
    auto success = TRY(internal_set(property_key, value, this));

    // 5. If success is false and Throw is true, throw a TypeError exception.
    if (!success && throw_exceptions == ShouldThrowExceptions::Yes) {
        // FIXME: Improve/contextualize error message
        return vm.throw_completion<TypeError>(global_object(), ErrorType::ObjectSetReturnedFalse);
    }

    // 6. Return success.
    return success;
}

// 7.3.5 CreateDataProperty ( O, P, V ), https://tc39.es/ecma262/#sec-createdataproperty
ThrowCompletionOr<bool> Object::create_data_property(PropertyKey const& property_key, Value value)
{
    // 1. Assert: Type(O) is Object.

    // 2. Assert: IsPropertyKey(P) is true.
    VERIFY(property_key.is_valid());

    // 3. Let newDesc be the PropertyDescriptor { [[Value]]: V, [[Writable]]: true, [[Enumerable]]: true, [[Configurable]]: true }.
    auto new_descriptor = PropertyDescriptor {
        .value = value,
        .writable = true,
        .enumerable = true,
        .configurable = true,
    };

    // 4. Return ? O.[[DefineOwnProperty]](P, newDesc).
    return internal_define_own_property(property_key, new_descriptor);
}

// 7.3.6 CreateMethodProperty ( O, P, V ), https://tc39.es/ecma262/#sec-createmethodproperty
ThrowCompletionOr<bool> Object::create_method_property(PropertyKey const& property_key, Value value)
{
    VERIFY(!value.is_empty());

    // 1. Assert: Type(O) is Object.

    // 2. Assert: IsPropertyKey(P) is true.
    VERIFY(property_key.is_valid());

    // 3. Let newDesc be the PropertyDescriptor { [[Value]]: V, [[Writable]]: true, [[Enumerable]]: false, [[Configurable]]: true }.
    auto new_descriptor = PropertyDescriptor {
        .value = value,
        .writable = true,
        .enumerable = false,
        .configurable = true,
    };

    // 4. Return ? O.[[DefineOwnProperty]](P, newDesc).
    return internal_define_own_property(property_key, new_descriptor);
}

// 7.3.7 CreateDataPropertyOrThrow ( O, P, V ), https://tc39.es/ecma262/#sec-createdatapropertyorthrow
ThrowCompletionOr<bool> Object::create_data_property_or_throw(PropertyKey const& property_key, Value value)
{
    VERIFY(!value.is_empty());
    auto& vm = this->vm();

    // 1. Assert: Type(O) is Object.

    // 2. Assert: IsPropertyKey(P) is true.
    VERIFY(property_key.is_valid());

    // 3. Let success be ? CreateDataProperty(O, P, V).
    auto success = TRY(create_data_property(property_key, value));

    // 4. If success is false, throw a TypeError exception.
    if (!success) {
        // FIXME: Improve/contextualize error message
        return vm.throw_completion<TypeError>(global_object(), ErrorType::ObjectDefineOwnPropertyReturnedFalse);
    }

    // 5. Return success.
    return success;
}

// 7.3.8 CreateNonEnumerableDataPropertyOrThrow ( O, P, V ), https://tc39.es/ecma262/#sec-createnonenumerabledatapropertyorthrow
ThrowCompletionOr<bool> Object::create_non_enumerable_data_property_or_throw(PropertyKey const& property_key, Value value)
{
    VERIFY(!value.is_empty());
    VERIFY(property_key.is_valid());

    // 1. Let newDesc be the PropertyDescriptor { [[Value]]: V, [[Writable]]: true, [[Enumerable]]: false, [[Configurable]]: true }.
    auto new_description = PropertyDescriptor { .value = value, .writable = true, .enumerable = false, .configurable = true };

    // 2. Return ? DefinePropertyOrThrow(O, P, newDesc).
    return define_property_or_throw(property_key, new_description);
}

// 7.3.9 DefinePropertyOrThrow ( O, P, desc ), https://tc39.es/ecma262/#sec-definepropertyorthrow
ThrowCompletionOr<bool> Object::define_property_or_throw(PropertyKey const& property_key, PropertyDescriptor const& property_descriptor)
{
    auto& vm = this->vm();

    // 1. Assert: Type(O) is Object.

    // 2. Assert: IsPropertyKey(P) is true.
    VERIFY(property_key.is_valid());

    // 3. Let success be ? O.[[DefineOwnProperty]](P, desc).
    auto success = TRY(internal_define_own_property(property_key, property_descriptor));

    // 4. If success is false, throw a TypeError exception.
    if (!success) {
        // FIXME: Improve/contextualize error message
        return vm.throw_completion<TypeError>(global_object(), ErrorType::ObjectDefineOwnPropertyReturnedFalse);
    }

    // 5. Return success.
    return success;
}

// 7.3.10 DeletePropertyOrThrow ( O, P ), https://tc39.es/ecma262/#sec-deletepropertyorthrow
ThrowCompletionOr<bool> Object::delete_property_or_throw(PropertyKey const& property_key)
{
    auto& vm = this->vm();

    // 1. Assert: Type(O) is Object.

    // 2. Assert: IsPropertyKey(P) is true.
    VERIFY(property_key.is_valid());

    // 3. Let success be ? O.[[Delete]](P).
    auto success = TRY(internal_delete(property_key));

    // 4. If success is false, throw a TypeError exception.
    if (!success) {
        // FIXME: Improve/contextualize error message
        return vm.throw_completion<TypeError>(global_object(), ErrorType::ObjectDeleteReturnedFalse);
    }

    // 5. Return success.
    return success;
}

// 7.3.12 HasProperty ( O, P ), https://tc39.es/ecma262/#sec-hasproperty
ThrowCompletionOr<bool> Object::has_property(PropertyKey const& property_key) const
{
    // 1. Assert: Type(O) is Object.

    // 2. Assert: IsPropertyKey(P) is true.
    VERIFY(property_key.is_valid());

    // 3. Return ? O.[[HasProperty]](P).
    return internal_has_property(property_key);
}

// 7.3.13 HasOwnProperty ( O, P ), https://tc39.es/ecma262/#sec-hasownproperty
ThrowCompletionOr<bool> Object::has_own_property(PropertyKey const& property_key) const
{
    // 1. Assert: Type(O) is Object.

    // 2. Assert: IsPropertyKey(P) is true.
    VERIFY(property_key.is_valid());

    // 3. Let desc be ? O.[[GetOwnProperty]](P).
    auto descriptor = TRY(internal_get_own_property(property_key));

    // 4. If desc is undefined, return false.
    if (!descriptor.has_value())
        return false;

    // 5. Return true.
    return true;
}

// 7.3.16 SetIntegrityLevel ( O, level ), https://tc39.es/ecma262/#sec-setintegritylevel
ThrowCompletionOr<bool> Object::set_integrity_level(IntegrityLevel level)
{
    auto& global_object = this->global_object();

    // 1. Assert: Type(O) is Object.

    // 2. Assert: level is either sealed or frozen.
    VERIFY(level == IntegrityLevel::Sealed || level == IntegrityLevel::Frozen);

    // 3. Let status be ? O.[[PreventExtensions]]().
    auto status = TRY(internal_prevent_extensions());

    // 4. If status is false, return false.
    if (!status)
        return false;

    // 5. Let keys be ? O.[[OwnPropertyKeys]]().
    auto keys = TRY(internal_own_property_keys());

    // 6. If level is sealed, then
    if (level == IntegrityLevel::Sealed) {
        // a. For each element k of keys, do
        for (auto& key : keys) {
            auto property_key = MUST(PropertyKey::from_value(global_object, key));

            // i. Perform ? DefinePropertyOrThrow(O, k, PropertyDescriptor { [[Configurable]]: false }).
            TRY(define_property_or_throw(property_key, { .configurable = false }));
        }
    }
    // 7. Else,
    else {
        // a. Assert: level is frozen.

        // b. For each element k of keys, do
        for (auto& key : keys) {
            auto property_key = MUST(PropertyKey::from_value(global_object, key));

            // i. Let currentDesc be ? O.[[GetOwnProperty]](k).
            auto current_descriptor = TRY(internal_get_own_property(property_key));

            // ii. If currentDesc is not undefined, then
            if (!current_descriptor.has_value())
                continue;

            PropertyDescriptor descriptor;

            // 1. If IsAccessorDescriptor(currentDesc) is true, then
            if (current_descriptor->is_accessor_descriptor()) {
                // a. Let desc be the PropertyDescriptor { [[Configurable]]: false }.
                descriptor = { .configurable = false };
            }
            // 2. Else,
            else {
                // a. Let desc be the PropertyDescriptor { [[Configurable]]: false, [[Writable]]: false }.
                descriptor = { .writable = false, .configurable = false };
            }

            // 3. Perform ? DefinePropertyOrThrow(O, k, desc).
            TRY(define_property_or_throw(property_key, descriptor));
        }
    }

    // 8. Return true.
    return true;
}

// 7.3.17 TestIntegrityLevel ( O, level ), https://tc39.es/ecma262/#sec-testintegritylevel
ThrowCompletionOr<bool> Object::test_integrity_level(IntegrityLevel level) const
{
    // 1. Assert: Type(O) is Object.

    // 2. Assert: level is either sealed or frozen.
    VERIFY(level == IntegrityLevel::Sealed || level == IntegrityLevel::Frozen);

    // 3. Let extensible be ? IsExtensible(O).
    auto extensible = TRY(is_extensible());

    // 4. If extensible is true, return false.
    // 5. NOTE: If the object is extensible, none of its properties are examined.
    if (extensible)
        return false;

    // 6. Let keys be ? O.[[OwnPropertyKeys]]().
    auto keys = TRY(internal_own_property_keys());

    // 7. For each element k of keys, do
    for (auto& key : keys) {
        auto property_key = MUST(PropertyKey::from_value(global_object(), key));

        // a. Let currentDesc be ? O.[[GetOwnProperty]](k).
        auto current_descriptor = TRY(internal_get_own_property(property_key));

        // b. If currentDesc is not undefined, then
        if (!current_descriptor.has_value())
            continue;
        // i. If currentDesc.[[Configurable]] is true, return false.
        if (*current_descriptor->configurable)
            return false;

        // ii. If level is frozen and IsDataDescriptor(currentDesc) is true, then
        if (level == IntegrityLevel::Frozen && current_descriptor->is_data_descriptor()) {
            // 1. If currentDesc.[[Writable]] is true, return false.
            if (*current_descriptor->writable)
                return false;
        }
    }

    // 8. Return true.
    return true;
}

// 7.3.24 EnumerableOwnPropertyNames ( O, kind ), https://tc39.es/ecma262/#sec-enumerableownpropertynames
ThrowCompletionOr<MarkedVector<Value>> Object::enumerable_own_property_names(PropertyKind kind) const
{
    // NOTE: This has been flattened for readability, so some `else` branches in the
    //       spec text have been replaced with `continue`s in the loop below.

    auto& global_object = this->global_object();

    // 1. Assert: Type(O) is Object.

    // 2. Let ownKeys be ? O.[[OwnPropertyKeys]]().
    auto own_keys = TRY(internal_own_property_keys());

    // 3. Let properties be a new empty List.
    auto properties = MarkedVector<Value> { heap() };

    // 4. For each element key of ownKeys, do
    for (auto& key : own_keys) {
        // a. If Type(key) is String, then
        if (!key.is_string())
            continue;
        auto property_key = MUST(PropertyKey::from_value(global_object, key));

        // i. Let desc be ? O.[[GetOwnProperty]](key).
        auto descriptor = TRY(internal_get_own_property(property_key));

        // ii. If desc is not undefined and desc.[[Enumerable]] is true, then
        if (descriptor.has_value() && *descriptor->enumerable) {
            // 1. If kind is key, append key to properties.
            if (kind == PropertyKind::Key) {
                properties.append(key);
                continue;
            }
            // 2. Else,

            // a. Let value be ? Get(O, key).
            auto value = TRY(get(property_key));

            // b. If kind is value, append value to properties.
            if (kind == PropertyKind::Value) {
                properties.append(value);
                continue;
            }
            // c. Else,

            // i. Assert: kind is key+value.
            VERIFY(kind == PropertyKind::KeyAndValue);

            // ii. Let entry be ! CreateArrayFromList(« key, value »).
            auto entry = Array::create_from(global_object, { key, value });

            // iii. Append entry to properties.
            properties.append(entry);
        }
    }

    // 5. Return properties.
    return { move(properties) };
}

// 7.3.26 CopyDataProperties ( target, source, excludedItems ), https://tc39.es/ecma262/#sec-copydataproperties
ThrowCompletionOr<Object*> Object::copy_data_properties(Value source, HashTable<PropertyKey> const& seen_names, GlobalObject& global_object)
{
    if (source.is_nullish())
        return this;

    auto* from_object = MUST(source.to_object(global_object));

    for (auto& next_key_value : TRY(from_object->internal_own_property_keys())) {
        auto next_key = MUST(PropertyKey::from_value(global_object, next_key_value));
        if (seen_names.contains(next_key))
            continue;

        auto desc = TRY(from_object->internal_get_own_property(next_key));

        if (desc.has_value() && desc->attributes().is_enumerable()) {
            auto prop_value = TRY(from_object->get(next_key));
            TRY(create_data_property_or_throw(next_key, prop_value));
        }
    }
    return this;
}

// 7.3.27 PrivateElementFind ( O, P ), https://tc39.es/ecma262/#sec-privateelementfind
PrivateElement* Object::private_element_find(PrivateName const& name)
{
    if (!m_private_elements)
        return nullptr;

    auto element = m_private_elements->find_if([&](auto const& element) {
        return element.key == name;
    });

    if (element.is_end())
        return nullptr;

    return &(*element);
}

// 7.3.28 PrivateFieldAdd ( O, P, value ), https://tc39.es/ecma262/#sec-privatefieldadd
ThrowCompletionOr<void> Object::private_field_add(PrivateName const& name, Value value)
{
    if (auto* entry = private_element_find(name); entry)
        return vm().throw_completion<TypeError>(global_object(), ErrorType::PrivateFieldAlreadyDeclared, name.description);
    if (!m_private_elements)
        m_private_elements = make<Vector<PrivateElement>>();
    m_private_elements->empend(name, PrivateElement::Kind::Field, value);
    return {};
}

// 7.3.29 PrivateMethodOrAccessorAdd ( O, method ), https://tc39.es/ecma262/#sec-privatemethodoraccessoradd
ThrowCompletionOr<void> Object::private_method_or_accessor_add(PrivateElement element)
{
    VERIFY(element.kind == PrivateElement::Kind::Method || element.kind == PrivateElement::Kind::Accessor);
    if (auto* entry = private_element_find(element.key); entry)
        return vm().throw_completion<TypeError>(global_object(), ErrorType::PrivateFieldAlreadyDeclared, element.key.description);
    if (!m_private_elements)
        m_private_elements = make<Vector<PrivateElement>>();
    m_private_elements->append(move(element));
    return {};
}

// 7.3.30 PrivateGet ( O, P ), https://tc39.es/ecma262/#sec-privateget
ThrowCompletionOr<Value> Object::private_get(PrivateName const& name)
{
    auto* entry = private_element_find(name);
    if (!entry)
        return vm().throw_completion<TypeError>(global_object(), ErrorType::PrivateFieldDoesNotExistOnObject, name.description);

    auto& value = entry->value;

    if (entry->kind != PrivateElement::Kind::Accessor)
        return value;

    VERIFY(value.is_accessor());
    auto* getter = value.as_accessor().getter();
    if (!getter)
        return vm().throw_completion<TypeError>(global_object(), ErrorType::PrivateFieldGetAccessorWithoutGetter, name.description);

    // 8. Return ? Call(getter, Receiver).
    return TRY(call(global_object(), *getter, this));
}

// 7.3.31 PrivateSet ( O, P, value ), https://tc39.es/ecma262/#sec-privateset
ThrowCompletionOr<void> Object::private_set(PrivateName const& name, Value value)
{
    auto* entry = private_element_find(name);
    if (!entry)
        return vm().throw_completion<TypeError>(global_object(), ErrorType::PrivateFieldDoesNotExistOnObject, name.description);

    if (entry->kind == PrivateElement::Kind::Field) {
        entry->value = value;
        return {};
    } else if (entry->kind == PrivateElement::Kind::Method) {
        return vm().throw_completion<TypeError>(global_object(), ErrorType::PrivateFieldSetMethod, name.description);
    }

    VERIFY(entry->kind == PrivateElement::Kind::Accessor);

    auto& accessor = entry->value;
    VERIFY(accessor.is_accessor());
    auto* setter = accessor.as_accessor().setter();
    if (!setter)
        return vm().throw_completion<TypeError>(global_object(), ErrorType::PrivateFieldSetAccessorWithoutSetter, name.description);

    TRY(call(global_object(), *setter, this, value));
    return {};
}

// 7.3.32 DefineField ( receiver, fieldRecord ), https://tc39.es/ecma262/#sec-definefield
ThrowCompletionOr<void> Object::define_field(Variant<PropertyKey, PrivateName> name, ECMAScriptFunctionObject* initializer)
{
    Value init_value = js_undefined();
    if (initializer)
        init_value = TRY(call(global_object(), *initializer, this));

    if (auto* property_key_ptr = name.get_pointer<PropertyKey>())
        TRY(create_data_property_or_throw(*property_key_ptr, init_value));
    else
        TRY(private_field_add(name.get<PrivateName>(), init_value));

    return {};
}

// 10.1 Ordinary Object Internal Methods and Internal Slots, https://tc39.es/ecma262/#sec-ordinary-object-internal-methods-and-internal-slots

// 10.1.1 [[GetPrototypeOf]] ( ), https://tc39.es/ecma262/#sec-ordinary-object-internal-methods-and-internal-slots-getprototypeof
ThrowCompletionOr<Object*> Object::internal_get_prototype_of() const
{
    // 1. Return O.[[Prototype]].
    return const_cast<Object*>(prototype());
}

// 10.1.2 [[SetPrototypeOf]] ( V ), https://tc39.es/ecma262/#sec-ordinary-object-internal-methods-and-internal-slots-setprototypeof-v
ThrowCompletionOr<bool> Object::internal_set_prototype_of(Object* new_prototype)
{
    // 1. Assert: Either Type(V) is Object or Type(V) is Null.

    // 2. Let current be O.[[Prototype]].
    // 3. If SameValue(V, current) is true, return true.
    if (prototype() == new_prototype)
        return true;

    // 4. Let extensible be O.[[Extensible]].
    // 5. If extensible is false, return false.
    if (!m_is_extensible)
        return false;

    // 6. Let p be V.
    auto* prototype = new_prototype;

    // 7. Let done be false.
    // 8. Repeat, while done is false,
    while (prototype) {
        // a. If p is null, set done to true.

        // b. Else if SameValue(p, O) is true, return false.
        if (prototype == this)
            return false;
        // c. Else,

        // i. If p.[[GetPrototypeOf]] is not the ordinary object internal method defined in 10.1.1, set done to true.
        // NOTE: This is a best-effort implementation; we don't have a good way of detecting whether certain virtual
        // Object methods have been overridden by a given object, but as ProxyObject is the only one doing that for
        // [[SetPrototypeOf]], this check does the trick.
        if (is<ProxyObject>(prototype))
            break;

        // ii. Else, set p to p.[[Prototype]].
        prototype = prototype->prototype();
    }

    // 9. Set O.[[Prototype]] to V.
    set_prototype(new_prototype);

    // 10. Return true.
    return true;
}

// 10.1.3 [[IsExtensible]] ( ), https://tc39.es/ecma262/#sec-ordinary-object-internal-methods-and-internal-slots-isextensible
ThrowCompletionOr<bool> Object::internal_is_extensible() const
{
    // 1. Return O.[[Extensible]].
    return m_is_extensible;
}

// 10.1.4 [[PreventExtensions]] ( ), https://tc39.es/ecma262/#sec-ordinary-object-internal-methods-and-internal-slots-preventextensions
ThrowCompletionOr<bool> Object::internal_prevent_extensions()
{
    // 1. Set O.[[Extensible]] to false.
    m_is_extensible = false;

    // 2. Return true.
    return true;
}

// 10.1.5 [[GetOwnProperty]] ( P ), https://tc39.es/ecma262/#sec-ordinary-object-internal-methods-and-internal-slots-getownproperty-p
ThrowCompletionOr<Optional<PropertyDescriptor>> Object::internal_get_own_property(PropertyKey const& property_key) const
{
    // 1. Assert: IsPropertyKey(P) is true.
    VERIFY(property_key.is_valid());

    // 2. If O does not have an own property with key P, return undefined.
    auto maybe_storage_entry = storage_get(property_key);
    if (!maybe_storage_entry.has_value())
        return Optional<PropertyDescriptor> {};

    // 3. Let D be a newly created Property Descriptor with no fields.
    PropertyDescriptor descriptor;

    // 4. Let X be O's own property whose key is P.
    auto [value, attributes] = *maybe_storage_entry;

    // 5. If X is a data property, then
    if (!value.is_accessor()) {
        // a. Set D.[[Value]] to the value of X's [[Value]] attribute.
        descriptor.value = value.value_or(js_undefined());

        // b. Set D.[[Writable]] to the value of X's [[Writable]] attribute.
        descriptor.writable = attributes.is_writable();
    }
    // 6. Else,
    else {
        // a. Assert: X is an accessor property.

        // b. Set D.[[Get]] to the value of X's [[Get]] attribute.
        descriptor.get = value.as_accessor().getter();

        // c. Set D.[[Set]] to the value of X's [[Set]] attribute.
        descriptor.set = value.as_accessor().setter();
    }

    // 7. Set D.[[Enumerable]] to the value of X's [[Enumerable]] attribute.
    descriptor.enumerable = attributes.is_enumerable();

    // 8. Set D.[[Configurable]] to the value of X's [[Configurable]] attribute.
    descriptor.configurable = attributes.is_configurable();

    // 9. Return D.
    return descriptor;
}

// 10.1.6 [[DefineOwnProperty]] ( P, Desc ), https://tc39.es/ecma262/#sec-ordinary-object-internal-methods-and-internal-slots-defineownproperty-p-desc
ThrowCompletionOr<bool> Object::internal_define_own_property(PropertyKey const& property_key, PropertyDescriptor const& property_descriptor)
{
    VERIFY(property_key.is_valid());
    // 1. Let current be ? O.[[GetOwnProperty]](P).
    auto current = TRY(internal_get_own_property(property_key));

    // 2. Let extensible be ? IsExtensible(O).
    auto extensible = TRY(is_extensible());

    // 3. Return ValidateAndApplyPropertyDescriptor(O, P, extensible, Desc, current).
    return validate_and_apply_property_descriptor(this, property_key, extensible, property_descriptor, current);
}

// 10.1.7 [[HasProperty]] ( P ), https://tc39.es/ecma262/#sec-ordinary-object-internal-methods-and-internal-slots-hasproperty-p
ThrowCompletionOr<bool> Object::internal_has_property(PropertyKey const& property_key) const
{
    // 1. Assert: IsPropertyKey(P) is true.
    VERIFY(property_key.is_valid());

    // 2. Let hasOwn be ? O.[[GetOwnProperty]](P).
    auto has_own = TRY(internal_get_own_property(property_key));

    // 3. If hasOwn is not undefined, return true.
    if (has_own.has_value())
        return true;

    // 4. Let parent be ? O.[[GetPrototypeOf]]().
    auto* parent = TRY(internal_get_prototype_of());

    // 5. If parent is not null, then
    if (parent) {
        // a. Return ? parent.[[HasProperty]](P).
        return parent->internal_has_property(property_key);
    }

    // 6. Return false.
    return false;
}

// 10.1.8 [[Get]] ( P, Receiver ), https://tc39.es/ecma262/#sec-ordinary-object-internal-methods-and-internal-slots-get-p-receiver
ThrowCompletionOr<Value> Object::internal_get(PropertyKey const& property_key, Value receiver) const
{
    VERIFY(!receiver.is_empty());

    // 1. Assert: IsPropertyKey(P) is true.
    VERIFY(property_key.is_valid());

    // 2. Let desc be ? O.[[GetOwnProperty]](P).
    auto descriptor = TRY(internal_get_own_property(property_key));

    // 3. If desc is undefined, then
    if (!descriptor.has_value()) {
        // a. Let parent be ? O.[[GetPrototypeOf]]().
        auto* parent = TRY(internal_get_prototype_of());

        // b. If parent is null, return undefined.
        if (!parent)
            return js_undefined();

        // c. Return ? parent.[[Get]](P, Receiver).
        return parent->internal_get(property_key, receiver);
    }

    // 4. If IsDataDescriptor(desc) is true, return desc.[[Value]].
    if (descriptor->is_data_descriptor())
        return *descriptor->value;

    // 5. Assert: IsAccessorDescriptor(desc) is true.
    VERIFY(descriptor->is_accessor_descriptor());

    // 6. Let getter be desc.[[Get]].
    auto* getter = *descriptor->get;

    // 7. If getter is undefined, return undefined.
    if (!getter)
        return js_undefined();

    // 8. Return ? Call(getter, Receiver).
    return TRY(call(global_object(), *getter, receiver));
}

// 10.1.9 [[Set]] ( P, V, Receiver ), https://tc39.es/ecma262/#sec-ordinary-object-internal-methods-and-internal-slots-set-p-v-receiver
ThrowCompletionOr<bool> Object::internal_set(PropertyKey const& property_key, Value value, Value receiver)
{
    VERIFY(!value.is_empty());
    VERIFY(!receiver.is_empty());

    // 1. Assert: IsPropertyKey(P) is true.
    VERIFY(property_key.is_valid());

    // 2. Let ownDesc be ? O.[[GetOwnProperty]](P).
    auto own_descriptor = TRY(internal_get_own_property(property_key));

    // 3. Return OrdinarySetWithOwnDescriptor(O, P, V, Receiver, ownDesc).
    return ordinary_set_with_own_descriptor(property_key, value, receiver, own_descriptor);
}

// 10.1.9.2 OrdinarySetWithOwnDescriptor ( O, P, V, Receiver, ownDesc ), https://tc39.es/ecma262/#sec-ordinarysetwithowndescriptor
ThrowCompletionOr<bool> Object::ordinary_set_with_own_descriptor(PropertyKey const& property_key, Value value, Value receiver, Optional<PropertyDescriptor> own_descriptor)
{
    // 1. Assert: IsPropertyKey(P) is true.
    VERIFY(property_key.is_valid());

    // 2. If ownDesc is undefined, then
    if (!own_descriptor.has_value()) {
        // a. Let parent be ? O.[[GetPrototypeOf]]().
        auto* parent = TRY(internal_get_prototype_of());

        // b. If parent is not null, then
        if (parent) {
            // i. Return ? parent.[[Set]](P, V, Receiver).
            return TRY(parent->internal_set(property_key, value, receiver));
        }
        // c. Else,
        else {
            // i. Set ownDesc to the PropertyDescriptor { [[Value]]: undefined, [[Writable]]: true, [[Enumerable]]: true, [[Configurable]]: true }.
            own_descriptor = PropertyDescriptor {
                .value = js_undefined(),
                .writable = true,
                .enumerable = true,
                .configurable = true,
            };
        }
    }

    // 3. If IsDataDescriptor(ownDesc) is true, then
    if (own_descriptor->is_data_descriptor()) {
        // a. If ownDesc.[[Writable]] is false, return false.
        if (!*own_descriptor->writable)
            return false;

        // b. If Type(Receiver) is not Object, return false.
        if (!receiver.is_object())
            return false;

        // c. Let existingDescriptor be ? Receiver.[[GetOwnProperty]](P).
        auto existing_descriptor = TRY(receiver.as_object().internal_get_own_property(property_key));

        // d. If existingDescriptor is not undefined, then
        if (existing_descriptor.has_value()) {
            // i. If IsAccessorDescriptor(existingDescriptor) is true, return false.
            if (existing_descriptor->is_accessor_descriptor())
                return false;

            // ii. If existingDescriptor.[[Writable]] is false, return false.
            if (!*existing_descriptor->writable)
                return false;

            // iii. Let valueDesc be the PropertyDescriptor { [[Value]]: V }.
            auto value_descriptor = PropertyDescriptor { .value = value };

            // iv. Return ? Receiver.[[DefineOwnProperty]](P, valueDesc).
            return TRY(receiver.as_object().internal_define_own_property(property_key, value_descriptor));
        }
        // e. Else,
        else {
            // i. Assert: Receiver does not currently have a property P.
            VERIFY(!receiver.as_object().storage_has(property_key));

            // ii. Return ? CreateDataProperty(Receiver, P, V).
            return TRY(receiver.as_object().create_data_property(property_key, value));
        }
    }

    // 4. Assert: IsAccessorDescriptor(ownDesc) is true.
    VERIFY(own_descriptor->is_accessor_descriptor());

    // 5. Let setter be ownDesc.[[Set]].
    auto* setter = *own_descriptor->set;

    // 6. If setter is undefined, return false.
    if (!setter)
        return false;

    // 7. Perform ? Call(setter, Receiver, « V »).
    (void)TRY(call(global_object(), *setter, receiver, value));

    // 8. Return true.
    return true;
}

// 10.1.10 [[Delete]] ( P ), https://tc39.es/ecma262/#sec-ordinary-object-internal-methods-and-internal-slots-delete-p
ThrowCompletionOr<bool> Object::internal_delete(PropertyKey const& property_key)
{
    // 1. Assert: IsPropertyKey(P) is true.
    VERIFY(property_key.is_valid());

    // 2. Let desc be ? O.[[GetOwnProperty]](P).
    auto descriptor = TRY(internal_get_own_property(property_key));

    // 3. If desc is undefined, return true.
    if (!descriptor.has_value())
        return true;

    // 4. If desc.[[Configurable]] is true, then
    if (*descriptor->configurable) {
        // a. Remove the own property with name P from O.
        storage_delete(property_key);

        // b. Return true.
        return true;
    }

    // 5. Return false.
    return false;
}

// 10.1.11 [[OwnPropertyKeys]] ( ), https://tc39.es/ecma262/#sec-ordinary-object-internal-methods-and-internal-slots-ownpropertykeys
ThrowCompletionOr<MarkedVector<Value>> Object::internal_own_property_keys() const
{
    auto& vm = this->vm();

    // 1. Let keys be a new empty List.
    MarkedVector<Value> keys { heap() };

    // 2. For each own property key P of O such that P is an array index, in ascending numeric index order, do
    for (auto& entry : m_indexed_properties) {
        // a. Add P as the last element of keys.
        keys.append(js_string(vm, String::number(entry.index())));
    }

    // 3. For each own property key P of O such that Type(P) is String and P is not an array index, in ascending chronological order of property creation, do
    for (auto& it : shape().property_table_ordered()) {
        if (it.key.is_string()) {
            // a. Add P as the last element of keys.
            keys.append(it.key.to_value(vm));
        }
    }

    // 4. For each own property key P of O such that Type(P) is Symbol, in ascending chronological order of property creation, do
    for (auto& it : shape().property_table_ordered()) {
        if (it.key.is_symbol()) {
            // a. Add P as the last element of keys.
            keys.append(it.key.to_value(vm));
        }
    }

    // 5. Return keys.
    return { move(keys) };
}

// 10.4.7.2 SetImmutablePrototype ( O, V ), https://tc39.es/ecma262/#sec-set-immutable-prototype
ThrowCompletionOr<bool> Object::set_immutable_prototype(Object* prototype)
{
    // 1. Assert: Either Type(V) is Object or Type(V) is Null.

    // 2. Let current be ? O.[[GetPrototypeOf]]().
    auto* current = TRY(internal_get_prototype_of());

    // 3. If SameValue(V, current) is true, return true.
    if (prototype == current)
        return true;

    // 4. Return false.
    return false;
}

Optional<ValueAndAttributes> Object::storage_get(PropertyKey const& property_key) const
{
    VERIFY(property_key.is_valid());

    Value value;
    PropertyAttributes attributes;

    if (property_key.is_number()) {
        auto value_and_attributes = m_indexed_properties.get(property_key.as_number());
        if (!value_and_attributes.has_value())
            return {};
        value = value_and_attributes->value;
        attributes = value_and_attributes->attributes;
    } else {
        auto metadata = shape().lookup(property_key.to_string_or_symbol());
        if (!metadata.has_value())
            return {};
        value = m_storage[metadata->offset];
        attributes = metadata->attributes;
    }
    return ValueAndAttributes { .value = value, .attributes = attributes };
}

bool Object::storage_has(PropertyKey const& property_key) const
{
    VERIFY(property_key.is_valid());
    if (property_key.is_number())
        return m_indexed_properties.has_index(property_key.as_number());
    return shape().lookup(property_key.to_string_or_symbol()).has_value();
}

void Object::storage_set(PropertyKey const& property_key, ValueAndAttributes const& value_and_attributes)
{
    VERIFY(property_key.is_valid());

    auto [value, attributes] = value_and_attributes;

    if (property_key.is_number()) {
        auto index = property_key.as_number();
        m_indexed_properties.put(index, value, attributes);
        return;
    }

    auto property_key_string_or_symbol = property_key.to_string_or_symbol();
    auto metadata = shape().lookup(property_key_string_or_symbol);

    if (!metadata.has_value()) {
        if (!m_shape->is_unique() && shape().property_count() > 100) {
            // If you add more than 100 properties to an object, let's stop doing
            // transitions to avoid filling up the heap with shapes.
            ensure_shape_is_unique();
        }

        if (m_shape->is_unique())
            m_shape->add_property_to_unique_shape(property_key_string_or_symbol, attributes);
        else
            set_shape(*m_shape->create_put_transition(property_key_string_or_symbol, attributes));

        m_storage.append(value);
        return;
    }

    if (attributes != metadata->attributes) {
        if (m_shape->is_unique())
            m_shape->reconfigure_property_in_unique_shape(property_key_string_or_symbol, attributes);
        else
            set_shape(*m_shape->create_configure_transition(property_key_string_or_symbol, attributes));
    }

    m_storage[metadata->offset] = value;
}

void Object::storage_delete(PropertyKey const& property_key)
{
    VERIFY(property_key.is_valid());
    VERIFY(storage_has(property_key));

    if (property_key.is_number())
        return m_indexed_properties.remove(property_key.as_number());

    auto metadata = shape().lookup(property_key.to_string_or_symbol());
    VERIFY(metadata.has_value());

    ensure_shape_is_unique();

    shape().remove_property_from_unique_shape(property_key.to_string_or_symbol(), metadata->offset);
    m_storage.remove(metadata->offset);
}

void Object::set_prototype(Object* new_prototype)
{
    if (prototype() == new_prototype)
        return;
    auto& shape = this->shape();
    if (shape.is_unique())
        shape.set_prototype_without_transition(new_prototype);
    else
        m_shape = shape.create_prototype_transition(new_prototype);
}

void Object::define_native_accessor(PropertyKey const& property_key, Function<ThrowCompletionOr<Value>(VM&, GlobalObject&)> getter, Function<ThrowCompletionOr<Value>(VM&, GlobalObject&)> setter, PropertyAttributes attribute)
{
    auto& vm = this->vm();
    String formatted_property_key;
    if (property_key.is_number()) {
        formatted_property_key = property_key.to_string();
    } else if (property_key.is_string()) {
        formatted_property_key = property_key.as_string();
    } else {
        formatted_property_key = String::formatted("[{}]", property_key.as_symbol()->description());
    }
    FunctionObject* getter_function = nullptr;
    if (getter) {
        auto name = String::formatted("get {}", formatted_property_key);
        getter_function = NativeFunction::create(global_object(), name, move(getter));
        getter_function->define_direct_property(vm.names.length, Value(0), Attribute::Configurable);
        getter_function->define_direct_property(vm.names.name, js_string(vm, name), Attribute::Configurable);
    }
    FunctionObject* setter_function = nullptr;
    if (setter) {
        auto name = String::formatted("set {}", formatted_property_key);
        setter_function = NativeFunction::create(global_object(), name, move(setter));
        setter_function->define_direct_property(vm.names.length, Value(1), Attribute::Configurable);
        setter_function->define_direct_property(vm.names.name, js_string(vm, name), Attribute::Configurable);
    }
    return define_direct_accessor(property_key, getter_function, setter_function, attribute);
}

void Object::define_direct_accessor(PropertyKey const& property_key, FunctionObject* getter, FunctionObject* setter, PropertyAttributes attributes)
{
    VERIFY(property_key.is_valid());

    auto existing_property = storage_get(property_key).value_or({}).value;
    auto* accessor = existing_property.is_accessor() ? &existing_property.as_accessor() : nullptr;
    if (!accessor) {
        accessor = Accessor::create(vm(), getter, setter);
        define_direct_property(property_key, accessor, attributes);
    } else {
        if (getter)
            accessor->set_getter(getter);
        if (setter)
            accessor->set_setter(setter);
    }
}

void Object::ensure_shape_is_unique()
{
    if (shape().is_unique())
        return;

    m_shape = m_shape->create_unique_clone();
}

// Simple side-effect free property lookup, following the prototype chain. Non-standard.
Value Object::get_without_side_effects(const PropertyKey& property_key) const
{
    auto* object = this;
    while (object) {
        auto value_and_attributes = object->storage_get(property_key);
        if (value_and_attributes.has_value())
            return value_and_attributes->value;
        object = object->prototype();
    }
    return {};
}

void Object::define_native_function(PropertyKey const& property_key, Function<ThrowCompletionOr<Value>(VM&, GlobalObject&)> native_function, i32 length, PropertyAttributes attribute)
{
    auto& vm = this->vm();
    String function_name;
    if (property_key.is_string()) {
        function_name = property_key.as_string();
    } else {
        function_name = String::formatted("[{}]", property_key.as_symbol()->description());
    }
    auto* function = NativeFunction::create(global_object(), function_name, move(native_function));
    function->define_direct_property(vm.names.length, Value(length), Attribute::Configurable);
    function->define_direct_property(vm.names.name, js_string(vm, function_name), Attribute::Configurable);
    define_direct_property(property_key, function, attribute);
}

// 20.1.2.3.1 ObjectDefineProperties ( O, Properties ), https://tc39.es/ecma262/#sec-objectdefineproperties
ThrowCompletionOr<Object*> Object::define_properties(Value properties)
{
    auto& global_object = this->global_object();

    // 1. Assert: Type(O) is Object.

    // 2. Let props be ? ToObject(Properties).
    auto* props = TRY(properties.to_object(global_object));

    // 3. Let keys be ? props.[[OwnPropertyKeys]]().
    auto keys = TRY(props->internal_own_property_keys());

    struct NameAndDescriptor {
        PropertyKey name;
        PropertyDescriptor descriptor;
    };

    // 4. Let descriptors be a new empty List.
    Vector<NameAndDescriptor> descriptors;

    // 5. For each element nextKey of keys, do
    for (auto& next_key : keys) {
        auto property_key = MUST(PropertyKey::from_value(global_object, next_key));

        // a. Let propDesc be ? props.[[GetOwnProperty]](nextKey).
        auto property_descriptor = TRY(props->internal_get_own_property(property_key));

        // b. If propDesc is not undefined and propDesc.[[Enumerable]] is true, then
        if (property_descriptor.has_value() && *property_descriptor->enumerable) {
            // i. Let descObj be ? Get(props, nextKey).
            auto descriptor_object = TRY(props->get(property_key));

            // ii. Let desc be ? ToPropertyDescriptor(descObj).
            auto descriptor = TRY(to_property_descriptor(global_object, descriptor_object));

            // iii. Append the pair (a two element List) consisting of nextKey and desc to the end of descriptors.
            descriptors.append({ property_key, descriptor });
        }
    }

    // 6. For each element pair of descriptors, do
    for (auto& [name, descriptor] : descriptors) {
        // a. Let P be the first element of pair.
        // b. Let desc be the second element of pair.

        // c. Perform ? DefinePropertyOrThrow(O, P, desc).
        TRY(define_property_or_throw(name, descriptor));
    }

    // 7. Return O.
    return this;
}

void Object::visit_edges(Cell::Visitor& visitor)
{
    Cell::visit_edges(visitor);
    visitor.visit(m_shape);

    for (auto& value : m_storage)
        visitor.visit(value);

    m_indexed_properties.for_each_value([&visitor](auto& value) {
        visitor.visit(value);
    });

    if (m_private_elements) {
        for (auto& private_element : *m_private_elements)
            visitor.visit(private_element.value);
    }
}

// 7.1.1.1 OrdinaryToPrimitive ( O, hint ), https://tc39.es/ecma262/#sec-ordinarytoprimitive
ThrowCompletionOr<Value> Object::ordinary_to_primitive(Value::PreferredType preferred_type) const
{
    VERIFY(preferred_type == Value::PreferredType::String || preferred_type == Value::PreferredType::Number);

    auto& vm = this->vm();

    AK::Array<PropertyKey, 2> method_names;

    // 1. If hint is string, then
    if (preferred_type == Value::PreferredType::String) {
        // a. Let methodNames be « "toString", "valueOf" ».
        method_names = { vm.names.toString, vm.names.valueOf };
    } else {
        // a. Let methodNames be « "valueOf", "toString" ».
        method_names = { vm.names.valueOf, vm.names.toString };
    }

    // 3. For each element name of methodNames, do
    for (auto& method_name : method_names) {
        // a. Let method be ? Get(O, name).
        auto method = TRY(get(method_name));

        // b. If IsCallable(method) is true, then
        if (method.is_function()) {
            // i. Let result be ? Call(method, O).
            auto result = TRY(call(global_object(), method.as_function(), const_cast<Object*>(this)));

            // ii. If Type(result) is not Object, return result.
            if (!result.is_object())
                return result;
        }
    }

    // 4. Throw a TypeError exception.
    return vm.throw_completion<TypeError>(global_object(), ErrorType::Convert, "object", preferred_type == Value::PreferredType::String ? "string" : "number");
}

}
