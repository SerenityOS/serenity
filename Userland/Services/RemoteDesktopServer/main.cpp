/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/MappedFile.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibCore/File.h>
#include <LibCore/LocalServer.h>
#include <LibCore/TCPServer.h>
#include <LibRemoteDesktop/RemoteCompositorServerConnection.h>
#include <RemoteDesktopServer/Client.h>
#include <RemoteDesktopServer/Configuration.h>
#include <RemoteDesktopServer/GfxClient.h>
#include <RemoteDesktopServer/Server.h>
#include <stdio.h>
#include <unistd.h>

#include <LibGfx/Bitmap.h>
#include <LibGfx/DisjointRectSet.h>
#include <LibGfx/FontDatabase.h>
#include <LibGfx/Painter.h>
#include <LibRemoteDesktop/RemoteCompositor.h>
int main(int argc, char** argv)
{
    if (pledge("stdio accept recvfd sendfd proc rpath unix sigaction inet unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/res", "r") < 0) {
        perror("unveil");
        return 1;
    }

    // TODO: Move this into a unit test
    Gfx::FontDatabase::set_default_font_query("Katica 10 400");
    auto test_bitmap = Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRA8888, { 400, 300 }, 1).release_value();
    auto test_bitmap2 = Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRA8888, { 400, 300 }, 1).release_value();
    auto test_bitmap3 = Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRA8888, { 400, 300 }, 1).release_value();
    VERIFY(test_bitmap->is_rect_equal(test_bitmap->rect(), *test_bitmap2, {}));
    Gfx::Painter painter(*test_bitmap2);
    painter.set_pixel(20, 3, Color::White);
    VERIFY(!test_bitmap->is_rect_equal(test_bitmap->rect(), *test_bitmap2, {}));
    auto diff = RemoteGfx::BitmapDiff::create(0, *test_bitmap, *test_bitmap2, {});
    VERIFY(!test_bitmap2->is_rect_equal(test_bitmap->rect(), *test_bitmap3, {}));
    diff.apply_to_bitmap(*test_bitmap3);
    VERIFY(test_bitmap2->is_rect_equal(test_bitmap->rect(), *test_bitmap3, {}));

    String default_listen_address = "0.0.0.0";
    u16 default_port = 3388;
    String root_path = "/www";

    String listen_address = default_listen_address;
    int port = default_port;

    Core::ArgsParser args_parser;
    args_parser.add_option(listen_address, "IP address to listen on", "listen-address", 'l', "listen_address");
    args_parser.add_option(port, "Port to listen on", "port", 'p', "port");
    args_parser.parse(argc, argv);

    auto ipv4_address = IPv4Address::from_string(listen_address);
    if (!ipv4_address.has_value()) {
        warnln("Invalid listen address: {}", listen_address);
        return 1;
    }

    if ((u16)port != port) {
        warnln("Invalid port number: {}", port);
        return 1;
    }

    RemoteDesktopServer::Configuration configuration;

    Core::EventLoop loop;
    dbgln("event loop: {:p}", &loop);

    auto remote_gfx_server = Core::LocalServer::construct();
    bool ok = remote_gfx_server->take_over_from_system_server("/tmp/portal/remotegfx");
    VERIFY(ok);

    RemoteGfx::RemoteGfxFontDatabase font_database;
    font_database.populate_own_fonts();
    auto server = RemoteDesktopServer::Server::construct(configuration, font_database, nullptr);
    if (!server->listen(ipv4_address.value(), port)) {
        warnln("Failed to listen on {}:{}", ipv4_address.value(), port);
        return 1;
    }

    remote_gfx_server->on_accept = [&](auto client_socket) {
        [[maybe_unused]] auto client = adopt_ref(*new RemoteDesktopServer::GfxClient(move(client_socket), server));
    };

    outln("Listening on {}:{}", ipv4_address.value(), port);

    unveil("/tmp/portal/remotecompositor", "rw");
    unveil(nullptr, nullptr);

    return loop.exec();
}
