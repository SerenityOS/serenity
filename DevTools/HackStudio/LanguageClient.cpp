/*
 * Copyright (c) 2020, the SerenityOS developers.
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

#include "LanguageClient.h"
#include <AK/String.h>
#include <AK/Vector.h>

namespace HackStudio {

void ServerConnection::handle(const Messages::LanguageClient::AutoCompleteSuggestions& message)
{
    if (m_language_client)
        m_language_client->provide_autocomplete_suggestions(message.suggestions());
}

void LanguageClient::open_file(const String& path)
{
    m_connection.post_message(Messages::LanguageServer::FileOpened(path));
}

void LanguageClient::set_file_content(const String& path, const String& content)
{
    m_connection.post_message(Messages::LanguageServer::SetFileContent(path, content));
}

void LanguageClient::insert_text(const String& path, const String& text, size_t line, size_t column)
{
    m_connection.post_message(Messages::LanguageServer::FileEditInsertText(path, text, line, column));
}

void LanguageClient::remove_text(const String& path, size_t from_line, size_t from_column, size_t to_line, size_t to_column)
{
    m_connection.post_message(Messages::LanguageServer::FileEditRemoveText(path, from_line, from_column, to_line, to_column));
}

void LanguageClient::request_autocomplete(const String& path, size_t cursor_line, size_t cursor_column)
{
    m_connection.post_message(Messages::LanguageServer::AutoCompleteSuggestions(path, cursor_line, cursor_column));
}

void LanguageClient::provide_autocomplete_suggestions(const Vector<AutoCompleteResponse>& suggestions)
{
    if (on_autocomplete_suggestions)
        on_autocomplete_suggestions(suggestions);

    // Otherwise, drop it on the floor :shrug:
}

}
