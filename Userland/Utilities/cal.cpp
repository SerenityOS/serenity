/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Karol Baraniecki <karol@baraniecki.eu>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/DateConstants.h>
#include <AK/Find.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/StringUtils.h>
#include <AK/StringView.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/DateTime.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <time.h>

#define ANSI_INVERT_OUTPUT "\e[7m"
#define ANSI_RESET_OUTPUT "\e[0m"

int constexpr month_width = "01 02 03 04 05 06 07"sv.length();
// three months plus padding between them
int constexpr year_width = 3 * month_width + 2 * "  "sv.length();

int current_year;
int current_month;
int current_day;

static ErrorOr<int> weekday_index(StringView weekday_name)
{
    auto is_same_weekday_name = [&weekday_name](StringView other) {
        return AK::StringUtils::equals_ignoring_ascii_case(weekday_name, other);
    };

    if (auto it = AK::find_if(AK::long_day_names.begin(), AK::long_day_names.end(), is_same_weekday_name); !it.is_end())
        return it.index();
    if (auto it = AK::find_if(AK::short_day_names.begin(), AK::short_day_names.end(), is_same_weekday_name); !it.is_end())
        return it.index();
    if (auto it = AK::find_if(AK::mini_day_names.begin(), AK::mini_day_names.end(), is_same_weekday_name); !it.is_end())
        return it.index();

    if (auto numeric_weekday = AK::StringUtils::convert_to_int(weekday_name); numeric_weekday.has_value())
        return numeric_weekday.value();

    return Error::from_string_view("Unknown weekday name"sv);
}

static ErrorOr<int> default_weekday_start()
{
    auto calendar_config = TRY(Core::ConfigFile::open_for_app("Calendar"sv));
    String default_first_day_of_week = TRY(String::from_byte_string(calendar_config->read_entry("View"sv, "FirstDayOfWeek"sv, "Sunday"sv)));
    return TRY(weekday_index(default_first_day_of_week));
}

static ErrorOr<StringView> month_name(int month)
{
    int month_index = month - 1;

    if (month_index < 0 || month_index >= static_cast<int>(AK::long_month_names.size()))
        return Error::from_string_view("Month out of range"sv);

    return AK::long_month_names.at(month_index);
}

static ErrorOr<String> weekday_names_header(int start_of_week)
{
    // Generates a header in a style of "Su Mo Tu We Th Fr Sa"

    Vector<String> weekdays;
    for (size_t i = 0; i < AK::mini_day_names.size(); i++) {
        size_t day_index = (i + start_of_week) % mini_day_names.size();
        TRY(weekdays.try_append(TRY(String::from_utf8(AK::mini_day_names.at(day_index)))));
    }
    return TRY(String::join(' ', weekdays));
}

enum class Header {
    MonthAndYear,
    Month,
};

static ErrorOr<Vector<String>> month_lines_to_print(Header header_mode, int start_of_week, int month, int year)
{
    Vector<String> lines;

    // FIXME: Both the month name and month header text should be provided by a locale
    String header;
    switch (header_mode) {
    case Header::Month:
        header = TRY(String::from_utf8(TRY(month_name(month))));
        break;
    case Header::MonthAndYear:
        header = TRY(String::formatted("{} - {}", TRY(month_name(month)), year));
        break;
    }

    TRY(lines.try_append(TRY(String::formatted("{: ^{}s}", header, month_width))));
    TRY(lines.try_append(TRY(weekday_names_header(start_of_week))));

    auto date_time = Core::DateTime::create(year, month, 1);
    int first_day_of_week_for_month = date_time.weekday();
    int days_in_month = date_time.days_in_month();

    first_day_of_week_for_month += 7 - start_of_week;
    first_day_of_week_for_month %= 7;

    Vector<String> days_in_row;
    int day = 1;
    for (int i = 1; day <= days_in_month; ++i) {
        if (i - 1 < first_day_of_week_for_month) {
            TRY(days_in_row.try_append("  "_string));
        } else {
            if (year == current_year && month == current_month && day == current_day) {
                TRY(days_in_row.try_append(TRY(String::formatted(ANSI_INVERT_OUTPUT "{:2}" ANSI_RESET_OUTPUT, day))));
            } else {
                TRY(days_in_row.try_append(TRY(String::formatted("{:2}", day))));
            }
            day++;
        }

        if (i % 7 == 0) {
            TRY(lines.try_append(TRY(String::join(' ', days_in_row))));
            days_in_row.clear();
        }
    }

    TRY(lines.try_append(TRY(String::join(' ', days_in_row))));

    return lines;
}

static void print_months_side_by_side(Vector<String> const& left_month, Vector<String> const& center_month, Vector<String> const& right_month)
{
    for (size_t i = 0; i < left_month.size() || i < center_month.size() || i < right_month.size(); i++) {
        StringView left = i < left_month.size() ? left_month[i] : ""sv;
        StringView center = i < center_month.size() ? center_month[i] : ""sv;
        StringView right = i < right_month.size() ? right_month[i] : ""sv;

        outln("{: <{}}  {: <{}}  {: <{}}", left, month_width, center, month_width, right, month_width);
    }
}

static void go_to_next_month(int& month, int& year)
{
    month += 1;
    if (month > 12) {
        year += 1;
        month = 1;
    }
}

static void go_to_previous_month(int& month, int& year)
{
    month -= 1;
    if (month < 1) {
        year -= 1;
        month = 12;
    }
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath cpath"));

    int month = 0;
    int year = 0;
    StringView week_start_day_name {};
    bool three_month_mode = false;
    bool year_mode = false;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Display a nice overview of a month or year, defaulting to the current month.");
    // FIXME: This should ensure one value gets parsed as just a year
    args_parser.add_positional_argument(month, "Month", "month", Core::ArgsParser::Required::No);
    args_parser.add_positional_argument(year, "Year", "year", Core::ArgsParser::Required::No);
    args_parser.add_option(week_start_day_name, "Day that starts the week", "starting-day", 's', "day");
    args_parser.add_option(year_mode, "Show the whole year at once", "year", 'y');
    args_parser.add_option(three_month_mode, "Show the previous and next month beside the current one", "three-month-view", '3');
    args_parser.parse(arguments);

    if (three_month_mode && year_mode) {
        warnln("cal: Cannot specify both --year and --three-month-mode at the same time");
        return 1;
    }

    time_t now = time(nullptr);
    auto* tm = localtime(&now);
    current_year = tm->tm_year + 1900;
    current_month = tm->tm_mon + 1;
    current_day = tm->tm_mday;

    // Hack: workaround one value parsing as a month
    if (month && !year) {
        year = month;
        month = 0;
    }

    if (!month && year)
        year_mode = true;

    int week_start_day;
    if (week_start_day_name.is_empty())
        week_start_day = TRY(default_weekday_start());
    else
        week_start_day = TRY(weekday_index(week_start_day_name));

    if (!year)
        year = current_year;
    if (!month)
        month = current_month;

    if (year_mode) {
        outln("{: ^{}}", TRY(String::formatted("Year {}", year)), year_width);

        for (int month_index = 1; month_index < 12; ++month_index) {
            outln();
            outln();
            Vector<String> lines_left = TRY(month_lines_to_print(Header::Month, week_start_day, month_index++, year));
            Vector<String> lines_center = TRY(month_lines_to_print(Header::Month, week_start_day, month_index++, year));
            Vector<String> lines_right = TRY(month_lines_to_print(Header::Month, week_start_day, month_index, year));
            print_months_side_by_side(lines_left, lines_center, lines_right);
        }
    } else if (three_month_mode) {
        int month_on_left = month, year_on_left = year;
        go_to_previous_month(month_on_left, year_on_left);

        int month_on_right = month, year_on_right = year;
        go_to_next_month(month_on_right, year_on_right);

        Vector<String> lines_previous_month = TRY(month_lines_to_print(Header::MonthAndYear, week_start_day, month_on_left, year_on_left));
        Vector<String> lines_current_month = TRY(month_lines_to_print(Header::MonthAndYear, week_start_day, month, year));
        Vector<String> lines_next_month = TRY(month_lines_to_print(Header::MonthAndYear, week_start_day, month_on_right, year_on_right));
        print_months_side_by_side(lines_previous_month, lines_current_month, lines_next_month);
    } else {
        Vector<String> lines = TRY(month_lines_to_print(Header::MonthAndYear, week_start_day, month, year));
        for (String const& line : lines) {
            outln("{}", line);
        }
    }

    return 0;
}
