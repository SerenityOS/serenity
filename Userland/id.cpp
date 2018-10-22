#include <LibC/unistd.h>
#include <LibC/stdio.h>

int main(int c, char** v)
{
    uid_t uid = getuid();
    gid_t gid = getgid();
    pid_t pid = getpid();
    printf("uid=%u, gid=%u, pid=%u\n", uid, gid, pid);
    return 0;
}

