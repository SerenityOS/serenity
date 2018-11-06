#include <signal.h>
#include <stdio.h>
#include <string.h>

int main(int, char**)
{
    for (int i = 1; i < 32; ++i) {
        printf("%d: '%s'\n", i, strsignal(i));
    }
    return 0;
}
