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

#include <LibSymbolClient/Client.h>

namespace SymbolClient {

Client::Client()
    : IPC::ServerConnection<SymbolClientEndpoint, SymbolServerEndpoint>(*this, "/tmp/portal/symbol")
{
    handshake();
}

void Client::handshake()
{
    send_sync<Messages::SymbolServer::Greet>();
}

void Client::handle(const Messages::SymbolClient::Dummy&)
{
}

Vector<Symbol> Client::symbolicate(const String& path, const Vector<FlatPtr>& addresses)
{
    auto response = send_sync<Messages::SymbolServer::Symbolicate>(path, addresses);
    if (!response->success())
        return {};

    Vector<Symbol> symbols;
    for (auto& symbol : response->symbols()) {
        Symbol s;
        s.name = symbol;
        symbols.append(move(s));
    }
    return symbols;
}

}
