/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
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

#include "ClientConnection.h"
#include "AutoComplete.h"
#include <AK/HashMap.h>
#include <LibCore/File.h>
#include <LibGUI/TextDocument.h>

// #define DEBUG_CPP_LANGUAGE_SERVER
// #define DEBUG_FILE_CONTENT

namespace LanguageServers::Cpp {

static HashMap<int, RefPtr<ClientConnection>> s_connections;

ClientConnection::ClientConnection(NonnullRefPtr<Core::LocalSocket> socket, int client_id)
    : IPC::ClientConnection<LanguageClientEndpoint, LanguageServerEndpoint>(*this, move(socket), client_id)
{
    s_connections.set(client_id, *this);
}

ClientConnection::~ClientConnection()
{
}

void ClientConnection::die()
{
    s_connections.remove(client_id());
    exit(0);
}

OwnPtr<Messages::LanguageServer::GreetResponse> ClientConnection::handle(const Messages::LanguageServer::Greet& message)
{
    m_project_root = LexicalPath(message.project_root());
#ifdef DEBUG_CPP_LANGUAGE_SERVER
    dbgln("project_root: {}", m_project_root);
#endif
    return make<Messages::LanguageServer::GreetResponse>(client_id());
}

class DefaultDocumentClient final : public GUI::TextDocument::Client {
public:
    virtual ~DefaultDocumentClient() override = default;
    virtual void document_did_append_line() override {};
    virtual void document_did_insert_line(size_t) override {};
    virtual void document_did_remove_line(size_t) override {};
    virtual void document_did_remove_all_lines() override {};
    virtual void document_did_change() override {};
    virtual void document_did_set_text() override {};
    virtual void document_did_set_cursor(const GUI::TextPosition&) override {};

    virtual bool is_automatic_indentation_enabled() const override { return true; }
    virtual int soft_tab_width() const override { return 4; }
};

static DefaultDocumentClient s_default_document_client;

void ClientConnection::handle(const Messages::LanguageServer::FileOpened& message)
{
    LexicalPath file_path(String::formatted("{}/{}", m_project_root, message.file_name()));
#ifdef DEBUG_CPP_LANGUAGE_SERVER
    dbgln("FileOpened: {}", file_path);
#endif

    auto file = Core::File::construct(file_path.string());
    if (!file->open(Core::IODevice::ReadOnly)) {
        errno = file->error();
        perror("open");
        dbgln("Failed to open project file: {}", file_path);
        return;
    }
    auto content = file->read_all();
    StringView content_view(content);
    auto document = GUI::TextDocument::create(&s_default_document_client);
    document->set_text(content_view);
    m_open_files.set(message.file_name(), document);
#ifdef DEBUG_FILE_CONTENT
    dbg() << document->text();
#endif
}

void ClientConnection::handle(const Messages::LanguageServer::FileEditInsertText& message)
{
#ifdef DEBUG_CPP_LANGUAGE_SERVER
    dbgln("InsertText for file: {}", message.file_name());
    dbgln("Text: {}", message.text());
    dbgln("[{}:{}]", message.start_line(), message.start_column());
#endif
    auto document = document_for(message.file_name());
    if (!document) {
        dbgln("file {} has not been opened", message.file_name());
        return;
    }
    GUI::TextPosition start_position { (size_t)message.start_line(), (size_t)message.start_column() };
    document->insert_at(start_position, message.text(), &s_default_document_client);
#ifdef DEBUG_FILE_CONTENT
    dbgln("{}", document->text());
#endif
}

void ClientConnection::handle(const Messages::LanguageServer::FileEditRemoveText& message)
{
#ifdef DEBUG_CPP_LANGUAGE_SERVER
    dbgln("RemoveText for file: {}", message.file_name());
    dbgln("[{}:{} - {}:{}]", message.start_line(), message.start_column(), message.end_line(), message.end_column());
#endif
    auto document = document_for(message.file_name());
    if (!document) {
        dbgln("file {} has not been opened", message.file_name());
        return;
    }
    GUI::TextPosition start_position { (size_t)message.start_line(), (size_t)message.start_column() };
    GUI::TextRange range {
        GUI::TextPosition { (size_t)message.start_line(),
            (size_t)message.start_column() },
        GUI::TextPosition { (size_t)message.end_line(),
            (size_t)message.end_column() }
    };

    document->remove(range);
#ifdef DEBUG_FILE_CONTENT
    dbgln("{}", document->text());
#endif
}

void ClientConnection::handle(const Messages::LanguageServer::AutoCompleteSuggestions& message)
{
#ifdef DEBUG_CPP_LANGUAGE_SERVER
    dbgln("AutoCompleteSuggestions for: {} {}:{}", message.file_name(), message.cursor_line(), message.cursor_column());
#endif

    auto document = document_for(message.file_name());
    if (!document) {
        dbgln("file {} has not been opened", message.file_name());
        return;
    }

    auto suggestions = AutoComplete::get_suggestions(document->text(), { (size_t)message.cursor_line(), (size_t)max(message.cursor_column(), message.cursor_column() - 1) });
    post_message(Messages::LanguageClient::AutoCompleteSuggestions(move(suggestions)));
}

RefPtr<GUI::TextDocument> ClientConnection::document_for(const String& file_name)
{
    auto document_optional = m_open_files.get(file_name);
    if (!document_optional.has_value())
        return nullptr;

    return document_optional.value();
}

void ClientConnection::handle(const Messages::LanguageServer::SetFileContent& message)
{
    auto document = document_for(message.file_name());
    if (!document) {
        dbgln("file {} has not been opened", message.file_name());
        return;
    }
    auto content = message.content();
    document->set_text(content.view());
}

}
