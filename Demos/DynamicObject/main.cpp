#include "lib.h"
#include <fcntl.h>
#include <stdio.h>
#include <sys/internals.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <unistd.h>

int main(int argc, char** argv, char** env)
{
    (void)argc;
    (void)argv;
    (void)env;

    printf("Well Hello Friends!\n");
    printf("trying to open /etc/fstab for writing..\n");
    int rc = open("/etc/fstab", O_RDWR);
    if (rc == -1) {
        int _errno = errno;
        perror("open failed");
        printf("rc: %d, errno: %d\n", rc, _errno);
        return func() + g_tls1 + g_tls2;
    }
}
