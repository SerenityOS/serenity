#include <AK/DeprecatedString.h>
#include <Ladybird/FontPlugin.h>
#include <Ladybird/ImageCodecPlugin.h>
#include <Ladybird/Utilities.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibCore/SystemServerTakeover.h>
#include <LibFileSystem/FileSystem.h>
#include <LibJS/Bytecode/Interpreter.h>
#include <LibMain/Main.h>
#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/Loader/FrameLoader.h>
#include <LibWeb/Platform/EventLoopPluginSerenity.h>
#include <WebContent/ConnectionFromClient.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Web::Platform::EventLoopPlugin::install(*new Web::Platform::EventLoopPluginSerenity);
    Core::EventLoop event_loop;

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
    Web::FrameLoader::set_default_favicon_path(DeprecatedString::formatted("{}/res/icons/16x16/app-browser.png", s_serenity_resource_root));
    Web::FrameLoader::set_error_page_url(DeprecatedString::formatted("file://{}/res/html/error.html", s_serenity_resource_root));
    Web::Platform::FontPlugin::install(*new Ladybird::FontPlugin(is_layout_test_mode));

    VERIFY(webcontent_fd_passing_socket >= 0);

    TRY(Web::Bindings::initialize_main_thread_vm());

    auto webcontent_socket = TRY(Core::take_over_socket_from_system_server("WebContent"sv));
    auto websocket_client = TRY(WebContent::ConnectionFromClient::try_create(move(webcontent_socket)));
    websocket_client->set_fd_passing_socket(TRY(Core::LocalSocket::adopt_fd(webcontent_fd_passing_socket)));

    return event_loop.exec();
}
