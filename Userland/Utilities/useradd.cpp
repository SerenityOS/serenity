/*
 * Copyright (c) 2019-2020, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Brandon Pruitt  <brapru@pm.me>
 * Copyright (c) 2022, Umut İnan Erdoğan <umutinanerdogan62@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Base64.h>
#include <AK/Random.h>
#include <AK/String.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <crypt.h>
#include <ctype.h>
#include <errno.h>
#include <pwd.h>
#include <shadow.h>
#include <stdio.h>
#include <string.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <unistd.h>

constexpr uid_t BASE_UID = 1000;
constexpr gid_t USERS_GID = 100;
constexpr char const* DEFAULT_SHELL = "/bin/sh";

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio wpath rpath cpath chown"));

    char const* home_path = nullptr;
    int uid = 0;
    int gid = USERS_GID;
    bool create_home_dir = false;
    char const* password = "";
    char const* shell = DEFAULT_SHELL;
    char const* gecos = "";
    char const* username = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_option(home_path, "Home directory for the new user", "home-dir", 'd', "path");
    args_parser.add_option(uid, "User ID (uid) for the new user", "uid", 'u', "uid");
    args_parser.add_option(gid, "Group ID (gid) for the new user", "gid", 'g', "gid");
    args_parser.add_option(password, "Encrypted password of the new user", "password", 'p', "password");
    args_parser.add_option(create_home_dir, "Create home directory if it does not exist", "create-home", 'm');
    args_parser.add_option(shell, "Path to the default shell binary for the new user", "shell", 's', "path-to-shell");
    args_parser.add_option(gecos, "GECOS name of the new user", "gecos", 'n', "general-info");
    args_parser.add_positional_argument(username, "Login user identity (username)", "login");

    args_parser.parse(arguments);

    // Let's run a quick sanity check on username
    if (strpbrk(username, "\\/!@#$%^&*()~+=`:\n")) {
        warnln("invalid character in username, {}", username);
        return 1;
    }

    // Disallow names starting with _ and -
    if (username[0] == '_' || username[0] == '-' || !isalpha(username[0])) {
        warnln("invalid username, {}", username);
        return 1;
    }

    auto passwd = TRY(Core::System::getpwnam(username));
    if (passwd.has_value()) {
        warnln("user {} already exists!", username);
        return 1;
    }

    if (uid < 0) {
        warnln("invalid uid {}!", uid);
        return 3;
    }

    // First, let's sort out the uid for the user
    if (uid > 0) {
        auto pwd = TRY(Core::System::getpwuid(static_cast<uid_t>(uid)));
        if (pwd.has_value()) {
            warnln("uid {} already exists!", uid);
            return 4;
        }
    } else {
        for (uid = BASE_UID;; uid++) {
            auto pwd = TRY(Core::System::getpwuid(static_cast<uid_t>(uid)));
            if (!pwd.has_value())
                break;
        }
    }

    if (gid < 0) {
        warnln("invalid gid {}", gid);
        return 3;
    }

    FILE* pwfile = fopen("/etc/passwd", "a");
    if (!pwfile) {
        perror("failed to open /etc/passwd");
        return 1;
    }

    FILE* spwdfile = fopen("/etc/shadow", "a");
    if (!spwdfile) {
        perror("failed to open /etc/shadow");
        return 1;
    }

    String home;
    if (!home_path)
        home = String::formatted("/home/{}", username);
    else
        home = home_path;

    if (create_home_dir) {
        auto mkdir_error = Core::System::mkdir(home, 0700);
        if (mkdir_error.is_error()) {
            int code = mkdir_error.release_error().code();
            warnln("Failed to create directory {}: {}", home, strerror(code));
            return 12;
        }

        auto chown_error = Core::System::chown(home, static_cast<uid_t>(uid), static_cast<gid_t>(gid));
        if (chown_error.is_error()) {
            int code = chown_error.release_error().code();
            warnln("Failed to change owner of {} to {}:{}: {}", home, uid, gid, strerror(code));

            if (rmdir(home.characters()) < 0) {
                warnln("Failed to remove directory {}: {}", home, strerror(errno));
            }

            return 12;
        }
    }

    auto get_salt = []() {
        char random_data[12];
        fill_with_random(random_data, sizeof(random_data));

        StringBuilder builder;
        builder.append("$5$");
        builder.append(encode_base64(ReadonlyBytes(random_data, sizeof(random_data))));

        return builder.build();
    };

    char* hash = crypt(password, get_salt().characters());

    struct passwd p;
    p.pw_name = const_cast<char*>(username);
    p.pw_passwd = const_cast<char*>("!");
    p.pw_dir = const_cast<char*>(home.characters());
    p.pw_uid = static_cast<uid_t>(uid);
    p.pw_gid = static_cast<gid_t>(gid);
    p.pw_shell = const_cast<char*>(shell);
    p.pw_gecos = const_cast<char*>(gecos);

    struct spwd s;
    s.sp_namp = const_cast<char*>(username);
    s.sp_pwdp = const_cast<char*>(hash);
    s.sp_lstchg = 18727;
    s.sp_min = 0;
    s.sp_max = 99999;
    s.sp_warn = -1;
    s.sp_inact = -1;
    s.sp_expire = -1;
    s.sp_flag = -1;

    if (putpwent(&p, pwfile) < 0) {
        perror("putpwent");
        return 1;
    }

    if (putspent(&s, spwdfile) < 0) {
        perror("putspent");
        return 1;
    }

    fclose(pwfile);
    fclose(spwdfile);

    return 0;
}
