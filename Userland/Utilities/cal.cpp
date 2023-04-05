/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/DateConstants.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/StringView.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DateTime.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <time.h>

#define ANSI_INVERT_OUTPUT "\e[7m"
#define ANSI_RESET_OUTPUT "\e[0m"

// TODO: months are in reality 20-characters wide, but each line contains an extra erronous unneeded space at the end
// so making this 20 exactly breaks formatting a bit
int constexpr month_width = "01 02 03 04 05 06 07"sv.length() + 1;
// three months plus padding between them
int constexpr year_width = 3 * month_width + 2 * "  "sv.length();

int current_year;
int current_month;
int current_day;

static ErrorOr<StringView> month_name(int month)
{
    int month_index = month - 1;

    if (month_index < 0 || month_index >= static_cast<int>(AK::long_month_names.size()))
        return Error::from_string_view("Month out of range"sv);

    return AK::long_month_names.at(month_index);
}

static ErrorOr<Vector<String>> month_lines_to_print(int month, int year)
{
    Vector<String> lines;

    // FIXME: Both the month name and month header text should be provided by a locale
    TRY(lines.try_append(TRY(String::formatted("{: ^{}s}", TRY(String::formatted("{} - {}", TRY(month_name(month)), year)), month_width))));
    TRY(lines.try_append(TRY(String::from_utf8("Su Mo Tu We Th Fr Sa"sv))));

    int day_to_print = 1;

    auto date_time = Core::DateTime::create(year, month, 1);
    int first_day_of_week_for_month = date_time.weekday();
    int days_in_the_month = date_time.days_in_month();

    StringBuilder row;
    for (int i = 1; day_to_print <= days_in_the_month; ++i) {
        if (i - 1 < first_day_of_week_for_month) {
            row.append("   "sv);
        } else {
            if (year == current_year && month == current_month && day_to_print == current_day) {
                row.appendff(ANSI_INVERT_OUTPUT "{:02}" ANSI_RESET_OUTPUT " ", day_to_print);
            } else {
                row.appendff("{:02} ", day_to_print);
            }
            day_to_print++;
        }

        if (i % 7 == 0) {
            TRY(lines.try_append(TRY(row.to_string())));
            row.clear();
        }
    }

    TRY(lines.try_append(TRY(row.to_string())));

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

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"));
    TRY(Core::System::unveil("/etc/timezone", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    int month = 0;
    int year = 0;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Display a nice overview of a month or year, defaulting to the current month.");
    // FIXME: This should ensure one value gets parsed as just a year
    args_parser.add_positional_argument(month, "Month", "month", Core::ArgsParser::Required::No);
    args_parser.add_positional_argument(year, "Year", "year", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

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

    bool year_mode = !month && year;

    if (!year)
        year = current_year;
    if (!month)
        month = current_month;

    if (year_mode) {
        outln("{: ^{}}", TRY(String::formatted("Year {}", year)), year_width);

        for (int i = 1; i < 12; ++i) {
            outln();
            outln();
            Vector<String> lines_left = TRY(month_lines_to_print(i++, year));
            Vector<String> lines_center = TRY(month_lines_to_print(i++, year));
            Vector<String> lines_right = TRY(month_lines_to_print(i, year));
            print_months_side_by_side(lines_left, lines_center, lines_right);
        }
    } else {
        Vector<String> lines = TRY(month_lines_to_print(month, year));
        for (String const& line : lines) {
            outln("{}", line);
        }
    }

    return 0;
}
