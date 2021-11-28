/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/DateTimeFormat.h>
#include <LibJS/Runtime/Intl/DateTimeFormatConstructor.h>

namespace JS::Intl {

// 11.2 The Intl.DateTimeFormat Constructor, https://tc39.es/ecma402/#sec-intl-datetimeformat-constructor
DateTimeFormatConstructor::DateTimeFormatConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.DateTimeFormat.as_string(), *global_object.function_prototype())
{
}

void DateTimeFormatConstructor::initialize(GlobalObject& global_object)
{
    NativeFunction::initialize(global_object);

    auto& vm = this->vm();

    // 11.3.1 Intl.DateTimeFormat.prototype, https://tc39.es/ecma402/#sec-intl.datetimeformat.prototype
    define_direct_property(vm.names.prototype, global_object.intl_date_time_format_prototype(), 0);
    define_direct_property(vm.names.length, Value(0), Attribute::Configurable);
}

// 11.2.1 Intl.DateTimeFormat ( [ locales [ , options ] ] ), https://tc39.es/ecma402/#sec-intl.datetimeformat
ThrowCompletionOr<Value> DateTimeFormatConstructor::call()
{
    // 1. If NewTarget is undefined, let newTarget be the active function object, else let newTarget be NewTarget.
    return TRY(construct(*this));
}

// 11.2.1 Intl.DateTimeFormat ( [ locales [ , options ] ] ), https://tc39.es/ecma402/#sec-intl.datetimeformat
ThrowCompletionOr<Object*> DateTimeFormatConstructor::construct(FunctionObject& new_target)
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    auto locales = vm.argument(0);
    auto options = vm.argument(1);

    // 2. Let dateTimeFormat be ? OrdinaryCreateFromConstructor(newTarget, "%DateTimeFormat.prototype%", « [[InitializedDateTimeFormat]], [[Locale]], [[Calendar]], [[NumberingSystem]], [[TimeZone]], [[Weekday]], [[Era]], [[Year]], [[Month]], [[Day]], [[DayPeriod]], [[Hour]], [[Minute]], [[Second]], [[FractionalSecondDigits]], [[TimeZoneName]], [[HourCycle]], [[Pattern]], [[BoundFormat]] »).
    auto* date_time_format = TRY(ordinary_create_from_constructor<DateTimeFormat>(global_object, new_target, &GlobalObject::intl_date_time_format_prototype));

    // 3. Perform ? InitializeDateTimeFormat(dateTimeFormat, locales, options).
    TRY(initialize_date_time_format(global_object, *date_time_format, locales, options));

    // 4. If the implementation supports the normative optional constructor mode of 4.3 Note 1, then
    //     a. Let this be the this value.
    //     b. Return ? ChainDateTimeFormat(dateTimeFormat, NewTarget, this).

    // 5. Return dateTimeFormat.
    return date_time_format;
}

}
