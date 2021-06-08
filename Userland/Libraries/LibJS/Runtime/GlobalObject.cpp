/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/Hex.h>
#include <AK/Platform.h>
#include <AK/TemporaryChange.h>
#include <AK/Utf8View.h>
#include <LibJS/Console.h>
#include <LibJS/Heap/DeferGC.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Lexer.h>
#include <LibJS/Parser.h>
#include <LibJS/Runtime/ArrayBufferConstructor.h>
#include <LibJS/Runtime/ArrayBufferPrototype.h>
#include <LibJS/Runtime/ArrayConstructor.h>
#include <LibJS/Runtime/ArrayIteratorPrototype.h>
#include <LibJS/Runtime/ArrayPrototype.h>
#include <LibJS/Runtime/BigIntConstructor.h>
#include <LibJS/Runtime/BigIntPrototype.h>
#include <LibJS/Runtime/BooleanConstructor.h>
#include <LibJS/Runtime/BooleanPrototype.h>
#include <LibJS/Runtime/ConsoleObject.h>
#include <LibJS/Runtime/DateConstructor.h>
#include <LibJS/Runtime/DatePrototype.h>
#include <LibJS/Runtime/ErrorConstructor.h>
#include <LibJS/Runtime/ErrorPrototype.h>
#include <LibJS/Runtime/FunctionConstructor.h>
#include <LibJS/Runtime/FunctionPrototype.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/IteratorPrototype.h>
#include <LibJS/Runtime/JSONObject.h>
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
#include <LibJS/Runtime/ReflectObject.h>
#include <LibJS/Runtime/RegExpConstructor.h>
#include <LibJS/Runtime/RegExpPrototype.h>
#include <LibJS/Runtime/Shape.h>
#include <LibJS/Runtime/StringConstructor.h>
#include <LibJS/Runtime/StringIteratorPrototype.h>
#include <LibJS/Runtime/StringPrototype.h>
#include <LibJS/Runtime/SymbolConstructor.h>
#include <LibJS/Runtime/SymbolPrototype.h>
#include <LibJS/Runtime/TypedArray.h>
#include <LibJS/Runtime/TypedArrayConstructor.h>
#include <LibJS/Runtime/TypedArrayPrototype.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

GlobalObject::GlobalObject()
    : ScopeObject(GlobalObjectTag::Tag)
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

    m_new_script_function_prototype_object_shape = vm.heap().allocate_without_global_object<Shape>(*this);
    m_new_script_function_prototype_object_shape->set_prototype_without_transition(m_object_prototype);
    m_new_script_function_prototype_object_shape->add_property_without_transition(vm.names.constructor, Attribute::Writable | Attribute::Configurable);

    static_cast<FunctionPrototype*>(m_function_prototype)->initialize(*this);
    static_cast<ObjectPrototype*>(m_object_prototype)->initialize(*this);

    set_prototype(m_object_prototype);

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, ArrayType) \
    if (!m_##snake_name##_prototype)                                                     \
        m_##snake_name##_prototype = heap().allocate<PrototypeName>(*this, *this);
    JS_ENUMERATE_BUILTIN_TYPES
#undef __JS_ENUMERATE

#define __JS_ENUMERATE(ClassName, snake_name) \
    if (!m_##snake_name##_prototype)          \
        m_##snake_name##_prototype = heap().allocate<ClassName##Prototype>(*this, *this);
    JS_ENUMERATE_ITERATOR_PROTOTYPES
#undef __JS_ENUMERATE

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.gc, gc, 0, attr);
    define_native_function(vm.names.isNaN, is_nan, 1, attr);
    define_native_function(vm.names.isFinite, is_finite, 1, attr);
    define_native_function(vm.names.parseFloat, parse_float, 1, attr);
    define_native_function(vm.names.parseInt, parse_int, 2, attr);
    define_native_function(vm.names.eval, eval, 1, attr);
    define_native_function(vm.names.encodeURI, encode_uri, 1, attr);
    define_native_function(vm.names.decodeURI, decode_uri, 1, attr);
    define_native_function(vm.names.encodeURIComponent, encode_uri_component, 1, attr);
    define_native_function(vm.names.decodeURIComponent, decode_uri_component, 1, attr);
    define_native_function(vm.names.escape, escape, 1, attr);
    define_native_function(vm.names.unescape, unescape, 1, attr);

    define_property(vm.names.NaN, js_nan(), 0);
    define_property(vm.names.Infinity, js_infinity(), 0);
    define_property(vm.names.undefined, js_undefined(), 0);

    define_property(vm.names.globalThis, this, attr);
    define_property(vm.names.console, heap().allocate<ConsoleObject>(*this, *this), attr);
    define_property(vm.names.Math, heap().allocate<MathObject>(*this, *this), attr);
    define_property(vm.names.JSON, heap().allocate<JSONObject>(*this, *this), attr);
    define_property(vm.names.Reflect, heap().allocate<ReflectObject>(*this, *this), attr);

    add_constructor(vm.names.Array, m_array_constructor, m_array_prototype);
    add_constructor(vm.names.ArrayBuffer, m_array_buffer_constructor, m_array_buffer_prototype);
    add_constructor(vm.names.BigInt, m_bigint_constructor, m_bigint_prototype);
    add_constructor(vm.names.Boolean, m_boolean_constructor, m_boolean_prototype);
    add_constructor(vm.names.Date, m_date_constructor, m_date_prototype);
    add_constructor(vm.names.Error, m_error_constructor, m_error_prototype);
    add_constructor(vm.names.Function, m_function_constructor, m_function_prototype);
    add_constructor(vm.names.Number, m_number_constructor, m_number_prototype);
    add_constructor(vm.names.Object, m_object_constructor, m_object_prototype);
    add_constructor(vm.names.Promise, m_promise_constructor, m_promise_prototype);
    add_constructor(vm.names.Proxy, m_proxy_constructor, nullptr);
    add_constructor(vm.names.RegExp, m_regexp_constructor, m_regexp_prototype);
    add_constructor(vm.names.String, m_string_constructor, m_string_prototype);
    add_constructor(vm.names.Symbol, m_symbol_constructor, m_symbol_prototype);

    initialize_constructor(vm.names.TypedArray, m_typed_array_constructor, m_typed_array_prototype);

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, ArrayType) \
    add_constructor(vm.names.ClassName, m_##snake_name##_constructor, m_##snake_name##_prototype);
    JS_ENUMERATE_ERROR_SUBCLASSES
    JS_ENUMERATE_TYPED_ARRAYS
#undef __JS_ENUMERATE
}

GlobalObject::~GlobalObject()
{
}

void GlobalObject::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);

    visitor.visit(m_empty_object_shape);
    visitor.visit(m_new_object_shape);
    visitor.visit(m_new_script_function_prototype_object_shape);
    visitor.visit(m_proxy_constructor);

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, ArrayType) \
    visitor.visit(m_##snake_name##_constructor);                                         \
    visitor.visit(m_##snake_name##_prototype);
    JS_ENUMERATE_ERROR_SUBCLASSES
    JS_ENUMERATE_BUILTIN_TYPES
#undef __JS_ENUMERATE

#define __JS_ENUMERATE(ClassName, snake_name) \
    visitor.visit(m_##snake_name##_prototype);
    JS_ENUMERATE_ITERATOR_PROTOTYPES
#undef __JS_ENUMERATE
}

JS_DEFINE_NATIVE_FUNCTION(GlobalObject::gc)
{
    dbgln("Forced garbage collection requested!");
    vm.heap().collect_garbage();
    return js_undefined();
}

JS_DEFINE_NATIVE_FUNCTION(GlobalObject::is_nan)
{
    auto number = vm.argument(0).to_number(global_object);
    if (vm.exception())
        return {};
    return Value(number.is_nan());
}

JS_DEFINE_NATIVE_FUNCTION(GlobalObject::is_finite)
{
    auto number = vm.argument(0).to_number(global_object);
    if (vm.exception())
        return {};
    return Value(number.is_finite_number());
}

JS_DEFINE_NATIVE_FUNCTION(GlobalObject::parse_float)
{
    if (vm.argument(0).is_number())
        return vm.argument(0);
    auto input_string = vm.argument(0).to_string(global_object);
    if (vm.exception())
        return {};
    auto trimmed_string = input_string.trim_whitespace(TrimMode::Left);
    for (size_t length = trimmed_string.length(); length > 0; --length) {
        // This can't throw, so no exception check is fine.
        auto number = Value(js_string(vm, trimmed_string.substring(0, length))).to_number(global_object);
        if (!number.is_nan())
            return number;
    }
    return js_nan();
}

JS_DEFINE_NATIVE_FUNCTION(GlobalObject::parse_int)
{
    // 18.2.5 parseInt ( string, radix )
    auto input_string = vm.argument(0).to_string(global_object);
    if (vm.exception())
        return {};

    // FIXME: There's a bunch of unnecessary string copying here.
    double sign = 1;
    auto s = input_string.trim_whitespace(TrimMode::Left);
    if (!s.is_empty() && s[0] == '-')
        sign = -1;
    if (!s.is_empty() && (s[0] == '+' || s[0] == '-'))
        s = s.substring(1, s.length() - 1);

    auto radix = vm.argument(1).to_i32(global_object);
    if (vm.exception())
        return {};

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

Optional<Variable> GlobalObject::get_from_scope(const FlyString& name) const
{
    auto value = get(name);
    if (value.is_empty())
        return {};
    return Variable { value, DeclarationKind::Var };
}

void GlobalObject::put_to_scope(const FlyString& name, Variable variable)
{
    put(name, variable.value);
}

bool GlobalObject::has_this_binding() const
{
    return true;
}

Value GlobalObject::get_this_binding(GlobalObject&) const
{
    return Value(this);
}

JS_DEFINE_NATIVE_FUNCTION(GlobalObject::eval)
{
    if (!vm.argument(0).is_string())
        return vm.argument(0);
    auto& code_string = vm.argument(0).as_string();
    JS::Parser parser { JS::Lexer { code_string.string() } };
    auto program = parser.parse_program();

    if (parser.has_errors()) {
        auto& error = parser.errors()[0];
        vm.throw_exception<SyntaxError>(global_object, error.to_string());
        return {};
    }

    auto& caller_frame = vm.call_stack().at(vm.call_stack().size() - 2);
    TemporaryChange scope_change(vm.call_frame().scope, caller_frame->scope);

    vm.interpreter().execute_statement(global_object, program);
    if (vm.exception())
        return {};
    return vm.last_value().value_or(js_undefined());
}

// 19.2.6.1.1 Encode ( string, unescapedSet )
static String encode([[maybe_unused]] JS::GlobalObject& global_object, const String& string, StringView unescaped_set)
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

// 19.2.6.1.2 Decode ( string, reservedSet )
static String decode(JS::GlobalObject& global_object, const String& string, StringView reserved_set)
{
    StringBuilder decoded_builder;
    auto expected_continuation_bytes = 0;
    for (size_t k = 0; k < string.length(); k++) {
        auto code_unit = string[k];
        if (code_unit != '%') {
            if (expected_continuation_bytes > 0) {
                global_object.vm().throw_exception<URIError>(global_object, ErrorType::URIMalformed);
                return {};
            }
            decoded_builder.append(code_unit);
            continue;
        }
        if (k + 2 >= string.length()) {
            global_object.vm().throw_exception<URIError>(global_object, ErrorType::URIMalformed);
            return {};
        }
        auto first_digit = decode_hex_digit(string[k + 1]);
        if (first_digit >= 16) {
            global_object.vm().throw_exception<URIError>(global_object, ErrorType::URIMalformed);
            return {};
        }
        auto second_digit = decode_hex_digit(string[k + 2]);
        if (second_digit >= 16) {
            global_object.vm().throw_exception<URIError>(global_object, ErrorType::URIMalformed);
            return {};
        }
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
        if (leading_ones == 1 || leading_ones > 4) {
            global_object.vm().throw_exception<URIError>(global_object, ErrorType::URIMalformed);
            return {};
        }
        decoded_builder.append(decoded_code_unit);
        expected_continuation_bytes = leading_ones - 1;
    }
    return decoded_builder.build();
}

JS_DEFINE_NATIVE_FUNCTION(GlobalObject::encode_uri)
{
    auto uri_string = vm.argument(0).to_string(global_object);
    if (vm.exception())
        return {};
    auto encoded = encode(global_object, uri_string, ";/?:@&=+$,abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_.!~*'()#"sv);
    if (vm.exception())
        return {};
    return js_string(vm, move(encoded));
}

JS_DEFINE_NATIVE_FUNCTION(GlobalObject::decode_uri)
{
    auto uri_string = vm.argument(0).to_string(global_object);
    if (vm.exception())
        return {};
    auto decoded = decode(global_object, uri_string, ";/?:@&=+$,#"sv);
    if (vm.exception())
        return {};
    return js_string(vm, move(decoded));
}

JS_DEFINE_NATIVE_FUNCTION(GlobalObject::encode_uri_component)
{
    auto uri_string = vm.argument(0).to_string(global_object);
    if (vm.exception())
        return {};
    auto encoded = encode(global_object, uri_string, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_.!~*'()"sv);
    if (vm.exception())
        return {};
    return js_string(vm, move(encoded));
}

JS_DEFINE_NATIVE_FUNCTION(GlobalObject::decode_uri_component)
{
    auto uri_string = vm.argument(0).to_string(global_object);
    if (vm.exception())
        return {};
    auto decoded = decode(global_object, uri_string, ""sv);
    if (vm.exception())
        return {};
    return js_string(vm, move(decoded));
}

JS_DEFINE_NATIVE_FUNCTION(GlobalObject::escape)
{
    auto string = vm.argument(0).to_string(global_object);
    if (vm.exception())
        return {};
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

JS_DEFINE_NATIVE_FUNCTION(GlobalObject::unescape)
{
    auto string = vm.argument(0).to_string(global_object);
    if (vm.exception())
        return {};
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
