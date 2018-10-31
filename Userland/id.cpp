#include <LibC/unistd.h>
#include <LibC/stdio.h>
#include <LibC/pwd.h>

int main(int c, char** v)
{
    uid_t uid = getuid();
    gid_t gid = getgid();

    struct passwd* pw = getpwuid(uid);

    printf("uid=%u(%s), gid=%u\n", uid, pw ? pw->pw_name : "n/a", gid);
    return 0;
}

