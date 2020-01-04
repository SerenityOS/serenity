#include <AK/Vector.h>
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
    struct passwd* pwd = nullptr;
    if (argc > 1) {
        pwd = getpwnam(argv[1]);
        if (!pwd) {
            fprintf(stderr, "No such user: %s\n", argv[1]);
            return 1;
        }
        uid = pwd->pw_uid;
        gid = pwd->pw_gid;
    }

    if (!pwd)
        pwd = getpwuid(0);

    if (!pwd) {
        fprintf(stderr, "No passwd entry.\n");
        return 1;
    }

    Vector<gid_t> extra_gids;
    for (auto* group = getgrent(); group; group = getgrent()) {
        for (size_t i = 0; group->gr_mem[i]; ++i) {
            if (!strcmp(pwd->pw_name, group->gr_mem[i]))
                extra_gids.append(group->gr_gid);
        }
    }
    endgrent();

    int rc = setgroups(extra_gids.size(), extra_gids.data());
    if (rc < 0) {
        perror("setgroups");
        return 1;
    }
    rc = setgid(uid);
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
