/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "../LanguageClient.h"
#include <AK/LexicalPath.h>
#include <DevTools/HackStudio/LanguageServers/LanguageClientEndpoint.h>
#include <DevTools/HackStudio/LanguageServers/LanguageServerEndpoint.h>
#include <LibIPC/ConnectionToServer.h>

#define LANGUAGE_CLIENT(language_name_, socket_name)                                                \
    namespace language_name_ {                                                                      \
    class ConnectionToServer final : public HackStudio::ConnectionToServer {                        \
        IPC_CLIENT_CONNECTION(ConnectionToServer, "/tmp/session/%sid/portal/language/" socket_name) \
    public:                                                                                         \
        static char const* language_name()                                                          \
        {                                                                                           \
            return #language_name_;                                                                 \
        }                                                                                           \
                                                                                                    \
    private:                                                                                        \
        ConnectionToServer(NonnullOwnPtr<Core::LocalSocket> socket, ByteString const& project_path) \
            : HackStudio::ConnectionToServer(move(socket), project_path)                            \
        {                                                                                           \
        }                                                                                           \
    };                                                                                              \
    }

namespace LanguageClients {

LANGUAGE_CLIENT(Cpp, "cpp"sv)
LANGUAGE_CLIENT(Shell, "shell"sv)

}

#undef LANGUAGE_CLIENT
