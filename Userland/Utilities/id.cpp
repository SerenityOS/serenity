/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <alloca.h>
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
    if (unveil("/etc/passwd", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/etc/group", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil");
        return 1;
    }

    if (pledge("stdio rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    Core::ArgsParser args_parser;
    args_parser.add_option(flag_print_uid, "Print UID", nullptr, 'u');
    args_parser.add_option(flag_print_gid, "Print GID", nullptr, 'g');
    args_parser.add_option(flag_print_gid_all, "Print all GIDs", nullptr, 'G');
    args_parser.add_option(flag_print_name, "Print name", nullptr, 'n');
    args_parser.parse(argc, argv);

    if (flag_print_name && !(flag_print_uid || flag_print_gid || flag_print_gid_all)) {
        warnln("cannot print only names or real IDs in default format");
        return 1;
    }

    if (flag_print_uid + flag_print_gid + flag_print_gid_all > 1) {
        warnln("cannot print \"only\" of more than one choice");
        return 1;
    }

    int status = print_id_objects();
    return status;
}

static bool print_uid_object(uid_t uid)
{
    if (flag_print_name) {
        struct passwd* pw = getpwuid(uid);
        out("{}", pw ? pw->pw_name : "n/a");
    } else
        out("{}", uid);

    return true;
}

static bool print_gid_object(gid_t gid)
{
    if (flag_print_name) {
        struct group* gr = getgrgid(gid);
        out("{}", gr ? gr->gr_name : "n/a");
    } else
        out("{}", gid);
    return true;
}

static bool print_gid_list()
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
                out("{}", gr->gr_name);
            else
                out("{}", extra_gids[g]);
            if (g != extra_gid_count - 1)
                out(" ");
        }
    }
    return true;
}

static bool print_full_id_list()
{
    uid_t uid = getuid();
    gid_t gid = getgid();
    struct passwd* pw = getpwuid(uid);
    struct group* gr = getgrgid(gid);

    out("uid={}({}) gid={}({})", uid, pw ? pw->pw_name : "n/a", gid, gr ? gr->gr_name : "n/a");

    int extra_gid_count = getgroups(0, nullptr);
    if (extra_gid_count) {
        auto* extra_gids = (gid_t*)alloca(extra_gid_count * sizeof(gid_t));
        int rc = getgroups(extra_gid_count, extra_gids);
        if (rc < 0) {
            perror("\ngetgroups");
            return false;
        }
        out(" groups=");
        for (int g = 0; g < extra_gid_count; ++g) {
            auto* gr = getgrgid(extra_gids[g]);
            if (gr)
                out("{}({})", extra_gids[g], gr->gr_name);
            else
                out("{}", extra_gids[g]);
            if (g != extra_gid_count - 1)
                out(",");
        }
    }
    return true;
}

static int print_id_objects()
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

    outln();
    return 0;
}
