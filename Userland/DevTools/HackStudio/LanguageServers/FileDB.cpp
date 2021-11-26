/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FileDB.h"

#include <AK/LexicalPath.h>
#include <AK/NonnullRefPtr.h>
#include <LibCore/File.h>

namespace LanguageServers {

RefPtr<const GUI::TextDocument> FileDB::get(const String& filename) const
{
    auto absolute_path = to_absolute_path(filename);
    auto document_optional = m_open_files.get(absolute_path);
    if (!document_optional.has_value())
        return nullptr;

    return *document_optional.value();
}

RefPtr<GUI::TextDocument> FileDB::get(const String& filename)
{
    auto document = reinterpret_cast<const FileDB*>(this)->get(filename);
    if (document.is_null())
        return nullptr;
    return adopt_ref(*const_cast<GUI::TextDocument*>(document.leak_ref()));
}

RefPtr<const GUI::TextDocument> FileDB::get_or_create_from_filesystem(const String& filename) const
{
    auto absolute_path = to_absolute_path(filename);
    auto document = get(absolute_path);
    if (document)
        return document;
    return create_from_filesystem(absolute_path);
}

RefPtr<GUI::TextDocument> FileDB::get_or_create_from_filesystem(const String& filename)
{
    auto document = reinterpret_cast<const FileDB*>(this)->get_or_create_from_filesystem(filename);
    if (document.is_null())
        return nullptr;
    return adopt_ref(*const_cast<GUI::TextDocument*>(document.leak_ref()));
}

bool FileDB::is_open(const String& filename) const
{
    return m_open_files.contains(to_absolute_path(filename));
}

bool FileDB::add(const String& filename, int fd)
{
    auto document = create_from_fd(fd);
    if (!document)
        return false;

    m_open_files.set(to_absolute_path(filename), document.release_nonnull());
    return true;
}

String FileDB::to_absolute_path(const String& filename) const
{
    if (LexicalPath { filename }.is_absolute()) {
        return filename;
    }
    if (m_project_root.is_null())
        return filename;
    return LexicalPath { String::formatted("{}/{}", m_project_root, filename) }.string();
}

RefPtr<GUI::TextDocument> FileDB::create_from_filesystem(const String& filename) const
{
    auto file = Core::File::open(to_absolute_path(filename), Core::OpenMode::ReadOnly);
    if (file.is_error()) {
        dbgln("failed to create document for {} from filesystem", filename);
        return nullptr;
    }
    return create_from_file(*file.value());
}

RefPtr<GUI::TextDocument> FileDB::create_from_fd(int fd) const
{
    auto file = Core::File::construct();
    if (!file->open(fd, Core::OpenMode::ReadOnly, Core::File::ShouldCloseFileDescriptor::Yes)) {
        errno = file->error();
        perror("open");
        dbgln("Failed to open project file");
        return nullptr;
    }
    return create_from_file(*file);
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

RefPtr<GUI::TextDocument> FileDB::create_from_file(Core::File& file) const
{
    auto content = file.read_all();
    StringView content_view(content);
    auto document = GUI::TextDocument::create(&s_default_document_client);
    document->set_text(content_view);
    return document;
}

void FileDB::on_file_edit_insert_text(const String& filename, const String& inserted_text, size_t start_line, size_t start_column)
{
    VERIFY(is_open(filename));
    auto document = get(filename);
    VERIFY(document);
    GUI::TextPosition start_position { start_line, start_column };
    document->insert_at(start_position, inserted_text, &s_default_document_client);

    dbgln_if(FILE_CONTENT_DEBUG, "{}", document->text());
}

void FileDB::on_file_edit_remove_text(const String& filename, size_t start_line, size_t start_column, size_t end_line, size_t end_column)
{
    // TODO: If file is not open - need to get its contents
    // Otherwise- somehow verify that respawned language server is synced with all file contents
    VERIFY(is_open(filename));
    auto document = get(filename);
    VERIFY(document);
    GUI::TextPosition start_position { start_line, start_column };
    GUI::TextRange range {
        GUI::TextPosition { start_line, start_column },
        GUI::TextPosition { end_line, end_column }
    };

    document->remove(range);
    dbgln_if(FILE_CONTENT_DEBUG, "{}", document->text());
}

RefPtr<GUI::TextDocument> FileDB::create_with_content(const String& content)
{
    StringView content_view(content);
    auto document = GUI::TextDocument::create(&s_default_document_client);
    document->set_text(content_view);
    return document;
}

bool FileDB::add(const String& filename, const String& content)
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
