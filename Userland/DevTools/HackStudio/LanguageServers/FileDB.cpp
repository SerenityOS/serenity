/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
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

#include "FileDB.h"

#include <AK/LexicalPath.h>
#include <LibCore/File.h>

namespace LanguageServers {

RefPtr<const GUI::TextDocument> FileDB::get(const String& file_name) const
{
    auto absolute_path = to_absolute_path(file_name);
    auto document_optional = m_open_files.get(absolute_path);
    if (!document_optional.has_value())
        return create_from_filesystem(absolute_path);

    return document_optional.value();
}

RefPtr<GUI::TextDocument> FileDB::get(const String& file_name)
{
    auto absolute_path = to_absolute_path(file_name);
    return adopt(*const_cast<GUI::TextDocument*>(reinterpret_cast<const FileDB*>(this)->get(absolute_path).leak_ref()));
}

bool FileDB::is_open(String file_name) const
{
    return m_open_files.contains(to_absolute_path(file_name));
}

bool FileDB::add(const String& file_name, int fd)
{
    auto document = create_from_fd(fd);
    if (!document)
        return false;

    m_open_files.set(to_absolute_path(file_name), document.release_nonnull());
    return true;
}

String FileDB::to_absolute_path(const String& file_name) const
{
    if (LexicalPath { file_name }.is_absolute()) {
        return file_name;
    }
    VERIFY(!m_project_root.is_null());
    return LexicalPath { String::formatted("{}/{}", m_project_root, file_name) }.string();
}

RefPtr<GUI::TextDocument> FileDB::create_from_filesystem(const String& file_name) const
{
    auto file = Core::File::open(to_absolute_path(file_name), Core::IODevice::ReadOnly);
    if (file.is_error()) {
        dbgln("failed to create document for {} from filesystem", file_name);
        return nullptr;
    }
    return create_from_file(*file.value());
}

RefPtr<GUI::TextDocument> FileDB::create_from_fd(int fd) const
{
    auto file = Core::File::construct();
    if (!file->open(fd, Core::IODevice::ReadOnly, Core::File::ShouldCloseFileDescriptor::Yes)) {
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
    virtual void document_did_change() override {};
    virtual void document_did_set_text() override {};
    virtual void document_did_set_cursor(const GUI::TextPosition&) override {};

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

void FileDB::on_file_edit_insert_text(const String& file_name, const String& inserted_text, size_t start_line, size_t start_column)
{
    auto document = get(file_name);
    if (!document) {
        dbgln("file {} has not been opened", file_name);
        return;
    }
    GUI::TextPosition start_position { start_line, start_column };
    document->insert_at(start_position, inserted_text, &s_default_document_client);

    dbgln_if(FILE_CONTENT_DEBUG, "{}", document->text());
}

void FileDB::on_file_edit_remove_text(const String& file_name, size_t start_line, size_t start_column, size_t end_line, size_t end_column)
{
    auto document = get(file_name);
    if (!document) {
        dbgln("file {} has not been opened", file_name);
        return;
    }
    GUI::TextPosition start_position { start_line, start_column };
    GUI::TextRange range {
        GUI::TextPosition { start_line, start_column },
        GUI::TextPosition { end_line, end_column }
    };

    document->remove(range);
    dbgln_if(FILE_CONTENT_DEBUG, "{}", document->text());
}

}
