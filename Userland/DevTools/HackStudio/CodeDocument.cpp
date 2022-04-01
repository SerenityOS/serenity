/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CodeDocument.h"

namespace HackStudio {

NonnullRefPtr<CodeDocument> CodeDocument::create(String const& file_path, Client* client)
{
    return adopt_ref(*new CodeDocument(file_path, client));
}

NonnullRefPtr<CodeDocument> CodeDocument::create(Client* client)
{
    return adopt_ref(*new CodeDocument(client));
}

CodeDocument::CodeDocument(String const& file_path, Client* client)
    : TextDocument(client)
    , m_file_path(file_path)
{
    auto lexical_path = LexicalPath(file_path);
    m_language = language_from_file(lexical_path);
    m_language_name = language_name_from_file(lexical_path);
}

CodeDocument::CodeDocument(Client* client)
    : TextDocument(client)
{
}

}
