/*
 * Copyright (c) 2022, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/DurationFormatPrototype.h>

namespace JS::Intl {

// 1.4 Properties of the Intl.DurationFormat Prototype Object, https://tc39.es/proposal-intl-duration-format/#sec-properties-of-intl-durationformat-prototype-object
DurationFormatPrototype::DurationFormatPrototype(GlobalObject& global_object)
    : PrototypeObject(*global_object.object_prototype())
{
}

void DurationFormatPrototype::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);

    auto& vm = this->vm();

    // 1.4.2 Intl.DurationFormat.prototype [ @@toStringTag ], https://tc39.es/proposal-intl-duration-format/#sec-Intl.DurationFormat.prototype-@@tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm, "Intl.DurationFormat"), Attribute::Configurable);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.resolvedOptions, resolved_options, 0, attr);
}

// 1.4.5 Intl.DurationFormat.prototype.resolvedOptions ( ), https://tc39.es/proposal-intl-duration-format/#sec-Intl.DurationFormat.prototype.resolvedOptions
JS_DEFINE_NATIVE_FUNCTION(DurationFormatPrototype::resolved_options)
{
    // 1. Let df be the this value.
    // 2. Perform ? RequireInternalSlot(df, [[InitializedDurationFormat]]).
    auto* duration_format = TRY(typed_this_object(global_object));

    // 3. Let options be ! OrdinaryObjectCreate(%Object.prototype%).
    auto* options = Object::create(global_object, global_object.object_prototype());

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
