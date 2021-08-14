/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <LibCore/DateTime.h>
#include <stdio.h>
#include <unistd.h>

class DiscordianDate {
public:
    explicit DiscordianDate(Core::DateTime date)
    {
        m_gregorian_date = date;
        m_yold = date.year() + 1166;

        uint16_t day = day_of_yold() + 1;
        if (is_leap_year() && day > m_st_tibs_day_of_yold)
            --day;

        m_day_of_week = day_of_week_from_day_of_yold(day);
        m_season = season_from_day_of_yold(day);
        m_date = date_from_day_of_yold(day);
    }

    const char* day_of_week() { return m_day_of_week.characters(); };
    const char* season() { return m_season.characters(); };
    uint16_t year() { return yold(); };
    uint16_t yold() { return m_yold; };
    uint16_t day_of_year() { return day_of_yold(); };
    uint16_t day_of_yold() { return m_gregorian_date.day_of_year(); };
    bool is_leap_year() { return m_gregorian_date.is_leap_year(); };
    bool is_st_tibs_day() { return (is_leap_year() && (day_of_yold() + 1) == m_st_tibs_day_of_yold); };

    String to_string()
    {
        if (is_st_tibs_day())
            return String::formatted("St. Tib's Day, in the YOLD {}", m_yold);
        return String::formatted("{}, day {} of {}, in the YOLD {}", m_day_of_week, m_date, m_season, m_yold);
    }

private:
    static const int m_days_in_week = 5;
    static const int m_days_in_season = 73;
    static const int m_st_tibs_day_of_yold = 60;
    Core::DateTime m_gregorian_date;
    String m_day_of_week;
    String m_season;
    int m_date;
    uint64_t m_yold; // 64-bit for use after X-Day in the YOLD 8661

    int date_from_day_of_yold(uint16_t day)
    {
        return (day % m_days_in_season == 0 ? m_days_in_season : day % m_days_in_season);
    }

    const char* day_of_week_from_day_of_yold(uint16_t day)
    {
        if (is_st_tibs_day())
            return nullptr;

        switch ((day % m_days_in_week) == 0 ? m_days_in_week : (day % m_days_in_week)) {
        case 1:
            return "Sweetmorn";
        case 2:
            return "Boomtime";
        case 3:
            return "Pungenday";
        case 4:
            return "Prickle-Prickle";
        case 5:
            return "Setting Orange";
        default:
            return nullptr;
        }
    }

    const char* season_from_day_of_yold(uint16_t day)
    {
        if (is_st_tibs_day())
            return nullptr;

        switch (((day % m_days_in_season) == 0 ? day - 1 : day) / m_days_in_season) {
        case 0:
            return "Chaos";
        case 1:
            return "Discord";
        case 2:
            return "Confusion";
        case 3:
            return "Bureaucracy";
        case 4:
            return "The Aftermath";
        default:
            return nullptr;
        }
    }
};

int main([[maybe_unused]] int argc, [[maybe_unused]] char** argv)
{
    if (pledge("stdio", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto date = Core::DateTime::now();
    outln("Today is {}", DiscordianDate(date).to_string());

    return 0;
}
