/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/String.h>
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

    // FIXME: This should be replaced with a proper comparison
    //        operator when we get the equivalent of strptime
    bool is_before(const String&) const;

private:
    time_t m_timestamp { 0 };
    unsigned m_year { 0 };
    unsigned m_month { 0 };
    unsigned m_day { 0 };
    unsigned m_hour { 0 };
    unsigned m_minute { 0 };
    unsigned m_second { 0 };
};

const LogStream& operator<<(const LogStream&, const DateTime&);

}
