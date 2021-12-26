/*
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2020, Nico Weber <thakis@chromium.org>
 * Copyright (c) 2021, Petróczi Zoltán <petroczizoltan@tutanota.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/GenericLexer.h>
#include <LibCore/DateTime.h>
#include <LibJS/Runtime/Date.h>
#include <LibJS/Runtime/DateConstructor.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/VM.h>
#include <ctype.h>
#include <sys/time.h>
#include <time.h>

namespace JS {

static Value parse_simplified_iso8601(const String& iso_8601)
{
    // Date.parse() is allowed to accept many formats. We strictly only accept things matching
    // http://www.ecma-international.org/ecma-262/#sec-date-time-string-format
    GenericLexer lexer(iso_8601);
    auto lex_n_digits = [&](size_t n, int& out) {
        if (lexer.tell_remaining() < n)
            return false;
        int r = 0;
        for (size_t i = 0; i < n; ++i) {
            char ch = lexer.consume();
            if (!isdigit(ch))
                return false;
            r = 10 * r + ch - '0';
        }
        out = r;
        return true;
    };

    int year = -1, month = -1, day = -1;
    int hours = -1, minutes = -1, seconds = -1, milliseconds = -1;
    char timezone = -1;
    int timezone_hours = -1, timezone_minutes = -1;
    auto lex_year = [&]() {
        if (lexer.consume_specific('+'))
            return lex_n_digits(6, year);
        if (lexer.consume_specific('-')) {
            int absolute_year;
            if (!lex_n_digits(6, absolute_year))
                return false;
            year = -absolute_year;
            return true;
        }
        return lex_n_digits(4, year);
    };
    auto lex_month = [&]() { return lex_n_digits(2, month) && month >= 1 && month <= 12; };
    auto lex_day = [&]() { return lex_n_digits(2, day) && day >= 1 && day <= 31; };
    auto lex_date = [&]() { return lex_year() && (!lexer.consume_specific('-') || (lex_month() && (!lexer.consume_specific('-') || lex_day()))); };

    auto lex_hours_minutes = [&](int& out_h, int& out_m) {
        int h, m;
        if (lex_n_digits(2, h) && h >= 0 && h <= 24 && lexer.consume_specific(':') && lex_n_digits(2, m) && m >= 0 && m <= 59) {
            out_h = h;
            out_m = m;
            return true;
        }
        return false;
    };
    auto lex_seconds = [&]() { return lex_n_digits(2, seconds) && seconds >= 0 && seconds <= 59; };
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

    // We parsed a valid date simplified ISO 8601 string. Values not present in the string are -1.
    VERIFY(year != -1); // A valid date string always has at least a year.
    struct tm tm = {};
    tm.tm_year = year - 1900;
    tm.tm_mon = month == -1 ? 0 : month - 1;
    tm.tm_mday = day == -1 ? 1 : day;
    tm.tm_hour = hours == -1 ? 0 : hours;
    tm.tm_min = minutes == -1 ? 0 : minutes;
    tm.tm_sec = seconds == -1 ? 0 : seconds;

    // http://www.ecma-international.org/ecma-262/#sec-date.parse:
    // "When the UTC offset representation is absent, date-only forms are interpreted as a UTC time and date-time forms are interpreted as a local time."
    time_t timestamp;
    if (timezone != -1 || hours == -1)
        timestamp = timegm(&tm);
    else
        timestamp = mktime(&tm);

    if (timezone == '-')
        timestamp += (timezone_hours * 60 + timezone_minutes) * 60;
    else if (timezone == '+')
        timestamp -= (timezone_hours * 60 + timezone_minutes) * 60;

    // FIXME: reject timestamp if resulting value wouldn't fit in a double

    if (milliseconds == -1)
        milliseconds = 0;
    return Value(1000.0 * timestamp + milliseconds);
}

DateConstructor::DateConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.Date, *global_object.function_prototype())
{
}

void DateConstructor::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    NativeFunction::initialize(global_object);
    define_property(vm.names.prototype, global_object.date_prototype(), 0);
    define_property(vm.names.length, Value(7), Attribute::Configurable);

    define_native_function(vm.names.now, now, 0, Attribute::Writable | Attribute::Configurable);
    define_native_function(vm.names.parse, parse, 1, Attribute::Writable | Attribute::Configurable);
    define_native_function(vm.names.UTC, utc, 1, Attribute::Writable | Attribute::Configurable);
}

DateConstructor::~DateConstructor()
{
}

Value DateConstructor::call()
{
    return js_string(heap(), Date::now(global_object())->string());
}

Value DateConstructor::construct(Function&)
{
    auto& vm = this->vm();
    if (vm.argument_count() == 0)
        return Date::now(global_object());

    auto create_invalid_date = [this]() {
        auto datetime = Core::DateTime::create(1970, 1, 1, 0, 0, 0);
        auto milliseconds = static_cast<i16>(0);
        return Date::create(global_object(), datetime, milliseconds, true);
    };

    if (vm.argument_count() == 1) {
        auto value = vm.argument(0);
        if (value.is_string())
            value = parse_simplified_iso8601(value.as_string().string());
        else
            value = value.to_number(global_object());

        if (vm.exception())
            return {};

        if (!value.is_finite_number()) {
            return create_invalid_date();
        }

        // A timestamp since the epoch, in UTC.
        double value_as_double = value.as_double();
        if (value_as_double > Date::time_clip)
            return create_invalid_date();
        auto datetime = Core::DateTime::from_timestamp(static_cast<time_t>(value_as_double / 1000));
        auto milliseconds = static_cast<i16>(fmod(value_as_double, 1000));
        return Date::create(global_object(), datetime, milliseconds);
    }

    // A date/time in components, in local time.
    auto arg_or = [&vm, this](size_t i, i32 fallback) { return vm.argument_count() > i ? vm.argument(i).to_number(global_object()) : Value(fallback); };

    auto year_value = vm.argument(0).to_number(global_object());
    if (vm.exception())
        return {};
    if (!year_value.is_finite_number()) {
        return create_invalid_date();
    }
    auto year = year_value.as_i32();

    auto month_index_value = vm.argument(1).to_number(global_object());
    if (vm.exception())
        return {};
    if (!month_index_value.is_finite_number()) {
        return create_invalid_date();
    }
    auto month_index = month_index_value.as_i32();

    auto day_value = arg_or(2, 1);
    if (vm.exception())
        return {};
    if (!day_value.is_finite_number()) {
        return create_invalid_date();
    }
    auto day = day_value.as_i32();

    auto hours_value = arg_or(3, 0);
    if (vm.exception())
        return {};
    if (!hours_value.is_finite_number()) {
        return create_invalid_date();
    }
    auto hours = hours_value.as_i32();

    auto minutes_value = arg_or(4, 0);
    if (vm.exception())
        return {};
    if (!minutes_value.is_finite_number()) {
        return create_invalid_date();
    }
    auto minutes = minutes_value.as_i32();

    auto seconds_value = arg_or(5, 0);
    if (vm.exception())
        return {};
    if (!seconds_value.is_finite_number()) {
        return create_invalid_date();
    }
    auto seconds = seconds_value.as_i32();

    auto milliseconds_value = arg_or(6, 0);
    if (vm.exception())
        return {};
    if (!milliseconds_value.is_finite_number()) {
        return create_invalid_date();
    }
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
    auto* date = Date::create(global_object(), datetime, milliseconds);
    if (date->time() > Date::time_clip)
        return create_invalid_date();
    return date;
}

JS_DEFINE_NATIVE_FUNCTION(DateConstructor::now)
{
    struct timeval tv;
    gettimeofday(&tv, nullptr);
    return Value(tv.tv_sec * 1000.0 + tv.tv_usec / 1000.0);
}

JS_DEFINE_NATIVE_FUNCTION(DateConstructor::parse)
{
    if (!vm.argument_count())
        return js_nan();

    auto iso_8601 = vm.argument(0).to_string(global_object);
    if (vm.exception())
        return {};

    return parse_simplified_iso8601(iso_8601);
}

JS_DEFINE_NATIVE_FUNCTION(DateConstructor::utc)
{
    auto arg_or = [&vm, &global_object](size_t i, i32 fallback) { return vm.argument_count() > i ? vm.argument(i).to_i32(global_object) : fallback; };
    int year = vm.argument(0).to_i32(global_object);
    if (year >= 0 && year <= 99)
        year += 1900;

    struct tm tm = {};
    tm.tm_year = year - 1900;
    tm.tm_mon = arg_or(1, 0); // 0-based in both tm and JavaScript
    tm.tm_mday = arg_or(2, 1);
    tm.tm_hour = arg_or(3, 0);
    tm.tm_min = arg_or(4, 0);
    tm.tm_sec = arg_or(5, 0);
    // timegm() doesn't read tm.tm_wday and tm.tm_yday, no need to fill them in.

    int milliseconds = arg_or(6, 0);
    return Value(1000.0 * timegm(&tm) + milliseconds);
}
}
