/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2026, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TerminalWindow.h"
#include <LibConfig/Client.h>
#include <LibCore/Account.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibGUI/Application.h>
#include <LibMain/Main.h>
#include <signal.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio tty rpath cpath wpath recvfd sendfd proc exec unix sigaction"));

    struct sigaction act;
    act.sa_mask = 0;
    // Do not trust that both function pointers overlap.
    act.sa_sigaction = nullptr;
    act.sa_flags = SA_NOCLDWAIT;
    act.sa_handler = SIG_IGN;
    TRY(Core::System::sigaction(SIGCHLD, &act, nullptr));

    auto app = TRY(GUI::Application::create(arguments));

    TRY(Core::System::pledge("stdio tty rpath cpath wpath recvfd sendfd proc exec unix"));

    Config::pledge_domain("Terminal");

    StringView command_to_execute;
    bool keep_open = false;

    Core::ArgsParser args_parser;
    args_parser.add_option(command_to_execute, "Execute this command inside the terminal", nullptr, 'e', "command");
    args_parser.add_option(keep_open, "Keep the terminal open after the command has finished executing", nullptr, 'k');
    args_parser.parse(arguments);

    if (keep_open && command_to_execute.is_empty()) {
        warnln("Option -k can only be used in combination with -e.");
        return 1;
    }

    // Read the user's shell before locking unveil so we can unveil the correct executable.
    auto user_shell = TRY(String::from_byte_string(TRY(Core::Account::self(Core::Account::Read::PasswdOnly)).shell()));
    if (user_shell.is_empty())
        user_shell = "/bin/Shell"_string;

    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil("/bin", "r"));
    TRY(Core::System::unveil("/proc", "r"));
    TRY(Core::System::unveil("/dev/pts", "rw"));
    TRY(Core::System::unveil("/dev/ptmx", "rw"));
    TRY(Core::System::unveil(user_shell.bytes_as_string_view(), "x"sv));
    TRY(Core::System::unveil("/bin/Terminal", "x"));
    TRY(Core::System::unveil("/bin/TerminalSettings", "x"));
    TRY(Core::System::unveil("/bin/utmpupdate", "x"));
    TRY(Core::System::unveil("/etc/passwd", "r"));
    TRY(Core::System::unveil("/etc/FileIconProvider.ini", "r"));
    TRY(Core::System::unveil("/tmp/session/%sid/portal/launch", "rw"));
    TRY(Core::System::unveil("/dev/beep", "rw"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto window = TerminalWindow::construct();
    TRY(window->initialize(command_to_execute, keep_open));
    window->show();

    int result = app->exec();
    TRY(window->cleanup());
    return result;
}
