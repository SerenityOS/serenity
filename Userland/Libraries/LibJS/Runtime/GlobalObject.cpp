/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/Hex.h>
#include <AK/Platform.h>
#include <AK/Utf8View.h>
#include <LibJS/Console.h>
#include <LibJS/Heap/DeferGC.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/AbstractOperations.h>
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
#include <LibJS/Runtime/GeneratorObjectPrototype.h>
#include <LibJS/Runtime/GlobalEnvironment.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/DateTimeFormatConstructor.h>
#include <LibJS/Runtime/Intl/DateTimeFormatPrototype.h>
#include <LibJS/Runtime/Intl/DisplayNamesConstructor.h>
#include <LibJS/Runtime/Intl/DisplayNamesPrototype.h>
#include <LibJS/Runtime/Intl/Intl.h>
#include <LibJS/Runtime/Intl/ListFormatConstructor.h>
#include <LibJS/Runtime/Intl/ListFormatPrototype.h>
#include <LibJS/Runtime/Intl/LocaleConstructor.h>
#include <LibJS/Runtime/Intl/LocalePrototype.h>
#include <LibJS/Runtime/Intl/NumberFormatConstructor.h>
#include <LibJS/Runtime/Intl/NumberFormatPrototype.h>
#include <LibJS/Runtime/IteratorPrototype.h>
#include <LibJS/Runtime/JSONObject.h>
#include <LibJS/Runtime/MapConstructor.h>
#include <LibJS/Runtime/MapIteratorPrototype.h>
#include <LibJS/Runtime/MapPrototype.h>
#include <LibJS/Runtime/MathObject.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/NumberConstructor.h>
#include <LibJS/Runtime/NumberPrototype.h>
#include <LibJS/Runtime/Object.h>
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
#include <LibJS/Runtime/Value.h>
#include <LibJS/Runtime/WeakMapConstructor.h>
#include <LibJS/Runtime/WeakMapPrototype.h>
#include <LibJS/Runtime/WeakRefConstructor.h>
#include <LibJS/Runtime/WeakRefPrototype.h>
#include <LibJS/Runtime/WeakSetConstructor.h>
#include <LibJS/Runtime/WeakSetPrototype.h>

namespace JS {

GlobalObject::GlobalObject()
    : Object(GlobalObjectTag::Tag)
    , m_console(make<Console>(*this))
{
}

void GlobalObject::initialize_global_object()
{
    auto& vm = this->vm();

    ensure_shape_is_unique();

    // These are done first since other prototypes depend on their presence.
    m_empty_object_shape = heap().allocate_without_global_object<Shape>(*this);
    m_object_prototype = heap().allocate_without_global_object<ObjectPrototype>(*this);
    m_function_prototype = heap().allocate_without_global_object<FunctionPrototype>(*this);

    m_new_object_shape = vm.heap().allocate_without_global_object<Shape>(*this);
    m_new_object_shape->set_prototype_without_transition(m_object_prototype);

    m_new_ordinary_function_prototype_object_shape = vm.heap().allocate_without_global_object<Shape>(*this);
    m_new_ordinary_function_prototype_object_shape->set_prototype_without_transition(m_object_prototype);
    m_new_ordinary_function_prototype_object_shape->add_property_without_transition(vm.names.constructor, Attribute::Writable | Attribute::Configurable);

    // Normally Heap::allocate() takes care of this, but these are allocated via allocate_without_global_object().

    static_cast<FunctionPrototype*>(m_function_prototype)->initialize(*this);

    static_cast<ObjectPrototype*>(m_object_prototype)->initialize(*this);

    Object::set_prototype(m_object_prototype);

    // This must be initialized before allocating AggregateErrorPrototype, which uses ErrorPrototype as its prototype.
    m_error_prototype = heap().allocate<ErrorPrototype>(*this, *this);

#define __JS_ENUMERATE(ClassName, snake_name) \
    if (!m_##snake_name##_prototype)          \
        m_##snake_name##_prototype = heap().allocate<ClassName##Prototype>(*this, *this);
    JS_ENUMERATE_ITERATOR_PROTOTYPES
#undef __JS_ENUMERATE

    // %GeneratorFunction.prototype.prototype% must be initialized separately as it has no
    // companion constructor
    m_generator_object_prototype = heap().allocate<GeneratorObjectPrototype>(*this, *this);
    m_generator_object_prototype->define_direct_property(vm.names.constructor, m_generator_function_constructor, Attribute::Configurable);

    m_async_from_sync_iterator_prototype = heap().allocate<AsyncFromSyncIteratorPrototype>(*this, *this);

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, ArrayType) \
    if (!m_##snake_name##_prototype)                                                     \
        m_##snake_name##_prototype = heap().allocate<PrototypeName>(*this, *this);
    JS_ENUMERATE_BUILTIN_TYPES
#undef __JS_ENUMERATE

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName) \
    if (!m_intl_##snake_name##_prototype)                                     \
        m_intl_##snake_name##_prototype = heap().allocate<Intl::PrototypeName>(*this, *this);
    JS_ENUMERATE_INTL_OBJECTS
#undef __JS_ENUMERATE

    // Must be allocated before `Intl::Intl` below.
#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName) \
    initialize_constructor(vm.names.ClassName, m_intl_##snake_name##_constructor, m_intl_##snake_name##_prototype);
    JS_ENUMERATE_INTL_OBJECTS
#undef __JS_ENUMERATE

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName) \
    if (!m_temporal_##snake_name##_prototype)                                 \
        m_temporal_##snake_name##_prototype = heap().allocate<Temporal::PrototypeName>(*this, *this);
    JS_ENUMERATE_TEMPORAL_OBJECTS
#undef __JS_ENUMERATE

    // Must be allocated before `Temporal::Temporal` below.
#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName) \
    initialize_constructor(vm.names.ClassName, m_temporal_##snake_name##_constructor, m_temporal_##snake_name##_prototype);
    JS_ENUMERATE_TEMPORAL_OBJECTS
#undef __JS_ENUMERATE

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.gc, gc, 0, attr);
    define_native_function(vm.names.isNaN, is_nan, 1, attr);
    define_native_function(vm.names.isFinite, is_finite, 1, attr);
    define_native_function(vm.names.parseFloat, parse_float, 1, attr);
    define_native_function(vm.names.parseInt, parse_int, 2, attr);
    define_native_function(vm.names.eval, eval, 1, attr);

    // 10.2.4.1 %ThrowTypeError% ( ), https://tc39.es/ecma262/#sec-%throwtypeerror%
    m_throw_type_error_function = NativeFunction::create(global_object(), {}, [](VM& vm, GlobalObject& global_object) {
        return vm.throw_completion<TypeError>(global_object, ErrorType::RestrictedFunctionPropertiesAccess);
    });
    m_throw_type_error_function->define_direct_property(vm.names.length, Value(0), 0);
    m_throw_type_error_function->define_direct_property(vm.names.name, js_string(vm, ""), 0);
    MUST(m_throw_type_error_function->internal_prevent_extensions());

    // 10.2.4 AddRestrictedFunctionProperties ( F, realm ), https://tc39.es/ecma262/#sec-addrestrictedfunctionproperties
    m_function_prototype->define_direct_accessor(vm.names.caller, m_throw_type_error_function, m_throw_type_error_function, Attribute::Configurable);
    m_function_prototype->define_direct_accessor(vm.names.arguments, m_throw_type_error_function, m_throw_type_error_function, Attribute::Configurable);

    define_native_function(vm.names.encodeURI, encode_uri, 1, attr);
    define_native_function(vm.names.decodeURI, decode_uri, 1, attr);
    define_native_function(vm.names.encodeURIComponent, encode_uri_component, 1, attr);
    define_native_function(vm.names.decodeURIComponent, decode_uri_component, 1, attr);
    define_native_function(vm.names.escape, escape, 1, attr);
    define_native_function(vm.names.unescape, unescape, 1, attr);

    define_direct_property(vm.names.NaN, js_nan(), 0);
    define_direct_property(vm.names.Infinity, js_infinity(), 0);
    define_direct_property(vm.names.undefined, js_undefined(), 0);

    define_direct_property(vm.names.globalThis, this, attr);
    define_direct_property(vm.names.console, heap().allocate<ConsoleObject>(*this, *this), attr);
    define_direct_property(vm.names.Atomics, heap().allocate<AtomicsObject>(*this, *this), attr);
    define_direct_property(vm.names.Math, heap().allocate<MathObject>(*this, *this), attr);
    define_direct_property(vm.names.JSON, heap().allocate<JSONObject>(*this, *this), attr);
    define_direct_property(vm.names.Reflect, heap().allocate<ReflectObject>(*this, *this), attr);
    define_direct_property(vm.names.Intl, heap().allocate<Intl::Intl>(*this, *this), attr);
    define_direct_property(vm.names.Temporal, heap().allocate<Temporal::Temporal>(*this, *this), attr);

    // This must be initialized before allocating AggregateErrorConstructor, which uses ErrorConstructor as its prototype.
    initialize_constructor(vm.names.Error, m_error_constructor, m_error_prototype);

    add_constructor(vm.names.AggregateError, m_aggregate_error_constructor, m_aggregate_error_prototype);
    add_constructor(vm.names.Array, m_array_constructor, m_array_prototype);
    add_constructor(vm.names.ArrayBuffer, m_array_buffer_constructor, m_array_buffer_prototype);
    add_constructor(vm.names.BigInt, m_bigint_constructor, m_bigint_prototype);
    add_constructor(vm.names.Boolean, m_boolean_constructor, m_boolean_prototype);
    add_constructor(vm.names.DataView, m_data_view_constructor, m_data_view_prototype);
    add_constructor(vm.names.Date, m_date_constructor, m_date_prototype);
    add_constructor(vm.names.Error, m_error_constructor, m_error_prototype);
    add_constructor(vm.names.FinalizationRegistry, m_finalization_registry_constructor, m_finalization_registry_prototype);
    add_constructor(vm.names.Function, m_function_constructor, m_function_prototype);
    add_constructor(vm.names.Map, m_map_constructor, m_map_prototype);
    add_constructor(vm.names.Number, m_number_constructor, m_number_prototype);
    add_constructor(vm.names.Object, m_object_constructor, m_object_prototype);
    add_constructor(vm.names.Promise, m_promise_constructor, m_promise_prototype);
    add_constructor(vm.names.Proxy, m_proxy_constructor, nullptr);
    add_constructor(vm.names.RegExp, m_regexp_constructor, m_regexp_prototype);
    add_constructor(vm.names.Set, m_set_constructor, m_set_prototype);
    add_constructor(vm.names.ShadowRealm, m_shadow_realm_constructor, m_shadow_realm_prototype);
    add_constructor(vm.names.String, m_string_constructor, m_string_prototype);
    add_constructor(vm.names.Symbol, m_symbol_constructor, m_symbol_prototype);
    add_constructor(vm.names.WeakMap, m_weak_map_constructor, m_weak_map_prototype);
    add_constructor(vm.names.WeakRef, m_weak_ref_constructor, m_weak_ref_prototype);
    add_constructor(vm.names.WeakSet, m_weak_set_constructor, m_weak_set_prototype);

    initialize_constructor(vm.names.TypedArray, m_typed_array_constructor, m_typed_array_prototype);

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, ArrayType) \
    add_constructor(vm.names.ClassName, m_##snake_name##_constructor, m_##snake_name##_prototype);
    JS_ENUMERATE_NATIVE_ERRORS
    JS_ENUMERATE_TYPED_ARRAYS
#undef __JS_ENUMERATE

    // The generator constructor cannot be initialized with add_constructor as it has no global binding
    m_generator_function_constructor = heap().allocate<GeneratorFunctionConstructor>(*this, *this);
    // 27.3.3.1 GeneratorFunction.prototype.constructor, https://tc39.es/ecma262/#sec-generatorfunction.prototype.constructor
    m_generator_function_prototype->define_direct_property(vm.names.constructor, m_generator_function_constructor, Attribute::Configurable);

    // The async generator constructor cannot be initialized with add_constructor as it has no global binding
    m_async_generator_function_constructor = heap().allocate<AsyncGeneratorFunctionConstructor>(*this, *this);
    // 27.4.3.1 AsyncGeneratorFunction.prototype.constructor, https://tc39.es/ecma262/#sec-asyncgeneratorfunction-prototype-constructor
    m_async_generator_function_prototype->define_direct_property(vm.names.constructor, m_async_generator_function_constructor, Attribute::Configurable);

    m_array_prototype_values_function = &m_array_prototype->get_without_side_effects(vm.names.values).as_function();
    m_date_constructor_now_function = &m_date_constructor->get_without_side_effects(vm.names.now).as_function();
    m_eval_function = &get_without_side_effects(vm.names.eval).as_function();
}

GlobalObject::~GlobalObject()
{
}

void GlobalObject::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);

    visitor.visit(m_empty_object_shape);
    visitor.visit(m_new_object_shape);
    visitor.visit(m_new_ordinary_function_prototype_object_shape);
    visitor.visit(m_proxy_constructor);
    visitor.visit(m_generator_object_prototype);
    visitor.visit(m_array_prototype_values_function);
    visitor.visit(m_date_constructor_now_function);
    visitor.visit(m_eval_function);
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
    visitor.visit(m_##snake_name##_prototype);
    JS_ENUMERATE_ITERATOR_PROTOTYPES
#undef __JS_ENUMERATE
}

Realm* GlobalObject::associated_realm()
{
    return m_associated_realm;
}

void GlobalObject::set_associated_realm(Badge<Realm>, Realm& realm)
{
    m_associated_realm = &realm;
}

JS_DEFINE_NATIVE_FUNCTION(GlobalObject::gc)
{
#ifdef __serenity__
    dbgln("Forced garbage collection requested!");
#endif
    vm.heap().collect_garbage();
    return js_undefined();
}

// 19.2.3 isNaN ( number ), https://tc39.es/ecma262/#sec-isnan-number
JS_DEFINE_NATIVE_FUNCTION(GlobalObject::is_nan)
{
    return Value(TRY(vm.argument(0).to_number(global_object)).is_nan());
}

// 19.2.2 isFinite ( number ), https://tc39.es/ecma262/#sec-isfinite-number
JS_DEFINE_NATIVE_FUNCTION(GlobalObject::is_finite)
{
    return Value(TRY(vm.argument(0).to_number(global_object)).is_finite_number());
}

// 19.2.4 parseFloat ( string ), https://tc39.es/ecma262/#sec-parsefloat-string
JS_DEFINE_NATIVE_FUNCTION(GlobalObject::parse_float)
{
    if (vm.argument(0).is_number())
        return vm.argument(0);
    auto input_string = TRY(vm.argument(0).to_string(global_object));
    auto trimmed_string = input_string.trim_whitespace(TrimMode::Left);
    for (size_t length = trimmed_string.length(); length > 0; --length) {
        auto number = MUST(Value(js_string(vm, trimmed_string.substring(0, length))).to_number(global_object));
        if (!number.is_nan())
            return number;
    }
    return js_nan();
}

// 19.2.5 parseInt ( string, radix ), https://tc39.es/ecma262/#sec-parseint-string-radix
JS_DEFINE_NATIVE_FUNCTION(GlobalObject::parse_int)
{
    auto input_string = TRY(vm.argument(0).to_string(global_object));

    // FIXME: There's a bunch of unnecessary string copying here.
    double sign = 1;
    auto s = input_string.trim_whitespace(TrimMode::Left);
    if (!s.is_empty() && s[0] == '-')
        sign = -1;
    if (!s.is_empty() && (s[0] == '+' || s[0] == '-'))
        s = s.substring(1, s.length() - 1);

    auto radix = TRY(vm.argument(1).to_i32(global_object));

    bool strip_prefix = true;
    if (radix != 0) {
        if (radix < 2 || radix > 36)
            return js_nan();
        if (radix != 16)
            strip_prefix = false;
    } else {
        radix = 10;
    }

    if (strip_prefix) {
        if (s.length() >= 2 && s[0] == '0' && (s[1] == 'x' || s[1] == 'X')) {
            s = s.substring(2, s.length() - 2);
            radix = 16;
        }
    }

    auto parse_digit = [&](u32 code_point, i32 radix) -> Optional<i32> {
        if (!is_ascii_alphanumeric(code_point) || radix <= 0)
            return {};
        auto digit = parse_ascii_base36_digit(code_point);
        if (digit >= (u32)radix)
            return {};
        return digit;
    };

    bool had_digits = false;
    double number = 0;
    for (auto code_point : Utf8View(s)) {
        auto digit = parse_digit(code_point, radix);
        if (!digit.has_value())
            break;
        had_digits = true;
        number *= radix;
        number += digit.value();
    }

    if (!had_digits)
        return js_nan();

    return Value(sign * number);
}

// 19.2.1 eval ( x ), https://tc39.es/ecma262/#sec-eval-x
JS_DEFINE_NATIVE_FUNCTION(GlobalObject::eval)
{
    return perform_eval(vm.argument(0), global_object, CallerMode::NonStrict, EvalMode::Indirect);
}

// 19.2.6.1.1 Encode ( string, unescapedSet ), https://tc39.es/ecma262/#sec-encode
static ThrowCompletionOr<String> encode([[maybe_unused]] JS::GlobalObject& global_object, const String& string, StringView unescaped_set)
{
    StringBuilder encoded_builder;
    for (unsigned char code_unit : string) {
        if (unescaped_set.contains(code_unit)) {
            encoded_builder.append(code_unit);
            continue;
        }
        // FIXME: check for unpaired surrogates and throw URIError
        encoded_builder.appendff("%{:02X}", code_unit);
    }
    return encoded_builder.build();
}

// 19.2.6.1.2 Decode ( string, reservedSet ), https://tc39.es/ecma262/#sec-decode
static ThrowCompletionOr<String> decode(JS::GlobalObject& global_object, const String& string, StringView reserved_set)
{
    StringBuilder decoded_builder;
    auto expected_continuation_bytes = 0;
    for (size_t k = 0; k < string.length(); k++) {
        auto code_unit = string[k];
        if (code_unit != '%') {
            if (expected_continuation_bytes > 0)
                return global_object.vm().throw_completion<URIError>(global_object, ErrorType::URIMalformed);

            decoded_builder.append(code_unit);
            continue;
        }

        if (k + 2 >= string.length())
            return global_object.vm().throw_completion<URIError>(global_object, ErrorType::URIMalformed);

        auto first_digit = decode_hex_digit(string[k + 1]);
        if (first_digit >= 16)
            return global_object.vm().throw_completion<URIError>(global_object, ErrorType::URIMalformed);

        auto second_digit = decode_hex_digit(string[k + 2]);
        if (second_digit >= 16)
            return global_object.vm().throw_completion<URIError>(global_object, ErrorType::URIMalformed);

        char decoded_code_unit = (first_digit << 4) | second_digit;
        k += 2;
        if (expected_continuation_bytes > 0) {
            decoded_builder.append(decoded_code_unit);
            expected_continuation_bytes--;
            continue;
        }

        if ((decoded_code_unit & 0x80) == 0) {
            if (reserved_set.contains(decoded_code_unit))
                decoded_builder.append(string.substring_view(k - 2, 3));
            else
                decoded_builder.append(decoded_code_unit);
            continue;
        }

        auto leading_ones = count_trailing_zeroes_32_safe(~decoded_code_unit) - 24;
        if (leading_ones == 1 || leading_ones > 4)
            return global_object.vm().throw_completion<URIError>(global_object, ErrorType::URIMalformed);

        decoded_builder.append(decoded_code_unit);
        expected_continuation_bytes = leading_ones - 1;
    }
    return decoded_builder.build();
}

// 19.2.6.4 encodeURI ( uri ), https://tc39.es/ecma262/#sec-encodeuri-uri
JS_DEFINE_NATIVE_FUNCTION(GlobalObject::encode_uri)
{
    auto uri_string = TRY(vm.argument(0).to_string(global_object));
    auto encoded = TRY(encode(global_object, uri_string, ";/?:@&=+$,abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_.!~*'()#"sv));
    return js_string(vm, move(encoded));
}

// 19.2.6.2 decodeURI ( encodedURI ), https://tc39.es/ecma262/#sec-decodeuri-encodeduri
JS_DEFINE_NATIVE_FUNCTION(GlobalObject::decode_uri)
{
    auto uri_string = TRY(vm.argument(0).to_string(global_object));
    auto decoded = TRY(decode(global_object, uri_string, ";/?:@&=+$,#"sv));
    return js_string(vm, move(decoded));
}

// 19.2.6.5 encodeURIComponent ( uriComponent ), https://tc39.es/ecma262/#sec-encodeuricomponent-uricomponent
JS_DEFINE_NATIVE_FUNCTION(GlobalObject::encode_uri_component)
{
    auto uri_string = TRY(vm.argument(0).to_string(global_object));
    auto encoded = TRY(encode(global_object, uri_string, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_.!~*'()"sv));
    return js_string(vm, move(encoded));
}

// 19.2.6.3 decodeURIComponent ( encodedURIComponent ), https://tc39.es/ecma262/#sec-decodeuricomponent-encodeduricomponent
JS_DEFINE_NATIVE_FUNCTION(GlobalObject::decode_uri_component)
{
    auto uri_string = TRY(vm.argument(0).to_string(global_object));
    auto decoded = TRY(decode(global_object, uri_string, ""sv));
    return js_string(vm, move(decoded));
}

// B.2.1.1 escape ( string ), https://tc39.es/ecma262/#sec-escape-string
JS_DEFINE_NATIVE_FUNCTION(GlobalObject::escape)
{
    auto string = TRY(vm.argument(0).to_string(global_object));
    StringBuilder escaped;
    for (auto code_point : Utf8View(string)) {
        if (code_point < 256) {
            if ("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@*_+-./"sv.contains(code_point))
                escaped.append(code_point);
            else
                escaped.appendff("%{:02X}", code_point);
            continue;
        }
        escaped.appendff("%u{:04X}", code_point); // FIXME: Handle utf-16 surrogate pairs
    }
    return js_string(vm, escaped.build());
}

// B.2.1.2 unescape ( string ), https://tc39.es/ecma262/#sec-unescape-string
JS_DEFINE_NATIVE_FUNCTION(GlobalObject::unescape)
{
    auto string = TRY(vm.argument(0).to_string(global_object));
    ssize_t length = string.length();
    StringBuilder unescaped(length);
    for (auto k = 0; k < length; ++k) {
        u32 code_point = string[k];
        if (code_point == '%') {
            if (k <= length - 6 && string[k + 1] == 'u' && is_ascii_hex_digit(string[k + 2]) && is_ascii_hex_digit(string[k + 3]) && is_ascii_hex_digit(string[k + 4]) && is_ascii_hex_digit(string[k + 5])) {
                code_point = (parse_ascii_hex_digit(string[k + 2]) << 12) | (parse_ascii_hex_digit(string[k + 3]) << 8) | (parse_ascii_hex_digit(string[k + 4]) << 4) | parse_ascii_hex_digit(string[k + 5]);
                k += 5;
            } else if (k <= length - 3 && is_ascii_hex_digit(string[k + 1]) && is_ascii_hex_digit(string[k + 2])) {
                code_point = (parse_ascii_hex_digit(string[k + 1]) << 4) | parse_ascii_hex_digit(string[k + 2]);
                k += 2;
            }
        }
        unescaped.append_code_point(code_point);
    }
    return js_string(vm, unescaped.build());
}

}
