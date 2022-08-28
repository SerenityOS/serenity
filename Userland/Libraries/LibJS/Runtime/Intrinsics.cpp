/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AggregateErrorConstructor.h>
#include <LibJS/Runtime/AggregateErrorPrototype.h>
#include <LibJS/Runtime/ArrayBufferConstructor.h>
#include <LibJS/Runtime/ArrayBufferPrototype.h>
#include <LibJS/Runtime/ArrayConstructor.h>
#include <LibJS/Runtime/ArrayIteratorPrototype.h>
#include <LibJS/Runtime/ArrayPrototype.h>
#include <LibJS/Runtime/AsyncFromSyncIteratorPrototype.h>
#include <LibJS/Runtime/AsyncFunctionConstructor.h>
#include <LibJS/Runtime/AsyncFunctionPrototype.h>
#include <LibJS/Runtime/AsyncGeneratorFunctionConstructor.h>
#include <LibJS/Runtime/AsyncGeneratorFunctionPrototype.h>
#include <LibJS/Runtime/AsyncGeneratorPrototype.h>
#include <LibJS/Runtime/AsyncIteratorPrototype.h>
#include <LibJS/Runtime/AtomicsObject.h>
#include <LibJS/Runtime/BigIntConstructor.h>
#include <LibJS/Runtime/BigIntPrototype.h>
#include <LibJS/Runtime/BooleanConstructor.h>
#include <LibJS/Runtime/BooleanPrototype.h>
#include <LibJS/Runtime/ConsoleObject.h>
#include <LibJS/Runtime/DataViewConstructor.h>
#include <LibJS/Runtime/DataViewPrototype.h>
#include <LibJS/Runtime/DateConstructor.h>
#include <LibJS/Runtime/DatePrototype.h>
#include <LibJS/Runtime/ErrorConstructor.h>
#include <LibJS/Runtime/ErrorPrototype.h>
#include <LibJS/Runtime/FinalizationRegistryConstructor.h>
#include <LibJS/Runtime/FinalizationRegistryPrototype.h>
#include <LibJS/Runtime/FunctionConstructor.h>
#include <LibJS/Runtime/FunctionPrototype.h>
#include <LibJS/Runtime/GeneratorFunctionConstructor.h>
#include <LibJS/Runtime/GeneratorFunctionPrototype.h>
#include <LibJS/Runtime/GeneratorPrototype.h>
#include <LibJS/Runtime/Intl/CollatorConstructor.h>
#include <LibJS/Runtime/Intl/CollatorPrototype.h>
#include <LibJS/Runtime/Intl/DateTimeFormatConstructor.h>
#include <LibJS/Runtime/Intl/DateTimeFormatPrototype.h>
#include <LibJS/Runtime/Intl/DisplayNamesConstructor.h>
#include <LibJS/Runtime/Intl/DisplayNamesPrototype.h>
#include <LibJS/Runtime/Intl/DurationFormatConstructor.h>
#include <LibJS/Runtime/Intl/DurationFormatPrototype.h>
#include <LibJS/Runtime/Intl/Intl.h>
#include <LibJS/Runtime/Intl/ListFormatConstructor.h>
#include <LibJS/Runtime/Intl/ListFormatPrototype.h>
#include <LibJS/Runtime/Intl/LocaleConstructor.h>
#include <LibJS/Runtime/Intl/LocalePrototype.h>
#include <LibJS/Runtime/Intl/NumberFormatConstructor.h>
#include <LibJS/Runtime/Intl/NumberFormatPrototype.h>
#include <LibJS/Runtime/Intl/PluralRulesConstructor.h>
#include <LibJS/Runtime/Intl/PluralRulesPrototype.h>
#include <LibJS/Runtime/Intl/RelativeTimeFormatConstructor.h>
#include <LibJS/Runtime/Intl/RelativeTimeFormatPrototype.h>
#include <LibJS/Runtime/Intl/SegmentIteratorPrototype.h>
#include <LibJS/Runtime/Intl/SegmenterConstructor.h>
#include <LibJS/Runtime/Intl/SegmenterPrototype.h>
#include <LibJS/Runtime/Intl/SegmentsPrototype.h>
#include <LibJS/Runtime/Intrinsics.h>
#include <LibJS/Runtime/IteratorPrototype.h>
#include <LibJS/Runtime/JSONObject.h>
#include <LibJS/Runtime/MapConstructor.h>
#include <LibJS/Runtime/MapIteratorPrototype.h>
#include <LibJS/Runtime/MapPrototype.h>
#include <LibJS/Runtime/MathObject.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/NumberConstructor.h>
#include <LibJS/Runtime/NumberPrototype.h>
#include <LibJS/Runtime/ObjectConstructor.h>
#include <LibJS/Runtime/ObjectPrototype.h>
#include <LibJS/Runtime/PromiseConstructor.h>
#include <LibJS/Runtime/PromisePrototype.h>
#include <LibJS/Runtime/ProxyConstructor.h>
#include <LibJS/Runtime/Realm.h>
#include <LibJS/Runtime/ReflectObject.h>
#include <LibJS/Runtime/RegExpConstructor.h>
#include <LibJS/Runtime/RegExpPrototype.h>
#include <LibJS/Runtime/RegExpStringIteratorPrototype.h>
#include <LibJS/Runtime/SetConstructor.h>
#include <LibJS/Runtime/SetIteratorPrototype.h>
#include <LibJS/Runtime/SetPrototype.h>
#include <LibJS/Runtime/ShadowRealmConstructor.h>
#include <LibJS/Runtime/ShadowRealmPrototype.h>
#include <LibJS/Runtime/Shape.h>
#include <LibJS/Runtime/StringConstructor.h>
#include <LibJS/Runtime/StringIteratorPrototype.h>
#include <LibJS/Runtime/StringPrototype.h>
#include <LibJS/Runtime/SymbolConstructor.h>
#include <LibJS/Runtime/SymbolPrototype.h>
#include <LibJS/Runtime/Temporal/CalendarConstructor.h>
#include <LibJS/Runtime/Temporal/CalendarPrototype.h>
#include <LibJS/Runtime/Temporal/DurationConstructor.h>
#include <LibJS/Runtime/Temporal/DurationPrototype.h>
#include <LibJS/Runtime/Temporal/InstantConstructor.h>
#include <LibJS/Runtime/Temporal/InstantPrototype.h>
#include <LibJS/Runtime/Temporal/PlainDateConstructor.h>
#include <LibJS/Runtime/Temporal/PlainDatePrototype.h>
#include <LibJS/Runtime/Temporal/PlainDateTimeConstructor.h>
#include <LibJS/Runtime/Temporal/PlainDateTimePrototype.h>
#include <LibJS/Runtime/Temporal/PlainMonthDayConstructor.h>
#include <LibJS/Runtime/Temporal/PlainMonthDayPrototype.h>
#include <LibJS/Runtime/Temporal/PlainTimeConstructor.h>
#include <LibJS/Runtime/Temporal/PlainTimePrototype.h>
#include <LibJS/Runtime/Temporal/PlainYearMonthConstructor.h>
#include <LibJS/Runtime/Temporal/PlainYearMonthPrototype.h>
#include <LibJS/Runtime/Temporal/Temporal.h>
#include <LibJS/Runtime/Temporal/TimeZoneConstructor.h>
#include <LibJS/Runtime/Temporal/TimeZonePrototype.h>
#include <LibJS/Runtime/Temporal/ZonedDateTimeConstructor.h>
#include <LibJS/Runtime/Temporal/ZonedDateTimePrototype.h>
#include <LibJS/Runtime/TypedArray.h>
#include <LibJS/Runtime/TypedArrayConstructor.h>
#include <LibJS/Runtime/TypedArrayPrototype.h>
#include <LibJS/Runtime/WeakMapConstructor.h>
#include <LibJS/Runtime/WeakMapPrototype.h>
#include <LibJS/Runtime/WeakRefConstructor.h>
#include <LibJS/Runtime/WeakRefPrototype.h>
#include <LibJS/Runtime/WeakSetConstructor.h>
#include <LibJS/Runtime/WeakSetPrototype.h>

namespace JS {

static void initialize_constructor(VM& vm, PropertyKey const& property_key, Object& constructor, Object* prototype, PropertyAttributes constructor_property_attributes = Attribute::Writable | Attribute::Configurable)
{
    constructor.define_direct_property(vm.names.name, js_string(vm, property_key.as_string()), Attribute::Configurable);
    if (prototype)
        prototype->define_direct_property(vm.names.constructor, &constructor, constructor_property_attributes);
}

// 9.3.2 CreateIntrinsics ( realmRec ), https://tc39.es/ecma262/#sec-createintrinsics
Intrinsics* Intrinsics::create(Realm& realm)
{
    auto& vm = realm.vm();

    // 1. Set realmRec.[[Intrinsics]] to a new Record.
    auto* intrinsics = vm.heap().allocate_without_realm<Intrinsics>();
    realm.set_intrinsics({}, *intrinsics);

    // 2. Set fields of realmRec.[[Intrinsics]] with the values listed in Table 6.
    //    The field names are the names listed in column one of the table.
    //    The value of each field is a new object value fully and recursively populated
    //    with property values as defined by the specification of each object in
    //    clauses 19 through 28. All object property values are newly created object
    //    values. All values that are built-in function objects are created by performing
    //    CreateBuiltinFunction(steps, length, name, slots, realmRec, prototype)
    //    where steps is the definition of that function provided by this specification,
    //    name is the initial value of the function's "name" property, length is the
    //    initial value of the function's "length" property, slots is a list of the
    //    names, if any, of the function's specified internal slots, and prototype
    //    is the specified value of the function's [[Prototype]] internal slot. The
    //    creation of the intrinsics and their properties must be ordered to avoid
    //    any dependencies upon objects that have not yet been created.
    intrinsics->initialize_intrinsics(realm);

    // 3. Perform AddRestrictedFunctionProperties(realmRec.[[Intrinsics]].[[%Function.prototype%]], realmRec).
    add_restricted_function_properties(static_cast<FunctionObject&>(*realm.intrinsics().function_prototype()), realm);

    // 4. Return unused.
    return intrinsics;
}

void Intrinsics::initialize_intrinsics(Realm& realm)
{
    auto& vm = this->vm();

    // These are done first since other prototypes depend on their presence.
    m_empty_object_shape = heap().allocate_without_realm<Shape>(realm);
    m_object_prototype = heap().allocate_without_realm<ObjectPrototype>(realm);
    m_function_prototype = heap().allocate_without_realm<FunctionPrototype>(realm);

    m_new_object_shape = heap().allocate_without_realm<Shape>(realm);
    m_new_object_shape->set_prototype_without_transition(m_object_prototype);

    m_new_ordinary_function_prototype_object_shape = heap().allocate_without_realm<Shape>(realm);
    m_new_ordinary_function_prototype_object_shape->set_prototype_without_transition(m_object_prototype);
    m_new_ordinary_function_prototype_object_shape->add_property_without_transition(vm.names.constructor, Attribute::Writable | Attribute::Configurable);

    // Normally Heap::allocate() takes care of this, but these are allocated via allocate_without_realm().
    static_cast<FunctionPrototype*>(m_function_prototype)->initialize(realm);
    static_cast<ObjectPrototype*>(m_object_prototype)->initialize(realm);

#define __JS_ENUMERATE(ClassName, snake_name) \
    VERIFY(!m_##snake_name##_prototype);      \
    m_##snake_name##_prototype = heap().allocate<ClassName##Prototype>(realm, realm);
    JS_ENUMERATE_ITERATOR_PROTOTYPES
#undef __JS_ENUMERATE

    // These must be initialized separately as they have no companion constructor
    m_async_from_sync_iterator_prototype = heap().allocate<AsyncFromSyncIteratorPrototype>(realm, realm);
    m_async_generator_prototype = heap().allocate<AsyncGeneratorPrototype>(realm, realm);
    m_generator_prototype = heap().allocate<GeneratorPrototype>(realm, realm);
    m_intl_segments_prototype = heap().allocate<Intl::SegmentsPrototype>(realm, realm);

    // These must be initialized before allocating...
    // - AggregateErrorPrototype, which uses ErrorPrototype as its prototype
    // - AggregateErrorConstructor, which uses ErrorConstructor as its prototype
    // - AsyncFunctionConstructor, which uses FunctionConstructor as its prototype
    m_error_prototype = heap().allocate<ErrorPrototype>(realm, realm);
    m_error_constructor = heap().allocate<ErrorConstructor>(realm, realm);
    m_function_constructor = heap().allocate<FunctionConstructor>(realm, realm);

    // Not included in JS_ENUMERATE_NATIVE_OBJECTS due to missing distinct prototype
    m_proxy_constructor = heap().allocate<ProxyConstructor>(realm, realm);

    // Global object functions
    m_eval_function = NativeFunction::create(realm, GlobalObject::eval, 1, vm.names.eval, &realm);
    m_is_finite_function = NativeFunction::create(realm, GlobalObject::is_finite, 1, vm.names.isFinite, &realm);
    m_is_nan_function = NativeFunction::create(realm, GlobalObject::is_nan, 1, vm.names.isNaN, &realm);
    m_parse_float_function = NativeFunction::create(realm, GlobalObject::parse_float, 1, vm.names.parseFloat, &realm);
    m_parse_int_function = NativeFunction::create(realm, GlobalObject::parse_int, 2, vm.names.parseInt, &realm);
    m_decode_uri_function = NativeFunction::create(realm, GlobalObject::decode_uri, 1, vm.names.decodeURI, &realm);
    m_decode_uri_component_function = NativeFunction::create(realm, GlobalObject::decode_uri_component, 1, vm.names.decodeURIComponent, &realm);
    m_encode_uri_function = NativeFunction::create(realm, GlobalObject::encode_uri, 1, vm.names.encodeURI, &realm);
    m_encode_uri_component_function = NativeFunction::create(realm, GlobalObject::encode_uri_component, 1, vm.names.encodeURIComponent, &realm);
    m_escape_function = NativeFunction::create(realm, GlobalObject::escape, 1, vm.names.escape, &realm);
    m_unescape_function = NativeFunction::create(realm, GlobalObject::unescape, 1, vm.names.unescape, &realm);

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, ArrayType)         \
    /* These are the prototypes allocated earlier, everything else must not yet exist.*/         \
    if constexpr (!IsOneOf<PrototypeName, ErrorPrototype, FunctionPrototype, ObjectPrototype>) { \
        VERIFY(!m_##snake_name##_prototype);                                                     \
        m_##snake_name##_prototype = heap().allocate<PrototypeName>(realm, realm);               \
    }                                                                                            \
    if constexpr (!IsOneOf<ConstructorName, ErrorConstructor, FunctionConstructor>) {            \
        VERIFY(!m_##snake_name##_constructor);                                                   \
        m_##snake_name##_constructor = heap().allocate<ConstructorName>(realm, realm);           \
    }
    JS_ENUMERATE_BUILTIN_TYPES
#undef __JS_ENUMERATE

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName)                 \
    VERIFY(!m_intl_##snake_name##_constructor);                                               \
    VERIFY(!m_intl_##snake_name##_prototype);                                                 \
    m_intl_##snake_name##_prototype = heap().allocate<Intl::PrototypeName>(realm, realm);     \
    m_intl_##snake_name##_constructor = heap().allocate<Intl::ConstructorName>(realm, realm); \
    initialize_constructor(vm, vm.names.ClassName, *m_intl_##snake_name##_constructor, m_intl_##snake_name##_prototype);
    JS_ENUMERATE_INTL_OBJECTS
#undef __JS_ENUMERATE

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName)                         \
    VERIFY(!m_temporal_##snake_name##_constructor);                                                   \
    VERIFY(!m_temporal_##snake_name##_prototype);                                                     \
    m_temporal_##snake_name##_prototype = heap().allocate<Temporal::PrototypeName>(realm, realm);     \
    m_temporal_##snake_name##_constructor = heap().allocate<Temporal::ConstructorName>(realm, realm); \
    initialize_constructor(vm, vm.names.ClassName, *m_temporal_##snake_name##_constructor, m_temporal_##snake_name##_prototype);
    JS_ENUMERATE_TEMPORAL_OBJECTS
#undef __JS_ENUMERATE

    // 10.2.4.1 %ThrowTypeError% ( ), https://tc39.es/ecma262/#sec-%throwtypeerror%
    m_throw_type_error_function = NativeFunction::create(
        realm, [](VM& vm) {
            return vm.throw_completion<TypeError>(ErrorType::RestrictedFunctionPropertiesAccess);
        },
        0, "", &realm);
    m_throw_type_error_function->define_direct_property(vm.names.length, Value(0), 0);
    m_throw_type_error_function->define_direct_property(vm.names.name, js_string(vm, ""), 0);
    MUST(m_throw_type_error_function->internal_prevent_extensions());

#define __JS_ENUMERATE(ClassName, snake_name) \
    VERIFY(!m_##snake_name##_object);         \
    m_##snake_name##_object = heap().allocate<ClassName>(realm, realm);
    JS_ENUMERATE_BUILTIN_NAMESPACE_OBJECTS
#undef __JS_ENUMERATE

    initialize_constructor(vm, vm.names.AggregateError, *m_aggregate_error_constructor, m_aggregate_error_prototype);
    initialize_constructor(vm, vm.names.Array, *m_array_constructor, m_array_prototype);
    initialize_constructor(vm, vm.names.ArrayBuffer, *m_array_buffer_constructor, m_array_buffer_prototype);
    initialize_constructor(vm, vm.names.BigInt, *m_bigint_constructor, m_bigint_prototype);
    initialize_constructor(vm, vm.names.Boolean, *m_boolean_constructor, m_boolean_prototype);
    initialize_constructor(vm, vm.names.DataView, *m_data_view_constructor, m_data_view_prototype);
    initialize_constructor(vm, vm.names.Date, *m_date_constructor, m_date_prototype);
    initialize_constructor(vm, vm.names.Error, *m_error_constructor, m_error_prototype);
    initialize_constructor(vm, vm.names.FinalizationRegistry, *m_finalization_registry_constructor, m_finalization_registry_prototype);
    initialize_constructor(vm, vm.names.Function, *m_function_constructor, m_function_prototype);
    initialize_constructor(vm, vm.names.Map, *m_map_constructor, m_map_prototype);
    initialize_constructor(vm, vm.names.Number, *m_number_constructor, m_number_prototype);
    initialize_constructor(vm, vm.names.Object, *m_object_constructor, m_object_prototype);
    initialize_constructor(vm, vm.names.Promise, *m_promise_constructor, m_promise_prototype);
    initialize_constructor(vm, vm.names.Proxy, *m_proxy_constructor, nullptr);
    initialize_constructor(vm, vm.names.RegExp, *m_regexp_constructor, m_regexp_prototype);
    initialize_constructor(vm, vm.names.Set, *m_set_constructor, m_set_prototype);
    initialize_constructor(vm, vm.names.ShadowRealm, *m_shadow_realm_constructor, m_shadow_realm_prototype);
    initialize_constructor(vm, vm.names.String, *m_string_constructor, m_string_prototype);
    initialize_constructor(vm, vm.names.Symbol, *m_symbol_constructor, m_symbol_prototype);
    initialize_constructor(vm, vm.names.TypedArray, *m_typed_array_constructor, m_typed_array_prototype);
    initialize_constructor(vm, vm.names.WeakMap, *m_weak_map_constructor, m_weak_map_prototype);
    initialize_constructor(vm, vm.names.WeakRef, *m_weak_ref_constructor, m_weak_ref_prototype);
    initialize_constructor(vm, vm.names.WeakSet, *m_weak_set_constructor, m_weak_set_prototype);

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, ArrayType) \
    initialize_constructor(vm, vm.names.ClassName, *m_##snake_name##_constructor, m_##snake_name##_prototype);
    JS_ENUMERATE_NATIVE_ERRORS
    JS_ENUMERATE_TYPED_ARRAYS
#undef __JS_ENUMERATE

    initialize_constructor(vm, vm.names.GeneratorFunction, *m_generator_function_constructor, m_generator_function_prototype, Attribute::Configurable);
    initialize_constructor(vm, vm.names.AsyncGeneratorFunction, *m_async_generator_function_constructor, m_async_generator_function_prototype, Attribute::Configurable);
    initialize_constructor(vm, vm.names.AsyncFunction, *m_async_function_constructor, m_async_function_prototype, Attribute::Configurable);

    // 27.5.1.1 Generator.prototype.constructor, https://tc39.es/ecma262/#sec-generator.prototype.constructor
    m_generator_prototype->define_direct_property(vm.names.constructor, m_generator_function_prototype, Attribute::Configurable);

    // 27.6.1.1 AsyncGenerator.prototype.constructor, https://tc39.es/ecma262/#sec-asyncgenerator-prototype-constructor
    m_async_generator_prototype->define_direct_property(vm.names.constructor, m_async_generator_function_prototype, Attribute::Configurable);

    m_array_prototype_values_function = &m_array_prototype->get_without_side_effects(vm.names.values).as_function();
    m_date_constructor_now_function = &m_date_constructor->get_without_side_effects(vm.names.now).as_function();
    m_json_parse_function = &m_json_object->get_without_side_effects(vm.names.parse).as_function();
    m_object_prototype_to_string_function = &m_object_prototype->get_without_side_effects(vm.names.toString).as_function();
}

void Intrinsics::visit_edges(Visitor& visitor)
{
    visitor.visit(m_empty_object_shape);
    visitor.visit(m_new_object_shape);
    visitor.visit(m_new_ordinary_function_prototype_object_shape);
    visitor.visit(m_proxy_constructor);
    visitor.visit(m_async_from_sync_iterator_prototype);
    visitor.visit(m_async_generator_prototype);
    visitor.visit(m_generator_prototype);
    visitor.visit(m_intl_segments_prototype);
    visitor.visit(m_array_prototype_values_function);
    visitor.visit(m_date_constructor_now_function);
    visitor.visit(m_eval_function);
    visitor.visit(m_json_parse_function);
    visitor.visit(m_object_prototype_to_string_function);
    visitor.visit(m_throw_type_error_function);

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, ArrayType) \
    visitor.visit(m_##snake_name##_constructor);                                         \
    visitor.visit(m_##snake_name##_prototype);
    JS_ENUMERATE_BUILTIN_TYPES
#undef __JS_ENUMERATE

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName) \
    visitor.visit(m_intl_##snake_name##_constructor);                         \
    visitor.visit(m_intl_##snake_name##_prototype);
    JS_ENUMERATE_INTL_OBJECTS
#undef __JS_ENUMERATE

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName) \
    visitor.visit(m_temporal_##snake_name##_constructor);                     \
    visitor.visit(m_temporal_##snake_name##_prototype);
    JS_ENUMERATE_TEMPORAL_OBJECTS
#undef __JS_ENUMERATE

#define __JS_ENUMERATE(ClassName, snake_name) \
    visitor.visit(m_##snake_name##_object);
    JS_ENUMERATE_BUILTIN_NAMESPACE_OBJECTS
#undef __JS_ENUMERATE

#define __JS_ENUMERATE(ClassName, snake_name) \
    visitor.visit(m_##snake_name##_prototype);
    JS_ENUMERATE_ITERATOR_PROTOTYPES
#undef __JS_ENUMERATE
}

// 10.2.4 AddRestrictedFunctionProperties ( F, realm ), https://tc39.es/ecma262/#sec-addrestrictedfunctionproperties
void add_restricted_function_properties(FunctionObject& function, Realm& realm)
{
    auto& vm = realm.vm();

    // 1. Assert: realm.[[Intrinsics]].[[%ThrowTypeError%]] exists and has been initialized.
    VERIFY(realm.intrinsics().throw_type_error_function());

    // 2. Let thrower be realm.[[Intrinsics]].[[%ThrowTypeError%]].
    auto* thrower = realm.intrinsics().throw_type_error_function();

    // 3. Perform ! DefinePropertyOrThrow(F, "caller", PropertyDescriptor { [[Get]]: thrower, [[Set]]: thrower, [[Enumerable]]: false, [[Configurable]]: true }).
    function.define_direct_accessor(vm.names.caller, thrower, thrower, Attribute::Configurable);

    // 4. Perform ! DefinePropertyOrThrow(F, "arguments", PropertyDescriptor { [[Get]]: thrower, [[Set]]: thrower, [[Enumerable]]: false, [[Configurable]]: true }).
    function.define_direct_accessor(vm.names.arguments, thrower, thrower, Attribute::Configurable);

    // 5. Return unused.
}

}
