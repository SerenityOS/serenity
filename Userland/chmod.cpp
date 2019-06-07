#include <stdio.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (argc != 3) {
        printf("usage: chmod <octal-mode> <path>\n");
        return 1;
    }

    unsigned mode;
    int rc = sscanf(argv[1], "%o", &mode);
    if (rc != 1) {
        perror("sscanf");
        return 1;
    }

    rc = chmod(argv[2], mode);
    if (rc < 0) {
        perror("chmod");
        return 1;
    }

    return 0;
}
