#include <unistd.h>
#include <stdio.h>
#include <pwd.h>
#include <grp.h>
#include <alloca.h>

extern "C" int main(int, char**);

int main(int argc, char** argv)
{
    uid_t uid;
    gid_t gid;
    if (argc == 1) {
        uid = 0;
        gid = 0;
    } else {
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
    if (rc < 0) {
        perror("execl");
        return 1;
    }
    return 0;
}
