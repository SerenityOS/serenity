/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <AK/String.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Accessor.h>
#include <LibJS/Runtime/BooleanObject.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/Date.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/NumberObject.h>
#include <LibJS/Runtime/ObjectPrototype.h>
#include <LibJS/Runtime/RegExpObject.h>
#include <LibJS/Runtime/StringObject.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

ObjectPrototype::ObjectPrototype(GlobalObject& global_object)
    : Object(Object::ConstructWithoutPrototypeTag::Tag, global_object)
{
}

void ObjectPrototype::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    Object::initialize(global_object);
    // This must be called after the constructor has returned, so that the below code
    // can find the ObjectPrototype through normal paths.
    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.hasOwnProperty, has_own_property, 1, attr);
    define_native_function(vm.names.toString, to_string, 0, attr);
    define_native_function(vm.names.toLocaleString, to_locale_string, 0, attr);
    define_native_function(vm.names.valueOf, value_of, 0, attr);
    define_native_function(vm.names.propertyIsEnumerable, property_is_enumerable, 1, attr);
    define_native_function(vm.names.isPrototypeOf, is_prototype_of, 1, attr);

    // Annex B
    define_native_function(vm.names.__defineGetter__, define_getter, 2, attr);
    define_native_function(vm.names.__defineSetter__, define_setter, 2, attr);
    define_native_function(vm.names.__lookupGetter__, lookup_getter, 1, attr);
    define_native_function(vm.names.__lookupSetter__, lookup_setter, 1, attr);
    define_native_accessor(vm.names.__proto__, proto_getter, proto_setter, Attribute::Configurable);
}

ObjectPrototype::~ObjectPrototype()
{
}

// 10.4.7.1 [[SetPrototypeOf]] ( V ), https://tc39.es/ecma262/#sec-immutable-prototype-exotic-objects-setprototypeof-v
ThrowCompletionOr<bool> ObjectPrototype::internal_set_prototype_of(Object* prototype)
{
    // 1. Return ? SetImmutablePrototype(O, V).
    return set_immutable_prototype(prototype);
}

// 20.1.3.2 Object.prototype.hasOwnProperty ( V ), https://tc39.es/ecma262/#sec-object.prototype.hasownproperty
JS_DEFINE_NATIVE_FUNCTION(ObjectPrototype::has_own_property)
{
    auto property_key = TRY(vm.argument(0).to_property_key(global_object));
    auto* this_object = TRY(vm.this_value(global_object).to_object(global_object));
    return Value(TRY(this_object->has_own_property(property_key)));
}

// 20.1.3.6 Object.prototype.toString ( ), https://tc39.es/ecma262/#sec-object.prototype.tostring
JS_DEFINE_NATIVE_FUNCTION(ObjectPrototype::to_string)
{
    auto this_value = vm.this_value(global_object);

    // 1. If the this value is undefined, return "[object Undefined]".
    if (this_value.is_undefined())
        return js_string(vm, "[object Undefined]");

    // 2. If the this value is null, return "[object Null]".
    if (this_value.is_null())
        return js_string(vm, "[object Null]");

    // 3. Let O be ! ToObject(this value).
    auto* object = MUST(this_value.to_object(global_object));

    // 4. Let isArray be ? IsArray(O).
    auto is_array = TRY(Value(object).is_array(global_object));

    String builtin_tag;

    // 5. If isArray is true, let builtinTag be "Array".
    if (is_array)
        builtin_tag = "Array";
    // 6. Else if O has a [[ParameterMap]] internal slot, let builtinTag be "Arguments".
    else if (object->has_parameter_map())
        builtin_tag = "Arguments";
    // 7. Else if O has a [[Call]] internal method, let builtinTag be "Function".
    else if (object->is_function())
        builtin_tag = "Function";
    // 8. Else if O has an [[ErrorData]] internal slot, let builtinTag be "Error".
    else if (is<Error>(object))
        builtin_tag = "Error";
    // 9. Else if O has a [[BooleanData]] internal slot, let builtinTag be "Boolean".
    else if (is<BooleanObject>(object))
        builtin_tag = "Boolean";
    // 10. Else if O has a [[NumberData]] internal slot, let builtinTag be "Number".
    else if (is<NumberObject>(object))
        builtin_tag = "Number";
    // 11. Else if O has a [[StringData]] internal slot, let builtinTag be "String".
    else if (is<StringObject>(object))
        builtin_tag = "String";
    // 12. Else if O has a [[DateValue]] internal slot, let builtinTag be "Date".
    else if (is<Date>(object))
        builtin_tag = "Date";
    // 13. Else if O has a [[RegExpMatcher]] internal slot, let builtinTag be "RegExp".
    else if (is<RegExpObject>(object))
        builtin_tag = "RegExp";
    // 14. Else, let builtinTag be "Object".
    else
        builtin_tag = "Object";

    // 15. Let tag be ? Get(O, @@toStringTag).
    auto to_string_tag = TRY(object->get(*vm.well_known_symbol_to_string_tag()));

    // Optimization: Instead of creating another PrimitiveString from builtin_tag, we separate tag and to_string_tag and add an additional branch to step 16.
    String tag;

    // 16. If Type(tag) is not String, set tag to builtinTag.
    if (!to_string_tag.is_string())
        tag = move(builtin_tag);
    else
        tag = to_string_tag.as_string().string();

    // 17. Return the string-concatenation of "[object ", tag, and "]".
    return js_string(vm, String::formatted("[object {}]", tag));
}

// 20.1.3.5 Object.prototype.toLocaleString ( [ reserved1 [ , reserved2 ] ] ), https://tc39.es/ecma262/#sec-object.prototype.tolocalestring
JS_DEFINE_NATIVE_FUNCTION(ObjectPrototype::to_locale_string)
{
    auto this_value = vm.this_value(global_object);
    return this_value.invoke(global_object, vm.names.toString);
}

// 20.1.3.7 Object.prototype.valueOf ( ), https://tc39.es/ecma262/#sec-object.prototype.valueof
JS_DEFINE_NATIVE_FUNCTION(ObjectPrototype::value_of)
{
    return TRY(vm.this_value(global_object).to_object(global_object));
}

// 20.1.3.4 Object.prototype.propertyIsEnumerable ( V ), https://tc39.es/ecma262/#sec-object.prototype.propertyisenumerable
JS_DEFINE_NATIVE_FUNCTION(ObjectPrototype::property_is_enumerable)
{
    // 1. Let P be ? ToPropertyKey(V).
    auto property_key = TRY(vm.argument(0).to_property_key(global_object));
    // 2. Let O be ? ToObject(this value).
    auto* this_object = TRY(vm.this_value(global_object).to_object(global_object));
    // 3. Let desc be ? O.[[GetOwnProperty]](P).
    auto property_descriptor = TRY(this_object->internal_get_own_property(property_key));
    // 4. If desc is undefined, return false.
    if (!property_descriptor.has_value())
        return Value(false);
    // 5. Return desc.[[Enumerable]].
    return Value(*property_descriptor->enumerable);
}

// 20.1.3.3 Object.prototype.isPrototypeOf ( V ), https://tc39.es/ecma262/#sec-object.prototype.isprototypeof
JS_DEFINE_NATIVE_FUNCTION(ObjectPrototype::is_prototype_of)
{
    auto object_argument = vm.argument(0);
    if (!object_argument.is_object())
        return Value(false);
    auto* object = &object_argument.as_object();
    auto* this_object = TRY(vm.this_value(global_object).to_object(global_object));

    for (;;) {
        object = TRY(object->internal_get_prototype_of());
        if (!object)
            return Value(false);
        if (same_value(this_object, object))
            return Value(true);
    }
}

// B.2.2.2 Object.prototype.__defineGetter__ ( P, getter ), https://tc39.es/ecma262/#sec-object.prototype.__defineGetter__
JS_DEFINE_NATIVE_FUNCTION(ObjectPrototype::define_getter)
{
    auto* object = TRY(vm.this_value(global_object).to_object(global_object));

    auto getter = vm.argument(1);
    if (!getter.is_function())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAFunction, getter.to_string_without_side_effects());

    auto descriptor = PropertyDescriptor { .get = &getter.as_function(), .enumerable = true, .configurable = true };

    auto key = TRY(vm.argument(0).to_property_key(global_object));

    TRY(object->define_property_or_throw(key, descriptor));

    return js_undefined();
}

// B.2.2.3 Object.prototype.__defineSetter__ ( P, getter ), https://tc39.es/ecma262/#sec-object.prototype.__defineSetter__
JS_DEFINE_NATIVE_FUNCTION(ObjectPrototype::define_setter)
{
    auto* object = TRY(vm.this_value(global_object).to_object(global_object));

    auto setter = vm.argument(1);
    if (!setter.is_function())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAFunction, setter.to_string_without_side_effects());

    auto descriptor = PropertyDescriptor { .set = &setter.as_function(), .enumerable = true, .configurable = true };

    auto key = TRY(vm.argument(0).to_property_key(global_object));

    TRY(object->define_property_or_throw(key, descriptor));

    return js_undefined();
}

// B.2.2.4 Object.prototype.__lookupGetter__ ( P ), https://tc39.es/ecma262/#sec-object.prototype.__lookupGetter__
JS_DEFINE_NATIVE_FUNCTION(ObjectPrototype::lookup_getter)
{
    auto* object = TRY(vm.this_value(global_object).to_object(global_object));

    auto key = TRY(vm.argument(0).to_property_key(global_object));

    while (object) {
        auto desc = TRY(object->internal_get_own_property(key));
        if (desc.has_value()) {
            if (desc->is_accessor_descriptor())
                return *desc->get ?: js_undefined();
            return js_undefined();
        }
        object = TRY(object->internal_get_prototype_of());
    }

    return js_undefined();
}

// B.2.2.5 Object.prototype.__lookupSetter__ ( P ), https://tc39.es/ecma262/#sec-object.prototype.__lookupSetter__
JS_DEFINE_NATIVE_FUNCTION(ObjectPrototype::lookup_setter)
{
    auto* object = TRY(vm.this_value(global_object).to_object(global_object));

    auto key = TRY(vm.argument(0).to_property_key(global_object));

    while (object) {
        auto desc = TRY(object->internal_get_own_property(key));
        if (desc.has_value()) {
            if (desc->is_accessor_descriptor())
                return *desc->set ?: js_undefined();
            return js_undefined();
        }
        object = TRY(object->internal_get_prototype_of());
    }

    return js_undefined();
}

// B.2.2.1.1 get Object.prototype.__proto__, https://tc39.es/ecma262/#sec-get-object.prototype.__proto__
JS_DEFINE_NATIVE_FUNCTION(ObjectPrototype::proto_getter)
{
    auto* object = TRY(vm.this_value(global_object).to_object(global_object));
    return TRY(object->internal_get_prototype_of());
}

// B.2.2.1.2 set Object.prototype.__proto__, https://tc39.es/ecma262/#sec-set-object.prototype.__proto__
JS_DEFINE_NATIVE_FUNCTION(ObjectPrototype::proto_setter)
{
    auto object = TRY(require_object_coercible(global_object, vm.this_value(global_object)));

    auto proto = vm.argument(0);
    if (!proto.is_object() && !proto.is_null())
        return js_undefined();

    if (!object.is_object())
        return js_undefined();

    auto status = TRY(object.as_object().internal_set_prototype_of(proto.is_object() ? &proto.as_object() : nullptr));
    if (!status) {
        // FIXME: Improve/contextualize error message
        return vm.throw_completion<TypeError>(global_object, ErrorType::ObjectSetPrototypeOfReturnedFalse);
    }
    return js_undefined();
}

}
