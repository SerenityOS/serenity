#include <AK/String.h>
#include <AK/QuickSort.h>
#include <AK/Vector.h>
#include <stdio.h>
#include <stdlib.h>

int main(int argc, char** argv)
{
    if (argc != 3) {
        printf("usage: tr <from> <to>");
        return 0;
    }

    char from = argv[1][0];
    char to = argv[2][0];

    for (;;) {
        char ch = fgetc(stdin);
        if (feof(stdin))
            break;
        if (ch == from)
            putchar(to);
        else
            putchar(ch);
    }

    return 0;
}
