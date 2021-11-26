/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/TimeZone.h>
#include <LibJS/Runtime/Temporal/TimeZoneConstructor.h>

namespace JS::Temporal {

// 11.2 The Temporal.TimeZone Constructor, https://tc39.es/proposal-temporal/#sec-temporal-timezone-constructor
TimeZoneConstructor::TimeZoneConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.TimeZone.as_string(), *global_object.function_prototype())
{
}

void TimeZoneConstructor::initialize(GlobalObject& global_object)
{
    NativeFunction::initialize(global_object);

    auto& vm = this->vm();

    // 11.3.1 Temporal.TimeZone.prototype, https://tc39.es/proposal-temporal/#sec-temporal-timezone-prototype
    define_direct_property(vm.names.prototype, global_object.temporal_time_zone_prototype(), 0);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.from, from, 1, attr);

    define_direct_property(vm.names.length, Value(1), Attribute::Configurable);
}

// 11.2.1 Temporal.TimeZone ( identifier ), https://tc39.es/proposal-temporal/#sec-temporal.timezone
ThrowCompletionOr<Value> TimeZoneConstructor::call()
{
    auto& vm = this->vm();

    // 1. If NewTarget is undefined, then
    // a. Throw a TypeError exception.
    return vm.throw_completion<TypeError>(global_object(), ErrorType::ConstructorWithoutNew, "Temporal.TimeZone");
}

// 11.2.1 Temporal.TimeZone ( identifier ), https://tc39.es/proposal-temporal/#sec-temporal.timezone
ThrowCompletionOr<Object*> TimeZoneConstructor::construct(FunctionObject& new_target)
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    // 2. Set identifier to ? ToString(identifier).
    auto identifier = TRY(vm.argument(0).to_string(global_object));

    String canonical;

    // 3. If identifier satisfies the syntax of a TimeZoneNumericUTCOffset (see 13.33), then
    if (is_valid_time_zone_numeric_utc_offset_syntax(identifier)) {
        // a. Let offsetNanoseconds be ? ParseTimeZoneOffsetString(identifier).
        auto offset_nanoseconds = TRY(parse_time_zone_offset_string(global_object, identifier));

        // b. Let canonical be ! FormatTimeZoneOffsetString(offsetNanoseconds).
        canonical = format_time_zone_offset_string(offset_nanoseconds);
    }
    // 4. Else,
    else {
        // a. If ! IsValidTimeZoneName(identifier) is false, then
        if (!is_valid_time_zone_name(identifier)) {
            // i. Throw a RangeError exception.
            return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidTimeZoneName);
        }

        // b. Let canonical be ! CanonicalizeTimeZoneName(identifier).
        canonical = canonicalize_time_zone_name(identifier);
    }

    // 5. Return ? CreateTemporalTimeZone(canonical, NewTarget).
    return TRY(create_temporal_time_zone(global_object, canonical, &new_target));
}

// 11.3.2 Temporal.TimeZone.from ( item ), https://tc39.es/proposal-temporal/#sec-temporal.timezone.from
JS_DEFINE_NATIVE_FUNCTION(TimeZoneConstructor::from)
{
    auto item = vm.argument(0);

    // 1. Return ? ToTemporalTimeZone(item).
    return TRY(to_temporal_time_zone(global_object, item));
}

}
