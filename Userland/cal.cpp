#include <stdio.h>
#include <time.h>

int dayOfWeek(int day, int month, int year)
{
    const int seekTable[] = { 0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4 };
    if (month < 3)
        --year;

    return (year + year / 4 - year / 100 + year / 400 + seekTable[month - 1] + day) % 7;
}

int getNumberOfDays(int month, int year)
{
    if (month == 2)
        return ((year % 400 == 0) || (year % 4 == 0 && year % 100 != 0)) ? 29 : 30;

    return (month == 1 || month == 3 || month == 5 || month == 7 || month == 8 || month == 10 || month == 12) ? 31 : 30;
}

int main(int argc, char** argv)
{
    (void)argc;
    (void)argv;

    time_t now = time(nullptr);
    auto* tm = localtime(&now);
    int targetDay = tm->tm_mday;
    int targetMonth = tm->tm_mon + 1;
    int targetYear = tm->tm_year + 1900;
    int targetDayOfWeek = dayOfWeek(1, targetMonth, targetYear);

    printf("     %02u - %04u    \n", targetMonth, targetYear);
    printf("Su Mo Tu We Th Fr Sa\n");

    for (int i = 1; i <= getNumberOfDays(targetMonth, targetYear); ++i) {
        if (i < targetDayOfWeek) {
            printf("  ");
        } else {
            printf(i != targetDay ? "%2d" : "\x1b[30;47m%2d\x1b[0m", i);
        }

        printf(i % 7 == 0 ? "\n" : " ");
    }
    printf("\n\n");
}
