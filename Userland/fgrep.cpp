#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <assert.h>

int main(int argc, char** argv)
{
    if (argc < 2) {
        printf("usage: fgrep <str>\n");
        return 0;
    }
    for (;;) {
        char buf[4096];
        auto* str = fgets(buf, sizeof(buf), stdin);
        if (str && strstr(str, argv[1]))
            write(1, buf, strlen(buf));
        if (feof(stdin))
            return 0;
        ASSERT(str);
    }
    return 0;
}
