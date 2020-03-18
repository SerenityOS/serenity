#include "Calendar.h"

const String name_of_month(int month)
{
    static const String month_names[] = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };
    return month_names[month - 1];
}

Calendar::Calendar(Core::DateTime date_time)
    : m_date_time(date_time)
    , m_selected_year(date_time.year())
    , m_selected_month(date_time.month())
{
}

Calendar::~Calendar()
{
}

const String Calendar::selected_date_text()
{
    return String::format("%s %d", name_of_month(m_selected_month).characters(), m_selected_year);
}

void Calendar::set_selected_date(int year, int month)
{
    m_selected_year = year;
    m_selected_month = month;
}

bool Calendar::is_today(Core::DateTime date_time) const
{
    return date_time.day() == m_date_time.day() && date_time.month() == m_date_time.month() && date_time.year() == m_date_time.year();
}
