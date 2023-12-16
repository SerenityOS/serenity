/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <DevTools/HackStudio/LanguageServers/ConnectionFromClient.h>
#include <LibCodeComprehension/Shell/ShellComprehensionEngine.h>
#include <LibCpp/Parser.h>

namespace LanguageServers::Shell {

class ConnectionFromClient final : public LanguageServers::ConnectionFromClient {
    C_OBJECT(ConnectionFromClient);

private:
    ConnectionFromClient(NonnullOwnPtr<Core::LocalSocket> socket)
        : LanguageServers::ConnectionFromClient(move(socket))
    {
        m_autocomplete_engine = make<CodeComprehension::Shell::ShellComprehensionEngine>(m_filedb);
        m_autocomplete_engine->set_declarations_of_document_callback = [this](ByteString const& filename, Vector<CodeComprehension::Declaration>&& declarations) {
            async_declarations_in_document(filename, move(declarations));
        };
        m_autocomplete_engine->set_todo_entries_of_document_callback = [this](ByteString const& filename, Vector<CodeComprehension::TodoEntry>&& todo_entries) {
            async_todo_entries_in_document(filename, move(todo_entries));
        };
    }
    virtual ~ConnectionFromClient() override = default;
};
}
