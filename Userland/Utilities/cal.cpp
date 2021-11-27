/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/DateTime.h>
#include <LibMain/Main.h>
#include <stdio.h>
#include <string.h>
#include <time.h>

const int line_width = 70;
const int line_count = 8;
const int column_width = 22;

char print_buffer[line_width * line_count];
char temp_buffer[line_width * 8];

int target_day;

int current_year;
int current_month;

static void append_to_print(char* buffer, int row, int column, char* text)
{
    int starting_point = (line_width * row) + (column * column_width);
    for (int i = 0; text[i] != '\0'; i++) {
        buffer[starting_point + i] = text[i];
    }
}

static void insert_month_to_print(int column, int month, int year)
{
    int printing_column = column;
    int printing_row = 0;

    // FIXME: Both the month name and month header text should be provided by a locale
    sprintf(temp_buffer, "     %02u - %04u    ", month, year);
    append_to_print(print_buffer, printing_row, printing_column, temp_buffer);
    printing_row++;

    sprintf(temp_buffer, "Su Mo Tu We Th Fr Sa");
    append_to_print(print_buffer, printing_row, printing_column, temp_buffer);
    printing_row++;
    int day_to_print = 1;
    auto date_time = Core::DateTime::create(year, month, 1);
    int first_day_of_week_for_month = date_time.weekday();
    int days_in_the_month = date_time.days_in_month();
    int last_written_chars = 0;
    for (int i = 1; day_to_print <= days_in_the_month; ++i) {
        if (i - 1 < first_day_of_week_for_month) {
            last_written_chars += sprintf(temp_buffer + last_written_chars, "   ");
        } else {
            if (year == current_year && month == current_month && target_day == day_to_print) {
                // FIXME: To replicate Unix cal it would be better to use "\x1b[30;47m%2d\x1b[0m " in here instead of *.
                //        However, doing that messes up the layout.
                last_written_chars += sprintf(temp_buffer + last_written_chars, "%2d*", day_to_print);
            } else {
                last_written_chars += sprintf(temp_buffer + last_written_chars, "%2d ", day_to_print);
            }
            day_to_print++;
        }

        append_to_print(print_buffer, printing_row, printing_column, temp_buffer);

        if (i % 7 == 0) {
            printing_row++;
            memset(temp_buffer, ' ', line_width * 8);
            temp_buffer[line_width * 8 - 1] = '\0';
            last_written_chars = 0;
        }
    }
}

static void clean_buffers()
{
    for (int i = 1; i < line_width * line_count; ++i) {
        print_buffer[i - 1] = i % line_width == 0 ? '\n' : ' ';
    }
    print_buffer[line_width * line_count - 1] = '\0';

    for (int i = 0; i < line_width; ++i) {
        temp_buffer[i] = ' ';
    }
    temp_buffer[line_width - 1] = '\0';
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    int day = 0;
    int month = 0;
    int year = 0;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Display a nice overview of a month or year, defaulting to the current month.");
    // FIXME: This should ensure two values get parsed as month + year
    args_parser.add_positional_argument(day, "Day of year", "day", Core::ArgsParser::Required::No);
    args_parser.add_positional_argument(month, "Month", "month", Core::ArgsParser::Required::No);
    args_parser.add_positional_argument(year, "Year", "year", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    time_t now = time(nullptr);
    auto* tm = localtime(&now);

    // Hack: workaround two values parsing as day + month.
    if (day && month && !year) {
        year = month;
        month = day;
        day = 0;
    }

    bool year_mode = !day && !month && year;

    if (!year)
        year = tm->tm_year + 1900;
    if (!month)
        month = tm->tm_mon + 1;
    if (!day)
        day = tm->tm_mday;

    current_year = year;
    current_month = month;

    clean_buffers();

    if (year_mode) {
        out("                           Year {:04}                            ", year);
        outln();
        outln();

        for (int i = 1; i < 12; ++i) {
            insert_month_to_print(0, i++, year);
            insert_month_to_print(1, i++, year);
            insert_month_to_print(2, i, year);
            outln("{}", print_buffer);
            clean_buffers();
        }
    } else {
        insert_month_to_print(0, month, year);
        outln("{}", print_buffer);
        clean_buffers();
    }

    return 0;
}
