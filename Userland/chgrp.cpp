#include <AK/String.h>
#include <grp.h>
#include <pwd.h>
#include <stdio.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio rpath chown", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (argc < 2) {
        printf("usage: chgrp <gid> <path>\n");
        return 0;
    }

    gid_t new_gid = -1;
    auto gid_arg = String(argv[1]);

    if (gid_arg.is_empty()) {
        fprintf(stderr, "Empty gid option\n");
        return 1;
    }

    bool ok;
    new_gid = gid_arg.to_uint(ok);

    if (!ok) {
        new_gid = getgrnam(gid_arg.characters())->gr_gid;

        if(!new_gid) {
            fprintf(stderr, "Invalid gid: '%s'\n", gid_arg.characters());
            return 1;
        }
    }

    int rc = chown(argv[2], -1, new_gid);
    if (rc < 0) {
        perror("chgrp");
        return 1;
    }

    return 0;
}
