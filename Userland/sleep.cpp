#include <LibC/unistd.h>
#include <LibC/stdio.h>

int main(int c, char** v)
{
    unsigned secs = 10;
    sleep(secs);
    return 0;
}

