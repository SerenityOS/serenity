#include <LibC/time.h>
#include <LibC/stdio.h>

int main(int argc, char** argv)
{
    (void) argc;
    (void) argv;

    time_t now = time(nullptr);
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

