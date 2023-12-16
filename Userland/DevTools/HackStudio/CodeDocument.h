/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/LexicalPath.h>
#include <LibGUI/TextDocument.h>
#include <LibSyntax/Language.h>

namespace HackStudio {

class Editor;

class CodeDocument final : public GUI::TextDocument {
public:
    virtual ~CodeDocument() override = default;
    static NonnullRefPtr<CodeDocument> create(ByteString const& file_path, Client* client = nullptr);
    static NonnullRefPtr<CodeDocument> create(Client* client = nullptr);

    Vector<size_t> const& breakpoint_lines() const { return m_breakpoint_lines; }
    Vector<size_t>& breakpoint_lines() { return m_breakpoint_lines; }
    Optional<size_t> execution_position() const { return m_execution_position; }
    void set_execution_position(size_t line) { m_execution_position = line; }
    void clear_execution_position() { m_execution_position.clear(); }
    ByteString const& file_path() const { return m_file_path; }
    Optional<Syntax::Language> const& language() const { return m_language; }

    enum class DiffType {
        None,
        AddedLine,
        ModifiedLine,
        DeletedLinesBefore,
    };
    DiffType line_difference(size_t line) const;
    void set_line_differences(Badge<Editor>, Vector<DiffType>);

private:
    explicit CodeDocument(ByteString const& file_path, Client* client = nullptr);
    explicit CodeDocument(Client* client = nullptr);

    ByteString m_file_path;
    Optional<Syntax::Language> m_language;
    Vector<size_t> m_breakpoint_lines;
    Optional<size_t> m_execution_position;

    Vector<DiffType> m_line_differences;
};

}
