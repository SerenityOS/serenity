/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <LibCore/ArgsParser.h>
#include <stdio.h>
#include <string.h>
#include <time.h>

const int line_width = 70;
const int line_count = 8;
const int column_width = 22;

char print_buffer[line_width * line_count];
char temp_buffer[line_width * 8];

int target_year;
int target_month;
int target_day;

int current_year;
int current_month;

int day_of_week(int day, int month, int year)
{
    static const int seek_table[] = { 0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4 };
    if (month < 3)
        --year;

    return (year + year / 4 - year / 100 + year / 400 + seek_table[month - 1] + day) % 7;
}

int get_number_of_days(int month, int year)
{
    bool is_leap_year = ((year % 400 == 0) || (year % 4 == 0 && year % 100 != 0));
    bool is_long_month = (month == 1 || month == 3 || month == 5 || month == 7 || month == 8 || month == 10 || month == 12);

    if (month == 2)
        return is_leap_year ? 29 : 28;

    return is_long_month ? 31 : 30;
}

void append_to_print(char* buffer, int row, int column, char* text)
{
    int starting_point = (line_width * row) + (column * column_width);
    for (int i = 0; text[i] != '\0'; i++) {
        buffer[starting_point + i] = text[i];
    }
}

void insert_month_to_print(int column, int month, int year)
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
    int first_day_of_week_for_month = day_of_week(1, month, year);
    int days_in_the_month = get_number_of_days(month, year);
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

void clean_buffers()
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

int main(int argc, char** argv)
{
    int day = 0;
    int month = 0;
    int year = 0;

    Core::ArgsParser args_parser;
    // FIXME: This should ensure two values get parsed as month + year
    args_parser.add_positional_argument(day, "Day of year", "day", Core::ArgsParser::Required::No);
    args_parser.add_positional_argument(month, "Month", "month", Core::ArgsParser::Required::No);
    args_parser.add_positional_argument(year, "Year", "year", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

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
        printf("                             ");
        printf("Year %4d", year);
        printf("                             \n\n");

        for (int i = 1; i < 12; ++i) {
            insert_month_to_print(0, i++, year);
            insert_month_to_print(1, i++, year);
            insert_month_to_print(2, i, year);
            printf(print_buffer);
            printf("\n");
            clean_buffers();
        }
    } else {
        insert_month_to_print(0, month, year);
        printf(print_buffer);
        printf("\n\n");
        clean_buffers();
    }

    return 0;
}
