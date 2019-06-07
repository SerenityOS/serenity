#include <errno.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    (void)argc;
    (void)argv;
    char buffer[HOST_NAME_MAX];
    int rc = gethostname(buffer, sizeof(buffer));
    if (rc < 0) {
        printf("gethostname() error: %s\n", strerror(errno));
        return 1;
    }
    printf("%s\n", buffer);
    return 0;
}
