#include "EventLoopImplementationGLib.h"
#include <AK/DeprecatedString.h>
#include <Ladybird/FontPlugin.h>
#include <Ladybird/HelperProcess.h>
#include <Ladybird/ImageCodecPlugin.h>
#include <Ladybird/Utilities.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibCore/SystemServerTakeover.h>
#include <LibFileSystem/FileSystem.h>
#include <LibJS/Bytecode/Interpreter.h>
#include <LibMain/Main.h>
#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/Loader/FileDirectoryLoader.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWeb/Platform/EventLoopPluginSerenity.h>
#include <LibWebView/RequestServerAdapter.h>
#include <LibWebView/WebSocketClientAdapter.h>
#include <WebContent/ConnectionFromClient.h>

#ifdef HAVE_LIBSOUP
#    include "RequestManagerSoup.h"
#endif

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    platform_init();

    int webcontent_fd_passing_socket = -1;
    bool is_layout_test_mode = false;
    bool use_lagom_networking = false;

    Core::ArgsParser args_parser;
    args_parser.add_option(webcontent_fd_passing_socket, "File descriptor", "webcontent-fd-passing-socket", 'c', "fd");
    args_parser.add_option(is_layout_test_mode, "Is layout test mode", "layout-test-mode", 0);
    args_parser.add_option(use_lagom_networking, "Enable Lagom servers for networking", "use-lagom-networking", 0);
    args_parser.parse(arguments);

    Web::Platform::ImageCodecPlugin::install(*new Ladybird::ImageCodecPlugin);
    Web::Platform::FontPlugin::install(*new Ladybird::FontPlugin(is_layout_test_mode));
    Web::Platform::EventLoopPlugin::install(*new Web::Platform::EventLoopPluginSerenity);

    Web::set_resource_directory_url(DeprecatedString::formatted("file://{}/res", s_serenity_resource_root));
    Web::set_directory_page_url(DeprecatedString::formatted("file://{}/res/html/directory.html", s_serenity_resource_root));

    if (use_lagom_networking) {
        auto candidate_request_server_paths = TRY(get_paths_for_helper_process("RequestServer"sv));
        auto request_server_client = TRY(launch_request_server_process(candidate_request_server_paths, s_serenity_resource_root));
        Web::ResourceLoader::initialize(TRY(WebView::RequestServerAdapter::try_create(move(request_server_client))));

        auto candidate_web_socket_paths = TRY(get_paths_for_helper_process("WebSocket"sv));
        auto web_socket_client = TRY(launch_web_socket_process(candidate_web_socket_paths, s_serenity_resource_root));
        Web::WebSockets::WebSocketClientManager::initialize(TRY(WebView::WebSocketClientManagerAdapter::try_create(move(web_socket_client))));
    } else {
        Core::EventLoopManager::install(*new EventLoopManagerGLib);
#ifdef HAVE_LIBSOUP
        Web::ResourceLoader::initialize(RequestManagerSoup::create());
        // Web::WebSockets::WebSocketClientManager::initialize(WebSocketClientManagerSoup::create());
#else
        VERIFY_NOT_REACHED();
#endif
    }

    Core::EventLoop event_loop;

    VERIFY(webcontent_fd_passing_socket >= 0);

    TRY(Web::Bindings::initialize_main_thread_vm());

    auto webcontent_socket = TRY(Core::take_over_socket_from_system_server("WebContent"sv));
    auto websocket_client = TRY(WebContent::ConnectionFromClient::try_create(move(webcontent_socket)));
    websocket_client->set_fd_passing_socket(TRY(Core::LocalSocket::adopt_fd(webcontent_fd_passing_socket)));

    return event_loop.exec();
}
