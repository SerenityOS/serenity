/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/Duration.h>
#include <LibJS/Runtime/Temporal/DurationPrototype.h>

namespace JS::Temporal {

// 7.3 Properties of the Temporal.Duration Prototype Object, https://tc39.es/proposal-temporal/#sec-properties-of-the-temporal-duration-prototype-object
DurationPrototype::DurationPrototype(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void DurationPrototype::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);

    auto& vm = this->vm();

    // 7.3.2 Temporal.Duration.prototype[ @@toStringTag ], https://tc39.es/proposal-temporal/#sec-temporal.duration.prototype-@@tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm.heap(), "Temporal.Duration"), Attribute::Configurable);

    define_native_accessor(vm.names.years, years_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.months, months_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.weeks, weeks_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.days, days_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.hours, hours_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.minutes, minutes_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.seconds, seconds_getter, {}, Attribute::Configurable);
}

static Duration* typed_this(GlobalObject& global_object)
{
    auto& vm = global_object.vm();
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};
    if (!is<Duration>(this_object)) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotA, "Temporal.Duration");
        return {};
    }
    return static_cast<Duration*>(this_object);
}

// 7.3.3 get Temporal.Duration.prototype.years, https://tc39.es/proposal-temporal/#sec-get-temporal.duration.prototype.years
JS_DEFINE_NATIVE_FUNCTION(DurationPrototype::years_getter)
{
    // 1. Let duration be the this value.
    // 2. Perform ? RequireInternalSlot(duration, [[InitializedTemporalDuration]]).
    auto* duration = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Return duration.[[Years]].
    return Value(duration->years());
}

// 7.3.4 get Temporal.Duration.prototype.months, https://tc39.es/proposal-temporal/#sec-get-temporal.duration.prototype.months
JS_DEFINE_NATIVE_FUNCTION(DurationPrototype::months_getter)
{
    // 1. Let duration be the this value.
    // 2. Perform ? RequireInternalSlot(duration, [[InitializedTemporalDuration]]).
    auto* duration = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Return duration.[[Months]].
    return Value(duration->months());
}

// 7.3.5 get Temporal.Duration.prototype.weeks, https://tc39.es/proposal-temporal/#sec-get-temporal.duration.prototype.weeks
JS_DEFINE_NATIVE_FUNCTION(DurationPrototype::weeks_getter)
{
    // 1. Let duration be the this value.
    // 2. Perform ? RequireInternalSlot(duration, [[InitializedTemporalDuration]]).
    auto* duration = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Return duration.[[Weeks]].
    return Value(duration->weeks());
}

// 7.3.6 get Temporal.Duration.prototype.days, https://tc39.es/proposal-temporal/#sec-get-temporal.duration.prototype.days
JS_DEFINE_NATIVE_FUNCTION(DurationPrototype::days_getter)
{
    // 1. Let duration be the this value.
    // 2. Perform ? RequireInternalSlot(duration, [[InitializedTemporalDuration]]).
    auto* duration = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Return duration.[[Days]].
    return Value(duration->days());
}

// 7.3.7 get Temporal.Duration.prototype.hours, https://tc39.es/proposal-temporal/#sec-get-temporal.duration.prototype.hours
JS_DEFINE_NATIVE_FUNCTION(DurationPrototype::hours_getter)
{
    // 1. Let duration be the this value.
    // 2. Perform ? RequireInternalSlot(duration, [[InitializedTemporalDuration]]).
    auto* duration = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Return duration.[[Hours]].
    return Value(duration->hours());
}

// 7.3.8 get Temporal.Duration.prototype.minutes, https://tc39.es/proposal-temporal/#sec-get-temporal.duration.prototype.minutes
JS_DEFINE_NATIVE_FUNCTION(DurationPrototype::minutes_getter)
{
    // 1. Let duration be the this value.
    // 2. Perform ? RequireInternalSlot(duration, [[InitializedTemporalDuration]]).
    auto* duration = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Return duration.[[Minutes]].
    return Value(duration->minutes());
}

// 7.3.9 get Temporal.Duration.prototype.seconds, https://tc39.es/proposal-temporal/#sec-get-temporal.duration.prototype.seconds
JS_DEFINE_NATIVE_FUNCTION(DurationPrototype::seconds_getter)
{
    // 1. Let duration be the this value.
    // 2. Perform ? RequireInternalSlot(duration, [[InitializedTemporalDuration]]).
    auto* duration = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Return duration.[[Seconds]].
    return Value(duration->seconds());
}

}
