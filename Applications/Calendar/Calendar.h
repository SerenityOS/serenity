#pragma once

#include <AK/String.h>

class Calendar final {
public:
    //NOTE: some methods from cal.cpp, maybe import these from AK/ in the future?
    static String get_name_of_month(int month);
    static int get_day();
    static int get_month();
    static int get_year();
    static bool is_today(int year, int month, int day);

    Calendar(int year, int month);
    ~Calendar();
    String get_selected_date_text();

    void set_selected_date(int year, int month);
    int get_selected_month() { return m_month; }
    int get_selected_year() { return m_year; }

private:
    int m_month;
    int m_year;
};
