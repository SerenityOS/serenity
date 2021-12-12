/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "JSComprehensionEngine.h"
#include <DevTools/HackStudio/LanguageServers/ClientConnection.h>

namespace LanguageServers::JS {

class ClientConnection final : public LanguageServers::ClientConnection {
    C_OBJECT(ClientConnection);

private:
    ClientConnection(NonnullRefPtr<Core::LocalSocket> socket)
        : LanguageServers::ClientConnection(move(socket))
    {
        m_autocomplete_engine = make<JSComprehensionEngine>(m_filedb);
        m_autocomplete_engine->set_declarations_of_document_callback = [this](String const& filename, Vector<GUI::AutocompleteProvider::Declaration>&& declarations) {
            async_declarations_in_document(filename, move(declarations));
        };
        m_autocomplete_engine->diagnostics_in_document_callback = [this](String const& filename, Vector<HackStudio::Diagnostic>&& diagnostics) {
            async_diagnostics_in_document(filename, move(diagnostics));
        };
    }

    virtual ~ClientConnection() override = default;
};
}
