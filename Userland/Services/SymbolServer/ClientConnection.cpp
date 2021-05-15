/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/MappedFile.h>
#include <LibDebug/DebugInfo.h>
#include <LibELF/Image.h>
#include <SymbolServer/ClientConnection.h>
#include <SymbolServer/SymbolClientEndpoint.h>

namespace SymbolServer {

struct CachedELF {
    NonnullRefPtr<MappedFile> mapped_file;
    Debug::DebugInfo debug_info;
};

static HashMap<String, OwnPtr<CachedELF>> s_cache;
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

void ClientConnection::greet()
{
}

Messages::SymbolServer::SymbolicateResponse ClientConnection::symbolicate(String const& path, u32 address)
{
    if (!s_cache.contains(path)) {
        auto mapped_file = MappedFile::map(path);
        if (mapped_file.is_error()) {
            dbgln("Failed to map {}: {}", path, mapped_file.error().string());
            s_cache.set(path, {});
            return { false, String {}, 0, String {}, 0 };
        }
        auto elf = make<ELF::Image>(mapped_file.value()->bytes());
        if (!elf->is_valid()) {
            dbgln("ELF not valid: {}", path);
            s_cache.set(path, {});
            return { false, String {}, 0, String {}, 0 };
        }
        Debug::DebugInfo debug_info(move(elf));
        auto cached_elf = make<CachedELF>(mapped_file.release_value(), move(debug_info));
        s_cache.set(path, move(cached_elf));
    }

    auto it = s_cache.find(path);
    VERIFY(it != s_cache.end());
    auto& cached_elf = it->value;

    if (!cached_elf)
        return { false, String {}, 0, String {}, 0 };

    u32 offset = 0;
    auto symbol = cached_elf->debug_info.elf().symbolicate(address, &offset);
    auto source_position = cached_elf->debug_info.get_source_position(address);
    String filename;
    u32 line_number = 0;
    if (source_position.has_value()) {
        filename = source_position.value().file_path;
        line_number = source_position.value().line_number;
    }

    return { true, symbol, offset, filename, line_number };
}

}
