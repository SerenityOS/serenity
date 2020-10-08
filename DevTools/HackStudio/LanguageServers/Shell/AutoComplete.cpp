/*
 * Copyright (c) 2020, the SerenityOS developers.
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

#include "AutoComplete.h"
#include <AK/HashTable.h>
#include <LibLine/SuggestionManager.h>
#include <Shell/AST.h>
#include <Shell/Parser.h>
#include <Shell/Shell.h>

// #define DEBUG_AUTOCOMPLETE

namespace LanguageServers::Shell {

Vector<AutoCompleteResponse> AutoComplete::get_suggestions(const String& code, size_t offset)
{
    // FIXME: No need to reparse this every time!
    auto ast = ::Shell::Parser { code }.parse();
    if (!ast)
        return {};

#ifdef DEBUG_AUTOCOMPLETE
    dbgln("Complete '{}'", code);
    ast->dump(1);
    dbgln("At offset {}", offset);
#endif

    auto result = ast->complete_for_editor(m_shell, offset);
    Vector<AutoCompleteResponse> completions;
    for (auto& entry : result) {
#ifdef DEBUG_AUTOCOMPLETE
        dbgln("Suggestion: '{}' starting at {}", entry.text_string, entry.input_offset);
#endif
        completions.append({ entry.text_string, entry.input_offset });
    }

    return completions;
}

}
