/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/HashMap.h>
#include <AK/NonnullRefPtr.h>
#include <LibCodeComprehension/FileDB.h>
#include <LibGUI/TextDocument.h>

namespace LanguageServers {

class FileDB final : public CodeComprehension::FileDB {
public:
    FileDB() = default;
    virtual Optional<ByteString> get_or_read_from_filesystem(StringView filename) const override;

    RefPtr<const GUI::TextDocument> get_document(ByteString const& filename) const;
    RefPtr<GUI::TextDocument> get_document(ByteString const& filename);

    bool add(ByteString const& filename, int fd);
    bool add(ByteString const& filename, ByteString const& content);

    void on_file_edit_insert_text(ByteString const& filename, ByteString const& inserted_text, size_t start_line, size_t start_column);
    void on_file_edit_remove_text(ByteString const& filename, size_t start_line, size_t start_column, size_t end_line, size_t end_column);
    ByteString to_absolute_path(ByteString const& filename) const;
    bool is_open(ByteString const& filename) const;

private:
    ErrorOr<NonnullRefPtr<GUI::TextDocument>> create_from_filesystem(ByteString const& filename) const;
    ErrorOr<NonnullRefPtr<GUI::TextDocument>> create_from_fd(int fd) const;
    ErrorOr<NonnullRefPtr<GUI::TextDocument>> create_from_file(NonnullOwnPtr<Core::File>) const;
    static RefPtr<GUI::TextDocument> create_with_content(ByteString const&);

private:
    HashMap<ByteString, NonnullRefPtr<GUI::TextDocument>> m_open_files;
    Optional<ByteString> m_project_root;
};

}
