#include <stdio.h>
#include <stdlib.h>

int main(int argc, char** argv)
{
    // FIXME: Allow setting via command-line argument.
    int line_count = 10;

    if (argc > 1) {
        for (int i = 1; i < argc; ++i) {
            if (argv[i][0] == '-') {
                line_count = atoi(&argv[i][1]);
            }
        }
    }

    for (int line = 0; line < line_count; ++line) {
        char buffer[BUFSIZ];
        auto* str = fgets(buffer, sizeof(buffer), stdin);
        if (!str)
            break;
        fputs(str, stdout);
    }

    return 0;
}
