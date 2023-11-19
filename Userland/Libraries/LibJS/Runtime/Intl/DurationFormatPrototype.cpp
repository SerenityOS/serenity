/*
 * Copyright (c) 2022, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/DurationFormatPrototype.h>

namespace JS::Intl {

JS_DEFINE_ALLOCATOR(DurationFormatPrototype);

// 1.4 Properties of the Intl.DurationFormat Prototype Object, https://tc39.es/proposal-intl-duration-format/#sec-properties-of-intl-durationformat-prototype-object
DurationFormatPrototype::DurationFormatPrototype(Realm& realm)
    : PrototypeObject(realm.intrinsics().object_prototype())
{
}

void DurationFormatPrototype::initialize(Realm& realm)
{
    Base::initialize(realm);

    auto& vm = this->vm();

    // 1.4.2 Intl.DurationFormat.prototype [ @@toStringTag ], https://tc39.es/proposal-intl-duration-format/#sec-Intl.DurationFormat.prototype-@@tostringtag
    define_direct_property(vm.well_known_symbol_to_string_tag(), PrimitiveString::create(vm, "Intl.DurationFormat"_string), Attribute::Configurable);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(realm, vm.names.format, format, 1, attr);
    define_native_function(realm, vm.names.formatToParts, format_to_parts, 1, attr);
    define_native_function(realm, vm.names.resolvedOptions, resolved_options, 0, attr);
}

// 1.4.3 Intl.DurationFormat.prototype.format ( duration ), https://tc39.es/proposal-intl-duration-format/#sec-Intl.DurationFormat.prototype.format
JS_DEFINE_NATIVE_FUNCTION(DurationFormatPrototype::format)
{
    // 1. Let df be this value.
    // 2. Perform ? RequireInternalSlot(df, [[InitializedDurationFormat]]).
    auto duration_format = TRY(typed_this_object(vm));

    // 3. Let record be ? ToDurationRecord(duration).
    auto record = TRY(to_duration_record(vm, vm.argument(0)));

    // 4. Let parts be PartitionDurationFormatPattern(df, record).
    auto parts = partition_duration_format_pattern(vm, duration_format, record);

    // 5. Let result be a new empty String.
    StringBuilder result;

    // 6. For each Record { [[Type]], [[Value]] } part in parts, do
    for (auto const& part : parts) {
        // a. Set result to the string-concatenation of result and part.[[Value]].
        result.append(part.value);
    }

    // 7. Return result.
    return PrimitiveString::create(vm, MUST(result.to_string()));
}

// 1.4.4 Intl.DurationFormat.prototype.formatToParts ( duration ), https://tc39.es/proposal-intl-duration-format/#sec-Intl.DurationFormat.prototype.formatToParts
JS_DEFINE_NATIVE_FUNCTION(DurationFormatPrototype::format_to_parts)
{
    auto& realm = *vm.current_realm();

    // 1. Let df be this value.
    // 2. Perform ? RequireInternalSlot(df, [[InitializedDurationFormat]]).
    auto duration_format = TRY(typed_this_object(vm));

    // 3. Let record be ? ToDurationRecord(duration).
    auto record = TRY(to_duration_record(vm, vm.argument(0)));

    // 4. Let parts be PartitionDurationFormatPattern(df, record).
    auto parts = partition_duration_format_pattern(vm, duration_format, record);

    // 5. Let result be ! ArrayCreate(0).
    auto result = MUST(Array::create(realm, 0));

    // 6. Let n be 0.
    // 7. For each { [[Type]], [[Value]] } part in parts, do
    for (size_t n = 0; n < parts.size(); ++n) {
        auto const& part = parts[n];

        // a. Let obj be OrdinaryObjectCreate(%ObjectPrototype%).
        auto object = Object::create(realm, realm.intrinsics().object_prototype());

        // b. Perform ! CreateDataPropertyOrThrow(obj, "type", part.[[Type]]).
        MUST(object->create_data_property_or_throw(vm.names.type, PrimitiveString::create(vm, part.type)));

        // c. Perform ! CreateDataPropertyOrThrow(obj, "value", part.[[Value]]).
        MUST(object->create_data_property_or_throw(vm.names.value, PrimitiveString::create(vm, part.value)));

        // d. Perform ! CreateDataPropertyOrThrow(result, ! ToString(n), obj).
        MUST(result->create_data_property_or_throw(n, object));

        // e. Increment n by 1.
    }

    // 8. Return result.
    return result;
}

// 1.4.5 Intl.DurationFormat.prototype.resolvedOptions ( ), https://tc39.es/proposal-intl-duration-format/#sec-Intl.DurationFormat.prototype.resolvedOptions
JS_DEFINE_NATIVE_FUNCTION(DurationFormatPrototype::resolved_options)
{
    auto& realm = *vm.current_realm();

    // 1. Let df be the this value.
    // 2. Perform ? RequireInternalSlot(df, [[InitializedDurationFormat]]).
    auto duration_format = TRY(typed_this_object(vm));

    // 3. Let options be OrdinaryObjectCreate(%Object.prototype%).
    auto options = Object::create(realm, realm.intrinsics().object_prototype());

    // 4. For each row of Table 2, except the header row, in table order, do
    //     a. Let p be the Property value of the current row.
    //     b. Let v be the value of df's internal slot whose name is the Internal Slot value of the current row.
    //     c. Assert: v is not undefined.
    //     d. Perform ! CreateDataPropertyOrThrow(options, p, v).
    MUST(options->create_data_property_or_throw(vm.names.locale, PrimitiveString::create(vm, duration_format->locale())));
    MUST(options->create_data_property_or_throw(vm.names.style, PrimitiveString::create(vm, duration_format->style_string())));
    MUST(options->create_data_property_or_throw(vm.names.years, PrimitiveString::create(vm, duration_format->years_style_string())));
    MUST(options->create_data_property_or_throw(vm.names.yearsDisplay, PrimitiveString::create(vm, duration_format->years_display_string())));
    MUST(options->create_data_property_or_throw(vm.names.months, PrimitiveString::create(vm, duration_format->months_style_string())));
    MUST(options->create_data_property_or_throw(vm.names.monthsDisplay, PrimitiveString::create(vm, duration_format->months_display_string())));
    MUST(options->create_data_property_or_throw(vm.names.weeks, PrimitiveString::create(vm, duration_format->weeks_style_string())));
    MUST(options->create_data_property_or_throw(vm.names.weeksDisplay, PrimitiveString::create(vm, duration_format->weeks_display_string())));
    MUST(options->create_data_property_or_throw(vm.names.days, PrimitiveString::create(vm, duration_format->days_style_string())));
    MUST(options->create_data_property_or_throw(vm.names.daysDisplay, PrimitiveString::create(vm, duration_format->days_display_string())));
    MUST(options->create_data_property_or_throw(vm.names.hours, PrimitiveString::create(vm, duration_format->hours_style_string())));
    MUST(options->create_data_property_or_throw(vm.names.hoursDisplay, PrimitiveString::create(vm, duration_format->hours_display_string())));
    MUST(options->create_data_property_or_throw(vm.names.minutes, PrimitiveString::create(vm, duration_format->minutes_style_string())));
    MUST(options->create_data_property_or_throw(vm.names.minutesDisplay, PrimitiveString::create(vm, duration_format->minutes_display_string())));
    MUST(options->create_data_property_or_throw(vm.names.seconds, PrimitiveString::create(vm, duration_format->seconds_style_string())));
    MUST(options->create_data_property_or_throw(vm.names.secondsDisplay, PrimitiveString::create(vm, duration_format->seconds_display_string())));
    MUST(options->create_data_property_or_throw(vm.names.milliseconds, PrimitiveString::create(vm, duration_format->milliseconds_style_string())));
    MUST(options->create_data_property_or_throw(vm.names.millisecondsDisplay, PrimitiveString::create(vm, duration_format->milliseconds_display_string())));
    MUST(options->create_data_property_or_throw(vm.names.microseconds, PrimitiveString::create(vm, duration_format->microseconds_style_string())));
    MUST(options->create_data_property_or_throw(vm.names.microsecondsDisplay, PrimitiveString::create(vm, duration_format->microseconds_display_string())));
    MUST(options->create_data_property_or_throw(vm.names.nanoseconds, PrimitiveString::create(vm, duration_format->nanoseconds_style_string())));
    MUST(options->create_data_property_or_throw(vm.names.nanosecondsDisplay, PrimitiveString::create(vm, duration_format->nanoseconds_display_string())));
    MUST(options->create_data_property_or_throw(vm.names.fractionalDigits, duration_format->has_fractional_digits() ? Value(duration_format->fractional_digits()) : js_undefined()));
    MUST(options->create_data_property_or_throw(vm.names.numberingSystem, PrimitiveString::create(vm, duration_format->numbering_system())));

    // 5. Return options.
    return options;
}

}
