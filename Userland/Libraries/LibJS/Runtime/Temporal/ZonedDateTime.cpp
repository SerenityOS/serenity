/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/Calendar.h>
#include <LibJS/Runtime/Temporal/Duration.h>
#include <LibJS/Runtime/Temporal/Instant.h>
#include <LibJS/Runtime/Temporal/PlainDate.h>
#include <LibJS/Runtime/Temporal/PlainDateTime.h>
#include <LibJS/Runtime/Temporal/TimeZone.h>
#include <LibJS/Runtime/Temporal/ZonedDateTime.h>
#include <LibJS/Runtime/Temporal/ZonedDateTimeConstructor.h>

namespace JS::Temporal {

// 6 Temporal.ZonedDateTime Objects, https://tc39.es/proposal-temporal/#sec-temporal-zoneddatetime-objects
ZonedDateTime::ZonedDateTime(BigInt const& nanoseconds, Object& time_zone, Object& calendar, Object& prototype)
    : Object(prototype)
    , m_nanoseconds(nanoseconds)
    , m_time_zone(time_zone)
    , m_calendar(calendar)
{
}

void ZonedDateTime::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);

    visitor.visit(&m_nanoseconds);
    visitor.visit(&m_time_zone);
    visitor.visit(&m_calendar);
}

// 6.5.1 InterpretISODateTimeOffset ( year, month, day, hour, minute, second, millisecond, microsecond, nanosecond, offsetBehaviour, offsetNanoseconds, timeZone, disambiguation, offsetOption, matchBehaviour ), https://tc39.es/proposal-temporal/#sec-temporal-interpretisodatetimeoffset
ThrowCompletionOr<BigInt const*> interpret_iso_date_time_offset(GlobalObject& global_object, i32 year, u8 month, u8 day, u8 hour, u8 minute, u8 second, u16 millisecond, u16 microsecond, u16 nanosecond, OffsetBehavior offset_behavior, double offset_nanoseconds, Value time_zone, StringView disambiguation, StringView offset_option, MatchBehavior match_behavior)
{
    auto& vm = global_object.vm();

    // 1. Assert: offsetNanoseconds is an integer.
    VERIFY(trunc(offset_nanoseconds) == offset_nanoseconds);

    // 2. Let calendar be ! GetISO8601Calendar().
    auto* calendar = get_iso8601_calendar(global_object);

    // 3. Let dateTime be ? CreateTemporalDateTime(year, month, day, hour, minute, second, millisecond, microsecond, nanosecond, calendar).
    auto* date_time = TRY(create_temporal_date_time(global_object, year, month, day, hour, minute, second, millisecond, microsecond, nanosecond, *calendar));

    // 4. If offsetBehaviour is wall, or offsetOption is "ignore", then
    if (offset_behavior == OffsetBehavior::Wall || offset_option == "ignore"sv) {
        // a. Let instant be ? BuiltinTimeZoneGetInstantFor(timeZone, dateTime, disambiguation).
        auto* instant = TRY(builtin_time_zone_get_instant_for(global_object, time_zone, *date_time, disambiguation));

        // b. Return instant.[[Nanoseconds]].
        return &instant->nanoseconds();
    }

    // 5. If offsetBehaviour is exact, or offsetOption is "use", then
    if (offset_behavior == OffsetBehavior::Exact || offset_option == "use"sv) {
        // a. Let epochNanoseconds be ! GetEpochFromISOParts(year, month, day, hour, minute, second, millisecond, microsecond, nanosecond).
        auto* epoch_nanoseconds = get_epoch_from_iso_parts(global_object, year, month, day, hour, minute, second, millisecond, microsecond, nanosecond);

        // b. Return epochNanoseconds − offsetNanoseconds.
        auto offset_nanoseconds_bigint = Crypto::SignedBigInteger::create_from((i64)offset_nanoseconds);
        return js_bigint(vm, epoch_nanoseconds->big_integer().minus(offset_nanoseconds_bigint));
    }

    // 6. Assert: offsetBehaviour is option.
    VERIFY(offset_behavior == OffsetBehavior::Option);

    // 7. Assert: offsetOption is "prefer" or "reject".
    VERIFY(offset_option.is_one_of("prefer"sv, "reject"sv));

    // 8. Let possibleInstants be ? GetPossibleInstantsFor(timeZone, dateTime).
    auto possible_instants = TRY(get_possible_instants_for(global_object, time_zone, *date_time));

    // 9. For each element candidate of possibleInstants, do
    for (auto& candidate_value : possible_instants) {
        // TODO: As per the comment in disambiguate_possible_instants, having a MarkedValueList<T> would allow us to remove this cast.
        auto& candidate = static_cast<Instant&>(candidate_value.as_object());

        // a. Let candidateNanoseconds be ? GetOffsetNanosecondsFor(timeZone, candidate).
        auto candidate_nanoseconds = TRY(get_offset_nanoseconds_for(global_object, time_zone, candidate));

        // b. If candidateNanoseconds = offsetNanoseconds, then
        if (candidate_nanoseconds == offset_nanoseconds) {
            // i. Return candidate.[[Nanoseconds]].
            return &candidate.nanoseconds();
        }

        // c. If matchBehaviour is match minutes, then
        if (match_behavior == MatchBehavior::MatchMinutes) {
            // i. Let roundedCandidateNanoseconds be ! RoundNumberToIncrement(candidateNanoseconds, 60 × 10^9, "halfExpand").
            auto rounded_candidate_nanoseconds = round_number_to_increment(candidate_nanoseconds, 60000000000, "halfExpand"sv);

            // ii. If roundedCandidateNanoseconds = offsetNanoseconds, then
            if (rounded_candidate_nanoseconds == offset_nanoseconds) {
                // 1. Return candidate.[[Nanoseconds]].
                return &candidate.nanoseconds();
            }
        }
    }

    // 10. If offsetOption is "reject", throw a RangeError exception.
    if (offset_option == "reject"sv)
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidZonedDateTimeOffset);

    // 11. Let instant be ? DisambiguatePossibleInstants(possibleInstants, timeZone, dateTime, disambiguation).
    auto* instant = TRY(disambiguate_possible_instants(global_object, possible_instants, time_zone, *date_time, disambiguation));

    // 12. Return instant.[[Nanoseconds]].
    return &instant->nanoseconds();
}

// 6.5.2 ToTemporalZonedDateTime ( item [ , options ] ), https://tc39.es/proposal-temporal/#sec-temporal-totemporalzoneddatetime
ThrowCompletionOr<ZonedDateTime*> to_temporal_zoned_date_time(GlobalObject& global_object, Value item, Object* options)
{
    auto& vm = global_object.vm();

    // 1. If options is not present, set options to ! OrdinaryObjectCreate(null).
    if (!options)
        options = Object::create(global_object, nullptr);

    // 2. Let offsetBehaviour be option.
    auto offset_behavior = OffsetBehavior::Option;

    // 3. Let matchBehaviour be match exactly.
    auto match_behavior = MatchBehavior::MatchExactly;

    Object* calendar = nullptr;
    Object* time_zone = nullptr;
    Optional<String> offset_string;
    ISODateTime result;

    // 4. If Type(item) is Object, then
    if (item.is_object()) {
        auto& item_object = item.as_object();

        // a. If item has an [[InitializedTemporalZonedDateTime]] internal slot, then
        if (is<ZonedDateTime>(item_object)) {
            // i. Return item.
            return &static_cast<ZonedDateTime&>(item_object);
        }

        // b. Let calendar be ? GetTemporalCalendarWithISODefault(item).
        calendar = TRY(get_temporal_calendar_with_iso_default(global_object, item_object));

        // c. Let fieldNames be ? CalendarFields(calendar, « "day", "hour", "microsecond", "millisecond", "minute", "month", "monthCode", "nanosecond", "second", "year" »).
        auto field_names = TRY(calendar_fields(global_object, *calendar, { "day"sv, "hour"sv, "microsecond"sv, "millisecond"sv, "minute"sv, "month"sv, "monthCode"sv, "nanosecond"sv, "second"sv, "year"sv }));

        // d. Append "timeZone" to fieldNames.
        field_names.append("timeZone");

        // e. Append "offset" to fieldNames.
        field_names.append("offset");

        // f. Let fields be ? PrepareTemporalFields(item, fieldNames, « "timeZone" »).
        auto* fields = TRY(prepare_temporal_fields(global_object, item_object, field_names, { "timeZone"sv }));

        // g. Let timeZone be ? Get(fields, "timeZone").
        auto time_zone_value = TRY(fields->get(vm.names.timeZone));

        // h. Set timeZone to ? ToTemporalTimeZone(timeZone).
        time_zone = TRY(to_temporal_time_zone(global_object, time_zone_value));

        // i. Let offsetString be ? Get(fields, "offset").
        auto offset_string_value = TRY(fields->get(vm.names.offset));

        // j. If offsetString is undefined, then
        if (offset_string_value.is_undefined()) {
            // i. Set offsetBehaviour to wall.
            offset_behavior = OffsetBehavior::Wall;
        }
        // k. Else,
        else {
            // i. Set offsetString to ? ToString(offsetString).
            offset_string = TRY(offset_string_value.to_string(global_object));
        }

        // l. Let result be ? InterpretTemporalDateTimeFields(calendar, fields, options).
        result = TRY(interpret_temporal_date_time_fields(global_object, *calendar, *fields, *options));
    }
    // 5. Else,
    else {
        // a. Perform ? ToTemporalOverflow(options).
        (void)TRY(to_temporal_overflow(global_object, *options));

        // b. Let string be ? ToString(item).
        auto string = TRY(item.to_string(global_object));

        // c. Let result be ? ParseTemporalZonedDateTimeString(string).
        auto parsed_result = TRY(parse_temporal_zoned_date_time_string(global_object, string));

        // NOTE: The ISODateTime struct inside parsed_result will be moved into `result` at the end of this path to avoid mismatching names.
        //       Thus, all remaining references to `result` in this path actually refers to `parsed_result`.

        // d. Assert: result.[[TimeZoneName]] is not undefined.
        VERIFY(parsed_result.time_zone.name.has_value());

        // e. Let offsetString be result.[[TimeZoneOffsetString]].
        offset_string = move(parsed_result.time_zone.offset);

        // f. If result.[[TimeZoneZ]] is true, then
        if (parsed_result.time_zone.z) {
            // i. Set offsetBehaviour to exact.
            offset_behavior = OffsetBehavior::Exact;
        }
        // g. Else if offsetString is undefined, then
        else if (!offset_string.has_value()) {
            // i. Set offsetBehaviour to wall.
            offset_behavior = OffsetBehavior::Wall;
        }

        // h. Let timeZone be ? CreateTemporalTimeZone(result.[[TimeZoneName]]).
        time_zone = TRY(create_temporal_time_zone(global_object, *parsed_result.time_zone.name));

        // i. Let calendar be ? ToTemporalCalendarWithISODefault(result.[[Calendar]]).
        calendar = TRY(to_temporal_calendar_with_iso_default(global_object, js_string(vm, parsed_result.date_time.calendar.value())));

        // j. Set matchBehaviour to match minutes.
        match_behavior = MatchBehavior::MatchMinutes;

        // See NOTE above about why this is done.
        result = move(parsed_result.date_time);
    }

    // 6. Let offsetNanoseconds be 0.
    double offset_nanoseconds = 0;

    // 7. If offsetBehaviour is option, then
    if (offset_behavior == OffsetBehavior::Option) {
        // a. Set offsetNanoseconds to ? ParseTimeZoneOffsetString(offsetString).
        offset_nanoseconds = TRY(parse_time_zone_offset_string(global_object, *offset_string));
    }

    // 8. Let disambiguation be ? ToTemporalDisambiguation(options).
    auto disambiguation = TRY(to_temporal_disambiguation(global_object, *options));

    // 9. Let offsetOption be ? ToTemporalOffset(options, "reject").
    auto offset_option = TRY(to_temporal_offset(global_object, *options, "reject"));

    // 10. Let epochNanoseconds be ? InterpretISODateTimeOffset(result.[[Year]], result.[[Month]], result.[[Day]], result.[[Hour]], result.[[Minute]], result.[[Second]], result.[[Millisecond]], result.[[Microsecond]], result.[[Nanosecond]], offsetBehaviour, offsetNanoseconds, timeZone, disambiguation, offsetOption, matchBehaviour).
    auto* epoch_nanoseconds = TRY(interpret_iso_date_time_offset(global_object, result.year, result.month, result.day, result.hour, result.minute, result.second, result.millisecond, result.microsecond, result.nanosecond, offset_behavior, offset_nanoseconds, time_zone, disambiguation, offset_option, match_behavior));

    // 11. Return ! CreateTemporalZonedDateTime(epochNanoseconds, timeZone, calendar).
    return MUST(create_temporal_zoned_date_time(global_object, *epoch_nanoseconds, *time_zone, *calendar));
}

// 6.5.3 CreateTemporalZonedDateTime ( epochNanoseconds, timeZone, calendar [ , newTarget ] ), https://tc39.es/proposal-temporal/#sec-temporal-createtemporalzoneddatetime
ThrowCompletionOr<ZonedDateTime*> create_temporal_zoned_date_time(GlobalObject& global_object, BigInt const& epoch_nanoseconds, Object& time_zone, Object& calendar, FunctionObject const* new_target)
{
    // 1. Assert: Type(epochNanoseconds) is BigInt.
    // 3. Assert: Type(timeZone) is Object.
    // 4. Assert: Type(calendar) is Object.

    // 2. Assert: ! IsValidEpochNanoseconds(epochNanoseconds) is true.
    VERIFY(is_valid_epoch_nanoseconds(epoch_nanoseconds));

    // 5. If newTarget is not present, set it to %Temporal.ZonedDateTime%.
    if (!new_target)
        new_target = global_object.temporal_zoned_date_time_constructor();

    // 6. Let object be ? OrdinaryCreateFromConstructor(newTarget, "%Temporal.ZonedDateTime.prototype%", « [[InitializedTemporalZonedDateTime]], [[Nanoseconds]], [[TimeZone]], [[Calendar]] »).
    // 7. Set object.[[Nanoseconds]] to epochNanoseconds.
    // 8. Set object.[[TimeZone]] to timeZone.
    // 9. Set object.[[Calendar]] to calendar.
    auto* object = TRY(ordinary_create_from_constructor<ZonedDateTime>(global_object, *new_target, &GlobalObject::temporal_time_zone_prototype, epoch_nanoseconds, time_zone, calendar));

    // 10. Return object.
    return object;
}

// 6.5.5 AddZonedDateTime ( epochNanoseconds, timeZone, calendar, years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds [ , options ] ), https://tc39.es/proposal-temporal/#sec-temporal-addzoneddatetime
ThrowCompletionOr<BigInt*> add_zoned_date_time(GlobalObject& global_object, BigInt const& epoch_nanoseconds, Value time_zone, Object& calendar, double years, double months, double weeks, double days, double hours, double minutes, double seconds, double milliseconds, double microseconds, double nanoseconds, Object* options)
{
    // 1. If options is not present, set options to ! OrdinaryObjectCreate(null).
    if (!options)
        options = Object::create(global_object, nullptr);

    // 2. If all of years, months, weeks, and days are 0, then
    if (years == 0 && months == 0 && weeks == 0 && days == 0) {
        // a. Return ! AddInstant(epochNanoseconds, hours, minutes, seconds, milliseconds, microseconds, nanoseconds).
        return MUST(add_instant(global_object, epoch_nanoseconds, hours, minutes, seconds, milliseconds, microseconds, nanoseconds));
    }

    // 3. Let instant be ! CreateTemporalInstant(epochNanoseconds).
    auto* instant = MUST(create_temporal_instant(global_object, epoch_nanoseconds));

    // 4. Let temporalDateTime be ? BuiltinTimeZoneGetPlainDateTimeFor(timeZone, instant, calendar).
    auto* temporal_date_time = TRY(builtin_time_zone_get_plain_date_time_for(global_object, time_zone, *instant, calendar));

    // 5. Let datePart be ? CreateTemporalDate(temporalDateTime.[[ISOYear]], temporalDateTime.[[ISOMonth]], temporalDateTime.[[ISODay]], calendar).
    auto* date_part = TRY(create_temporal_date(global_object, temporal_date_time->iso_year(), temporal_date_time->iso_month(), temporal_date_time->iso_day(), calendar));

    // 6. Let dateDuration be ? CreateTemporalDuration(years, months, weeks, days, 0, 0, 0, 0, 0, 0).
    auto* date_duration = TRY(create_temporal_duration(global_object, years, months, weeks, days, 0, 0, 0, 0, 0, 0));

    // 7. Let addedDate be ? CalendarDateAdd(calendar, datePart, dateDuration, options).
    auto* added_date = TRY(calendar_date_add(global_object, calendar, *date_part, *date_duration, options));

    // 8. Let intermediateDateTime be ? CreateTemporalDateTime(addedDate.[[ISOYear]], addedDate.[[ISOMonth]], addedDate.[[ISODay]], temporalDateTime.[[ISOHour]], temporalDateTime.[[ISOMinute]], temporalDateTime.[[ISOSecond]], temporalDateTime.[[ISOMillisecond]], temporalDateTime.[[ISOMicrosecond]], temporalDateTime.[[ISONanosecond]], calendar).
    auto* intermediate_date_time = TRY(create_temporal_date_time(global_object, added_date->iso_year(), added_date->iso_month(), added_date->iso_day(), temporal_date_time->iso_hour(), temporal_date_time->iso_minute(), temporal_date_time->iso_second(), temporal_date_time->iso_millisecond(), temporal_date_time->iso_microsecond(), temporal_date_time->iso_nanosecond(), calendar));

    // 9. Let intermediateInstant be ? BuiltinTimeZoneGetInstantFor(timeZone, intermediateDateTime, "compatible").
    auto* intermediate_instant = TRY(builtin_time_zone_get_instant_for(global_object, time_zone, *intermediate_date_time, "compatible"sv));

    // 10. Return ! AddInstant(intermediateInstant.[[Nanoseconds]], hours, minutes, seconds, milliseconds, microseconds, nanoseconds).
    return MUST(add_instant(global_object, intermediate_instant->nanoseconds(), hours, minutes, seconds, milliseconds, microseconds, nanoseconds));
}

// 6.5.7 NanosecondsToDays ( nanoseconds, relativeTo ), https://tc39.es/proposal-temporal/#sec-temporal-nanosecondstodays
ThrowCompletionOr<NanosecondsToDaysResult> nanoseconds_to_days(GlobalObject& global_object, BigInt const& nanoseconds_bigint, Value relative_to_value)
{
    auto& vm = global_object.vm();

    // 1. Assert: Type(nanoseconds) is BigInt.

    // 2. Set nanoseconds to ℝ(nanoseconds).
    auto nanoseconds = nanoseconds_bigint.big_integer();

    // 3. Let sign be ! ℝ(Sign(𝔽(nanoseconds))).
    i8 sign;
    if (nanoseconds == Crypto::UnsignedBigInteger { 0 })
        sign = 0;
    else if (nanoseconds.is_negative())
        sign = -1;
    else
        sign = 1;

    // 4. Let dayLengthNs be 8.64 × 10^13.
    auto day_length_ns = "86400000000000"_sbigint;

    // 5. If sign is 0, then
    if (sign == 0) {
        // a. Return the Record { [[Days]]: 0, [[Nanoseconds]]: 0, [[DayLength]]: dayLengthNs }.
        return NanosecondsToDaysResult { .days = 0, .nanoseconds = make_handle(js_bigint(vm, { 0 })), .day_length = day_length_ns.to_double() };
    }

    // 6. If Type(relativeTo) is not Object or relativeTo does not have an [[InitializedTemporalZonedDateTime]] internal slot, then
    if (!relative_to_value.is_object() || !is<ZonedDateTime>(relative_to_value.as_object())) {
        // a. Return the Record { [[Days]]: the integral part of nanoseconds / dayLengthNs, [[Nanoseconds]]: (abs(nanoseconds) modulo dayLengthNs) × sign, [[DayLength]]: dayLengthNs }.
        return NanosecondsToDaysResult {
            .days = nanoseconds.divided_by(day_length_ns).quotient.to_double(),
            .nanoseconds = make_handle(js_bigint(vm, Crypto::SignedBigInteger { nanoseconds.unsigned_value() }.divided_by(day_length_ns).remainder.multiplied_by(Crypto::SignedBigInteger { sign }))),
            .day_length = day_length_ns.to_double()
        };
    }

    auto& relative_to = static_cast<ZonedDateTime&>(relative_to_value.as_object());

    // 7. Let startNs be ℝ(relativeTo.[[Nanoseconds]]).
    auto& start_ns = relative_to.nanoseconds().big_integer();

    // 8. Let startInstant be ! CreateTemporalInstant(ℤ(startNs)).
    auto* start_instant = MUST(create_temporal_instant(global_object, *js_bigint(vm, start_ns)));

    // 9. Let startDateTime be ? BuiltinTimeZoneGetPlainDateTimeFor(relativeTo.[[TimeZone]], startInstant, relativeTo.[[Calendar]]).
    auto* start_date_time = TRY(builtin_time_zone_get_plain_date_time_for(global_object, &relative_to.time_zone(), *start_instant, relative_to.calendar()));

    // 10. Let endNs be startNs + nanoseconds.
    auto end_ns = start_ns.plus(nanoseconds);

    // 11. Let endInstant be ! CreateTemporalInstant(ℤ(endNs)).
    auto* end_instant = MUST(create_temporal_instant(global_object, *js_bigint(vm, end_ns)));

    // 12. Let endDateTime be ? BuiltinTimeZoneGetPlainDateTimeFor(relativeTo.[[TimeZone]], endInstant, relativeTo.[[Calendar]]).
    auto* end_date_time = TRY(builtin_time_zone_get_plain_date_time_for(global_object, &relative_to.time_zone(), *end_instant, relative_to.calendar()));

    // 13. Let dateDifference be ? DifferenceISODateTime(startDateTime.[[ISOYear]], startDateTime.[[ISOMonth]], startDateTime.[[ISODay]], startDateTime.[[ISOHour]], startDateTime.[[ISOMinute]], startDateTime.[[ISOSecond]], startDateTime.[[ISOMillisecond]], startDateTime.[[ISOMicrosecond]], startDateTime.[[ISONanosecond]], endDateTime.[[ISOYear]], endDateTime.[[ISOMonth]], endDateTime.[[ISODay]], endDateTime.[[ISOHour]], endDateTime.[[ISOMinute]], endDateTime.[[ISOSecond]], endDateTime.[[ISOMillisecond]], endDateTime.[[ISOMicrosecond]], endDateTime.[[ISONanosecond]], relativeTo.[[Calendar]], "day").
    auto date_difference = TRY(difference_iso_date_time(global_object, start_date_time->iso_year(), start_date_time->iso_month(), start_date_time->iso_day(), start_date_time->iso_hour(), start_date_time->iso_minute(), start_date_time->iso_second(), start_date_time->iso_millisecond(), start_date_time->iso_microsecond(), start_date_time->iso_nanosecond(), end_date_time->iso_year(), end_date_time->iso_month(), end_date_time->iso_day(), end_date_time->iso_hour(), end_date_time->iso_minute(), end_date_time->iso_second(), end_date_time->iso_millisecond(), end_date_time->iso_microsecond(), end_date_time->iso_nanosecond(), relative_to.calendar(), "day"sv));

    // 14. Let days be dateDifference.[[Days]].
    auto days = date_difference.days;

    // 15. Let intermediateNs be ℝ(? AddZonedDateTime(ℤ(startNs), relativeTo.[[TimeZone]], relativeTo.[[Calendar]], 0, 0, 0, days, 0, 0, 0, 0, 0, 0)).
    auto intermediate_ns = TRY(add_zoned_date_time(global_object, *js_bigint(vm, start_ns), &relative_to.time_zone(), relative_to.calendar(), 0, 0, 0, days, 0, 0, 0, 0, 0, 0))->big_integer();

    // 16. If sign is 1, then
    if (sign == 1) {
        // a. Repeat, while days > 0 and intermediateNs > endNs,
        while (days > 0 && intermediate_ns > end_ns) {
            // i. Set days to days − 1.
            days--;

            // ii. Set intermediateNs to ℝ(? AddZonedDateTime(ℤ(startNs), relativeTo.[[TimeZone]], relativeTo.[[Calendar]], 0, 0, 0, days, 0, 0, 0, 0, 0, 0)).
            intermediate_ns = TRY(add_zoned_date_time(global_object, *js_bigint(vm, start_ns), &relative_to.time_zone(), relative_to.calendar(), 0, 0, 0, days, 0, 0, 0, 0, 0, 0))->big_integer();
        }
    }

    // 17. Set nanoseconds to endNs − intermediateNs.
    nanoseconds = end_ns.minus(intermediate_ns);

    // 18. Let done be false.
    // 19. Repeat, while done is false,
    while (true) {
        // a. Let oneDayFartherNs be ℝ(? AddZonedDateTime(ℤ(intermediateNs), relativeTo.[[TimeZone]], relativeTo.[[Calendar]], 0, 0, 0, sign, 0, 0, 0, 0, 0, 0)).
        auto one_day_farther_ns = TRY(add_zoned_date_time(global_object, *js_bigint(vm, intermediate_ns), &relative_to.time_zone(), relative_to.calendar(), 0, 0, 0, sign, 0, 0, 0, 0, 0, 0))->big_integer();

        // b. Set dayLengthNs to oneDayFartherNs − intermediateNs.
        day_length_ns = one_day_farther_ns.minus(intermediate_ns);

        // c. If (nanoseconds − dayLengthNs) × sign ≥ 0, then
        if (nanoseconds.minus(day_length_ns).multiplied_by(Crypto::SignedBigInteger { sign }) >= "0"_sbigint) {
            // i. Set nanoseconds to nanoseconds − dayLengthNs.
            nanoseconds = nanoseconds.minus(day_length_ns);

            // ii. Set intermediateNs to oneDayFartherNs.
            intermediate_ns = move(one_day_farther_ns);

            // iii. Set days to days + sign.
            days += sign;
        }
        // d. Else,
        else {
            // i. Set done to true.
            break;
        }
    }

    // 20. Return the Record { [[Days]]: days, [[Nanoseconds]]: nanoseconds, [[DayLength]]: abs(dayLengthNs) }.
    return NanosecondsToDaysResult { .days = days, .nanoseconds = make_handle(js_bigint(vm, move(nanoseconds))), .day_length = fabs(day_length_ns.to_double()) };
}

}
