/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/StringView.h>
#include <LibIPC/Forward.h>
#include <time.h>

namespace Core {

// Represents a time in local time.
class DateTime {
public:
    time_t timestamp() const { return m_timestamp; }

    unsigned year() const { return m_year; }
    unsigned month() const { return m_month; }
    unsigned day() const { return m_day; }

    unsigned hour() const { return m_hour; }
    unsigned minute() const { return m_minute; }
    unsigned second() const { return m_second; }
    unsigned weekday() const;
    unsigned days_in_month() const;
    unsigned day_of_year() const;
    bool is_leap_year() const;

    void set_time(int year, int month = 1, int day = 1, int hour = 0, int minute = 0, int second = 0);
    String to_string(StringView format = "%Y-%m-%d %H:%M:%S"sv) const;

    static DateTime create(int year, int month = 1, int day = 1, int hour = 0, int minute = 0, int second = 0);
    static DateTime now();
    static DateTime from_timestamp(time_t);
    static Optional<DateTime> parse(StringView format, String const& string);

    bool operator<(DateTime const& other) const { return m_timestamp < other.m_timestamp; }

private:
    time_t m_timestamp { 0 };
    int m_year { 0 };
    int m_month { 0 };
    int m_day { 0 };
    int m_hour { 0 };
    int m_minute { 0 };
    int m_second { 0 };
};

}

namespace IPC {

bool encode(Encoder&, Core::DateTime const&);
ErrorOr<void> decode(Decoder&, Core::DateTime&);

}
