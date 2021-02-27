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

#pragma once

#include "FileDB.h"
#include <DevTools/HackStudio/AutoCompleteResponse.h>
#include <LibGUI/AutocompleteProvider.h>
#include <LibGUI/TextPosition.h>

namespace LanguageServers::Cpp {

class ClientConnection;

class AutoCompleteEngine {
public:
    AutoCompleteEngine(ClientConnection&, const FileDB& filedb);
    virtual ~AutoCompleteEngine();

    virtual Vector<GUI::AutocompleteProvider::Entry> get_suggestions(const String& file, const GUI::TextPosition& autocomplete_position) = 0;

    // TODO: In the future we can pass the range that was edited and only re-parse what we have to.
    virtual void on_edit([[maybe_unused]] const String& file) {};
    virtual void file_opened([[maybe_unused]] const String& file) {};

    virtual Optional<GUI::AutocompleteProvider::ProjectLocation> find_declaration_of(const String&, const GUI::TextPosition&) { return {}; };

public:
    Function<void(ClientConnection&, String, Vector<GUI::AutocompleteProvider::Declaration>)> set_declarations_of_document_callback;

protected:
    const FileDB& filedb() const { return m_filedb; }
    void set_declarations_of_document(const String&, Vector<GUI::AutocompleteProvider::Declaration>&&);

private:
    ClientConnection& m_connection;
    const FileDB& m_filedb;
};
}
