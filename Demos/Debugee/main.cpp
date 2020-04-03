#include <cstdio>
int main(int, char**)
{
    printf("before breakpoint\n");
    asm("int3");
    printf("after breakpoint\n");
    return 0;
}
