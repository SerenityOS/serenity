#include <stdio.h>
#include <string.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (argc < 2) {
        printf("usage: fgrep <str>\n");
        return 0;
    }
    for (;;) {
        char buf[4096];
        fgets(buf, sizeof(buf), stdin);
        if (feof(stdin))
            return 0;
        if (strstr(buf, argv[1]))
            write(1, buf, strlen(buf));
    }
    return 0;
}
