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

#include <AK/Function.h>
#include <LibLine/SuggestionManager.h>

namespace Line {

CompletionSuggestion::CompletionSuggestion(const StringView& completion, const StringView& trailing_trivia, Style style)
    : style(style)
    , text_string(completion)
    , is_valid(true)
{
    Utf8View text_u8 { completion };
    Utf8View trivia_u8 { trailing_trivia };

    for (auto cp : text_u8)
        text.append(cp);

    for (auto cp : trivia_u8)
        this->trailing_trivia.append(cp);

    text_view = Utf32View { text.data(), text.size() };
    trivia_view = Utf32View { this->trailing_trivia.data(), this->trailing_trivia.size() };
}

void SuggestionManager::set_suggestions(Vector<CompletionSuggestion>&& suggestions)
{
    m_suggestions = move(suggestions);

    // make sure we were not given invalid suggestions
    for (auto& suggestion : m_suggestions)
        ASSERT(suggestion.is_valid);

    size_t common_suggestion_prefix { 0 };
    if (m_suggestions.size() == 1) {
        m_largest_common_suggestion_prefix_length = m_suggestions[0].text_view.length();
    } else if (m_suggestions.size()) {
        u32 last_valid_suggestion_code_point;

        for (;; ++common_suggestion_prefix) {
            if (m_suggestions[0].text_view.length() <= common_suggestion_prefix)
                goto no_more_commons;

            last_valid_suggestion_code_point = m_suggestions[0].text_view.code_points()[common_suggestion_prefix];

            for (auto& suggestion : m_suggestions) {
                if (suggestion.text_view.length() <= common_suggestion_prefix || suggestion.text_view.code_points()[common_suggestion_prefix] != last_valid_suggestion_code_point) {
                    goto no_more_commons;
                }
            }
        }
    no_more_commons:;
        m_largest_common_suggestion_prefix_length = common_suggestion_prefix;
    } else {
        m_largest_common_suggestion_prefix_length = 0;
    }
}

void SuggestionManager::next()
{
    if (m_suggestions.size())
        m_next_suggestion_index = (m_next_suggestion_index + 1) % m_suggestions.size();
    else
        m_next_suggestion_index = 0;
}

void SuggestionManager::previous()
{
    if (m_next_suggestion_index == 0)
        m_next_suggestion_index = m_suggestions.size();
    m_next_suggestion_index--;
}

const CompletionSuggestion& SuggestionManager::suggest()
{
    m_last_shown_suggestion = m_suggestions[m_next_suggestion_index];
    m_selected_suggestion_index = m_next_suggestion_index;
    return m_last_shown_suggestion;
}

void SuggestionManager::set_current_suggestion_initiation_index(size_t index)
{

    if (m_last_shown_suggestion_display_length)
        m_last_shown_suggestion.start_index = index - m_next_suggestion_static_offset - m_last_shown_suggestion_display_length;
    else
        m_last_shown_suggestion.start_index = index - m_next_suggestion_static_offset - m_next_suggestion_invariant_offset;

    m_last_shown_suggestion_display_length = m_last_shown_suggestion.text_view.length();
    m_last_shown_suggestion_was_complete = true;
}

SuggestionManager::CompletionAttemptResult SuggestionManager::attempt_completion(CompletionMode mode, size_t initiation_start_index)
{
    CompletionAttemptResult result { mode };

    if (m_next_suggestion_index < m_suggestions.size()) {
        auto can_complete = m_next_suggestion_invariant_offset <= m_largest_common_suggestion_prefix_length;
        if (!m_last_shown_suggestion.text.is_null()) {
            ssize_t actual_offset;
            size_t shown_length = m_last_shown_suggestion_display_length;
            switch (mode) {
            case CompletePrefix:
                actual_offset = 0;
                break;
            case ShowSuggestions:
                actual_offset = 0 - m_largest_common_suggestion_prefix_length + m_next_suggestion_invariant_offset;
                if (can_complete)
                    shown_length = m_largest_common_suggestion_prefix_length + m_last_shown_suggestion.trivia_view.length();
                break;
            default:
                if (m_last_shown_suggestion_display_length == 0)
                    actual_offset = 0;
                else
                    actual_offset = 0 - m_last_shown_suggestion_display_length + m_next_suggestion_invariant_offset;
                break;
            }

            result.offset_region_to_remove = { m_next_suggestion_invariant_offset, shown_length };
            result.new_cursor_offset = actual_offset;
        }

        auto& suggestion = suggest();
        set_current_suggestion_initiation_index(initiation_start_index);

        if (mode == CompletePrefix) {
            // Only auto-complete *if possible*.
            if (can_complete) {
                result.insert.append(suggestion.text_view.substring_view(m_next_suggestion_invariant_offset, m_largest_common_suggestion_prefix_length - m_next_suggestion_invariant_offset));
                m_last_shown_suggestion_display_length = m_largest_common_suggestion_prefix_length;
                // Do not increment the suggestion index, as the first tab should only be a *peek*.
                if (m_suggestions.size() == 1) {
                    // If there's one suggestion, commit and forget.
                    result.new_completion_mode = DontComplete;
                    // Add in the trivia of the last selected suggestion.
                    result.insert.append(suggestion.trivia_view);
                    m_last_shown_suggestion_display_length = 0;
                    result.style_to_apply = suggestion.style;
                    m_last_shown_suggestion_was_complete = true;
                    return result;
                }
            } else {
                m_last_shown_suggestion_display_length = 0;
            }
            result.new_completion_mode = CompletionMode::ShowSuggestions;
            m_last_shown_suggestion_was_complete = false;
            m_last_shown_suggestion = String::empty();
        } else {
            result.insert.append(suggestion.text_view.substring_view(m_next_suggestion_invariant_offset, suggestion.text_view.length() - m_next_suggestion_invariant_offset));
            // Add in the trivia of the last selected suggestion.
            result.insert.append(suggestion.trivia_view);
            m_last_shown_suggestion_display_length += suggestion.trivia_view.length();
        }
    } else {
        m_next_suggestion_index = 0;
    }
    return result;
}

size_t SuggestionManager::for_each_suggestion(Function<IterationDecision(const CompletionSuggestion&, size_t)> callback) const
{
    size_t start_index { 0 };
    for (auto& suggestion : m_suggestions) {
        if (start_index++ < m_last_displayed_suggestion_index)
            continue;
        if (callback(suggestion, start_index - 1) == IterationDecision::Break)
            break;
    }
    return start_index;
}

}
