/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <AK/Utf16View.h>
#include <AK/Utf32View.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/StringConstructor.h>
#include <LibJS/Runtime/StringObject.h>
#include <LibJS/Runtime/Utf16String.h>

namespace JS {

StringConstructor::StringConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.String.as_string(), *global_object.function_prototype())
{
}

void StringConstructor::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    NativeFunction::initialize(global_object);

    // 22.1.2.3 String.prototype, https://tc39.es/ecma262/#sec-string.prototype
    define_direct_property(vm.names.prototype, global_object.string_prototype(), 0);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.raw, raw, 1, attr);
    define_native_function(vm.names.fromCharCode, from_char_code, 1, attr);
    define_native_function(vm.names.fromCodePoint, from_code_point, 1, attr);

    define_direct_property(vm.names.length, Value(1), Attribute::Configurable);
}

StringConstructor::~StringConstructor()
{
}

// 22.1.1.1 String ( value ), https://tc39.es/ecma262/#sec-string-constructor-string-value
ThrowCompletionOr<Value> StringConstructor::call()
{
    if (!vm().argument_count())
        return js_string(heap(), "");
    if (vm().argument(0).is_symbol())
        return js_string(heap(), vm().argument(0).as_symbol().to_string());
    return TRY(vm().argument(0).to_primitive_string(global_object()));
}

// 22.1.1.1 String ( value ), https://tc39.es/ecma262/#sec-string-constructor-string-value
ThrowCompletionOr<Object*> StringConstructor::construct(FunctionObject& new_target)
{
    auto& vm = global_object().vm();

    PrimitiveString* primitive_string;
    if (!vm.argument_count())
        primitive_string = js_string(vm, "");
    else
        primitive_string = TRY(vm.argument(0).to_primitive_string(global_object()));
    auto* prototype = TRY(get_prototype_from_constructor(global_object(), new_target, &GlobalObject::string_prototype));
    return StringObject::create(global_object(), *primitive_string, *prototype);
}

// 22.1.2.4 String.raw ( template, ...substitutions ), https://tc39.es/ecma262/#sec-string.raw
JS_DEFINE_NATIVE_FUNCTION(StringConstructor::raw)
{
    auto* cooked = TRY(vm.argument(0).to_object(global_object));
    auto raw_value = TRY(cooked->get(vm.names.raw));
    auto* raw = TRY(raw_value.to_object(global_object));
    auto literal_segments = TRY(length_of_array_like(global_object, *raw));

    if (literal_segments == 0)
        return js_string(vm, "");

    const auto number_of_substituions = vm.argument_count() - 1;

    StringBuilder builder;
    for (size_t i = 0; i < literal_segments; ++i) {
        auto next_key = String::number(i);
        auto next_segment_value = TRY(raw->get(next_key));
        auto next_segment = TRY(next_segment_value.to_string(global_object));

        builder.append(next_segment);

        if (i + 1 == literal_segments)
            break;

        if (i < number_of_substituions) {
            auto next = vm.argument(i + 1);
            auto next_sub = TRY(next.to_string(global_object));
            builder.append(next_sub);
        }
    }
    return js_string(vm, builder.build());
}

// 22.1.2.1 String.fromCharCode ( ...codeUnits ), https://tc39.es/ecma262/#sec-string.fromcharcode
JS_DEFINE_NATIVE_FUNCTION(StringConstructor::from_char_code)
{
    Vector<u16, 1> string;
    string.ensure_capacity(vm.argument_count());

    for (size_t i = 0; i < vm.argument_count(); ++i)
        string.append(TRY(vm.argument(i).to_u16(global_object)));

    return js_string(vm, Utf16String(move(string)));
}

// 22.1.2.2 String.fromCodePoint ( ...codePoints ), https://tc39.es/ecma262/#sec-string.fromcodepoint
JS_DEFINE_NATIVE_FUNCTION(StringConstructor::from_code_point)
{
    Vector<u16, 1> string;
    string.ensure_capacity(vm.argument_count()); // This will be an under-estimate if any code point is > 0xffff.

    for (size_t i = 0; i < vm.argument_count(); ++i) {
        auto next_code_point = TRY(vm.argument(i).to_number(global_object));
        if (!next_code_point.is_integral_number())
            return vm.throw_completion<RangeError>(global_object, ErrorType::InvalidCodePoint, next_code_point.to_string_without_side_effects());
        auto code_point = TRY(next_code_point.to_i32(global_object));
        if (code_point < 0 || code_point > 0x10FFFF)
            return vm.throw_completion<RangeError>(global_object, ErrorType::InvalidCodePoint, next_code_point.to_string_without_side_effects());

        AK::code_point_to_utf16(string, static_cast<u32>(code_point));
    }

    return js_string(vm, Utf16String(move(string)));
}

}
