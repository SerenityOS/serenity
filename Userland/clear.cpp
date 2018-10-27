#include <LibC/stdio.h>

int main(int, char**)
{
    printf("\033[3J\033[H\033[2J");
    return 0;
}

