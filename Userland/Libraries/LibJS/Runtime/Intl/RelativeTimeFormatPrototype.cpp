/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/RelativeTimeFormatPrototype.h>

namespace JS::Intl {

// 17.4 Properties of the Intl.RelativeTimeFormat Prototype Object, https://tc39.es/ecma402/#sec-properties-of-intl-relativetimeformat-prototype-object
RelativeTimeFormatPrototype::RelativeTimeFormatPrototype(GlobalObject& global_object)
    : PrototypeObject(*global_object.object_prototype())
{
}

void RelativeTimeFormatPrototype::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);

    auto& vm = this->vm();

    // 17.4.2 Intl.RelativeTimeFormat.prototype[ @@toStringTag ], https://tc39.es/ecma402/#sec-Intl.RelativeTimeFormat.prototype-toStringTag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm, "Intl.RelativeTimeFormat"sv), Attribute::Configurable);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.format, format, 2, attr);
    define_native_function(vm.names.formatToParts, format_to_parts, 2, attr);
    define_native_function(vm.names.resolvedOptions, resolved_options, 0, attr);
}

// 17.4.3 Intl.RelativeTimeFormat.prototype.format ( value, unit ), https://tc39.es/ecma402/#sec-Intl.RelativeTimeFormat.prototype.format
JS_DEFINE_NATIVE_FUNCTION(RelativeTimeFormatPrototype::format)
{
    // 1. Let relativeTimeFormat be the this value.
    // 2. Perform ? RequireInternalSlot(relativeTimeFormat, [[InitializedRelativeTimeFormat]]).
    auto* relative_time_format = TRY(typed_this_object(global_object));

    // 3. Let value be ? ToNumber(value).
    auto value = TRY(vm.argument(0).to_number(global_object));

    // 4. Let unit be ? ToString(unit).
    auto unit = TRY(vm.argument(1).to_string(global_object));

    // 5. Return ? FormatRelativeTime(relativeTimeFormat, value, unit).
    auto formatted = TRY(format_relative_time(global_object, *relative_time_format, value.as_double(), unit));
    return js_string(vm, move(formatted));
}

// 17.4.4 Intl.RelativeTimeFormat.prototype.formatToParts ( value, unit ), https://tc39.es/ecma402/#sec-Intl.RelativeTimeFormat.prototype.formatToParts
JS_DEFINE_NATIVE_FUNCTION(RelativeTimeFormatPrototype::format_to_parts)
{
    // 1. Let relativeTimeFormat be the this value.
    // 2. Perform ? RequireInternalSlot(relativeTimeFormat, [[InitializedRelativeTimeFormat]]).
    auto* relative_time_format = TRY(typed_this_object(global_object));

    // 3. Let value be ? ToNumber(value).
    auto value = TRY(vm.argument(0).to_number(global_object));

    // 4. Let unit be ? ToString(unit).
    auto unit = TRY(vm.argument(1).to_string(global_object));

    // 5. Return ? FormatRelativeTimeToParts(relativeTimeFormat, value, unit).
    return TRY(format_relative_time_to_parts(global_object, *relative_time_format, value.as_double(), unit));
}

// 17.4.5 Intl.RelativeTimeFormat.prototype.resolvedOptions ( ), https://tc39.es/ecma402/#sec-intl.relativetimeformat.prototype.resolvedoptions
JS_DEFINE_NATIVE_FUNCTION(RelativeTimeFormatPrototype::resolved_options)
{
    // 1. Let relativeTimeFormat be the this value.
    // 2. Perform ? RequireInternalSlot(relativeTimeFormat, [[InitializedRelativeTimeFormat]]).
    auto* relative_time_format = TRY(typed_this_object(global_object));

    // 3. Let options be ! OrdinaryObjectCreate(%Object.prototype%).
    auto* options = Object::create(global_object, global_object.object_prototype());

    // 4. For each row of Table 15, except the header row, in table order, do
    //     a. Let p be the Property value of the current row.
    //     b. Let v be the value of relativeTimeFormat's internal slot whose name is the Internal Slot value of the current row.
    //     c. Assert: v is not undefined.
    //     d. Perform ! CreateDataPropertyOrThrow(options, p, v).
    MUST(options->create_data_property_or_throw(vm.names.locale, js_string(vm, relative_time_format->locale())));
    MUST(options->create_data_property_or_throw(vm.names.style, js_string(vm, relative_time_format->style_string())));
    MUST(options->create_data_property_or_throw(vm.names.numeric, js_string(vm, relative_time_format->numeric_string())));
    MUST(options->create_data_property_or_throw(vm.names.numberingSystem, js_string(vm, relative_time_format->numbering_system())));

    // 5. Return options.
    return options;
}

}
