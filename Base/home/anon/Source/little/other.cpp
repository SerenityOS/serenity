#include "other.h"
#include <stdio.h>

int func()
{
    int x = 1;
    int y = 2;
    StructInHeader mystruct;
    printf("x: %d\n", x);
    printf("y: %d\n", y);
    printf("x+y: %d\n", x + y);
    return x + y;
}
