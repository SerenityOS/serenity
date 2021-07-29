/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/Calendar.h>
#include <LibJS/Runtime/Temporal/PlainDate.h>
#include <LibJS/Runtime/Temporal/PlainDateTime.h>
#include <LibJS/Runtime/Temporal/PlainDateTimePrototype.h>

namespace JS::Temporal {

// 5.3 Properties of the Temporal.PlainDateTime Prototype Object, https://tc39.es/proposal-temporal/#sec-properties-of-the-temporal-plaindatetime-prototype-object
PlainDateTimePrototype::PlainDateTimePrototype(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void PlainDateTimePrototype::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);

    auto& vm = this->vm();

    // 5.3.2 Temporal.PlainDateTime.prototype[ @@toStringTag ], https://tc39.es/proposal-temporal/#sec-temporal.plaindatetime.prototype-@@tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm.heap(), "Temporal.PlainDateTime"), Attribute::Configurable);

    define_native_accessor(vm.names.calendar, calendar_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.year, year_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.month, month_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.monthCode, month_code_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.day, day_getter, {}, Attribute::Configurable);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.valueOf, value_of, 0, attr);
    define_native_function(vm.names.toPlainDate, to_plain_date, 0, attr);
    define_native_function(vm.names.getISOFields, get_iso_fields, 0, attr);
}

static PlainDateTime* typed_this(GlobalObject& global_object)
{
    auto& vm = global_object.vm();
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};
    if (!is<PlainDateTime>(this_object)) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotA, "Temporal.PlainDateTime");
        return {};
    }
    return static_cast<PlainDateTime*>(this_object);
}

// 5.3.3 get Temporal.PlainDateTime.prototype.calendar, https://tc39.es/proposal-temporal/#sec-get-temporal.plaindatetime.prototype.calendar
JS_DEFINE_NATIVE_FUNCTION(PlainDateTimePrototype::calendar_getter)
{
    // 1. Let dateTime be the this value.
    // 2. Perform ? RequireInternalSlot(dateTime, [[InitializedTemporalDateTime]]).
    auto* date_time = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Return dateTime.[[Calendar]].
    return Value(&date_time->calendar());
}

// 5.3.4 get Temporal.PlainDateTime.prototype.year, https://tc39.es/proposal-temporal/#sec-get-temporal.plaindatetime.prototype.year
JS_DEFINE_NATIVE_FUNCTION(PlainDateTimePrototype::year_getter)
{
    // 1. Let dateTime be the this value.
    // 2. Perform ? RequireInternalSlot(dateTime, [[InitializedTemporalDateTime]]).
    auto* date_time = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Let calendar be dateTime.[[Calendar]].
    auto& calendar = date_time->calendar();

    // 4. Return ? CalendarYear(calendar, dateTime).
    return Value(calendar_year(global_object, calendar, *date_time));
}

// 5.3.5 get Temporal.PlainDateTime.prototype.month, https://tc39.es/proposal-temporal/#sec-get-temporal.plaindatetime.prototype.month
JS_DEFINE_NATIVE_FUNCTION(PlainDateTimePrototype::month_getter)
{
    // 1. Let dateTime be the this value.
    // 2. Perform ? RequireInternalSlot(dateTime, [[InitializedTemporalDateTime]]).
    auto* date_time = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Let calendar be dateTime.[[Calendar]].
    auto& calendar = date_time->calendar();

    // 4. Return ? CalendarMonth(calendar, dateTime).
    return Value(calendar_month(global_object, calendar, *date_time));
}

// 5.3.6 get Temporal.PlainDateTime.prototype.monthCode, https://tc39.es/proposal-temporal/#sec-get-temporal.plaindatetime.prototype.monthcode
JS_DEFINE_NATIVE_FUNCTION(PlainDateTimePrototype::month_code_getter)
{
    // 1. Let dateTime be the this value.
    // 2. Perform ? RequireInternalSlot(dateTime, [[InitializedTemporalDateTime]]).
    auto* date_time = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Let calendar be dateTime.[[Calendar]].
    auto& calendar = date_time->calendar();

    // 4. Return ? CalendarMonthCode(calendar, dateTime).
    return js_string(vm, calendar_month_code(global_object, calendar, *date_time));
}

// 5.3.7 get Temporal.PlainDateTime.prototype.day, https://tc39.es/proposal-temporal/#sec-get-temporal.plaindatetime.prototype.day
JS_DEFINE_NATIVE_FUNCTION(PlainDateTimePrototype::day_getter)
{
    // 1. Let dateTime be the this value.
    // 2. Perform ? RequireInternalSlot(dateTime, [[InitializedTemporalDateTime]]).
    auto* date_time = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Let calendar be dateTime.[[Calendar]].
    auto& calendar = date_time->calendar();

    // 4. Return ? CalendarDay(calendar, dateTime).
    return Value(calendar_day(global_object, calendar, *date_time));
}

// 5.3.35 Temporal.PlainDateTime.prototype.valueOf ( ), https://tc39.es/proposal-temporal/#sec-temporal.plaindatetime.prototype.valueof
JS_DEFINE_NATIVE_FUNCTION(PlainDateTimePrototype::value_of)
{
    // 1. Throw a TypeError exception.
    vm.throw_exception<TypeError>(global_object, ErrorType::Convert, "Temporal.PlainDateTime", "a primitive value");
    return {};
}

// 5.3.37 Temporal.PlainDateTime.prototype.toPlainDate ( ), https://tc39.es/proposal-temporal/#sec-temporal.plaindatetime.prototype.toplaindate
JS_DEFINE_NATIVE_FUNCTION(PlainDateTimePrototype::to_plain_date)
{
    // 1. Let dateTime be the this value.
    // 2. Perform ? RequireInternalSlot(dateTime, [[InitializedTemporalDateTime]]).
    auto* date_time = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Return ? CreateTemporalDate(dateTime.[[ISOYear]], dateTime.[[ISOMonth]], dateTime.[[ISODay]], dateTime.[[Calendar]]).
    return create_temporal_date(global_object, date_time->iso_year(), date_time->iso_month(), date_time->iso_day(), date_time->calendar());
}

// 5.3.41 Temporal.PlainDateTime.prototype.getISOFields ( ), https://tc39.es/proposal-temporal/#sec-temporal.plaindatetime.prototype.getisofields
JS_DEFINE_NATIVE_FUNCTION(PlainDateTimePrototype::get_iso_fields)
{
    // 1. Let dateTime be the this value.
    // 2. Perform ? RequireInternalSlot(dateTime, [[InitializedTemporalDateTime]]).
    auto* date_time = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Let fields be ! OrdinaryObjectCreate(%Object.prototype%).
    auto* fields = Object::create(global_object, global_object.object_prototype());

    // 4. Perform ! CreateDataPropertyOrThrow(fields, "calendar", dateTime.[[Calendar]]).
    fields->create_data_property_or_throw(vm.names.calendar, Value(&date_time->calendar()));

    // 5. Perform ! CreateDataPropertyOrThrow(fields, "isoDay", 𝔽(dateTime.[[ISODay]])).
    fields->create_data_property_or_throw(vm.names.isoDay, Value(date_time->iso_day()));

    // 6. Perform ! CreateDataPropertyOrThrow(fields, "isoHour", 𝔽(dateTime.[[ISOHour]])).
    fields->create_data_property_or_throw(vm.names.isoHour, Value(date_time->iso_hour()));

    // 7. Perform ! CreateDataPropertyOrThrow(fields, "isoMicrosecond", 𝔽(dateTime.[[ISOMicrosecond]])).
    fields->create_data_property_or_throw(vm.names.isoMicrosecond, Value(date_time->iso_microsecond()));

    // 8. Perform ! CreateDataPropertyOrThrow(fields, "isoMillisecond", 𝔽(dateTime.[[ISOMillisecond]])).
    fields->create_data_property_or_throw(vm.names.isoMillisecond, Value(date_time->iso_millisecond()));

    // 9. Perform ! CreateDataPropertyOrThrow(fields, "isoMinute", 𝔽(dateTime.[[ISOMinute]])).
    fields->create_data_property_or_throw(vm.names.isoMinute, Value(date_time->iso_minute()));

    // 10. Perform ! CreateDataPropertyOrThrow(fields, "isoMonth", 𝔽(dateTime.[[ISOMonth]])).
    fields->create_data_property_or_throw(vm.names.isoMonth, Value(date_time->iso_month()));

    // 11. Perform ! CreateDataPropertyOrThrow(fields, "isoNanosecond", 𝔽(dateTime.[[ISONanosecond]])).
    fields->create_data_property_or_throw(vm.names.isoNanosecond, Value(date_time->iso_nanosecond()));

    // 12. Perform ! CreateDataPropertyOrThrow(fields, "isoSecond", 𝔽(dateTime.[[ISOSecond]])).
    fields->create_data_property_or_throw(vm.names.isoSecond, Value(date_time->iso_second()));

    // 13. Perform ! CreateDataPropertyOrThrow(fields, "isoYear", 𝔽(dateTime.[[ISOYear]])).
    fields->create_data_property_or_throw(vm.names.isoYear, Value(date_time->iso_year()));

    // 14. Return fields.
    return fields;
}

}
