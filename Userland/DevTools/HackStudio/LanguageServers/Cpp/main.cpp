/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ConnectionFromClient.h"
#include "Tests.h"
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibCore/LocalServer.h>
#include <LibCore/System.h>
#include <LibIPC/SingleServer.h>
#include <LibMain/Main.h>

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
    TRY(Core::System::pledge("stdio unix recvfd rpath"));

    auto client = TRY(IPC::take_over_accepted_client_from_system_server<LanguageServers::Cpp::ConnectionFromClient>());

    TRY(Core::System::pledge("stdio recvfd rpath"));
    TRY(Core::System::unveil("/usr/include", "r"));

    // unveil will be sealed later, when we know the project's root path.
    return event_loop.exec();
}
