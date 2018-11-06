#include <LibC/unistd.h>
#include <LibC/stdio.h>
#include <LibC/pwd.h>
#include <LibC/grp.h>

int main(int c, char** v)
{
    uid_t uid = getuid();
    gid_t gid = getgid();

    struct passwd* pw = getpwuid(uid);
    struct group* gr = getgrgid(gid);

    printf("uid=%u(%s), gid=%u(%s)\n", uid, pw ? pw->pw_name : "n/a", gid, gr ? gr->gr_name : "n/a", getpid());
    return 0;
}

