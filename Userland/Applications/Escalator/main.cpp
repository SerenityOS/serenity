/*
 * Copyright (c) 2022, Ashley N. <dev-serenity@ne0ndrag0n.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "EscalatorWindow.h"
#include <AK/String.h>
#include <LibCore/Account.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibGUI/Application.h>
#include <LibGUI/Desktop.h>
#include <LibGUI/MessageBox.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Vector<StringView> command;
    Core::ArgsParser args_parser;
    StringView description;
    bool preserve_env = false;
    args_parser.set_general_help("Escalate privilege to root for a given command using a GUI prompt.");
    args_parser.set_stop_on_first_non_option(true);
    args_parser.add_option(description, "Custom prompt to use for dialog", "prompt", 'P', "prompt");
    args_parser.add_option(preserve_env, "Preserve user environment when running command", "preserve-env", 'E');
    args_parser.add_positional_argument(command, "Command to run at elevated privilege level", "command");
    args_parser.parse(arguments);

    TRY(Core::System::pledge("stdio recvfd sendfd thread cpath rpath wpath unix proc exec id"));

    auto app = TRY(GUI::Application::try_create(arguments));

    auto executable_path = Core::File::resolve_executable_from_environment(command[0]);
    if (!executable_path.has_value()) {
        GUI::MessageBox::show_error(nullptr, String::formatted("Could not execute command {}: Command not found.", command[0]));
        return 127;
    }

    auto current_user = TRY(Core::Account::self());
    auto window = TRY(EscalatorWindow::try_create(executable_path.value(), command, EscalatorWindow::Options { description, current_user, preserve_env }));

    if (current_user.uid() != 0) {
        window->show();
        return app->exec();
    } else {
        // Run directly as root if already root uid.
        TRY(window->execute_command());
        return 0;
    }
}
