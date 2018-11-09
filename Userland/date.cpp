#include <LibC/time.h>
#include <LibC/stdio.h>

int main(int argc, char** argv)
{
    (void) argc;
    (void) argv;
    time_t now = time(nullptr);
    printf("%u\n", now);
    return 0;
}

