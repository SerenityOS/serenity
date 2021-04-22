/*
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <LibCore/DateTime.h>
#include <LibJS/Heap/Heap.h>
#include <LibJS/Runtime/Date.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

Date* Date::create(GlobalObject& global_object, Core::DateTime datetime, u16 milliseconds, bool is_invalid)
{
    return global_object.heap().allocate<Date>(global_object, datetime, milliseconds, is_invalid, *global_object.date_prototype());
}

Date::Date(Core::DateTime datetime, u16 milliseconds, bool is_invalid, Object& prototype)
    : Object(prototype)
    , m_datetime(datetime)
    , m_milliseconds(milliseconds)
    , m_is_invalid(is_invalid)
{
}

Date::~Date()
{
}

tm Date::to_utc_tm() const
{
    time_t timestamp = m_datetime.timestamp();
    struct tm tm;
    gmtime_r(&timestamp, &tm);
    return tm;
}

int Date::utc_date() const
{
    return to_utc_tm().tm_mday;
}

int Date::utc_day() const
{
    return to_utc_tm().tm_wday;
}

int Date::utc_full_year() const
{
    return to_utc_tm().tm_year + 1900;
}

int Date::utc_hours() const
{
    return to_utc_tm().tm_hour;
}

int Date::utc_minutes() const
{
    return to_utc_tm().tm_min;
}

int Date::utc_month() const
{
    return to_utc_tm().tm_mon;
}

int Date::utc_seconds() const
{
    return to_utc_tm().tm_sec;
}

String Date::gmt_date_string() const
{
    // Mon, 18 Dec 1995 17:28:35 GMT
    // FIXME: Note that we're totally cheating with the timezone part here..
    return datetime().to_string("%a, %e %b %Y %T GMT");
}

String Date::iso_date_string() const
{
    auto tm = to_utc_tm();
    int year = tm.tm_year + 1900;
    int month = tm.tm_mon + 1;

    StringBuilder builder;
    if (year < 0)
        builder.appendf("-%06d", -year);
    else if (year > 9999)
        builder.appendf("+%06d", year);
    else
        builder.appendf("%04d", year);
    builder.append('-');
    builder.appendf("%02d", month);
    builder.append('-');
    builder.appendf("%02d", tm.tm_mday);
    builder.append('T');
    builder.appendf("%02d", tm.tm_hour);
    builder.append(':');
    builder.appendf("%02d", tm.tm_min);
    builder.append(':');
    builder.appendf("%02d", tm.tm_sec);
    builder.append('.');
    builder.appendf("%03d", m_milliseconds);
    builder.append('Z');

    return builder.build();
}

}
