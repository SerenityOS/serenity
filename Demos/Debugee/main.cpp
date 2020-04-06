#include <cstdio>
int main(int, char**)
{
    printf("Debuggee main\n");
    int s = 0;
    for (int i = 0; i < 10; ++i) {
        s++;
    }
    printf("s: %d\n", s);
    // asm("int3");
    return 0;
}
