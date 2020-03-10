#pragma once

#include <AK/String.h>
#include <LibCore/DateTime.h>

const String name_of_month(int month);

class Calendar final {
public:
    Calendar(Core::DateTime date_time);
    ~Calendar();

    const String selected_date_text();
    void set_selected_date(int year, int month);
    int selected_year() const { return m_selected_year; }
    int selected_month() const { return m_selected_month; }
    bool is_today(Core::DateTime date_time) const;

private:
    Core::DateTime m_date_time;
    int m_selected_year { 0 };
    int m_selected_month { 0 };
};
