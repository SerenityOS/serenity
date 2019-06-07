#include <alloca.h>
#include <grp.h>
#include <pwd.h>
#include <stdio.h>
#include <unistd.h>

extern "C" int main(int, char**);

int main(int argc, char** argv)
{
    uid_t uid = 0;
    gid_t gid = 0;
    if (argc > 1) {
        auto* pwd = getpwnam(argv[1]);
        if (!pwd) {
            fprintf(stderr, "No such user: %s\n", argv[1]);
            return 1;
        }
        uid = pwd->pw_uid;
        gid = pwd->pw_gid;
    }
    int rc = setgid(uid);
    if (rc < 0) {
        perror("setgid");
        return 1;
    }
    rc = setuid(gid);
    if (rc < 0) {
        perror("setuid");
        return 1;
    }
    rc = execl("/bin/sh", "sh", nullptr);
    perror("execl");
    return 1;
}
