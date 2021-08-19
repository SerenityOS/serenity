/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
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

    void set_time(unsigned year, unsigned month = 1, unsigned day = 0, unsigned hour = 0, unsigned minute = 0, unsigned second = 0);
    String to_string(const String& format = "%Y-%m-%d %H:%M:%S") const;

    static DateTime create(unsigned year, unsigned month = 1, unsigned day = 0, unsigned hour = 0, unsigned minute = 0, unsigned second = 0);
    static DateTime now();
    static DateTime from_timestamp(time_t);
    static DateTime from_tm(struct tm);
    static Optional<DateTime> parse(const String& format, const String& string);

    bool operator<(const DateTime& other) const { return m_timestamp < other.m_timestamp; }

private:
    time_t m_timestamp { 0 };
    unsigned m_year { 0 };
    unsigned m_month { 0 };
    unsigned m_day { 0 };
    unsigned m_hour { 0 };
    unsigned m_minute { 0 };
    unsigned m_second { 0 };
};

}

namespace IPC {

bool encode(IPC::Encoder&, const Core::DateTime&);
bool decode(IPC::Decoder&, Core::DateTime&);

}
