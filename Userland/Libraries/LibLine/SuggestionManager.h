/*
 * Copyright (c) 2020, The SerenityOS developers.
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

#include <AK/Forward.h>
#include <AK/String.h>
#include <AK/Utf32View.h>
#include <AK/Utf8View.h>
#include <LibLine/Style.h>
#include <stdlib.h>

namespace Line {

// FIXME: These objects are pretty heavy since they store two copies of text
//        somehow get rid of one.
struct CompletionSuggestion {
private:
    struct ForSearchTag {
    };

public:
    static constexpr ForSearchTag ForSearch {};

    // Intentionally not explicit. (To allow suggesting bare strings)
    CompletionSuggestion(const String& completion)
        : CompletionSuggestion(completion, "", {})
    {
    }

    CompletionSuggestion(const String& completion, ForSearchTag)
        : text_string(completion)
    {
    }

    CompletionSuggestion(const StringView& completion, const StringView& trailing_trivia)
        : CompletionSuggestion(completion, trailing_trivia, {})
    {
    }

    CompletionSuggestion(const StringView& completion, const StringView& trailing_trivia, Style style);

    bool operator==(const CompletionSuggestion& suggestion) const
    {
        return suggestion.text_string == text_string;
    }

    Vector<u32> text;
    Vector<u32> trailing_trivia;
    Style style;
    size_t start_index { 0 };
    size_t input_offset { 0 };

    Utf32View text_view;
    Utf32View trivia_view;
    String text_string;
    bool is_valid { false };
};

class SuggestionManager {
    friend class Editor;

public:
    void set_suggestions(Vector<CompletionSuggestion>&& suggestions);
    void set_current_suggestion_initiation_index(size_t start_index);

    size_t count() const { return m_suggestions.size(); }
    size_t display_length() const { return m_last_shown_suggestion_display_length; }
    size_t start_index() const { return m_last_displayed_suggestion_index; }
    size_t next_index() const { return m_next_suggestion_index; }
    void set_start_index(size_t index) const { m_last_displayed_suggestion_index = index; }

    size_t for_each_suggestion(Function<IterationDecision(const CompletionSuggestion&, size_t)>) const;

    enum CompletionMode {
        DontComplete,
        CompletePrefix,
        ShowSuggestions,
        CycleSuggestions,
    };

    class CompletionAttemptResult {
    public:
        CompletionMode new_completion_mode;

        ssize_t new_cursor_offset { 0 };

        struct {
            size_t start;
            size_t end;
        } offset_region_to_remove { 0, 0 }; // The region to remove as defined by [start, end) translated by (old_cursor + new_cursor_offset)

        Vector<Utf32View> insert {};

        Optional<Style> style_to_apply {};
    };

    CompletionAttemptResult attempt_completion(CompletionMode, size_t initiation_start_index);

    void next();
    void previous();
    void set_suggestion_variants(size_t static_offset, size_t invariant_offset, size_t suggestion_index) const
    {
        m_next_suggestion_index = suggestion_index;
        m_next_suggestion_static_offset = static_offset;
        m_next_suggestion_invariant_offset = invariant_offset;
    }

    const CompletionSuggestion& suggest();
    const CompletionSuggestion& current_suggestion() const { return m_last_shown_suggestion; }
    bool is_current_suggestion_complete() const { return m_last_shown_suggestion_was_complete; }

    void reset()
    {
        m_last_shown_suggestion = String::empty();
        m_last_shown_suggestion_display_length = 0;
        m_suggestions.clear();
        m_last_displayed_suggestion_index = 0;
    }

private:
    SuggestionManager()
    {
    }

    Vector<CompletionSuggestion> m_suggestions;
    CompletionSuggestion m_last_shown_suggestion { String::empty() };
    size_t m_last_shown_suggestion_display_length { 0 };
    bool m_last_shown_suggestion_was_complete { false };
    mutable size_t m_next_suggestion_index { 0 };
    mutable size_t m_next_suggestion_invariant_offset { 0 };
    mutable size_t m_next_suggestion_static_offset { 0 };
    size_t m_largest_common_suggestion_prefix_length { 0 };
    mutable size_t m_last_displayed_suggestion_index { 0 };
    size_t m_selected_suggestion_index { 0 };
};

}
