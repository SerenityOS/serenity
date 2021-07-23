/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/AbstractOperations.h>
#include <LibJS/Runtime/Temporal/Calendar.h>
#include <LibJS/Runtime/Temporal/CalendarPrototype.h>
#include <LibJS/Runtime/Temporal/PlainDate.h>

namespace JS::Temporal {

// 12.4 Properties of the Temporal.Calendar Prototype Object, https://tc39.es/proposal-temporal/#sec-properties-of-the-temporal-calendar-prototype-object
CalendarPrototype::CalendarPrototype(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void CalendarPrototype::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);

    auto& vm = this->vm();

    // 12.4.2 Temporal.Calendar.prototype[ @@toStringTag ], https://tc39.es/proposal-temporal/#sec-temporal.calendar.prototype-@@tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm.heap(), "Temporal.Calendar"), Attribute::Configurable);

    define_native_accessor(vm.names.id, id_getter, {}, Attribute::Configurable);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.dateFromFields, date_from_fields, 2, attr);
    define_native_function(vm.names.year, year, 1, attr);
    define_native_function(vm.names.month, month, 1, attr);
    define_native_function(vm.names.toString, to_string, 0, attr);
    define_native_function(vm.names.toJSON, to_json, 0, attr);
}

static Calendar* typed_this(GlobalObject& global_object)
{
    auto& vm = global_object.vm();
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};
    if (!is<Calendar>(this_object)) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotA, "Temporal.Calendar");
        return {};
    }
    return static_cast<Calendar*>(this_object);
}

// 12.4.3 get Temporal.Calendar.prototype.id, https://tc39.es/proposal-temporal/#sec-get-temporal.calendar.prototype.id
JS_DEFINE_NATIVE_FUNCTION(CalendarPrototype::id_getter)
{
    // 1. Let calendar be the this value.
    auto calendar = vm.this_value(global_object);

    // 2. Return ? ToString(calendar).
    return js_string(vm, calendar.to_string(global_object));
}

// 12.4.4 Temporal.Calendar.prototype.dateFromFields ( fields, options ), https://tc39.es/proposal-temporal/#sec-temporal.calendar.prototype.datefromfields
// NOTE: This is the minimum dateFromFields implementation for engines without ECMA-402.
JS_DEFINE_NATIVE_FUNCTION(CalendarPrototype::date_from_fields)
{
    // 1. Let calendar be the this value.
    // 2. Perform ? RequireInternalSlot(calendar, [[InitializedTemporalCalendar]]).
    auto* calendar = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Assert: calendar.[[Identifier]] is "iso8601".
    VERIFY(calendar->identifier() == "iso8601"sv);

    // 4. If Type(fields) is not Object, throw a TypeError exception.
    auto fields = vm.argument(0);
    if (!fields.is_object()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAnObject, fields.to_string_without_side_effects());
        return {};
    }

    // 5. Set options to ? GetOptionsObject(options).
    auto* options = get_options_object(global_object, vm.argument(1));
    if (vm.exception())
        return {};

    // 6. Let result be ? ISODateFromFields(fields, options).
    auto result = iso_date_from_fields(global_object, fields.as_object(), *options);
    if (vm.exception())
        return {};

    // 7. Return ? CreateTemporalDate(result.[[Year]], result.[[Month]], result.[[Day]], calendar).
    return create_temporal_date(global_object, result->year, result->month, result->day, *calendar);
}

// 12.4.9 Temporal.Calendar.prototype.year ( temporalDateLike ), https://tc39.es/proposal-temporal/#sec-temporal.calendar.prototype.year
// NOTE: This is the minimum year implementation for engines without ECMA-402.
JS_DEFINE_NATIVE_FUNCTION(CalendarPrototype::year)
{
    // 1. Let calendar be the this value.
    // 2. Perform ? RequireInternalSlot(calendar, [[InitializedTemporalCalendar]]).
    auto* calendar = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Assert: calendar.[[Identifier]] is "iso8601".
    VERIFY(calendar->identifier() == "iso8601"sv);

    auto temporal_date_like = vm.argument(0);
    // 4. If Type(temporalDateLike) is not Object or temporalDateLike does not have an [[InitializedTemporalDate]] or [[InitializedTemporalYearMonth]] internal slot, then
    // TODO PlainYearMonth objects
    if (!temporal_date_like.is_object() || !is<PlainDate>(temporal_date_like.as_object())) {
        // a. Set temporalDateLike to ? ToTemporalDate(temporalDateLike).
        temporal_date_like = to_temporal_date(global_object, temporal_date_like);
        if (vm.exception())
            return {};
    }

    // 5. Return ! ISOYear(temporalDateLike).
    return Value(iso_year(temporal_date_like.as_object()));
}

// 12.4.10 Temporal.Calendar.prototype.month ( temporalDateLike ), https://tc39.es/proposal-temporal/#sec-temporal.calendar.prototype.month
// NOTE: This is the minimum month implementation for engines without ECMA-402.
JS_DEFINE_NATIVE_FUNCTION(CalendarPrototype::month)
{
    // 1. Let calendar be the this value.
    // 2. Perform ? RequireInternalSlot(calendar, [[InitializedTemporalCalendar]]).
    auto* calendar = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Assert: calendar.[[Identifier]] is "iso8601".
    VERIFY(calendar->identifier() == "iso8601"sv);

    // 4. If Type(temporalDateLike) is Object and temporalDateLike has an [[InitializedTemporalMonthDay]] internal slot, then
    // a. Throw a TypeError exception.
    // TODO

    auto temporal_date_like = vm.argument(0);
    // 5. If Type(temporalDateLike) is not Object or temporalDateLike does not have an [[InitializedTemporalDate]] or [[InitializedTemporalYearMonth]] internal slot, then
    // TODO PlainYearMonth objects
    if (!temporal_date_like.is_object() || !is<PlainDate>(temporal_date_like.as_object())) {
        // a. Set temporalDateLike to ? ToTemporalDate(temporalDateLike).
        temporal_date_like = to_temporal_date(global_object, temporal_date_like);
        if (vm.exception())
            return {};
    }

    // 6. Return ! ISOMonth(temporalDateLike).
    return Value(iso_month(temporal_date_like.as_object()));
}

// 12.4.23 Temporal.Calendar.prototype.toString ( ), https://tc39.es/proposal-temporal/#sec-temporal.calendar.prototype.tostring
JS_DEFINE_NATIVE_FUNCTION(CalendarPrototype::to_string)
{
    // 1. Let calendar be the this value.
    // 2. Perform ? RequireInternalSlot(calendar, [[InitializedTemporalCalendar]]).
    auto* calendar = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Return calendar.[[Identifier]].
    return js_string(vm, calendar->identifier());
}

// 12.4.24 Temporal.Calendar.prototype.toJSON ( ), https://tc39.es/proposal-temporal/#sec-temporal.calendar.prototype.tojson
JS_DEFINE_NATIVE_FUNCTION(CalendarPrototype::to_json)
{
    // 1. Let calendar be the this value.
    auto calendar = vm.this_value(global_object);

    // 2. Return ? ToString(calendar).
    return js_string(vm, calendar.to_string(global_object));
}

}
