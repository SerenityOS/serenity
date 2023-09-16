/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <AK/OwnPtr.h>
#include <Ladybird/Utilities.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibCore/LocalServer.h>
#include <LibCore/System.h>
#include <LibFileSystem/FileSystem.h>
#include <LibIPC/SingleServer.h>
#include <LibTLS/Certificate.h>
#include <RequestServer/ConnectionFromClient.h>
#include <RequestServer/GeminiProtocol.h>
#include <RequestServer/HttpProtocol.h>
#include <RequestServer/HttpsProtocol.h>

// FIXME: Share b/w RequestServer and WebSocket
ErrorOr<String> find_certificates(StringView serenity_resource_root)
{
    auto cert_path = TRY(String::formatted("{}/res/ladybird/cacert.pem", serenity_resource_root));
    if (!FileSystem::exists(cert_path)) {
        auto app_dir = LexicalPath::dirname(TRY(Core::System::current_executable_path()).to_deprecated_string());

        cert_path = TRY(String::formatted("{}/cacert.pem", LexicalPath(app_dir).parent()));
        if (!FileSystem::exists(cert_path))
            return Error::from_string_view("Don't know how to load certs!"sv);
    }
    return cert_path;
}

ErrorOr<int> service_main(int ipc_socket, int fd_passing_socket)
{
    // Ensure the certificates are read out here.
    DefaultRootCACertificates::set_default_certificate_path(TRY(find_certificates(s_serenity_resource_root)));
    [[maybe_unused]] auto& certs = DefaultRootCACertificates::the();

    Core::EventLoop event_loop;

    // FIXME: Don't leak these :V
    [[maybe_unused]] auto* gemini = new RequestServer::GeminiProtocol;
    [[maybe_unused]] auto* http = new RequestServer::HttpProtocol;
    [[maybe_unused]] auto* https = new RequestServer::HttpsProtocol;

    auto socket = TRY(Core::LocalSocket::adopt_fd(ipc_socket));
    auto client = TRY(RequestServer::ConnectionFromClient::try_create(move(socket)));
    client->set_fd_passing_socket(TRY(Core::LocalSocket::adopt_fd(fd_passing_socket)));

    return event_loop.exec();
}
