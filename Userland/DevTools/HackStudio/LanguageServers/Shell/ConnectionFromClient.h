/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "ShellComprehensionEngine.h"
#include <DevTools/HackStudio/LanguageServers/ConnectionFromClient.h>
#include <LibCpp/Parser.h>

namespace LanguageServers::Shell {

class ConnectionFromClient final : public LanguageServers::ConnectionFromClient {
    C_OBJECT(ConnectionFromClient);

private:
    ConnectionFromClient(NonnullOwnPtr<Core::Stream::LocalSocket> socket)
        : LanguageServers::ConnectionFromClient(move(socket))
    {
        m_autocomplete_engine = make<ShellComprehensionEngine>(m_filedb);
        m_autocomplete_engine->set_declarations_of_document_callback = [this](String const& filename, Vector<GUI::AutocompleteProvider::Declaration>&& declarations) {
            async_declarations_in_document(filename, move(declarations));
        };
        m_autocomplete_engine->set_todo_entries_of_document_callback = [this](String const& filename, Vector<Cpp::Parser::TodoEntry>&& todo_entries) {
            async_todo_entries_in_document(filename, move(todo_entries));
        };
    }
    virtual ~ConnectionFromClient() override = default;
};
}
