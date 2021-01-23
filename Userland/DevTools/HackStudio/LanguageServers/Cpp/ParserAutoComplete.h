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

#include "LibCpp/AST.h"
#include <AK/String.h>
#include <AK/Vector.h>
#include <DevTools/HackStudio/AutoCompleteResponse.h>
#include <LibCpp/Parser.h>
#include <LibGUI/TextPosition.h>

namespace LanguageServers::Cpp {

using namespace ::Cpp;

class ParserAutoComplete {
public:
    ParserAutoComplete(const String& code);

    Vector<GUI::AutocompleteProvider::Entry> get_suggestions(const GUI::TextPosition& autocomplete_position) const;

private:
    Vector<GUI::AutocompleteProvider::Entry> autocomplete_property(const MemberExpression&, const StringView partial_text) const;
    Vector<GUI::AutocompleteProvider::Entry> autocomplete_identifier(const ASTNode&) const;
    String type_of(const Expression&) const;
    String type_of_property(const Identifier&) const;
    String type_of_variable(const Identifier&) const;
    bool is_property(const ASTNode&) const;
    bool is_empty_property(const ASTNode&, const Position& autocomplete_position) const;

    struct PropertyInfo {
        StringView name;
        RefPtr<Type> type;
    };
    Vector<PropertyInfo> properties_of_type(const String& type) const;

private:
    String m_code;
    Cpp::Parser m_parser;
};

}
