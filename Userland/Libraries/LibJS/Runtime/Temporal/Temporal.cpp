/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/CalendarConstructor.h>
#include <LibJS/Runtime/Temporal/DurationConstructor.h>
#include <LibJS/Runtime/Temporal/InstantConstructor.h>
#include <LibJS/Runtime/Temporal/Now.h>
#include <LibJS/Runtime/Temporal/PlainDateConstructor.h>
#include <LibJS/Runtime/Temporal/PlainDateTimeConstructor.h>
#include <LibJS/Runtime/Temporal/PlainMonthDayConstructor.h>
#include <LibJS/Runtime/Temporal/PlainTimeConstructor.h>
#include <LibJS/Runtime/Temporal/PlainYearMonthConstructor.h>
#include <LibJS/Runtime/Temporal/Temporal.h>
#include <LibJS/Runtime/Temporal/TimeZoneConstructor.h>
#include <LibJS/Runtime/Temporal/ZonedDateTimeConstructor.h>

namespace JS::Temporal {

// 1 The Temporal Object, https://tc39.es/proposal-temporal/#sec-temporal-objects
Temporal::Temporal(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void Temporal::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);

    auto& vm = this->vm();

    // 1.1.1 Temporal [ @@toStringTag ], https://tc39.es/proposal-temporal/#sec-temporal-@@tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm, "Temporal"), Attribute::Configurable);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_direct_property(vm.names.Now, heap().allocate<Now>(global_object, global_object), attr);
    define_direct_property(vm.names.Calendar, global_object.temporal_calendar_constructor(), attr);
    define_direct_property(vm.names.Duration, global_object.temporal_duration_constructor(), attr);
    define_direct_property(vm.names.Instant, global_object.temporal_instant_constructor(), attr);
    define_direct_property(vm.names.PlainDate, global_object.temporal_plain_date_constructor(), attr);
    define_direct_property(vm.names.PlainDateTime, global_object.temporal_plain_date_time_constructor(), attr);
    define_direct_property(vm.names.PlainMonthDay, global_object.temporal_plain_month_day_constructor(), attr);
    define_direct_property(vm.names.PlainTime, global_object.temporal_plain_time_constructor(), attr);
    define_direct_property(vm.names.PlainYearMonth, global_object.temporal_plain_year_month_constructor(), attr);
    define_direct_property(vm.names.TimeZone, global_object.temporal_time_zone_constructor(), attr);
    define_direct_property(vm.names.ZonedDateTime, global_object.temporal_zoned_date_time_constructor(), attr);
}

}
