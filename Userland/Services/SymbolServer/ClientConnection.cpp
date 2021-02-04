/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
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

#include <AK/MappedFile.h>
#include <LibELF/Image.h>
#include <SymbolServer/ClientConnection.h>
#include <SymbolServer/SymbolClientEndpoint.h>

namespace SymbolServer {

struct CachedELF {
    NonnullRefPtr<MappedFile> mapped_file;
    ELF::Image elf;
};

static HashMap<String, CachedELF> s_cache;
static HashMap<int, RefPtr<ClientConnection>> s_connections;

ClientConnection::ClientConnection(NonnullRefPtr<Core::LocalSocket> socket, int client_id)
    : IPC::ClientConnection<SymbolClientEndpoint, SymbolServerEndpoint>(*this, move(socket), client_id)
{
    s_connections.set(client_id, *this);
}

ClientConnection::~ClientConnection()
{
}

void ClientConnection::die()
{
    s_connections.remove(client_id());
}

OwnPtr<Messages::SymbolServer::GreetResponse> ClientConnection::handle(const Messages::SymbolServer::Greet&)
{
    return make<Messages::SymbolServer::GreetResponse>();
}

OwnPtr<Messages::SymbolServer::SymbolicateResponse> ClientConnection::handle(const Messages::SymbolServer::Symbolicate& message)
{
    auto path = message.path();
    if (!s_cache.contains(path)) {
        auto mapped_file = MappedFile::map(path);
        if (mapped_file.is_error()) {
            dbgln("Failed to map {}: {}", path, mapped_file.error().string());
            return make<Messages::SymbolServer::SymbolicateResponse>(false, String {}, 0, String {}, 0);
        }
        auto elf = ELF::Image(mapped_file.value()->bytes());
        if (!elf.is_valid()) {
            dbgln("ELF not valid: {}", path);
            return make<Messages::SymbolServer::SymbolicateResponse>(false, String {}, 0, String {}, 0);
        }
        auto cached_elf = CachedELF { mapped_file.release_value(), move(elf) };
        s_cache.set(path, move(cached_elf));
    }

    auto it = s_cache.find(path);
    ASSERT(it != s_cache.end());
    auto& cached_elf = it->value;

    u32 offset = 0;
    auto symbol = cached_elf.elf.symbolicate(message.address(), &offset);

    return make<Messages::SymbolServer::SymbolicateResponse>(true, symbol, offset, String {}, 0);
}

}
