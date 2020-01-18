/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

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
    if (pledge("stdio rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }
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

    if (flag_print_name && !(flag_print_uid || flag_print_gid || flag_print_gid_all)) {
        fprintf(stderr, "cannot print only names or real IDs in default format\n");
        return 1;
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
