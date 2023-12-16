/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CodeDocument.h"

namespace HackStudio {

NonnullRefPtr<CodeDocument> CodeDocument::create(ByteString const& file_path, Client* client)
{
    return adopt_ref(*new CodeDocument(file_path, client));
}

NonnullRefPtr<CodeDocument> CodeDocument::create(Client* client)
{
    return adopt_ref(*new CodeDocument(client));
}

CodeDocument::CodeDocument(ByteString const& file_path, Client* client)
    : TextDocument(client)
    , m_file_path(file_path)
{
    auto lexical_path = LexicalPath(file_path);
    m_language = Syntax::language_from_filename(lexical_path);
}

CodeDocument::CodeDocument(Client* client)
    : TextDocument(client)
{
}

CodeDocument::DiffType CodeDocument::line_difference(size_t line) const
{
    return m_line_differences[line];
}

void CodeDocument::set_line_differences(Badge<HackStudio::Editor>, Vector<HackStudio::CodeDocument::DiffType> line_differences)
{
    m_line_differences = move(line_differences);
}

}
