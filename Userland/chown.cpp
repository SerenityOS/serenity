#include <AK/String.h>
#include <stdio.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (argc < 2) {
        printf("usage: chown <uid[:gid]> <path>\n");
        return 0;
    }

    uid_t new_uid = -1;
    gid_t new_gid = -1;

    auto parts = String(argv[1]).split(':');
    if (parts.is_empty()) {
        fprintf(stderr, "Empty uid/gid spec\n");
        return 1;
    }
    bool ok;
    new_uid = parts[0].to_uint(ok);
    if (!ok) {
        fprintf(stderr, "Invalid uid: '%s'\n", parts[0].characters());
        return 1;
    }
    if (parts.size() == 2) {
        new_gid = parts[1].to_uint(ok);
        if (!ok) {
            fprintf(stderr, "Invalid gid: '%s'\n", parts[1].characters());
            return 1;
        }
    }

    int rc = chown(argv[2], new_uid, new_gid);
    if (rc < 0) {
        perror("chown");
        return 1;
    }

    return 0;
}
