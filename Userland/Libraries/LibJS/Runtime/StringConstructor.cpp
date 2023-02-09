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

StringConstructor::StringConstructor(Realm& realm)
    : NativeFunction(realm.vm().names.String.as_string(), *realm.intrinsics().function_prototype())
{
}

ThrowCompletionOr<void> StringConstructor::initialize(Realm& realm)
{
    auto& vm = this->vm();
    MUST_OR_THROW_OOM(NativeFunction::initialize(realm));

    // 22.1.2.3 String.prototype, https://tc39.es/ecma262/#sec-string.prototype
    define_direct_property(vm.names.prototype, realm.intrinsics().string_prototype(), 0);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(realm, vm.names.raw, raw, 1, attr);
    define_native_function(realm, vm.names.fromCharCode, from_char_code, 1, attr);
    define_native_function(realm, vm.names.fromCodePoint, from_code_point, 1, attr);

    define_direct_property(vm.names.length, Value(1), Attribute::Configurable);

    return {};
}

// 22.1.1.1 String ( value ), https://tc39.es/ecma262/#sec-string-constructor-string-value
ThrowCompletionOr<Value> StringConstructor::call()
{
    auto& vm = this->vm();
    if (!vm.argument_count())
        return PrimitiveString::create(vm, String {});
    if (vm.argument(0).is_symbol())
        return PrimitiveString::create(vm, vm.argument(0).as_symbol().to_deprecated_string());
    return TRY(vm.argument(0).to_primitive_string(vm));
}

// 22.1.1.1 String ( value ), https://tc39.es/ecma262/#sec-string-constructor-string-value
ThrowCompletionOr<NonnullGCPtr<Object>> StringConstructor::construct(FunctionObject& new_target)
{
    auto& vm = this->vm();
    auto& realm = *vm.current_realm();

    PrimitiveString* primitive_string;
    if (!vm.argument_count())
        primitive_string = PrimitiveString::create(vm, String {});
    else
        primitive_string = TRY(vm.argument(0).to_primitive_string(vm));
    auto* prototype = TRY(get_prototype_from_constructor(vm, new_target, &Intrinsics::string_prototype));
    return MUST_OR_THROW_OOM(StringObject::create(realm, *primitive_string, *prototype));
}

// 22.1.2.4 String.raw ( template, ...substitutions ), https://tc39.es/ecma262/#sec-string.raw
JS_DEFINE_NATIVE_FUNCTION(StringConstructor::raw)
{
    auto* cooked = TRY(vm.argument(0).to_object(vm));
    auto raw_value = TRY(cooked->get(vm.names.raw));
    auto* raw = TRY(raw_value.to_object(vm));
    auto literal_segments = TRY(length_of_array_like(vm, *raw));

    if (literal_segments == 0)
        return PrimitiveString::create(vm, String {});

    auto const number_of_substituions = vm.argument_count() - 1;

    StringBuilder builder;
    for (size_t i = 0; i < literal_segments; ++i) {
        auto next_key = DeprecatedString::number(i);
        auto next_segment_value = TRY(raw->get(next_key));
        auto next_segment = TRY(next_segment_value.to_deprecated_string(vm));

        builder.append(next_segment);

        if (i + 1 == literal_segments)
            break;

        if (i < number_of_substituions) {
            auto next = vm.argument(i + 1);
            auto next_sub = TRY(next.to_deprecated_string(vm));
            builder.append(next_sub);
        }
    }
    return PrimitiveString::create(vm, builder.to_deprecated_string());
}

// 22.1.2.1 String.fromCharCode ( ...codeUnits ), https://tc39.es/ecma262/#sec-string.fromcharcode
JS_DEFINE_NATIVE_FUNCTION(StringConstructor::from_char_code)
{
    Utf16Data string;
    string.ensure_capacity(vm.argument_count());

    for (size_t i = 0; i < vm.argument_count(); ++i)
        string.append(TRY(vm.argument(i).to_u16(vm)));

    return PrimitiveString::create(vm, TRY(Utf16String::create(vm, move(string))));
}

// 22.1.2.2 String.fromCodePoint ( ...codePoints ), https://tc39.es/ecma262/#sec-string.fromcodepoint
JS_DEFINE_NATIVE_FUNCTION(StringConstructor::from_code_point)
{
    Utf16Data string;
    string.ensure_capacity(vm.argument_count()); // This will be an under-estimate if any code point is > 0xffff.

    for (size_t i = 0; i < vm.argument_count(); ++i) {
        auto next_code_point = TRY(vm.argument(i).to_number(vm));
        if (!next_code_point.is_integral_number())
            return vm.throw_completion<RangeError>(ErrorType::InvalidCodePoint, next_code_point.to_string_without_side_effects());
        auto code_point = TRY(next_code_point.to_i32(vm));
        if (code_point < 0 || code_point > 0x10FFFF)
            return vm.throw_completion<RangeError>(ErrorType::InvalidCodePoint, next_code_point.to_string_without_side_effects());

        TRY_OR_THROW_OOM(vm, code_point_to_utf16(string, static_cast<u32>(code_point)));
    }

    return PrimitiveString::create(vm, TRY(Utf16String::create(vm, move(string))));
}

}
