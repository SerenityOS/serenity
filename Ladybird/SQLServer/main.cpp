/*
 * Copyright (c) 2022-2024, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteString.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/Directory.h>
#include <LibCore/EventLoop.h>
#include <LibCore/StandardPaths.h>
#include <LibIPC/SingleServer.h>
#include <LibMain/Main.h>
#include <SQLServer/ConnectionFromClient.h>

#if defined(AK_OS_MACOS)
#    include <LibCore/Platform/ProcessStatisticsMach.h>
#endif

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    AK::set_rich_debug_enabled(true);

    StringView mach_server_name;

    Core::ArgsParser args_parser;
    args_parser.add_option(mach_server_name, "Mach server name", "mach-server-name", 0, "mach_server_name");
    args_parser.parse(arguments);

    auto database_path = ByteString::formatted("{}/Ladybird", Core::StandardPaths::data_directory());
    TRY(Core::Directory::create(database_path, Core::Directory::CreateDirectories::Yes));

    Core::EventLoop loop;

#if defined(AK_OS_MACOS)
    if (!mach_server_name.is_empty())
        Core::Platform::register_with_mach_server(mach_server_name);
#endif

    auto client = TRY(IPC::take_over_accepted_client_from_system_server<SQLServer::ConnectionFromClient>());
    client->set_database_path(database_path);

    return loop.exec();
}
