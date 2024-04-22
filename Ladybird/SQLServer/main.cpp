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
#include <LibIPC/MultiServer.h>
#include <LibMain/Main.h>
#include <SQLServer/ConnectionFromClient.h>

#if defined(AK_OS_MACOS)
#    include <LibCore/Platform/ProcessStatisticsMach.h>
#endif

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    AK::set_rich_debug_enabled(true);

    StringView pid_file;
    StringView mach_server_name;

    Core::ArgsParser args_parser;
    args_parser.add_option(pid_file, "Path to the PID file for the SQLServer singleton process", "pid-file", 'p', "pid_file");
    args_parser.add_option(mach_server_name, "Mach server name", "mach-server-name", 0, "mach_server_name");
    args_parser.parse(arguments);

    VERIFY(!pid_file.is_empty());

    auto database_path = ByteString::formatted("{}/Ladybird", Core::StandardPaths::data_directory());
    TRY(Core::Directory::create(database_path, Core::Directory::CreateDirectories::Yes));

    Core::EventLoop loop;

#if defined(AK_OS_MACOS)
    if (!mach_server_name.is_empty())
        Core::Platform::register_with_mach_server(mach_server_name);
#endif

    auto server = TRY(IPC::MultiServer<SQLServer::ConnectionFromClient>::try_create());
    u64 connection_count { 0 };

    server->on_new_client = [&](auto& client) {
        client.set_database_path(database_path);
        ++connection_count;

        client.on_disconnect = [&]() {
            if (--connection_count == 0) {
                MUST(Core::System::unlink(pid_file));
                loop.quit(0);
            }
        };
    };

    return loop.exec();
}
