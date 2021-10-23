/*
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2020, Nico Weber <thakis@chromium.org>
 * Copyright (c) 2021, Petróczi Zoltán <petroczizoltan@tutanota.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/GenericLexer.h>
#include <LibCore/DateTime.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Date.h>
#include <LibJS/Runtime/DateConstructor.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/VM.h>
#include <sys/time.h>
#include <time.h>

namespace JS {

// 21.4.3.2 Date.parse ( string ), https://tc39.es/ecma262/#sec-date.parse
static Value parse_simplified_iso8601(const String& iso_8601)
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
    struct tm tm = {};
    tm.tm_year = *year - 1900;
    tm.tm_mon = !month.has_value() ? 0 : *month - 1;
    tm.tm_mday = day.value_or(1);
    tm.tm_hour = hours.value_or(0);
    tm.tm_min = minutes.value_or(0);
    tm.tm_sec = seconds.value_or(0);

    // https://tc39.es/ecma262/#sec-date.parse:
    // "When the UTC offset representation is absent, date-only forms are interpreted as a UTC time and date-time forms are interpreted as a local time."
    time_t timestamp;
    if (timezone.has_value() || !hours.has_value())
        timestamp = timegm(&tm);
    else
        timestamp = mktime(&tm);

    if (timezone == '-')
        timestamp += (*timezone_hours * 60 + *timezone_minutes) * 60;
    else if (timezone == '+')
        timestamp -= (*timezone_hours * 60 + *timezone_minutes) * 60;

    // FIXME: reject timestamp if resulting value wouldn't fit in a double

    return Value(1000.0 * timestamp + milliseconds.value_or(0));
}

static Value parse_date_string(String const& date_string)
{
    auto value = parse_simplified_iso8601(date_string);
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
    define_native_function(vm.names.UTC, utc, 1, attr);

    define_direct_property(vm.names.length, Value(7), Attribute::Configurable);
}

DateConstructor::~DateConstructor()
{
}

struct DatetimeAndMilliseconds {
    Core::DateTime datetime;
    i16 milliseconds { 0 };
};

static DatetimeAndMilliseconds now()
{
    struct timeval tv;
    gettimeofday(&tv, nullptr);
    auto datetime = Core::DateTime::now();
    auto milliseconds = static_cast<i16>(tv.tv_usec / 1000);
    return { datetime, milliseconds };
}

// 21.4.2.1 Date ( ...values ), https://tc39.es/ecma262/#sec-date
ThrowCompletionOr<Value> DateConstructor::call()
{
    auto [datetime, milliseconds] = JS::now();
    auto* date = Date::create(global_object(), datetime, milliseconds, false);
    return js_string(heap(), date->string());
}

// 21.4.2.1 Date ( ...values ), https://tc39.es/ecma262/#sec-date
ThrowCompletionOr<Object*> DateConstructor::construct(FunctionObject& new_target)
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    if (vm.argument_count() == 0) {
        auto [datetime, milliseconds] = JS::now();
        return TRY(ordinary_create_from_constructor<Date>(global_object, new_target, &GlobalObject::date_prototype, datetime, milliseconds, false));
    }

    auto create_invalid_date = [&global_object, &new_target]() -> ThrowCompletionOr<Date*> {
        auto datetime = Core::DateTime::create(1970, 1, 1, 0, 0, 0);
        auto milliseconds = static_cast<i16>(0);
        return ordinary_create_from_constructor<Date>(global_object, new_target, &GlobalObject::date_prototype, datetime, milliseconds, true);
    };

    if (vm.argument_count() == 1) {
        auto value = vm.argument(0);
        if (value.is_string())
            value = parse_date_string(value.as_string().string());
        else
            value = TRY(value.to_number(global_object));

        if (!value.is_finite_number())
            return TRY(create_invalid_date());

        // A timestamp since the epoch, in UTC.
        double value_as_double = value.as_double();
        if (value_as_double > Date::time_clip)
            return TRY(create_invalid_date());
        auto datetime = Core::DateTime::from_timestamp(static_cast<time_t>(value_as_double / 1000));
        auto milliseconds = static_cast<i16>(fmod(value_as_double, 1000));
        return TRY(ordinary_create_from_constructor<Date>(global_object, new_target, &GlobalObject::date_prototype, datetime, milliseconds, false));
    }

    // A date/time in components, in local time.
    auto arg_or = [&vm, &global_object](size_t i, i32 fallback) -> ThrowCompletionOr<Value> {
        return vm.argument_count() > i ? vm.argument(i).to_number(global_object) : Value(fallback);
    };

    auto year_value = TRY(vm.argument(0).to_number(global_object));
    if (!year_value.is_finite_number())
        return TRY(create_invalid_date());
    auto year = year_value.as_i32();

    auto month_index_value = TRY(vm.argument(1).to_number(global_object));
    if (!month_index_value.is_finite_number())
        return TRY(create_invalid_date());
    auto month_index = month_index_value.as_i32();

    auto day_value = TRY(arg_or(2, 1));
    if (!day_value.is_finite_number())
        return TRY(create_invalid_date());
    auto day = day_value.as_i32();

    auto hours_value = TRY(arg_or(3, 0));
    if (!hours_value.is_finite_number())
        return TRY(create_invalid_date());
    auto hours = hours_value.as_i32();

    auto minutes_value = TRY(arg_or(4, 0));
    if (!minutes_value.is_finite_number())
        return TRY(create_invalid_date());
    auto minutes = minutes_value.as_i32();

    auto seconds_value = TRY(arg_or(5, 0));
    if (!seconds_value.is_finite_number())
        return TRY(create_invalid_date());
    auto seconds = seconds_value.as_i32();

    auto milliseconds_value = TRY(arg_or(6, 0));
    if (!milliseconds_value.is_finite_number())
        return TRY(create_invalid_date());
    auto milliseconds = milliseconds_value.as_i32();

    seconds += milliseconds / 1000;
    milliseconds %= 1000;
    if (milliseconds < 0) {
        seconds -= 1;
        milliseconds += 1000;
    }

    if (year >= 0 && year <= 99)
        year += 1900;
    int month = month_index + 1;
    auto datetime = Core::DateTime::create(year, month, day, hours, minutes, seconds);
    auto time = datetime.timestamp() * 1000.0 + milliseconds;
    if (time > Date::time_clip)
        return TRY(create_invalid_date());
    return TRY(ordinary_create_from_constructor<Date>(global_object, new_target, &GlobalObject::date_prototype, datetime, milliseconds, false));
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

    return parse_date_string(date_string);
}

// 21.4.3.4 Date.UTC ( year [ , month [ , date [ , hours [ , minutes [ , seconds [ , ms ] ] ] ] ] ] ), https://tc39.es/ecma262/#sec-date.utc
JS_DEFINE_NATIVE_FUNCTION(DateConstructor::utc)
{
    auto arg_or = [&vm, &global_object](size_t i, i32 fallback) -> ThrowCompletionOr<i32> {
        return vm.argument_count() > i ? vm.argument(i).to_i32(global_object) : fallback;
    };
    int year = TRY(vm.argument(0).to_i32(global_object));
    if (year >= 0 && year <= 99)
        year += 1900;

    struct tm tm = {};
    tm.tm_year = year - 1900;
    tm.tm_mon = TRY(arg_or(1, 0)); // 0-based in both tm and JavaScript
    tm.tm_mday = TRY(arg_or(2, 1));
    tm.tm_hour = TRY(arg_or(3, 0));
    tm.tm_min = TRY(arg_or(4, 0));
    tm.tm_sec = TRY(arg_or(5, 0));
    // timegm() doesn't read tm.tm_wday and tm.tm_yday, no need to fill them in.

    int milliseconds = TRY(arg_or(6, 0));
    return Value(1000.0 * timegm(&tm) + milliseconds);
}

}
