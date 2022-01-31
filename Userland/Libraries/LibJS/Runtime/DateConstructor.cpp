/*
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2020, Nico Weber <thakis@chromium.org>
 * Copyright (c) 2021, Petróczi Zoltán <petroczizoltan@tutanota.com>
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/GenericLexer.h>
#include <AK/Time.h>
#include <LibCore/DateTime.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Date.h>
#include <LibJS/Runtime/DateConstructor.h>
#include <LibJS/Runtime/DatePrototype.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/VM.h>
#include <sys/time.h>
#include <time.h>

namespace JS {

// 21.4.3.2 Date.parse ( string ), https://tc39.es/ecma262/#sec-date.parse
static Value parse_simplified_iso8601(GlobalObject& global_object, const String& iso_8601)
{
    // 21.4.1.15 Date Time String Format, https://tc39.es/ecma262/#sec-date-time-string-format
    GenericLexer lexer(iso_8601);
    auto lex_n_digits = [&](size_t n, Optional<int>& out) {
        if (lexer.tell_remaining() < n)
            return false;
        int r = 0;
        for (size_t i = 0; i < n; ++i) {
            char ch = lexer.consume();
            if (!is_ascii_digit(ch))
                return false;
            r = 10 * r + ch - '0';
        }
        out = r;
        return true;
    };

    Optional<int> year;
    Optional<int> month;
    Optional<int> day;
    Optional<int> hours;
    Optional<int> minutes;
    Optional<int> seconds;
    Optional<int> milliseconds;
    Optional<char> timezone;
    Optional<int> timezone_hours;
    Optional<int> timezone_minutes;

    auto lex_year = [&]() {
        if (lexer.consume_specific('+'))
            return lex_n_digits(6, year);
        if (lexer.consume_specific('-')) {
            Optional<int> absolute_year;
            if (!lex_n_digits(6, absolute_year))
                return false;
            year = -absolute_year.value();
            return true;
        }
        return lex_n_digits(4, year);
    };
    auto lex_month = [&]() { return lex_n_digits(2, month) && *month >= 1 && *month <= 12; };
    auto lex_day = [&]() { return lex_n_digits(2, day) && *day >= 1 && *day <= 31; };
    auto lex_date = [&]() { return lex_year() && (!lexer.consume_specific('-') || (lex_month() && (!lexer.consume_specific('-') || lex_day()))); };

    auto lex_hours_minutes = [&](Optional<int>& out_h, Optional<int>& out_m) {
        Optional<int> h;
        Optional<int> m;
        if (lex_n_digits(2, h) && *h >= 0 && *h <= 24 && lexer.consume_specific(':') && lex_n_digits(2, m) && *m >= 0 && *m <= 59) {
            out_h = move(h);
            out_m = move(m);
            return true;
        }
        return false;
    };
    auto lex_seconds = [&]() { return lex_n_digits(2, seconds) && *seconds >= 0 && *seconds <= 59; };
    auto lex_milliseconds = [&]() { return lex_n_digits(3, milliseconds); };
    auto lex_seconds_milliseconds = [&]() { return lex_seconds() && (!lexer.consume_specific('.') || lex_milliseconds()); };
    auto lex_timezone = [&]() {
        if (lexer.consume_specific('+')) {
            timezone = '+';
            return lex_hours_minutes(timezone_hours, timezone_minutes);
        }
        if (lexer.consume_specific('-')) {
            timezone = '-';
            return lex_hours_minutes(timezone_hours, timezone_minutes);
        }
        if (lexer.consume_specific('Z'))
            timezone = 'Z';
        return true;
    };
    auto lex_time = [&]() { return lex_hours_minutes(hours, minutes) && (!lexer.consume_specific(':') || lex_seconds_milliseconds()) && lex_timezone(); };

    if (!lex_date() || (lexer.consume_specific('T') && !lex_time()) || !lexer.is_eof()) {
        return js_nan();
    }

    // We parsed a valid date simplified ISO 8601 string.
    VERIFY(year.has_value()); // A valid date string always has at least a year.
    auto time = AK::Time::from_timestamp(*year, month.value_or(1), day.value_or(1), hours.value_or(0), minutes.value_or(0), seconds.value_or(0), milliseconds.value_or(0));
    auto time_ms = static_cast<double>(time.to_milliseconds());

    // https://tc39.es/ecma262/#sec-date.parse:
    // "When the UTC offset representation is absent, date-only forms are interpreted as a UTC time and date-time forms are interpreted as a local time."
    if (!timezone.has_value() && hours.has_value())
        time_ms = utc_time(time_ms);

    if (timezone == '-')
        time_ms += *timezone_hours * 3'600'000 + *timezone_minutes * 60'000;
    else if (timezone == '+')
        time_ms -= *timezone_hours * 3'600'000 + *timezone_minutes * 60'000;

    return time_clip(global_object, Value(time_ms));
}

static Value parse_date_string(GlobalObject& global_object, String const& date_string)
{
    auto value = parse_simplified_iso8601(global_object, date_string);
    if (value.is_finite_number())
        return value;

    // Date.parse() is allowed to accept an arbitrary number of implementation-defined formats.
    // Parse formats of this type: "Wed Apr 17 23:08:53 +0000 2019"
    auto maybe_datetime = Core::DateTime::parse("%a %b %e %T %z %Y", date_string);
    if (maybe_datetime.has_value())
        return Value(1000.0 * maybe_datetime.value().timestamp());

    return js_nan();
}

DateConstructor::DateConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.Date.as_string(), *global_object.function_prototype())
{
}

void DateConstructor::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    NativeFunction::initialize(global_object);

    // 21.4.3.3 Date.prototype, https://tc39.es/ecma262/#sec-date.prototype
    define_direct_property(vm.names.prototype, global_object.date_prototype(), 0);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.now, now, 0, attr);
    define_native_function(vm.names.parse, parse, 1, attr);
    define_native_function(vm.names.UTC, utc, 7, attr);

    define_direct_property(vm.names.length, Value(7), Attribute::Configurable);
}

DateConstructor::~DateConstructor()
{
}

// 21.4.2.1 Date ( ...values ), https://tc39.es/ecma262/#sec-date
ThrowCompletionOr<Value> DateConstructor::call()
{
    // 1. If NewTarget is undefined, then
    //     a. Let now be the time value (UTC) identifying the current time.
    auto now = AK::Time::now_realtime().to_milliseconds();

    //     b. Return ToDateString(now).
    return js_string(vm(), to_date_string(now));
}

// 21.4.2.1 Date ( ...values ), https://tc39.es/ecma262/#sec-date
ThrowCompletionOr<Object*> DateConstructor::construct(FunctionObject& new_target)
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    Value date_value;

    // 2. Let numberOfArgs be the number of elements in values.
    // 3. If numberOfArgs = 0, then
    if (vm.argument_count() == 0) {
        // a. Let dv be the time value (UTC) identifying the current time.
        auto now = AK::Time::now_realtime().to_milliseconds();
        date_value = Value(static_cast<double>(now));
    }
    // 4. Else if numberOfArgs = 1, then
    else if (vm.argument_count() == 1) {
        // a. Let value be values[0].
        auto value = vm.argument(0);
        Value time_value;

        // b. If Type(value) is Object and value has a [[DateValue]] internal slot, then
        if (value.is_object() && is<Date>(value.as_object())) {
            // i. Let tv be ! thisTimeValue(value).
            time_value = MUST(this_time_value(global_object, value));
        }
        // c. Else,
        else {
            // i. Let v be ? ToPrimitive(value).
            auto primitive = TRY(value.to_primitive(global_object));

            // ii. If Type(v) is String, then
            if (primitive.is_string()) {
                // 1. Assert: The next step never returns an abrupt completion because Type(v) is String.
                // 2. Let tv be the result of parsing v as a date, in exactly the same manner as for the parse method (21.4.3.2).
                time_value = parse_date_string(global_object, primitive.as_string().string());
            }
            // iii. Else,
            else {
                // 1. Let tv be ? ToNumber(v).
                time_value = TRY(primitive.to_number(global_object));
            }
        }

        // d. Let dv be TimeClip(tv).
        date_value = time_clip(global_object, time_value);
    }
    // 5. Else,
    else {
        // a. Assert: numberOfArgs ≥ 2.
        // b. Let y be ? ToNumber(values[0]).
        auto year = TRY(vm.argument(0).to_number(global_object));
        // c. Let m be ? ToNumber(values[1]).
        auto month = TRY(vm.argument(1).to_number(global_object));

        auto arg_or = [&vm, &global_object](size_t i, i32 fallback) -> ThrowCompletionOr<Value> {
            return vm.argument_count() > i ? vm.argument(i).to_number(global_object) : Value(fallback);
        };

        // d. If numberOfArgs > 2, let dt be ? ToNumber(values[2]); else let dt be 1𝔽.
        auto date = TRY(arg_or(2, 1));
        // e. If numberOfArgs > 3, let h be ? ToNumber(values[3]); else let h be +0𝔽.
        auto hours = TRY(arg_or(3, 0));
        // f. If numberOfArgs > 4, let min be ? ToNumber(values[4]); else let min be +0𝔽.
        auto minutes = TRY(arg_or(4, 0));
        // g. If numberOfArgs > 5, let s be ? ToNumber(values[5]); else let s be +0𝔽.
        auto seconds = TRY(arg_or(5, 0));
        // h. If numberOfArgs > 6, let milli be ? ToNumber(values[6]); else let milli be +0𝔽.
        auto milliseconds = TRY(arg_or(6, 0));

        // i. If y is NaN, let yr be NaN.
        // j. Else,
        if (!year.is_nan()) {
            // i. Let yi be ! ToIntegerOrInfinity(y).
            auto year_double = MUST(year.to_integer_or_infinity(global_object));

            // ii. If 0 ≤ yi ≤ 99, let yr be 1900𝔽 + 𝔽(yi); otherwise, let yr be y.
            if (0 <= year_double && year_double <= 99)
                year = Value(1900 + year_double);
        }

        // k. Let finalDate be MakeDate(MakeDay(yr, m, dt), MakeTime(h, min, s, milli)).
        auto day = make_day(global_object, year, month, date);
        auto time = make_time(global_object, hours, minutes, seconds, milliseconds);
        auto final_date = make_date(day, time);

        // l. Let dv be TimeClip(UTC(finalDate)).
        date_value = time_clip(global_object, Value(utc_time(final_date.as_double())));
    }

    // 6. Let O be ? OrdinaryCreateFromConstructor(NewTarget, "%Date.prototype%", « [[DateValue]] »).
    // 7. Set O.[[DateValue]] to dv.
    // 8. Return O.
    return TRY(ordinary_create_from_constructor<Date>(global_object, new_target, &GlobalObject::date_prototype, date_value.as_double()));
}

// 21.4.3.1 Date.now ( ), https://tc39.es/ecma262/#sec-date.now
JS_DEFINE_NATIVE_FUNCTION(DateConstructor::now)
{
    struct timeval tv;
    gettimeofday(&tv, nullptr);
    return Value(floor(tv.tv_sec * 1000.0 + tv.tv_usec / 1000.0));
}

// 21.4.3.2 Date.parse ( string ), https://tc39.es/ecma262/#sec-date.parse
JS_DEFINE_NATIVE_FUNCTION(DateConstructor::parse)
{
    if (!vm.argument_count())
        return js_nan();

    auto date_string = TRY(vm.argument(0).to_string(global_object));

    return parse_date_string(global_object, date_string);
}

// 21.4.3.4 Date.UTC ( year [ , month [ , date [ , hours [ , minutes [ , seconds [ , ms ] ] ] ] ] ] ), https://tc39.es/ecma262/#sec-date.utc
JS_DEFINE_NATIVE_FUNCTION(DateConstructor::utc)
{
    auto arg_or = [&vm, &global_object](size_t i, i32 fallback) -> ThrowCompletionOr<Value> {
        return vm.argument_count() > i ? vm.argument(i).to_number(global_object) : Value(fallback);
    };

    // 1. Let y be ? ToNumber(year).
    auto year = TRY(vm.argument(0).to_number(global_object));
    // 2. If month is present, let m be ? ToNumber(month); else let m be +0𝔽.
    auto month = TRY(arg_or(1, 0));
    // 3. If date is present, let dt be ? ToNumber(date); else let dt be 1𝔽.
    auto date = TRY(arg_or(2, 1));
    // 4. If hours is present, let h be ? ToNumber(hours); else let h be +0𝔽.
    auto hours = TRY(arg_or(3, 0));
    // 5. If minutes is present, let min be ? ToNumber(minutes); else let min be +0𝔽.
    auto minutes = TRY(arg_or(4, 0));
    // 6. If seconds is present, let s be ? ToNumber(seconds); else let s be +0𝔽.
    auto seconds = TRY(arg_or(5, 0));
    // 7. If ms is present, let milli be ? ToNumber(ms); else let milli be +0𝔽.
    auto milliseconds = TRY(arg_or(6, 0));

    // 8. If y is NaN, let yr be NaN.
    // 9. Else,
    if (!year.is_nan()) {
        // a. Let yi be ! ToIntegerOrInfinity(y).
        auto year_double = MUST(year.to_integer_or_infinity(global_object));

        // b. If 0 ≤ yi ≤ 99, let yr be 1900𝔽 + 𝔽(yi); otherwise, let yr be y.
        if (0 <= year_double && year_double <= 99)
            year = Value(1900 + year_double);
    }

    // 10. Return TimeClip(MakeDate(MakeDay(yr, m, dt), MakeTime(h, min, s, milli))).
    auto day = make_day(global_object, year, month, date);
    auto time = make_time(global_object, hours, minutes, seconds, milliseconds);
    return time_clip(global_object, make_date(day, time));
}

}
