/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
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
    CompletionSuggestion(String const& completion)
        : CompletionSuggestion(completion, "", {})
    {
    }

    CompletionSuggestion(String const& completion, ForSearchTag)
        : text_string(completion)
    {
    }

    CompletionSuggestion(StringView completion, StringView trailing_trivia, StringView display_trivia = "")
        : CompletionSuggestion(completion, trailing_trivia, display_trivia, {})
    {
    }

    CompletionSuggestion(StringView completion, StringView trailing_trivia, StringView display_trivia, Style style);

    bool operator==(CompletionSuggestion const& suggestion) const
    {
        return suggestion.text_string == text_string;
    }

    Vector<u32> text;
    Vector<u32> trailing_trivia;
    Vector<u32> display_trivia;
    Style style;
    size_t start_index { 0 };
    size_t input_offset { 0 };
    size_t static_offset { 0 };
    size_t invariant_offset { 0 };
    bool allow_commit_without_listing { true };

    Utf32View text_view;
    Utf32View trivia_view;
    Utf32View display_trivia_view;
    String text_string;
    String display_trivia_string;
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

    size_t for_each_suggestion(Function<IterationDecision(CompletionSuggestion const&, size_t)>) const;

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

        // This bit of data will be removed, but restored if the suggestion is rejected.
        size_t static_offset_from_cursor { 0 };

        Vector<Utf32View> insert {};

        Optional<Style> style_to_apply {};

        bool avoid_committing_to_single_suggestion { false };
    };

    CompletionAttemptResult attempt_completion(CompletionMode, size_t initiation_start_index);

    void next();
    void previous();

    CompletionSuggestion const& suggest();
    CompletionSuggestion const& current_suggestion() const { return m_last_shown_suggestion; }
    bool is_current_suggestion_complete() const { return m_last_shown_suggestion_was_complete; }

    void reset()
    {
        m_last_shown_suggestion = String::empty();
        m_last_shown_suggestion_display_length = 0;
        m_suggestions.clear();
        m_last_displayed_suggestion_index = 0;
        m_next_suggestion_index = 0;
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
    size_t m_largest_common_suggestion_prefix_length { 0 };
    mutable size_t m_last_displayed_suggestion_index { 0 };
    size_t m_selected_suggestion_index { 0 };
};

}
