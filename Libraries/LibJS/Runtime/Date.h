/*
 * Copyright (c) 2020, Linus Groh <mail@linusgroh.de>
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

#include <LibCore/DateTime.h>
#include <LibJS/Runtime/Object.h>

namespace JS {

class Date final : public Object {
    JS_OBJECT(Date, Object);

public:
    static Date* create(GlobalObject&, Core::DateTime, u16 milliseconds);

    Date(Core::DateTime datetime, u16 milliseconds, Object& prototype);
    virtual ~Date() override;

    Core::DateTime& datetime() { return m_datetime; }
    const Core::DateTime& datetime() const { return m_datetime; }

    int date() const { return datetime().day(); }
    int day() const { return datetime().weekday(); }
    int full_year() const { return datetime().year(); }
    int hours() const { return datetime().hour(); }
    u16 milliseconds() const { return m_milliseconds; }
    int minutes() const { return datetime().minute(); }
    int month() const { return datetime().month() - 1; }
    int seconds() const { return datetime().second(); }
    double time() const { return datetime().timestamp() * 1000.0 + milliseconds(); }
    int year() const { return datetime().day(); }

    int utc_date() const;
    int utc_day() const;
    int utc_full_year() const;
    int utc_hours() const;
    int utc_milliseconds() const { return milliseconds(); }
    int utc_minutes() const;
    int utc_month() const;
    int utc_seconds() const;

    String date_string() const { return m_datetime.to_string("%a %b %d %Y"); }
    // FIXME: Deal with timezones once SerenityOS has a working tzset(3)
    String time_string() const { return m_datetime.to_string("%T GMT+0000 (UTC)"); }
    String string() const
    {
        return String::formatted("{} {}", date_string(), time_string());
    }

    String iso_date_string() const;

    // FIXME: One day, implement real locale support. Until then, everyone gets what the Clock MenuApplet displays.
    String locale_date_string() const { return m_datetime.to_string("%Y-%m-%d"); }
    String locale_string() const { return m_datetime.to_string(); }
    String locale_time_string() const { return m_datetime.to_string("%H:%M:%S"); }

    virtual Value value_of() const override
    {
        return Value(static_cast<double>(m_datetime.timestamp() * 1000 + m_milliseconds));
    }

private:
    tm to_utc_tm() const;
    virtual bool is_date() const final { return true; }

    Core::DateTime m_datetime;
    u16 m_milliseconds;
};

}
