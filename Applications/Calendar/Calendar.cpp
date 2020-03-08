#include "Calendar.h"
#include <time.h>

String Calendar::get_name_of_month(int month)
{
    switch (month) {
    case 1:
        return "Jan";
    case 2:
        return "Feb";
    case 3:
        return "Mar";
    case 4:
        return "Apr";
    case 5:
        return "May";
    case 6:
        return "Jun";
    case 7:
        return "Jul";
    case 8:
        return "Aug";
    case 9:
        return "Sep";
    case 10:
        return "Oct";
    case 11:
        return "Nov";
    case 12:
        return "Dec";
    default:
        ASSERT(false);
    }

    return "";
}

int Calendar::get_day()
{
    time_t now = time(nullptr);
    auto* tm = localtime(&now);
    return tm->tm_mday;
}

int Calendar::get_month()
{
    time_t now = time(nullptr);
    auto* tm = localtime(&now);
    return tm->tm_mon + 1;
}

int Calendar::get_year()
{
    time_t now = time(nullptr);
    auto* tm = localtime(&now);
    return tm->tm_year + 1900;
}

bool Calendar::is_today(int year, int month, int day)
{
    return day == get_day() && month == get_month() && year == get_year();
}

Calendar::Calendar(int year, int month)
    : m_month(month)
    , m_year(year)
{
}

Calendar::~Calendar()
{
}

void Calendar::set_selected_date(int year, int month)
{
    m_year = year;
    m_month = month;
}

String Calendar::get_selected_date_text()
{
    return String::format("%s %d", get_name_of_month(m_month).characters(), m_year);
}
