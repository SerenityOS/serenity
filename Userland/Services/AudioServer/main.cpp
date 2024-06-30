/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ConnectionFromClient.h"
#include "ConnectionFromManagerClient.h"
#include "Mixer.h"
#include <LibCore/ConfigFile.h>
#include <LibCore/LocalServer.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments)
{
    TRY(Core::System::pledge("stdio recvfd thread accept cpath rpath wpath unix"));

    auto config = TRY(Core::ConfigFile::open_for_app("Audio", Core::ConfigFile::AllowWriting::Yes));
    TRY(Core::System::unveil(config->filename(), "rwc"sv));

    auto audio_unveil_result = Core::System::unveil("/dev/audio", "wc");
    // System may not have audio devices, which we handle gracefully.
    if (audio_unveil_result.is_error())
        dbgln("Couldn't unveil audio devices: {}", audio_unveil_result.error());

    TRY(Core::System::unveil(nullptr, nullptr));

    Core::EventLoop event_loop;
    auto mixer = TRY(AudioServer::Mixer::try_create(config));
    auto server = TRY(Core::LocalServer::try_create());
    TRY(server->take_over_from_system_server("/tmp/session/%sid/portal/audio"));

    server->on_accept = [&](NonnullOwnPtr<Core::LocalSocket> client_socket) {
        static int s_next_client_id = 0;
        int client_id = ++s_next_client_id;
        (void)IPC::new_client_connection<AudioServer::ConnectionFromClient>(move(client_socket), client_id, *mixer);
    };

    auto manager_server = TRY(Core::LocalServer::try_create());
    TRY(manager_server->take_over_from_system_server("/tmp/session/%sid/portal/audiomanager"));

    manager_server->on_accept = [&](NonnullOwnPtr<Core::LocalSocket> client_socket) {
        static int s_next_client_id = 0;
        int client_id = ++s_next_client_id;
        (void)IPC::new_client_connection<AudioServer::ConnectionFromManagerClient>(move(client_socket), client_id, *mixer);
    };

    TRY(Core::System::pledge("stdio recvfd thread accept cpath rpath wpath"));

    return event_loop.exec();
}
