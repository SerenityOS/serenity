/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "CppComprehensionEngine.h"
#include <DevTools/HackStudio/LanguageServers/ClientConnection.h>

namespace LanguageServers::Cpp {

class ClientConnection final : public LanguageServers::ClientConnection {
    C_OBJECT(ClientConnection);

public:
    ClientConnection(NonnullRefPtr<Core::LocalSocket> socket, int client_id)
        : LanguageServers::ClientConnection(move(socket), client_id)
    {
        m_autocomplete_engine = make<CppComprehensionEngine>(m_filedb);
        m_autocomplete_engine->set_declarations_of_document_callback = [this](const String& filename, Vector<GUI::AutocompleteProvider::Declaration>&& declarations) {
            async_declarations_in_document(filename, move(declarations));
        };
    }

    virtual ~ClientConnection() override = default;
};
}
