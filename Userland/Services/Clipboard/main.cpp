/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Clipboard/ConnectionFromClient.h>
#include <Clipboard/Storage.h>
#include <LibCore/EventLoop.h>
#include <LibCore/System.h>
#include <LibIPC/MultiServer.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd accept"));
    Core::EventLoop event_loop;
    TRY(Core::System::unveil(nullptr, nullptr));

    auto server = TRY(IPC::MultiServer<Clipboard::ConnectionFromClient>::try_create());

    Clipboard::Storage::the().on_content_change = [&] {
        Clipboard::ConnectionFromClient::for_each_client([&](auto& client) {
            client.notify_about_clipboard_change();
        });
    };

    return event_loop.exec();
}
