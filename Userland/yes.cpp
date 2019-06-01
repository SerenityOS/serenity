#include <stdio.h>

int main(int argc, char** argv)
{
    if (argc > 1) {
        for (;;) {
            puts(argv[1]);
        }
    } else {
        for (;;) {
            puts("yes");
        }
    }
    return 0;
}
