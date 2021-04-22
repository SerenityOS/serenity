/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/Vector.h>
#include <DevTools/HackStudio/AutoCompleteResponse.h>
#include <DevTools/HackStudio/LanguageServers/AutoCompleteEngine.h>
#include <LibCpp/Lexer.h>
#include <LibGUI/TextPosition.h>

namespace LanguageServers::Cpp {

using namespace ::Cpp;

class LexerAutoComplete : public AutoCompleteEngine {
public:
    LexerAutoComplete(ClientConnection&, const FileDB& filedb);

    virtual Vector<GUI::AutocompleteProvider::Entry> get_suggestions(const String& file, const GUI::TextPosition& autocomplete_position) override;

private:
    Optional<size_t> token_in_position(const Vector<Cpp::Token>&, const GUI::TextPosition&);
    StringView text_of_token(const Vector<String>& lines, const Cpp::Token&);
    Vector<GUI::AutocompleteProvider::Entry> identifier_prefixes(const Vector<String>& lines, const Vector<Cpp::Token>&, size_t target_token_index);
};

}
