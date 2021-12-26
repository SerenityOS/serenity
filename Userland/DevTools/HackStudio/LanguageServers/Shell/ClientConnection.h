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
        m_autocomplete_engine = make<AutoComplete>(m_filedb);
        m_autocomplete_engine->set_declarations_of_document_callback = [this](const String& filename, Vector<GUI::AutocompleteProvider::Declaration>&& declarations) {
            async_declarations_in_document(filename, move(declarations));
        };
    }
    virtual ~ClientConnection() override = default;
};
}
