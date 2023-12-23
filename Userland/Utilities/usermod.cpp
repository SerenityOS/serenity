/*
 * Copyright (c) 2021, Brandon Pruitt <brapru@pm.me>
 * Copyright (c) 2023, Tim Ledbetter <timledbetter@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/Account.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibFileSystem/FileSystem.h>
#include <LibMain/Main.h>
#include <pwd.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

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
    TRY(Core::System::pledge("stdio wpath rpath cpath fattr tty"));
    TRY(Core::System::unveil("/etc", "rwc"));

    uid_t uid = 0;
    bool append_extra_gids = false;
    Optional<gid_t> gid;
    bool lock = false;
    bool remove_extra_gids = false;
    bool unlock = false;
    StringView new_home_directory;
    bool move_home = false;
    StringView shell;
    StringView gecos;
    StringView username;
    Vector<gid_t> extra_gids;

    auto args_parser = Core::ArgsParser();
    args_parser.set_general_help("Modify a user account");
    args_parser.add_option(append_extra_gids, "Append the supplementary groups specified with the -G option to the user", "append", 'a');
    args_parser.add_option(uid, "The new numerical value of the user's ID", "uid", 'u', "uid");
    args_parser.add_option(Core::ArgsParser::Option {
        .argument_mode = Core::ArgsParser::OptionArgumentMode::Required,
        .help_string = "The group name or number of the user's new initial login group",
        .long_name = "gid",
        .short_name = 'g',
        .value_name = "group",
        .accept_value = [&gid](StringView group) {
            if (auto maybe_gid = group_string_to_gid(group); maybe_gid.has_value())
                gid = move(maybe_gid);

            return gid.has_value();
        },
    });
    args_parser.add_option(Core::ArgsParser::Option {
        .argument_mode = Core::ArgsParser::OptionArgumentMode::Required,
        .help_string = "Set the user's supplementary groups. Groups are specified with a comma-separated list. Group names or numbers may be used",
        .long_name = "groups",
        .short_name = 'G',
        .value_name = "groups",
        .accept_value = [&extra_gids](StringView comma_separated_groups) {
            auto groups = comma_separated_groups.split_view(',');
            for (auto group : groups) {
                if (auto gid = group_string_to_gid(group); gid.has_value())
                    extra_gids.append(gid.value());
            }
            return true;
        },
    });
    args_parser.add_option(lock, "Lock password", "lock", 'L');
    args_parser.add_option(remove_extra_gids, "Remove the supplementary groups specified with the -G option from the user", "remove", 'r');
    args_parser.add_option(unlock, "Unlock password", "unlock", 'U');
    args_parser.add_option(new_home_directory, "The user's new login directory", "home", 'd', "new-home");
    args_parser.add_option(move_home, "Move the content of the user's home directory to the new location", "move", 'm');
    args_parser.add_option(shell, "The name of the user's new login shell", "shell", 's', "path-to-shell");
    args_parser.add_option(gecos, "Change the GECOS field of the user", "gecos", 'n', "general-info");
    args_parser.add_positional_argument(username, "Username of the account to modify", "username");

    args_parser.parse(arguments);

    if (extra_gids.is_empty() && (append_extra_gids || remove_extra_gids)) {
        warnln("The -a and -r options can only be used with the -G option");
        args_parser.print_usage(stderr, arguments.strings[0]);
        return 1;
    }

    if (append_extra_gids && remove_extra_gids) {
        warnln("The -a and -r options are mutually exclusive");
        args_parser.print_usage(stderr, arguments.strings[0]);
        return 1;
    }

    if (lock && unlock) {
        warnln("The -L and -U options are mutually exclusive");
        args_parser.print_usage(stderr, arguments.strings[0]);
        return 1;
    }

    auto account_or_error = Core::Account::from_name(username);

    if (account_or_error.is_error()) {
        warnln("usermod: {}", strerror(account_or_error.error().code()));
        return 1;
    }

    // target_account is the account we are modifying.
    auto& target_account = account_or_error.value();

    if (move_home) {
        TRY(Core::System::unveil(target_account.home_directory(), "c"sv));
        TRY(Core::System::unveil(new_home_directory, "wc"sv));
    }

    unveil(nullptr, nullptr);

    if (uid) {
        if (getpwuid(uid)) {
            warnln("uid {} already exists", uid);
            return 1;
        }

        target_account.set_uid(uid);
    }

    if (gid.has_value())
        target_account.set_gid(gid.value());

    if (lock) {
        target_account.set_password_enabled(false);
    }

    if (unlock) {
        target_account.set_password_enabled(true);
    }

    if (!new_home_directory.is_empty()) {
        if (move_home) {
            auto maybe_error = Core::System::rename(target_account.home_directory(), new_home_directory);
            if (maybe_error.is_error()) {
                if (maybe_error.error().code() == EXDEV) {
                    auto result = FileSystem::copy_file_or_directory(
                        new_home_directory, target_account.home_directory(),
                        FileSystem::RecursionMode::Allowed,
                        FileSystem::LinkMode::Disallowed,
                        FileSystem::AddDuplicateFileMarker::No);

                    if (result.is_error()) {
                        warnln("usermod: could not move directory {} : {}", target_account.home_directory().characters(), static_cast<Error const&>(result.error()));
                        return 1;
                    }
                    maybe_error = Core::System::unlink(target_account.home_directory());
                    if (maybe_error.is_error())
                        warnln("usermod: unlink {} : {}", target_account.home_directory(), maybe_error.error().code());
                } else {
                    warnln("usermod: could not move directory {} : {}", target_account.home_directory(), maybe_error.error().code());
                }
            }
        }

        target_account.set_home_directory(new_home_directory);
    }

    if (!shell.is_empty()) {
        target_account.set_shell(shell);
    }

    if (!gecos.is_empty()) {
        target_account.set_gecos(gecos);
    }

    if (append_extra_gids) {
        for (auto gid : target_account.extra_gids())
            extra_gids.append(gid);
    }

    if (remove_extra_gids) {
        Vector<gid_t> current_extra_gids = target_account.extra_gids();
        for (auto gid : extra_gids)
            current_extra_gids.remove_all_matching([gid](auto current_gid) { return current_gid == gid; });

        extra_gids = move(current_extra_gids);
    }

    if (!extra_gids.is_empty() || remove_extra_gids) {
        target_account.set_extra_gids(extra_gids);
    }

    TRY(Core::System::pledge("stdio wpath rpath cpath fattr"));

    TRY(target_account.sync());

    return 0;
}
