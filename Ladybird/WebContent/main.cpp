/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#define AK_DONT_REPLACE_STD

#include "../EventLoopPluginQt.h"
#include "../FontPluginQt.h"
#include "../ImageCodecPluginLadybird.h"
#include "../RequestManagerQt.h"
#include "../Utilities.h"
#include "../WebSocketClientManagerLadybird.h"
#include <AK/LexicalPath.h>
#include <LibCore/EventLoop.h>
#include <LibCore/File.h>
#include <LibCore/LocalServer.h>
#include <LibCore/System.h>
#include <LibIPC/SingleServer.h>
#include <LibMain/Main.h>
#include <LibWeb/Loader/ContentFilter.h>
#include <LibWeb/Loader/FrameLoader.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWeb/WebSockets/WebSocket.h>
#include <QGuiApplication>
#include <QSocketNotifier>
#include <QTimer>
#include <WebContent/ConnectionFromClient.h>

static ErrorOr<void> load_content_filters();

extern String s_serenity_resource_root;

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    // NOTE: This is only used for the Core::Socket inside the IPC connection.
    // FIXME: Refactor things so we can get rid of this somehow.
    Core::EventLoop event_loop;

    platform_init();

    QGuiApplication app(arguments.argc, arguments.argv);

    Web::Platform::EventLoopPlugin::install(*new Ladybird::EventLoopPluginQt);
    Web::Platform::ImageCodecPlugin::install(*new Ladybird::ImageCodecPluginLadybird);

    Web::ResourceLoader::initialize(RequestManagerQt::create());
    Web::WebSockets::WebSocketClientManager::initialize(Ladybird::WebSocketClientManagerLadybird::create());

    Web::FrameLoader::set_default_favicon_path(String::formatted("{}/res/icons/16x16/app-browser.png", s_serenity_resource_root));

    Web::Platform::FontPlugin::install(*new Ladybird::FontPluginQt);

    Web::FrameLoader::set_error_page_url(String::formatted("file://{}/res/html/error.html", s_serenity_resource_root));

    auto maybe_content_filter_error = load_content_filters();
    if (maybe_content_filter_error.is_error())
        dbgln("Failed to load content filters: {}", maybe_content_filter_error.error());

    auto client = TRY(IPC::take_over_accepted_client_from_system_server<WebContent::ConnectionFromClient>());

    auto* fd_passing_socket_spec = getenv("FD_PASSING_SOCKET");
    VERIFY(fd_passing_socket_spec);
    auto fd_passing_socket_spec_string = String(fd_passing_socket_spec);
    auto maybe_fd_passing_socket = fd_passing_socket_spec_string.to_int();
    VERIFY(maybe_fd_passing_socket.has_value());

    client->set_fd_passing_socket(TRY(Core::Stream::LocalSocket::adopt_fd(maybe_fd_passing_socket.value())));

    QSocketNotifier notifier(client->socket().fd().value(), QSocketNotifier::Type::Read);
    QObject::connect(&notifier, &QSocketNotifier::activated, [&] {
        client->socket().notifier()->on_ready_to_read();
    });

    struct DeferredInvokerQt final : IPC::DeferredInvoker {
        virtual ~DeferredInvokerQt() = default;
        virtual void schedule(Function<void()> callback) override
        {
            QTimer::singleShot(0, move(callback));
        }
    };

    client->set_deferred_invoker(make<DeferredInvokerQt>());

    return app.exec();
}

static ErrorOr<void> load_content_filters()
{
    auto file_or_error = Core::Stream::File::open(String::formatted("{}/home/anon/.config/BrowserContentFilters.txt", s_serenity_resource_root), Core::Stream::OpenMode::Read);
    if (file_or_error.is_error())
        file_or_error = Core::Stream::File::open(String::formatted("{}/res/ladybird/BrowserContentFilters.txt", s_serenity_resource_root), Core::Stream::OpenMode::Read);
    if (file_or_error.is_error())
        return file_or_error.release_error();
    auto file = file_or_error.release_value();
    auto ad_filter_list = TRY(Core::Stream::BufferedFile::create(move(file)));
    auto buffer = TRY(ByteBuffer::create_uninitialized(4096));
    while (TRY(ad_filter_list->can_read_line())) {
        auto line = TRY(ad_filter_list->read_line(buffer));
        if (!line.is_empty()) {
            Web::ContentFilter::the().add_pattern(line);
        }
    }
    return {};
}
