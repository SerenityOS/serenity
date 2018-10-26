#include <LibC/unistd.h>
#include <LibC/stdio.h>
#include <LibC/errno.h>
#include <LibC/string.h>

int main(int c, char** v)
{
    char buffer[HOST_NAME_MAX];
    int rc = gethostname(buffer, sizeof(buffer));
    if (rc < 0) {
        printf("gethostname() error: %s\n", strerror(errno));
        return 1;
    }
    printf("%s\n", buffer);
    return 0;
}

