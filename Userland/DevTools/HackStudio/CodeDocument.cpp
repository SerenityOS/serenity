/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CodeDocument.h"

namespace HackStudio {

NonnullRefPtr<CodeDocument> CodeDocument::create(const String& file_path, Client* client)
{
    return adopt_ref(*new CodeDocument(file_path, client));
}

NonnullRefPtr<CodeDocument> CodeDocument::create(Client* client)
{
    return adopt_ref(*new CodeDocument(client));
}

CodeDocument::CodeDocument(const String& file_path, Client* client)
    : TextDocument(client)
    , m_file_path(file_path)
{
    // Check base name before extension to catch files like CMakeLists.txt
    m_language = language_from_file_name(LexicalPath::basename(file_path));
    if (m_language != Language::Unknown) {
        m_language_name = language_name_from_file_name(LexicalPath::basename(file_path));
    } else {
        m_language = language_from_file_extension(LexicalPath::extension(file_path));
        m_language_name = language_name_from_file_extension(LexicalPath::extension(file_path));
    }
}

CodeDocument::CodeDocument(Client* client)
    : TextDocument(client)
{
}

CodeDocument::~CodeDocument()
{
}

}
