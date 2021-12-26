#include <unistd.h>
#include <stdio.h>
#include <pwd.h>
#include <grp.h>
#include <alloca.h>

int main(int c, char** v)
{
    uid_t uid = getuid();
    gid_t gid = getgid();

    struct passwd* pw = getpwuid(uid);
    struct group* gr = getgrgid(gid);

    printf("uid=%u(%s), gid=%u(%s)", uid, pw ? pw->pw_name : "n/a", gid, gr ? gr->gr_name : "n/a", getpid());

    int extra_gid_count = getgroups(0, nullptr);
    if (extra_gid_count) {
        auto* extra_gids = (gid_t*)alloca(extra_gid_count * sizeof(gid_t));
        int rc = getgroups(extra_gid_count, extra_gids);
        if (rc < 0) {
            perror("\ngetgroups");
            return 1;
        }
        printf(",");
        for (int g = 0; g < extra_gid_count; ++g) {
            auto* gr = getgrgid(extra_gids[g]);
            if (gr)
                printf("%u(%s)", extra_gids[g], gr->gr_name);
            else
                printf("%u", extra_gids[g]);
            if (g != extra_gid_count - 1)
                printf(",");
        }
    }
    printf("\n");
    return 0;
}

