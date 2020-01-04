#include <alloca.h>
#include <getopt.h>
#include <grp.h>
#include <pwd.h>
#include <stdio.h>
#include <unistd.h>

static int print_id_objects();

static bool flag_print_uid = false;
static bool flag_print_gid = false;
static bool flag_print_name = false;
static bool flag_print_gid_all = false;

int main(int argc, char** argv)
{
    static const char* valid_option_characters = "ugGn";
    int opt;
    while ((opt = getopt(argc, argv, valid_option_characters)) != -1) {
        switch (opt) {
        case 'u':
            flag_print_uid = true;
            break;
        case 'g':
            flag_print_gid = true;
            break;
        case 'G':
            flag_print_gid_all = true;
            break;
        case 'n':
            flag_print_name = true;
            break;

        default:
            fprintf(stderr, "usage: id [-%s]\n", valid_option_characters);
            return 1;
        }
    }

    if (flag_print_uid + flag_print_gid + flag_print_gid_all > 1) {
        fprintf(stderr, "cannot print \"only\" of more than one choice\n");
        return 1;
    }

    int status = print_id_objects();
    return status;
}

bool print_uid_object(uid_t uid)
{
    if (flag_print_name) {
        struct passwd* pw = getpwuid(uid);
        printf("%s", pw ? pw->pw_name : "n/a");
    } else
        printf("%u", uid);

    return true;
}

bool print_gid_object(gid_t gid)
{
    if (flag_print_name) {
        struct group* gr = getgrgid(gid);
        printf("%s", gr ? gr->gr_name : "n/a");
    } else
        printf("%u", gid);
    return true;
}

bool print_gid_list()
{
    int extra_gid_count = getgroups(0, nullptr);
    if (extra_gid_count) {
        auto* extra_gids = (gid_t*)alloca(extra_gid_count * sizeof(gid_t));
        int rc = getgroups(extra_gid_count, extra_gids);

        if (rc < 0) {
            perror("\ngetgroups");
            return false;
        }

        for (int g = 0; g < extra_gid_count; ++g) {
            auto* gr = getgrgid(extra_gids[g]);
            if (flag_print_name && gr)
                printf("%s", gr->gr_name);
            else
                printf("%u", extra_gids[g]);
            if (g != extra_gid_count - 1)
                printf(" ");
        }
    }
    return true;
}

bool print_full_id_list()
{

    uid_t uid = getuid();
    gid_t gid = getgid();
    struct passwd* pw = getpwuid(uid);
    struct group* gr = getgrgid(gid);

    printf("uid=%u(%s) gid=%u(%s)", uid, pw ? pw->pw_name : "n/a", gid, gr ? gr->gr_name : "n/a");

    int extra_gid_count = getgroups(0, nullptr);
    if (extra_gid_count) {
        auto* extra_gids = (gid_t*)alloca(extra_gid_count * sizeof(gid_t));
        int rc = getgroups(extra_gid_count, extra_gids);
        if (rc < 0) {
            perror("\ngetgroups");
            return false;
        }
        printf(" groups=");
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
    return true;
}

int print_id_objects()
{
    if (flag_print_uid) {
        if (!print_uid_object(getuid()))
            return 1;
    } else if (flag_print_gid) {
        if (!print_gid_object(getgid()))
            return 1;
    } else if (flag_print_gid_all) {
        if (!print_gid_list())
            return 1;
    } else {
        if (!print_full_id_list())
            return 1;
    }

    printf("\n");
    return 0;
}
