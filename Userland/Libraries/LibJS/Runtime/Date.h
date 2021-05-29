/*
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/DateTime.h>
#include <LibJS/Runtime/Object.h>

namespace JS {

class Date final : public Object {
    JS_OBJECT(Date, Object);

public:
    static Date* create(GlobalObject&, Core::DateTime, u16 milliseconds, bool is_invalid = false);

    Date(Core::DateTime datetime, u16 milliseconds, bool is_invalid, Object& prototype);
    virtual ~Date() override;

    Core::DateTime& datetime() { return m_datetime; }
    const Core::DateTime& datetime() const { return m_datetime; }

    int date() const { return datetime().day(); }
    int day() const { return datetime().weekday(); }
    int hours() const { return datetime().hour(); }
    u16 milliseconds() const { return m_milliseconds; }
    int minutes() const { return datetime().minute(); }
    int month() const { return datetime().month() - 1; }
    int seconds() const { return datetime().second(); }
    double time() const { return datetime().timestamp() * 1000.0 + milliseconds(); }
    int year() const { return datetime().year(); }

    bool is_invalid() const { return m_is_invalid; }
    void set_is_invalid(bool value) { m_is_invalid = value; }

    int utc_date() const;
    int utc_day() const;
    int utc_full_year() const;
    int utc_hours() const;
    int utc_milliseconds() const { return milliseconds(); }
    int utc_minutes() const;
    int utc_month() const;
    int utc_seconds() const;

    void set_milliseconds(u16 milliseconds)
    {
        m_milliseconds = milliseconds;
    }

    String date_string() const { return m_datetime.to_string("%a %b %d %Y"); }
    // FIXME: Deal with timezones once SerenityOS has a working tzset(3)
    String time_string() const { return m_datetime.to_string("%T GMT+0000 (UTC)"); }
    String string() const
    {
        if (is_invalid())
            return "Invalid Date";

        return String::formatted("{} {}", date_string(), time_string());
    }

    String gmt_date_string() const;
    String iso_date_string() const;

    // FIXME: One day, implement real locale support. Until then, everyone gets what the Clock Applet displays.
    String locale_date_string() const { return m_datetime.to_string("%Y-%m-%d"); }
    String locale_string() const { return m_datetime.to_string(); }
    String locale_time_string() const { return m_datetime.to_string("%H:%M:%S"); }

    virtual Value value_of() const override
    {
        return Value(static_cast<double>(m_datetime.timestamp() * 1000 + m_milliseconds));
    }

private:
    tm to_utc_tm() const;

    Core::DateTime m_datetime;
    u16 m_milliseconds;
    bool m_is_invalid { false };
};

}
