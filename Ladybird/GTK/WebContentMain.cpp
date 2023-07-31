#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibCore/SystemServerTakeover.h>
#include <LibJS/Bytecode/Interpreter.h>
#include <LibMain/Main.h>
#include <LibWeb/Bindings/MainThreadVM.h>
#include <WebContent/ConnectionFromClient.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Core::EventLoop event_loop;

    int webcontent_fd_passing_socket = -1;
    bool is_layout_test_mode = false;
    bool use_javascript_bytecode = false;

    Core::ArgsParser args_parser;
    args_parser.add_option(webcontent_fd_passing_socket, "File descriptor", "webcontent-fd-passing-socket", 'c', "fd");
    args_parser.add_option(is_layout_test_mode, "Is layout test mode", "layout-test-mode", 0);
    args_parser.add_option(use_javascript_bytecode, "Enable JavaScript bytecode VM", "use-bytecode", 0);
    args_parser.parse(arguments);

    JS::Bytecode::Interpreter::set_enabled(use_javascript_bytecode);
    VERIFY(webcontent_fd_passing_socket >= 0);

    TRY(Web::Bindings::initialize_main_thread_vm());

    auto webcontent_socket = TRY(Core::take_over_socket_from_system_server("WebContent"sv));
    auto websocket_client = TRY(WebContent::ConnectionFromClient::try_create(move(webcontent_socket)));
    websocket_client->set_fd_passing_socket(TRY(Core::LocalSocket::adopt_fd(webcontent_fd_passing_socket)));

    return event_loop.exec();
}
