/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Ariel Don <ariel@arieldon.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/CheckedFormatString.h>
#include <AK/GenericLexer.h>
#include <AK/Time.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibFileSystem/FileSystem.h>
#include <LibMain/Main.h>
#include <LibTimeZone/TimeZone.h>
#include <ctype.h>
#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/stat.h>
#include <time.h>

static ByteString program_name;

template<typename... Parameters>
[[noreturn]] static void err(CheckedFormatString<Parameters...>&& fmtstr, Parameters const&... parameters)
{
    warn("{}: ", program_name);
    warnln(move(fmtstr), parameters...);
    exit(1);
}

inline bool validate_timestamp(unsigned year, unsigned month, unsigned day, unsigned hour, unsigned minute, unsigned second)
{
    return (year >= 1970) && (month >= 1 && month <= 12) && (day >= 1 && day <= static_cast<unsigned>(days_in_month(year, month))) && (hour <= 23) && (minute <= 59) && (second <= 59);
}

static void parse_time(StringView input_time, timespec& atime, timespec& mtime)
{
    // Parse [[CC]YY]MMDDhhmm[.SS] format, where brackets signify optional
    // parameters.
    if (input_time.length() < 8)
        err("invalid time format '{}' -- too short", input_time);
    else if (input_time.length() > 15)
        err("invalid time format '{}' -- too long", input_time);

    Vector<u8> parameters;
    GenericLexer lexer(input_time);
    unsigned year, month, day, hour, minute, second;

    auto lex_number = [&]() {
        auto literal = lexer.consume(2);
        if (literal.length() < 2)
            err("invalid time format '{}' -- expected 2 digits per parameter", input_time);
        auto maybe_parameter = literal.to_number<unsigned>();
        if (maybe_parameter.has_value())
            parameters.append(maybe_parameter.value());
        else
            err("invalid time format '{}'", input_time);
    };

    while (!lexer.is_eof() && lexer.next_is(is_ascii_digit))
        lex_number();
    if (parameters.size() > 6)
        err("invalid time format '{}' -- too many parameters", input_time);

    if (lexer.consume_specific('.')) {
        lex_number();
        second = parameters.take_last();
    } else {
        second = 0;
    }

    auto current_year = seconds_since_epoch_to_year(time(nullptr));
    auto current_century = current_year / 100;
    if (parameters.size() == 6)
        year = parameters.take_first() * 100 + parameters.take_first();
    else if (parameters.size() == 5)
        year = current_century * 100 + parameters.take_first();
    else
        year = current_year;

    minute = parameters.take_last();
    hour = parameters.take_last();
    day = parameters.take_last();
    month = parameters.take_last();

    if (validate_timestamp(year, month, day, hour, minute, second))
        atime = mtime = AK::UnixDateTime::from_unix_time_parts(year, month, day, hour, minute, second, 0).to_timespec();
    else
        err("invalid time format '{}'", input_time);
}

static void parse_datetime(StringView input_datetime, timespec& atime, timespec& mtime)
{
    // Parse YYYY-MM-DDThh:mm:SS[.frac][tz] or YYYY-MM-DDThh:mm:SS[,frac][tz]
    // formats, where brackets signify optional parameters.
    GenericLexer lexer(input_datetime);
    unsigned year, month, day, hour, minute, second, millisecond;
    StringView time_zone;

    auto lex_number = [&](unsigned& value, size_t n) {
        auto maybe_value = lexer.consume(n).to_number<unsigned>();
        if (!maybe_value.has_value())
            err("invalid datetime format '{}' -- expected number at index {}", input_datetime, lexer.tell());
        else
            value = maybe_value.value();
    };

    lex_number(year, 4);
    if (!lexer.consume_specific('-'))
        err("invalid datetime format '{}' -- expected '-' after year", input_datetime);
    lex_number(month, 2);
    if (!lexer.consume_specific('-'))
        err("invalid datetime format '{}' -- expected '-' after month", input_datetime);
    lex_number(day, 2);

    // Parse the time designator -- a single 'T' or ' ' according to POSIX.
    if (!lexer.consume_specific('T') && !lexer.consume_specific(' '))
        err("invalid datetime format '{}' -- expected 'T' or ' ' for time designator", input_datetime);

    lex_number(hour, 2);
    if (!lexer.consume_specific(':'))
        err("invalid datetime format '{}' -- expected ':' after hour", input_datetime);
    lex_number(minute, 2);
    if (!lexer.consume_specific(':'))
        err("invalid datetime format '{}' -- expected ':' after minute", input_datetime);
    lex_number(second, 2);

    millisecond = 0;
    if (!lexer.is_eof()) {
        if (lexer.consume_specific(',') || lexer.consume_specific('.')) {
            auto fractional_second = lexer.consume_while(is_ascii_digit);
            if (fractional_second.is_empty())
                err("invalid datetime format '{}' -- expected floating seconds", input_datetime);
            for (u8 i = 0; i < 3 && i < fractional_second.length(); ++i) {
                unsigned n = fractional_second[i] - '0';
                switch (i) {
                case 0:
                    millisecond += 100 * n;
                    break;
                case 1:
                    millisecond += 10 * n;
                    break;
                case 2:
                    millisecond += n;
                    break;
                default:
                    VERIFY_NOT_REACHED();
                }
            }
        }

        time_zone = lexer.consume_all();
        if (!time_zone.is_empty() && time_zone != "Z")
            err("invalid datetime format '{}' -- failed to parse time zone", input_datetime);
    }

    if (validate_timestamp(year, month, day, hour, minute, second)) {
        auto timestamp = AK::UnixDateTime::from_unix_time_parts(year, month, day, hour, minute, second, millisecond);
        auto time = timestamp.to_timespec();
        if (time_zone.is_empty() && TimeZone::system_time_zone() != "UTC") {
            auto offset = TimeZone::get_time_zone_offset(TimeZone::system_time_zone(), timestamp);
            if (offset.has_value())
                time.tv_sec -= offset.value().seconds;
            else
                err("failed to get the system time zone");
        }
        atime = mtime = time;
    } else {
        err("invalid datetime format '{}'", input_datetime);
    }
}

static void reference_time(StringView reference_path, timespec& atime, timespec& mtime)
{
    auto maybe_buffer = Core::System::stat(reference_path);
    if (maybe_buffer.is_error())
        err("failed to reference times of '{}': {}", reference_path, maybe_buffer.release_error());
    auto buffer = maybe_buffer.release_value();
    atime.tv_sec = buffer.st_atime;
    atime.tv_nsec = buffer.st_atim.tv_nsec;
    mtime.tv_sec = buffer.st_mtime;
    mtime.tv_nsec = buffer.st_mtim.tv_nsec;
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath cpath fattr"));

    program_name = arguments.strings[0];

    Vector<ByteString> paths;

    timespec times[2];
    auto& atime = times[0];
    auto& mtime = times[1];

    bool update_atime = false;
    bool update_mtime = false;
    bool no_create_file = false;

    ByteString input_datetime = "";
    ByteString input_time = "";
    ByteString reference_path = "";

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Create a file or update file access time and/or modification time.");
    args_parser.add_ignored(nullptr, 'f');
    args_parser.add_option(update_atime, "Change access time of file", nullptr, 'a');
    args_parser.add_option(no_create_file, "Do not create a file if it does not exist", nullptr, 'c');
    args_parser.add_option(update_mtime, "Change modification time of file", nullptr, 'm');
    args_parser.add_option(input_datetime, "Use specified datetime instead of current time", nullptr, 'd', "datetime");
    args_parser.add_option(input_time, "Use specified time instead of current time", nullptr, 't', "time");
    args_parser.add_option(reference_path, "Use time of file specified by reference path instead of current time", nullptr, 'r', "reference");
    args_parser.add_positional_argument(paths, "Files to touch", "path", Core::ArgsParser::Required::Yes);
    args_parser.parse(arguments);

    if (input_datetime.is_empty() + input_time.is_empty() + reference_path.is_empty() < 2)
        err("cannot specify a time with more than one option");

    if (!input_datetime.is_empty())
        parse_datetime(input_datetime, atime, mtime);
    else if (!input_time.is_empty())
        parse_time(input_time, atime, mtime);
    else if (!reference_path.is_empty())
        reference_time(reference_path, atime, mtime);
    else
        atime.tv_nsec = mtime.tv_nsec = UTIME_NOW;

    // According to POSIX, if neither -a nor -m are specified, then the program
    // should behave as if both are specified.
    if (!update_atime && !update_mtime)
        update_atime = update_mtime = true;
    if (update_atime && !update_mtime)
        mtime.tv_nsec = UTIME_OMIT;
    if (update_mtime && !update_atime)
        atime.tv_nsec = UTIME_OMIT;

    auto has_errors = false;
    for (auto path : paths) {
        if (FileSystem::exists(path)) {
            if (utimensat(AT_FDCWD, path.characters(), times, 0) == -1) {
                warnln("failed to touch '{}': {}", path, strerror(errno));
                has_errors = true;
            }
        } else if (!no_create_file) {
            auto error_or_fd = Core::System::open(path, O_CREAT, 0100644);
            if (error_or_fd.is_error()) {
                warnln("failed to open '{}': {}", path, strerror(error_or_fd.error().code()));
                has_errors = true;
                continue;
            }

            if (futimens(error_or_fd.value(), times) == -1) {
                warnln("failed to touch '{}': {}", path, strerror(errno));
                has_errors = true;
                continue;
            }
            (void)Core::System::close(error_or_fd.value());
        }
    }
    return has_errors ? 1 : 0;
}
