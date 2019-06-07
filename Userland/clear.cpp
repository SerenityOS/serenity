#include <stdio.h>

int main(int, char**)
{
    printf("\033[3J\033[H\033[2J");
    fflush(stdout);
    return 0;
}
