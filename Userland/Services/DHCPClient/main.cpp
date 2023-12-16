/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DHCPv4Client.h"
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments args)
{
    Vector<ByteString> interfaces;

    Core::ArgsParser parser;
    parser.add_positional_argument(interfaces, "Interfaces to run DHCP server on", "interfaces");
    parser.parse(args);

    TRY(Core::System::pledge("stdio unix inet cpath rpath"));
    Core::EventLoop event_loop;

    TRY(Core::System::unveil("/sys/kernel/net/", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto client = TRY(DHCPv4Client::try_create(interfaces));

    TRY(Core::System::pledge("stdio inet cpath rpath"));
    return event_loop.exec();
}
