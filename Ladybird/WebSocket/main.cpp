/*
 * Copyright (c) 2021, Dex♪ <dexes.ttp@gmail.com>
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibCore/LocalServer.h>
#include <LibCore/System.h>
#include <LibFileSystem/FileSystem.h>
#include <LibIPC/SingleServer.h>
#include <LibMain/Main.h>
#include <LibTLS/Certificate.h>
#include <WebSocket/ConnectionFromClient.h>

// FIXME: Share b/w RequestServer and WebSocket
ErrorOr<ByteString> find_certificates(StringView serenity_resource_root)
{
    auto cert_path = ByteString::formatted("{}/res/ladybird/cacert.pem", serenity_resource_root);
    if (!FileSystem::exists(cert_path)) {
        auto app_dir = LexicalPath::dirname(TRY(Core::System::current_executable_path()));

        cert_path = ByteString::formatted("{}/cacert.pem", LexicalPath(app_dir).parent());
        if (!FileSystem::exists(cert_path))
            return Error::from_string_view("Don't know how to load certs!"sv);
    }
    return cert_path;
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    AK::set_rich_debug_enabled(true);

    int fd_passing_socket { -1 };
    StringView serenity_resource_root;
    Vector<ByteString> certificates;

    Core::ArgsParser args_parser;
    args_parser.add_option(fd_passing_socket, "File descriptor of the fd passing socket", "fd-passing-socket", 'c', "fd-passing-socket");
    args_parser.add_option(certificates, "Path to a certificate file", "certificate", 'C', "certificate");
    args_parser.add_option(serenity_resource_root, "Absolute path to directory for serenity resources", "serenity-resource-root", 'r', "serenity-resource-root");
    args_parser.parse(arguments);

    // Ensure the certificates are read out here.
    if (certificates.is_empty())
        certificates.append(TRY(find_certificates(serenity_resource_root)));
    DefaultRootCACertificates::set_default_certificate_paths(certificates.span());
    [[maybe_unused]] auto& certs = DefaultRootCACertificates::the();

    Core::EventLoop event_loop;

    auto client = TRY(IPC::take_over_accepted_client_from_system_server<WebSocket::ConnectionFromClient>());
    client->set_fd_passing_socket(TRY(Core::LocalSocket::adopt_fd(fd_passing_socket)));

    return event_loop.exec();
}
