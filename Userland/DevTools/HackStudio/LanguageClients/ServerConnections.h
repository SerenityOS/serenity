/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include "../LanguageClient.h"
#include <AK/LexicalPath.h>
#include <DevTools/HackStudio/LanguageServers/LanguageClientEndpoint.h>
#include <DevTools/HackStudio/LanguageServers/LanguageServerEndpoint.h>
#include <LibIPC/ServerConnection.h>

#define LANGUAGE_CLIENT(namespace_, socket_name)                                               \
    namespace namespace_ {                                                                     \
    class ServerConnection : public HackStudio::ServerConnection {                             \
        C_OBJECT(ServerConnection)                                                             \
    private:                                                                                   \
        ServerConnection(const String& project_path)                                           \
            : HackStudio::ServerConnection("/tmp/portal/language/" #socket_name, project_path) \
        {                                                                                      \
        }                                                                                      \
    };                                                                                         \
    }

namespace LanguageClients {

LANGUAGE_CLIENT(Cpp, cpp)
LANGUAGE_CLIENT(Shell, shell)

}

#undef LANGUAGE_CLIENT
