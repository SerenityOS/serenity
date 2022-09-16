/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "EventLoopPluginSerenity.h"
#include "FontPluginSerenity.h"
#include "ImageCodecPluginSerenity.h"
#include <LibCore/EventLoop.h>
#include <LibCore/LocalServer.h>
#include <LibCore/System.h>
#include <LibIPC/SingleServer.h>
#include <LibMain/Main.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWeb/Platform/EventLoopPlugin.h>
#include <LibWeb/WebSockets/WebSocket.h>
#include <LibWebView/RequestServerAdapter.h>
#include <LibWebView/WebSocketClientAdapter.h>
#include <WebContent/ConnectionFromClient.h>

ErrorOr<int> serenity_main(Main::Arguments)
{
    Core::EventLoop event_loop;
    TRY(Core::System::pledge("stdio recvfd sendfd accept unix rpath"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil("/etc/timezone", "r"));
    TRY(Core::System::unveil("/tmp/user/%uid/portal/request", "rw"));
    TRY(Core::System::unveil("/tmp/user/%uid/portal/image", "rw"));
    TRY(Core::System::unveil("/tmp/user/%uid/portal/websocket", "rw"));
    TRY(Core::System::unveil(nullptr, nullptr));

    Web::Platform::EventLoopPlugin::install(*new WebContent::EventLoopPluginSerenity);
    Web::Platform::ImageCodecPlugin::install(*new WebContent::ImageCodecPluginSerenity);
    Web::Platform::FontPlugin::install(*new WebContent::FontPluginSerenity);

    Web::WebSockets::WebSocketClientManager::initialize(TRY(WebView::WebSocketClientManagerAdapter::try_create()));
    Web::ResourceLoader::initialize(TRY(WebView::RequestServerAdapter::try_create()));

    auto client = TRY(IPC::take_over_accepted_client_from_system_server<WebContent::ConnectionFromClient>());
    return event_loop.exec();
}
