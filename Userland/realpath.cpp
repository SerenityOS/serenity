#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (argc != 2) {
        printf("usage: realpath <path>\n");
        return 1;
    }

    char* value = realpath(argv[1], nullptr);
    if (value == nullptr) {
        printf("realpath() error: %s\n", strerror(errno));
        return 1;
    }
    printf("%s\n", value);
    free(value);
    return 0;
}
