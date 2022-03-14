/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ConnectionFromClient.h"
#include <LibCore/System.h>
#include <LibGUI/Application.h>
#include <LibIPC/MultiServer.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    using enum Kernel::Pledge;
    TRY((Core::System::Promise<stdio, Kernel::Pledge::recvfd, Kernel::Pledge::sendfd, Kernel::Pledge::accept, rpath, unix>::pledge()));

    auto app = TRY(GUI::Application::try_create(arguments));
    auto server = TRY(IPC::MultiServer<NotificationServer::ConnectionFromClient>::try_create());

    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));
    TRY((Core::System::Promise<stdio, Kernel::Pledge::recvfd, Kernel::Pledge::sendfd, Kernel::Pledge::accept, rpath>::pledge()));

    return app->exec();
}
