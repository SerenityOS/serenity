/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibIPC/ServerConnection.h>
#include <SymbolServer/SymbolClientEndpoint.h>
#include <SymbolServer/SymbolServerEndpoint.h>

namespace SymbolClient {

struct Symbol {
    FlatPtr address { 0 };
    String name {};
    u32 offset { 0 };
    String filename {};
    u32 line_number { 0 };
};

Vector<Symbol> symbolicate_thread(pid_t pid, pid_t tid);

class Client
    : public IPC::ServerConnection<SymbolClientEndpoint, SymbolServerEndpoint>
    , public SymbolClientEndpoint {
    C_OBJECT(Client);

public:
    virtual void handshake() override;

    Optional<Symbol> symbolicate(const String& path, FlatPtr address);

private:
    Client();

    virtual void dummy() override;
};

}
