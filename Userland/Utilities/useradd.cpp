/*
 * Copyright (c) 2019-2020, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Brandon Pruitt  <brapru@pm.me>
 * Copyright (c) 2022, Umut İnan Erdoğan <umutinanerdogan62@gmail.com>
 * Copyright (c) 2023, Tim Ledbetter <timledbetter@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Base64.h>
#include <AK/ByteString.h>
#include <AK/CharacterTypes.h>
#include <AK/Random.h>
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
constexpr auto DEFAULT_SHELL = "/bin/sh"sv;

static Optional<gid_t> group_string_to_gid(StringView group)
{
    auto maybe_gid = group.to_number<gid_t>();
    auto maybe_group_or_error = maybe_gid.has_value()
        ? Core::System::getgrgid(maybe_gid.value())
        : Core::System::getgrnam(group);

    if (maybe_group_or_error.is_error()) {
        warnln("Error resolving group '{}': {}", group, maybe_group_or_error.release_error());
        return {};
    }

    auto maybe_group = maybe_group_or_error.release_value();
    if (!maybe_group.has_value()) {
        warnln("Group '{}' does not exist", group);
        return {};
    }

    return maybe_group->gr_gid;
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio wpath rpath cpath chown"));

    StringView home_path;
    uid_t uid = 0;
    gid_t gid = USERS_GID;
    bool create_home_dir = false;
    ByteString password = "";
    ByteString shell = DEFAULT_SHELL;
    ByteString gecos = "";
    ByteString username;

    Core::ArgsParser args_parser;
    args_parser.add_option(home_path, "Home directory for the new user", "home-dir", 'd', "path");
    args_parser.add_option(uid, "User ID (uid) for the new user", "uid", 'u', "uid");
    args_parser.add_option(Core::ArgsParser::Option {
        .argument_mode = Core::ArgsParser::OptionArgumentMode::Required,
        .help_string = "Group name or number (gid) for the new user",
        .long_name = "gid",
        .short_name = 'g',
        .value_name = "group",
        .accept_value = [&gid](StringView group) {
            auto maybe_gid = group_string_to_gid(group);
            if (maybe_gid.has_value())
                gid = maybe_gid.value();

            return maybe_gid.has_value();
        },
    });
    args_parser.add_option(password, "Encrypted password of the new user", "password", 'p', "password");
    args_parser.add_option(create_home_dir, "Create home directory if it does not exist", "create-home", 'm');
    args_parser.add_option(shell, "Path to the default shell binary for the new user", "shell", 's', "path-to-shell");
    args_parser.add_option(gecos, "GECOS name of the new user", "gecos", 'n', "general-info");
    args_parser.add_positional_argument(username, "Login user identity (username)", "login");

    if (!args_parser.parse(arguments, Core::ArgsParser::FailureBehavior::Ignore)) {
        args_parser.print_usage(stderr, arguments.strings[0]);
        return 3;
    }

    // Let's run a quick sanity check on username
    if (username.find_any_of("\\/!@#$%^&*()~+=`:\n"sv, ByteString::SearchDirection::Forward).has_value()) {
        warnln("invalid character in username, {}", username);
        return 1;
    }

    // Disallow names starting with _ and -
    if (username[0] == '_' || username[0] == '-' || !is_ascii_alpha(username[0])) {
        warnln("invalid username, {}", username);
        return 1;
    }

    auto passwd = TRY(Core::System::getpwnam(username));
    if (passwd.has_value()) {
        warnln("user {} already exists!", username);
        return 1;
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

    ByteString home;
    if (home_path.is_empty())
        home = ByteString::formatted("/home/{}", username);
    else
        home = home_path;

    if (create_home_dir) {
        bool existed = false;
        auto mkdir_error = Core::System::mkdir(home, 0700);
        if (mkdir_error.is_error()) {
            int code = mkdir_error.release_error().code();
            warnln("Failed to create directory {}: {}", home, strerror(code));

            if (code != EEXIST)
                return 12;
            existed = true;
        }

        auto chown_error = Core::System::chown(home, static_cast<uid_t>(uid), static_cast<gid_t>(gid));
        if (chown_error.is_error()) {
            int code = chown_error.release_error().code();
            warnln("Failed to change owner of {} to {}:{}: {}", home, uid, gid, strerror(code));

            if (!existed && rmdir(home.characters()) < 0) {
                warnln("Failed to remove directory {}: {}", home, strerror(errno));
            }

            return 12;
        }
    }

    auto get_salt = []() -> ErrorOr<ByteString> {
        char random_data[12];
        fill_with_random({ random_data, sizeof(random_data) });

        StringBuilder builder;
        builder.append("$5$"sv);
        builder.append(TRY(encode_base64({ random_data, sizeof(random_data) })));

        return builder.to_byte_string();
    };

    char* hash = crypt(password.characters(), TRY(get_salt()).characters());

    struct passwd p;
    p.pw_name = const_cast<char*>(username.characters());
    p.pw_passwd = const_cast<char*>("!");
    p.pw_dir = const_cast<char*>(home.characters());
    p.pw_uid = static_cast<uid_t>(uid);
    p.pw_gid = static_cast<gid_t>(gid);
    p.pw_shell = const_cast<char*>(shell.characters());
    p.pw_gecos = const_cast<char*>(gecos.characters());

    struct spwd s;
    s.sp_namp = const_cast<char*>(username.characters());
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
