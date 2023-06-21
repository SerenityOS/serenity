/*
 * Copyright (c) 2020, Fei Wu <f.eiwu@yahoo.com>
 * Copyright (c) 2021, Brandon Pruitt <brapru@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/Account.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibFileSystem/FileSystem.h>
#include <LibMain/Main.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio wpath rpath cpath fattr"));
    TRY(Core::System::unveil("/etc/", "rwc"));

    StringView username;
    bool remove_home = false;

    Core::ArgsParser args_parser;
    args_parser.add_option(remove_home, "Remove home directory", "remove", 'r');
    args_parser.add_positional_argument(username, "Login user identity (username)", "login");
    args_parser.parse(arguments);

    auto account_or_error = Core::Account::from_name(username);

    if (account_or_error.is_error()) {
        warnln("Core::Account::from_name: {}", account_or_error.error());
        return 1;
    }

    auto& target_account = account_or_error.value();

    if (remove_home)
        TRY(Core::System::unveil(target_account.home_directory(), "c"sv));

    TRY(Core::System::unveil(nullptr, nullptr));

    target_account.set_deleted();
    TRY(target_account.sync());

    if (remove_home) {
        if (access(target_account.home_directory().characters(), F_OK) == -1)
            return 0;

        auto const real_path = TRY(FileSystem::real_path(target_account.home_directory()));

        if (real_path == "/"sv) {
            warnln("home directory is /, not deleted!");
            return 12;
        }

        if (auto result = FileSystem::remove(real_path, FileSystem::RecursionMode::Allowed); result.is_error()) {
            warnln("{}", result.release_error());
            return 12;
        }
    }

    return 0;
}
