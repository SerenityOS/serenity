#include <stdio.h>

int main(int argc, char** argv)
{
    for (int i = 1; i < argc; ++i) {
        fputs(argv[i], stdout);
        if (i != argc - 1)
            fputc(' ', stdout);
    }
    printf("\n");
    return 0;
}
