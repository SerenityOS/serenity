/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Clipboard/ClientConnection.h>
#include <Clipboard/Storage.h>
#include <LibCore/EventLoop.h>
#include <LibCore/System.h>
#include <LibIPC/MultiServer.h>
#include <LibMain/Main.h>

const char* serenity_get_initial_promises()
{
    return "stdio recvfd sendfd accept";
}

ErrorOr<int> serenity_main(Main::Arguments)
{
    Core::EventLoop event_loop;
    TRY(Core::System::unveil(nullptr, nullptr));

    auto server = TRY(IPC::MultiServer<Clipboard::ClientConnection>::try_create());

    Clipboard::Storage::the().on_content_change = [&] {
        Clipboard::ClientConnection::for_each_client([&](auto& client) {
            client.notify_about_clipboard_change();
        });
    };

    return event_loop.exec();
}
