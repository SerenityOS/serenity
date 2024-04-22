/*
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "HelperProcess.h"
#include "Utilities.h"
#include <LibCore/Environment.h>
#include <LibCore/SingletonProcess.h>
#include <LibWebView/ProcessManager.h>

ErrorOr<NonnullRefPtr<WebView::WebContentClient>> launch_web_content_process(
    WebView::ViewImplementation& view,
    ReadonlySpan<ByteString> candidate_web_content_paths,
    Ladybird::WebContentOptions const& web_content_options,
    Optional<IPC::File> request_server_socket)
{
    int socket_fds[2] {};
    TRY(Core::System::socketpair(AF_LOCAL, SOCK_STREAM, 0, socket_fds));

    int ui_fd = socket_fds[0];
    int wc_fd = socket_fds[1];

    auto child_pid = TRY(Core::System::fork());
    if (child_pid == 0) {
        TRY(Core::System::close(ui_fd));

        auto takeover_string = TRY(String::formatted("WebContent:{}", wc_fd));
        TRY(Core::Environment::set("SOCKET_TAKEOVER"sv, takeover_string, Core::Environment::Overwrite::Yes));

        ErrorOr<void> result;
        for (auto const& path : candidate_web_content_paths) {
            constexpr auto callgrind_prefix_length = 3;

            if (Core::System::access(path, X_OK).is_error())
                continue;

            auto arguments = Vector {
                "valgrind"sv,
                "--tool=callgrind"sv,
                "--instr-atstart=no"sv,
                path.view(),
                "--command-line"sv,
                web_content_options.command_line,
                "--executable-path"sv,
                web_content_options.executable_path,
            };
            if (web_content_options.enable_callgrind_profiling == Ladybird::EnableCallgrindProfiling::No)
                arguments.remove(0, callgrind_prefix_length);
            if (web_content_options.is_layout_test_mode == Ladybird::IsLayoutTestMode::Yes)
                arguments.append("--layout-test-mode"sv);
            if (web_content_options.use_lagom_networking == Ladybird::UseLagomNetworking::Yes)
                arguments.append("--use-lagom-networking"sv);
            if (web_content_options.enable_gpu_painting == Ladybird::EnableGPUPainting::Yes)
                arguments.append("--use-gpu-painting"sv);
            if (web_content_options.wait_for_debugger == Ladybird::WaitForDebugger::Yes)
                arguments.append("--wait-for-debugger"sv);
            if (web_content_options.log_all_js_exceptions == Ladybird::LogAllJSExceptions::Yes)
                arguments.append("--log-all-js-exceptions"sv);
            if (web_content_options.enable_idl_tracing == Ladybird::EnableIDLTracing::Yes)
                arguments.append("--enable-idl-tracing"sv);
            if (web_content_options.expose_internals_object == Ladybird::ExposeInternalsObject::Yes)
                arguments.append("--expose-internals-object"sv);
            if (auto server = mach_server_name(); server.has_value()) {
                arguments.append("--mach-server-name"sv);
                arguments.append(server.value());
            }
            String fd_string;
            if (request_server_socket.has_value()) {
                arguments.append("--request-server-socket"sv);
                fd_string = MUST(String::number(request_server_socket->fd()));
                arguments.append(fd_string.bytes_as_string_view());
            }

            result = Core::System::exec(arguments[0], arguments.span(), Core::System::SearchInPath::Yes);
            if (!result.is_error())
                break;
        }

        if (result.is_error())
            warnln("Could not launch any of {}: {}", candidate_web_content_paths, result.error());
        VERIFY_NOT_REACHED();
    }

    TRY(Core::System::close(wc_fd));

    auto socket = TRY(Core::LocalSocket::adopt_fd(ui_fd));
    TRY(socket->set_blocking(true));

    auto new_client = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) WebView::WebContentClient(move(socket), view)));

    if (web_content_options.enable_callgrind_profiling == Ladybird::EnableCallgrindProfiling::Yes) {
        dbgln();
        dbgln("\033[1;45mLaunched WebContent process under callgrind!\033[0m");
        dbgln("\033[100mRun `\033[4mcallgrind_control -i on\033[24m` to start instrumentation and `\033[4mcallgrind_control -i off\033[24m` stop it again.\033[0m");
        dbgln();
    }

    return new_client;
}

template<typename Client>
ErrorOr<NonnullRefPtr<Client>> launch_generic_server_process(ReadonlySpan<ByteString> candidate_server_paths, StringView server_name, Vector<StringView> extra_arguments = {})
{
    int socket_fds[2] {};
    TRY(Core::System::socketpair(AF_LOCAL, SOCK_STREAM, 0, socket_fds));

    int ui_fd = socket_fds[0];
    int server_fd = socket_fds[1];

    auto child_pid = TRY(Core::System::fork());
    if (child_pid == 0) {
        TRY(Core::System::close(ui_fd));

        auto takeover_string = TRY(String::formatted("{}:{}", server_name, server_fd));
        TRY(Core::Environment::set("SOCKET_TAKEOVER"sv, takeover_string, Core::Environment::Overwrite::Yes));

        ErrorOr<void> result;
        for (auto const& path : candidate_server_paths) {

            if (Core::System::access(path, X_OK).is_error())
                continue;

            auto arguments = Vector<StringView> {
                path.view(),
            };

            if (!extra_arguments.is_empty())
                arguments.extend(extra_arguments);

            result = Core::System::exec(arguments[0], arguments.span(), Core::System::SearchInPath::Yes);
            if (!result.is_error())
                break;
        }

        if (result.is_error())
            warnln("Could not launch any of {}: {}", candidate_server_paths, result.error());
        VERIFY_NOT_REACHED();
    }

    TRY(Core::System::close(server_fd));

    auto socket = TRY(Core::LocalSocket::adopt_fd(ui_fd));
    TRY(socket->set_blocking(true));

    auto new_client = TRY(try_make_ref_counted<Client>(move(socket)));

    WebView::ProcessManager::the().add_process(WebView::process_type_from_name(server_name), child_pid);

    return new_client;
}

ErrorOr<NonnullRefPtr<ImageDecoderClient::Client>> launch_image_decoder_process(ReadonlySpan<ByteString> candidate_image_decoder_paths)
{
    return launch_generic_server_process<ImageDecoderClient::Client>(candidate_image_decoder_paths, "ImageDecoder"sv);
}

ErrorOr<NonnullRefPtr<Web::HTML::WebWorkerClient>> launch_web_worker_process(ReadonlySpan<ByteString> candidate_web_worker_paths, NonnullRefPtr<Protocol::RequestClient> request_client)
{
    auto socket = TRY(connect_new_request_server_client(move(request_client)));

    Vector<StringView> arguments;
    String fd_string = MUST(String::number(socket.fd()));

    arguments.append("--request-server-socket"sv);
    arguments.append(fd_string.bytes_as_string_view());

    return launch_generic_server_process<Web::HTML::WebWorkerClient>(candidate_web_worker_paths, "WebWorker"sv, move(arguments));
}

ErrorOr<NonnullRefPtr<Protocol::RequestClient>> launch_request_server_process(ReadonlySpan<ByteString> candidate_request_server_paths, StringView serenity_resource_root, Vector<ByteString> const& certificates)
{
    Vector<StringView> arguments;
    if (!serenity_resource_root.is_empty()) {
        arguments.append("--serenity-resource-root"sv);
        arguments.append(serenity_resource_root);
    }

    Vector<ByteString> certificate_args;
    for (auto const& certificate : certificates) {
        certificate_args.append(ByteString::formatted("--certificate={}", certificate));
        arguments.append(certificate_args.last().view());
    }

    if (auto server = mach_server_name(); server.has_value()) {
        arguments.append("--mach-server-name"sv);
        arguments.append(server.value());
    }

    return launch_generic_server_process<Protocol::RequestClient>(candidate_request_server_paths, "RequestServer"sv, move(arguments));
}

ErrorOr<NonnullRefPtr<SQL::SQLClient>> launch_sql_server_process(ReadonlySpan<ByteString> candidate_sql_server_paths)
{
    Vector<ByteString> arguments;

    if (auto server = mach_server_name(); server.has_value()) {
        arguments.append("--mach-server-name"sv);
        arguments.append(server.value());
    }

    auto [client, pid] = TRY(Core::launch_singleton_process<SQL::SQLClient>("SQLServer"sv, candidate_sql_server_paths, arguments));
    WebView::ProcessManager::the().add_process(WebView::ProcessType::SQLServer, pid);

    return client;
}

ErrorOr<IPC::File> connect_new_request_server_client(Protocol::RequestClient& client)
{
    auto new_socket = client.send_sync_but_allow_failure<Messages::RequestServer::ConnectNewClient>();
    if (!new_socket)
        return Error::from_string_literal("Failed to connect to RequestServer");

    auto socket = new_socket->take_client_socket();

    // FIXME: IPC::Files transferred over the wire are always set O_CLOEXEC during decoding.
    //        Perhaps we should add an option to IPC::File to allow the receiver to decide whether to
    //        make it O_CLOEXEC or not. Or an attribute in the .ipc file?
    auto fd = socket.fd();
    auto fd_flags = MUST(Core::System::fcntl(fd, F_GETFD));
    fd_flags &= ~FD_CLOEXEC;
    MUST(Core::System::fcntl(fd, F_SETFD, fd_flags));

    return socket;
}
