#include <stdio.h>
#include <string.h>
#include <time.h>

int main(int argc, char** argv)
{
    time_t now = time(nullptr);

    if (argc == 2 && !strcmp(argv[1], "-u")) {
        printf("%u\n", now);
        return 0;
    }

    auto* tm = localtime(&now);
    printf("%4u-%02u-%02u %02u:%02u:%02u\n",
        tm->tm_year + 1900,
        tm->tm_mon + 1,
        tm->tm_mday,
        tm->tm_hour,
        tm->tm_min,
        tm->tm_sec);
    return 0;
}
