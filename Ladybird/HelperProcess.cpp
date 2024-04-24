/*
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "HelperProcess.h"
#include "Utilities.h"
#include <AK/Enumerate.h>
#include <LibCore/Process.h>
#include <LibWebView/ProcessManager.h>

enum class RegisterWithProcessManager {
    No,
    Yes,
};

template<typename ClientType, typename SpawnFunction>
static ErrorOr<NonnullRefPtr<ClientType>> launch_server_process_impl(
    StringView server_name,
    ReadonlySpan<ByteString> candidate_server_paths,
    Vector<ByteString> const& arguments,
    RegisterWithProcessManager register_with_process_manager,
    SpawnFunction&& spawn_function)
{
    for (auto [i, path] : enumerate(candidate_server_paths)) {
        auto result = spawn_function(Core::ProcessSpawnOptions {
            .name = server_name,
            .executable = path,
            .arguments = arguments,
        });

        if (!result.is_error()) {
            auto process = result.release_value();

            if (register_with_process_manager == RegisterWithProcessManager::Yes)
                WebView::ProcessManager::the().add_process(WebView::process_type_from_name(server_name), process.process.pid());

            return move(process.client);
        }

        if (i == candidate_server_paths.size() - 1) {
            warnln("Could not launch any of {}: {}", candidate_server_paths, result.error());
            return result.release_error();
        }
    }

    VERIFY_NOT_REACHED();
}

template<typename ClientType, typename... ClientArguments>
static ErrorOr<NonnullRefPtr<ClientType>> launch_generic_server_process(
    StringView server_name,
    ReadonlySpan<ByteString> candidate_server_paths,
    Vector<ByteString> const& arguments,
    RegisterWithProcessManager register_with_process_manager,
    ClientArguments&&... client_arguments)
{
    return launch_server_process_impl<ClientType>(server_name, candidate_server_paths, arguments, register_with_process_manager, [&](auto options) {
        return Core::IPCProcess::spawn<ClientType>(move(options), forward<ClientArguments>(client_arguments)...);
    });
}

template<typename ClientType, typename... ClientArguments>
static ErrorOr<NonnullRefPtr<ClientType>> launch_singleton_server_process(
    StringView server_name,
    ReadonlySpan<ByteString> candidate_server_paths,
    Vector<ByteString> const& arguments,
    RegisterWithProcessManager register_with_process_manager,
    ClientArguments&&... client_arguments)
{
    return launch_server_process_impl<ClientType>(server_name, candidate_server_paths, arguments, register_with_process_manager, [&](auto options) {
        return Core::IPCProcess::spawn_singleton<ClientType>(move(options), forward<ClientArguments>(client_arguments)...);
    });
}

ErrorOr<NonnullRefPtr<WebView::WebContentClient>> launch_web_content_process(
    WebView::ViewImplementation& view,
    ReadonlySpan<ByteString> candidate_web_content_paths,
    Ladybird::WebContentOptions const& web_content_options,
    Optional<IPC::File> request_server_socket)
{
    Vector<ByteString> arguments {
        "--command-line"sv,
        web_content_options.command_line.to_byte_string(),
        "--executable-path"sv,
        web_content_options.executable_path.to_byte_string(),
    };

    if (web_content_options.enable_callgrind_profiling == Ladybird::EnableCallgrindProfiling::Yes) {
        arguments.prepend("--instr-atstart=no"sv);
        arguments.prepend("--tool=callgrind"sv);
        arguments.prepend("valgrind"sv);
    }
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
    if (request_server_socket.has_value()) {
        arguments.append("--request-server-socket"sv);
        arguments.append(ByteString::number(request_server_socket->fd()));
    }

    auto web_content_client = TRY(launch_generic_server_process<WebView::WebContentClient>("WebContent"sv, candidate_web_content_paths, arguments, RegisterWithProcessManager::No, view));

    if (web_content_options.enable_callgrind_profiling == Ladybird::EnableCallgrindProfiling::Yes) {
        dbgln();
        dbgln("\033[1;45mLaunched WebContent process under callgrind!\033[0m");
        dbgln("\033[100mRun `\033[4mcallgrind_control -i on\033[24m` to start instrumentation and `\033[4mcallgrind_control -i off\033[24m` stop it again.\033[0m");
        dbgln();
    }

    return web_content_client;
}

ErrorOr<NonnullRefPtr<ImageDecoderClient::Client>> launch_image_decoder_process(ReadonlySpan<ByteString> candidate_image_decoder_paths)
{
    return launch_generic_server_process<ImageDecoderClient::Client>("ImageDecoder"sv, candidate_image_decoder_paths, {}, RegisterWithProcessManager::Yes);
}

ErrorOr<NonnullRefPtr<Web::HTML::WebWorkerClient>> launch_web_worker_process(ReadonlySpan<ByteString> candidate_web_worker_paths, NonnullRefPtr<Protocol::RequestClient> request_client)
{
    auto socket = TRY(connect_new_request_server_client(move(request_client)));

    Vector<ByteString> arguments {
        "--request-server-socket"sv,
        ByteString::number(socket.fd()),
    };

    return launch_generic_server_process<Web::HTML::WebWorkerClient>("WebWorker"sv, candidate_web_worker_paths, arguments, RegisterWithProcessManager::Yes);
}

ErrorOr<NonnullRefPtr<Protocol::RequestClient>> launch_request_server_process(ReadonlySpan<ByteString> candidate_request_server_paths, StringView serenity_resource_root, Vector<ByteString> const& certificates)
{
    Vector<ByteString> arguments;

    if (!serenity_resource_root.is_empty()) {
        arguments.append("--serenity-resource-root"sv);
        arguments.append(serenity_resource_root);
    }

    for (auto const& certificate : certificates)
        arguments.append(ByteString::formatted("--certificate={}", certificate));

    if (auto server = mach_server_name(); server.has_value()) {
        arguments.append("--mach-server-name"sv);
        arguments.append(server.value());
    }

    return launch_generic_server_process<Protocol::RequestClient>("RequestServer"sv, candidate_request_server_paths, arguments, RegisterWithProcessManager::Yes);
}

ErrorOr<NonnullRefPtr<SQL::SQLClient>> launch_sql_server_process(ReadonlySpan<ByteString> candidate_sql_server_paths)
{
    Vector<ByteString> arguments;

    if (auto server = mach_server_name(); server.has_value()) {
        arguments.append("--mach-server-name"sv);
        arguments.append(server.value());
    }

    return launch_singleton_server_process<SQL::SQLClient>("SQLServer"sv, candidate_sql_server_paths, arguments, RegisterWithProcessManager::Yes);
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
