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

#include "CodeDocument.h"

namespace HackStudio {

NonnullRefPtr<CodeDocument> CodeDocument::create(const String& file_path, Client* client)
{
    return adopt(*new CodeDocument(file_path, client));
}

NonnullRefPtr<CodeDocument> CodeDocument::create(Client* client)
{
    return adopt(*new CodeDocument(client));
}

CodeDocument::CodeDocument(const String& file_path, Client* client)
    : TextDocument(client)
    , m_file_path(file_path)
{
    LexicalPath lexical_path(file_path);

    if (lexical_path.has_extension(".cpp") || lexical_path.has_extension(".h"))
        m_language = Language::Cpp;
    else if (lexical_path.has_extension(".js"))
        m_language = Language::JavaScript;
    else if (lexical_path.has_extension(".ini"))
        m_language = Language::Ini;
    else if (lexical_path.has_extension(".sh"))
        m_language = Language::Shell;
}

CodeDocument::CodeDocument(Client* client)
    : TextDocument(client)
{
}

CodeDocument::~CodeDocument()
{
}

}
