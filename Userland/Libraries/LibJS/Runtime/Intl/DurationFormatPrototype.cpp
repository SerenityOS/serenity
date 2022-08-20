/*
 * Copyright (c) 2022, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/DurationFormatPrototype.h>

namespace JS::Intl {

// 1.4 Properties of the Intl.DurationFormat Prototype Object, https://tc39.es/proposal-intl-duration-format/#sec-properties-of-intl-durationformat-prototype-object
DurationFormatPrototype::DurationFormatPrototype(Realm& realm)
    : PrototypeObject(*realm.global_object().object_prototype())
{
}

void DurationFormatPrototype::initialize(Realm& realm)
{
    Object::initialize(realm);

    auto& vm = this->vm();

    // 1.4.2 Intl.DurationFormat.prototype [ @@toStringTag ], https://tc39.es/proposal-intl-duration-format/#sec-Intl.DurationFormat.prototype-@@tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm, "Intl.DurationFormat"), Attribute::Configurable);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.format, format, 1, attr);
    define_native_function(vm.names.formatToParts, format_to_parts, 1, attr);
    define_native_function(vm.names.resolvedOptions, resolved_options, 0, attr);
}

// 1.4.3 Intl.DurationFormat.prototype.format ( duration ), https://tc39.es/proposal-intl-duration-format/#sec-Intl.DurationFormat.prototype.format
JS_DEFINE_NATIVE_FUNCTION(DurationFormatPrototype::format)
{
    // 1. Let df be this value.
    // 2. Perform ? RequireInternalSlot(df, [[InitializedDurationFormat]]).
    auto* duration_format = TRY(typed_this_object(global_object));

    // 3. Let record be ? ToDurationRecord(duration).
    auto record = TRY(to_duration_record(vm, vm.argument(0)));

    // 4. If IsValidDurationRecord(record) is false, throw a RangeError exception.
    if (!is_valid_duration_record(record))
        return vm.throw_completion<RangeError>(ErrorType::TemporalInvalidDurationLikeObject);

    // 5. Let formatted be ? PartitionDurationFormatPattern(df, record).
    auto formatted = TRY(partition_duration_format_pattern(vm, *duration_format, record));

    // 6. Let result be a new empty String.
    StringBuilder result;

    // 7. For each element part in formatted, in List order, do
    for (auto const& part : formatted) {
        // a. Set result to the string-concatenation of result and part.[[Value]].
        result.append(part.value);
    }

    // 8. Return result.
    return js_string(vm, result.build());
}

// 1.4.4 Intl.DurationFormat.prototype.formatToParts ( duration ), https://tc39.es/proposal-intl-duration-format/#sec-Intl.DurationFormat.prototype.formatToParts
JS_DEFINE_NATIVE_FUNCTION(DurationFormatPrototype::format_to_parts)
{
    auto& realm = *global_object.associated_realm();

    // 1. Let df be this value.
    // 2. Perform ? RequireInternalSlot(df, [[InitializedDurationFormat]]).
    auto* duration_format = TRY(typed_this_object(global_object));

    // 3. Let record be ? ToDurationRecord(duration).
    auto record = TRY(to_duration_record(vm, vm.argument(0)));

    // 4. If IsValidDurationRecord(record) is false, throw a RangeError exception.
    if (!is_valid_duration_record(record))
        return vm.throw_completion<RangeError>(ErrorType::TemporalInvalidDurationLikeObject);

    // 5. Let formatted be ? PartitionDurationFormatPattern(df, record).
    auto formatted = TRY(partition_duration_format_pattern(vm, *duration_format, record));

    // 6. Let result be ! ArrayCreate(0).
    auto* result = MUST(Array::create(realm, 0));

    // 7. Let n be 0.
    // 8. For each element part in formatted, in List order, do
    for (size_t n = 0; n < formatted.size(); ++n) {
        auto const& part = formatted[n];

        // a. Let obj be ! OrdinaryObjectCreate(%ObjectPrototype%).
        auto* object = Object::create(realm, global_object.object_prototype());

        // b. Perform ! CreateDataPropertyOrThrow(obj, "type", part.[[Type]]).
        MUST(object->create_data_property_or_throw(vm.names.type, js_string(vm, part.type)));

        // c. Perform ! CreateDataPropertyOrThrow(obj, "value", part.[[Value]]).
        MUST(object->create_data_property_or_throw(vm.names.value, js_string(vm, part.value)));

        // d. Perform ! CreateDataPropertyOrThrow(result, ! ToString(n), obj).
        MUST(result->create_data_property_or_throw(n, object));

        // e. Increment n by 1.
    }

    // 9. Return result.
    return result;
}

// 1.4.5 Intl.DurationFormat.prototype.resolvedOptions ( ), https://tc39.es/proposal-intl-duration-format/#sec-Intl.DurationFormat.prototype.resolvedOptions
JS_DEFINE_NATIVE_FUNCTION(DurationFormatPrototype::resolved_options)
{
    auto& realm = *global_object.associated_realm();

    // 1. Let df be the this value.
    // 2. Perform ? RequireInternalSlot(df, [[InitializedDurationFormat]]).
    auto* duration_format = TRY(typed_this_object(global_object));

    // 3. Let options be ! OrdinaryObjectCreate(%Object.prototype%).
    auto* options = Object::create(realm, global_object.object_prototype());

    // 4. For each row of Table 2, except the header row, in table order, do
    //     a. Let p be the Property value of the current row.
    //     b. Let v be the value of df's internal slot whose name is the Internal Slot value of the current row.
    //     c. Assert: v is not undefined.
    //     d. Perform ! CreateDataPropertyOrThrow(options, p, v).
    MUST(options->create_data_property_or_throw(vm.names.locale, js_string(vm, duration_format->locale())));
    MUST(options->create_data_property_or_throw(vm.names.style, js_string(vm, duration_format->style_string())));
    MUST(options->create_data_property_or_throw(vm.names.years, js_string(vm, duration_format->years_style_string())));
    MUST(options->create_data_property_or_throw(vm.names.yearsDisplay, js_string(vm, duration_format->years_display_string())));
    MUST(options->create_data_property_or_throw(vm.names.months, js_string(vm, duration_format->months_style_string())));
    MUST(options->create_data_property_or_throw(vm.names.monthsDisplay, js_string(vm, duration_format->months_display_string())));
    MUST(options->create_data_property_or_throw(vm.names.weeks, js_string(vm, duration_format->weeks_style_string())));
    MUST(options->create_data_property_or_throw(vm.names.weeksDisplay, js_string(vm, duration_format->weeks_display_string())));
    MUST(options->create_data_property_or_throw(vm.names.days, js_string(vm, duration_format->days_style_string())));
    MUST(options->create_data_property_or_throw(vm.names.daysDisplay, js_string(vm, duration_format->days_display_string())));
    MUST(options->create_data_property_or_throw(vm.names.hours, js_string(vm, duration_format->hours_style_string())));
    MUST(options->create_data_property_or_throw(vm.names.hoursDisplay, js_string(vm, duration_format->hours_display_string())));
    MUST(options->create_data_property_or_throw(vm.names.minutes, js_string(vm, duration_format->minutes_style_string())));
    MUST(options->create_data_property_or_throw(vm.names.minutesDisplay, js_string(vm, duration_format->minutes_display_string())));
    MUST(options->create_data_property_or_throw(vm.names.seconds, js_string(vm, duration_format->seconds_style_string())));
    MUST(options->create_data_property_or_throw(vm.names.secondsDisplay, js_string(vm, duration_format->seconds_display_string())));
    MUST(options->create_data_property_or_throw(vm.names.milliseconds, js_string(vm, duration_format->milliseconds_style_string())));
    MUST(options->create_data_property_or_throw(vm.names.millisecondsDisplay, js_string(vm, duration_format->milliseconds_display_string())));
    MUST(options->create_data_property_or_throw(vm.names.microseconds, js_string(vm, duration_format->microseconds_style_string())));
    MUST(options->create_data_property_or_throw(vm.names.microsecondsDisplay, js_string(vm, duration_format->microseconds_display_string())));
    MUST(options->create_data_property_or_throw(vm.names.nanoseconds, js_string(vm, duration_format->nanoseconds_style_string())));
    MUST(options->create_data_property_or_throw(vm.names.nanosecondsDisplay, js_string(vm, duration_format->nanoseconds_display_string())));
    MUST(options->create_data_property_or_throw(vm.names.fractionalDigits, duration_format->has_fractional_digits() ? Value(duration_format->fractional_digits()) : js_undefined()));
    MUST(options->create_data_property_or_throw(vm.names.numberingSystem, js_string(vm, duration_format->numbering_system())));

    // 5. Return options.
    return options;
}

}
