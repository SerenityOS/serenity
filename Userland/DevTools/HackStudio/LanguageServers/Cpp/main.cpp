/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ClientConnection.h"
#include "Tests.h"
#include <AK/LexicalPath.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibCore/LocalServer.h>
#include <LibCore/System.h>
#include <LibIPC/ClientConnection.h>
#include <LibMain/Main.h>
#include <unistd.h>

static ErrorOr<int> mode_server();

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    bool tests = false;

    Core::ArgsParser parser;
    parser.add_option(tests, "Run tests", "tests", 't');
    parser.parse(arguments);

    if (tests)
        return run_tests();

    return mode_server();
}

ErrorOr<int> mode_server()
{
    Core::EventLoop event_loop;
    TRY(Core::System::pledge("stdio unix recvfd rpath ", nullptr));

    auto socket = Core::LocalSocket::take_over_accepted_socket_from_system_server();
    IPC::new_client_connection<LanguageServers::Cpp::ClientConnection>(socket.release_nonnull(), 1);

    TRY(Core::System::pledge("stdio recvfd rpath", nullptr));
    TRY(Core::System::unveil("/usr/include", "r"));

    // unveil will be sealed later, when we know the project's root path.
    return event_loop.exec();
}
