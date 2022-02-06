/*
 * Copyright (c) 2022, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SystemServerTakeover.h"
#include <LibCore/System.h>

namespace Core {

HashMap<String, int> s_overtaken_sockets {};
bool s_overtaken_sockets_parsed { false };

static void parse_sockets_from_system_server()
{
    VERIFY(!s_overtaken_sockets_parsed);

    constexpr auto socket_takeover = "SOCKET_TAKEOVER";
    const char* sockets = getenv(socket_takeover);
    if (!sockets) {
        s_overtaken_sockets_parsed = true;
        return;
    }

    for (auto& socket : StringView(sockets).split_view(' ')) {
        auto params = socket.split_view(':');
        s_overtaken_sockets.set(params[0].to_string(), strtol(params[1].to_string().characters(), nullptr, 10));
    }

    s_overtaken_sockets_parsed = true;
    // We wouldn't want our children to think we're passing
    // them a socket either, so unset the env variable.
    unsetenv(socket_takeover);
}

ErrorOr<NonnullOwnPtr<Core::Stream::LocalSocket>> take_over_socket_from_system_server(String const& socket_path)
{
    if (!s_overtaken_sockets_parsed)
        parse_sockets_from_system_server();

    int fd;
    if (socket_path.is_null()) {
        // We want the first (and only) socket.
        VERIFY(s_overtaken_sockets.size() == 1);
        fd = s_overtaken_sockets.begin()->value;
    } else {
        auto it = s_overtaken_sockets.find(socket_path);
        if (it == s_overtaken_sockets.end())
            return Error::from_string_literal("Non-existent socket requested"sv);
        fd = it->value;
    }

    // Sanity check: it has to be a socket.
    auto stat = TRY(Core::System::fstat(fd));

    if (!S_ISSOCK(stat.st_mode))
        return Error::from_string_literal("The fd we got from SystemServer is not a socket"sv);

    auto socket = TRY(Core::Stream::LocalSocket::adopt_fd(fd));
    // It had to be !CLOEXEC for obvious reasons, but we
    // don't need it to be !CLOEXEC anymore, so set the
    // CLOEXEC flag now.
    TRY(socket->set_close_on_exec(true));

    return socket;
}

}
