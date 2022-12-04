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
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibCore/File.h>
#include <LibCore/LocalServer.h>
#include <LibCore/System.h>
#include <LibCore/SystemServerTakeover.h>
#include <LibIPC/ConnectionFromClient.h>
#include <LibMain/Main.h>
#include <LibWeb/Loader/ContentFilter.h>
#include <LibWeb/Loader/FrameLoader.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWeb/WebSockets/WebSocket.h>
#include <QGuiApplication>
#include <QSocketNotifier>
#include <QTimer>
#include <WebContent/ConnectionFromClient.h>
#include <WebContent/PageHost.h>
#include <WebContent/WebDriverConnection.h>

static ErrorOr<void> load_content_filters();

extern DeprecatedString s_serenity_resource_root;

struct DeferredInvokerQt final : IPC::DeferredInvoker {
    virtual ~DeferredInvokerQt() = default;
    virtual void schedule(Function<void()> callback) override
    {
        QTimer::singleShot(0, move(callback));
    }
};

template<typename ConnectionType, typename... Args>
static ErrorOr<NonnullRefPtr<ConnectionType>> create_connection_from_passed_socket(int passing_socket_fd, StringView socket_name, QSocketNotifier& notifier, Args&&... args)
{
    auto socket = TRY(Core::take_over_socket_from_system_server(socket_name));
    auto client = TRY(ConnectionType::try_create(move(socket), std::forward<Args>(args)...));

    VERIFY(passing_socket_fd >= 0);
    client->set_fd_passing_socket(TRY(Core::Stream::LocalSocket::adopt_fd(passing_socket_fd)));

    notifier.setSocket(client->socket().fd().value());
    notifier.setEnabled(true);

    QObject::connect(&notifier, &QSocketNotifier::activated, [client]() mutable {
        client->socket().notifier()->on_ready_to_read();
    });

    client->set_deferred_invoker(make<DeferredInvokerQt>());
    return client;
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    // NOTE: This is only used for the Core::Socket inside the IPC connection.
    // FIXME: Refactor things so we can get rid of this somehow.
    Core::EventLoop event_loop;

    QGuiApplication app(arguments.argc, arguments.argv);

    platform_init();

    Web::Platform::EventLoopPlugin::install(*new Ladybird::EventLoopPluginQt);
    Web::Platform::ImageCodecPlugin::install(*new Ladybird::ImageCodecPluginLadybird);

    Web::ResourceLoader::initialize(RequestManagerQt::create());
    Web::WebSockets::WebSocketClientManager::initialize(Ladybird::WebSocketClientManagerLadybird::create());

    Web::FrameLoader::set_default_favicon_path(DeprecatedString::formatted("{}/res/icons/16x16/app-browser.png", s_serenity_resource_root));

    Web::Platform::FontPlugin::install(*new Ladybird::FontPluginQt);

    Web::FrameLoader::set_error_page_url(DeprecatedString::formatted("file://{}/res/html/error.html", s_serenity_resource_root));

    auto maybe_content_filter_error = load_content_filters();
    if (maybe_content_filter_error.is_error())
        dbgln("Failed to load content filters: {}", maybe_content_filter_error.error());

    int webcontent_fd_passing_socket { -1 };
    int webdriver_fd_passing_socket { -1 };

    Core::ArgsParser args_parser;
    args_parser.add_option(webcontent_fd_passing_socket, "File descriptor of the passing socket for the WebContent connection", "webcontent-fd-passing-socket", 'c', "webcontent_fd_passing_socket");
    args_parser.add_option(webdriver_fd_passing_socket, "File descriptor of the passing socket for the WebDriver connection", "webdriver-fd-passing-socket", 'd', "webdriver_fd_passing_socket");
    args_parser.parse(arguments);

    QSocketNotifier webcontent_notifier(QSocketNotifier::Type::Read);
    auto webcontent_client = TRY(create_connection_from_passed_socket<WebContent::ConnectionFromClient>(webcontent_fd_passing_socket, "WebContent"sv, webcontent_notifier));

    QSocketNotifier webdriver_notifier(QSocketNotifier::Type::Read);
    RefPtr<WebContent::WebDriverConnection> webdriver_client;
    if (webdriver_fd_passing_socket >= 0)
        webdriver_client = TRY(create_connection_from_passed_socket<WebContent::WebDriverConnection>(webdriver_fd_passing_socket, "WebDriver"sv, webdriver_notifier, webcontent_client->page_host()));

    return app.exec();
}

static ErrorOr<void> load_content_filters()
{
    auto file_or_error = Core::Stream::File::open(DeprecatedString::formatted("{}/home/anon/.config/BrowserContentFilters.txt", s_serenity_resource_root), Core::Stream::OpenMode::Read);
    if (file_or_error.is_error())
        file_or_error = Core::Stream::File::open(DeprecatedString::formatted("{}/res/ladybird/BrowserContentFilters.txt", s_serenity_resource_root), Core::Stream::OpenMode::Read);
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
