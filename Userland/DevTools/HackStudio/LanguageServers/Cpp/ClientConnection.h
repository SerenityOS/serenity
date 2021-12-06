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

private:
    ClientConnection(NonnullRefPtr<Core::LocalSocket> socket)
        : LanguageServers::ClientConnection(move(socket))
    {
        m_autocomplete_engine = make<CppComprehensionEngine>(m_filedb);
        m_autocomplete_engine->set_declarations_of_document_callback = [this](const String& filename, Vector<GUI::AutocompleteProvider::Declaration>&& declarations) {
            async_declarations_in_document(filename, move(declarations));
        };
        m_autocomplete_engine->set_todo_entries_of_document_callback = [this](String const& filename, Vector<Cpp::Parser::TodoEntry>&& todo_entries) {
            async_todo_entries_in_document(filename, move(todo_entries));
        };
    }

    virtual ~ClientConnection() override = default;
};
}
