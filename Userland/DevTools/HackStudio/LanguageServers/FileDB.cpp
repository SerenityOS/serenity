/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FileDB.h"

#include <AK/Debug.h>
#include <AK/LexicalPath.h>
#include <AK/NonnullRefPtr.h>
#include <LibCore/File.h>

namespace LanguageServers {

RefPtr<const GUI::TextDocument> FileDB::get_document(ByteString const& filename) const
{
    auto absolute_path = to_absolute_path(filename);
    auto document_optional = m_open_files.get(absolute_path);
    if (!document_optional.has_value())
        return nullptr;

    return *document_optional.value();
}

RefPtr<GUI::TextDocument> FileDB::get_document(ByteString const& filename)
{
    auto document = reinterpret_cast<FileDB const*>(this)->get_document(filename);
    if (document.is_null())
        return nullptr;
    return adopt_ref(*const_cast<GUI::TextDocument*>(document.leak_ref()));
}

Optional<ByteString> FileDB::get_or_read_from_filesystem(StringView filename) const
{
    auto absolute_path = to_absolute_path(filename);
    auto document = get_document(absolute_path);
    if (document)
        return document->text();

    auto document_or_error = create_from_filesystem(absolute_path);
    if (document_or_error.is_error()) {
        dbgln("Failed to create document '{}': {}", absolute_path, document_or_error.error());
        return {};
    }
    return document_or_error.value()->text();
}

bool FileDB::is_open(ByteString const& filename) const
{
    return m_open_files.contains(to_absolute_path(filename));
}

bool FileDB::add(ByteString const& filename, int fd)
{
    auto document_or_error = create_from_fd(fd);
    if (document_or_error.is_error()) {
        dbgln("Failed to create document: {}", document_or_error.error());
        return false;
    }

    m_open_files.set(to_absolute_path(filename), document_or_error.release_value());
    return true;
}

ByteString FileDB::to_absolute_path(ByteString const& filename) const
{
    if (LexicalPath { filename }.is_absolute()) {
        return filename;
    }
    if (!m_project_root.has_value())
        return filename;
    return LexicalPath { ByteString::formatted("{}/{}", *m_project_root, filename) }.string();
}

ErrorOr<NonnullRefPtr<GUI::TextDocument>> FileDB::create_from_filesystem(ByteString const& filename) const
{
    auto file = TRY(Core::File::open(to_absolute_path(filename), Core::File::OpenMode::Read));
    return create_from_file(move(file));
}

ErrorOr<NonnullRefPtr<GUI::TextDocument>> FileDB::create_from_fd(int fd) const
{
    auto file = TRY(Core::File::adopt_fd(fd, Core::File::OpenMode::Read));
    return create_from_file(move(file));
}

class DefaultDocumentClient final : public GUI::TextDocument::Client {
public:
    virtual ~DefaultDocumentClient() override = default;
    virtual void document_did_append_line() override {};
    virtual void document_did_insert_line(size_t) override {};
    virtual void document_did_remove_line(size_t) override {};
    virtual void document_did_remove_all_lines() override {};
    virtual void document_did_change(GUI::AllowCallback) override {};
    virtual void document_did_set_text(GUI::AllowCallback) override {};
    virtual void document_did_set_cursor(const GUI::TextPosition&) override {};
    virtual void document_did_update_undo_stack() override { }

    virtual bool is_automatic_indentation_enabled() const override { return false; }
    virtual int soft_tab_width() const override { return 4; }
};
static DefaultDocumentClient s_default_document_client;

ErrorOr<NonnullRefPtr<GUI::TextDocument>> FileDB::create_from_file(NonnullOwnPtr<Core::File> file) const
{
    auto content = TRY(file->read_until_eof());
    auto document = GUI::TextDocument::create(&s_default_document_client);
    document->set_text(content);
    return document;
}

void FileDB::on_file_edit_insert_text(ByteString const& filename, ByteString const& inserted_text, size_t start_line, size_t start_column)
{
    VERIFY(is_open(filename));
    auto document = get_document(filename);
    VERIFY(document);
    GUI::TextPosition start_position { start_line, start_column };
    document->insert_at(start_position, inserted_text, &s_default_document_client);

    dbgln_if(FILE_CONTENT_DEBUG, "{}", document->text());
}

void FileDB::on_file_edit_remove_text(ByteString const& filename, size_t start_line, size_t start_column, size_t end_line, size_t end_column)
{
    // TODO: If file is not open - need to get its contents
    // Otherwise- somehow verify that respawned language server is synced with all file contents
    VERIFY(is_open(filename));
    auto document = get_document(filename);
    VERIFY(document);
    GUI::TextPosition start_position { start_line, start_column };
    GUI::TextRange range {
        GUI::TextPosition { start_line, start_column },
        GUI::TextPosition { end_line, end_column }
    };

    document->remove(range);
    dbgln_if(FILE_CONTENT_DEBUG, "{}", document->text());
}

RefPtr<GUI::TextDocument> FileDB::create_with_content(ByteString const& content)
{
    StringView content_view(content);
    auto document = GUI::TextDocument::create(&s_default_document_client);
    document->set_text(content_view);
    return document;
}

bool FileDB::add(ByteString const& filename, ByteString const& content)
{
    auto document = create_with_content(content);
    if (!document) {
        VERIFY_NOT_REACHED();
        return false;
    }

    m_open_files.set(to_absolute_path(filename), document.release_nonnull());
    return true;
}

}
