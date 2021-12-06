/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ClientConnection.h"
#include <LibCore/System.h>
#include <LibGUI/Application.h>
#include <LibIPC/MultiServer.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd accept rpath unix"));

    auto app = TRY(GUI::Application::try_create(arguments));
    auto server = TRY(IPC::MultiServer<NotificationServer::ClientConnection>::try_create());

    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));
    TRY(Core::System::pledge("stdio recvfd sendfd accept rpath"));

    return app->exec();
}
