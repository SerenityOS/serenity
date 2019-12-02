#include <stdio.h>
#include <time.h>

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

int main(int argc, char** argv)
{
    (void)argc;
    (void)argv;

    time_t now = time(nullptr);
    auto* tm = localtime(&now);
    int target_day = tm->tm_mday;
    int target_month = tm->tm_mon + 1;
    int target_year = tm->tm_year + 1900;
    int target_day_of_week = day_of_week(1, target_month, target_year);

    printf("     %02u - %04u    \n", target_month, target_year);
    printf("Su Mo Tu We Th Fr Sa\n");

    for (int i = 1; i <= get_number_of_days(target_month, target_year); ++i) {
        if (i < target_day_of_week) {
            printf("  ");
        } else {
            printf(i != target_day ? "%2d" : "\x1b[30;47m%2d\x1b[0m", i);
        }

        printf(i % 7 == 0 ? "\n" : " ");
    }
    printf("\n\n");
}
