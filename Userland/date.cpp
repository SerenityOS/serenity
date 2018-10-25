#include <LibC/time.h>
#include <LibC/stdio.h>

int main(int c, char** v)
{
    time_t now = time(nullptr);
    printf("%u\n", now);
    return 0;
}

