/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "AutoComplete.h"
#include <DevTools/HackStudio/LanguageServers/ClientConnection.h>

namespace LanguageServers::Shell {

class ClientConnection final : public LanguageServers::ClientConnection {
    C_OBJECT(ClientConnection);

    ClientConnection(NonnullRefPtr<Core::LocalSocket> socket, int client_id)
        : LanguageServers::ClientConnection(move(socket), client_id)
    {
        m_autocomplete_engine = make<AutoComplete>(*this, m_filedb);
        m_autocomplete_engine->set_declarations_of_document_callback = &ClientConnection::set_declarations_of_document_callback;
    }
    virtual ~ClientConnection() override = default;

private:
    virtual void set_auto_complete_mode(String const&) override { }
};
}
