/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include "Editor.h"
#include <AK/StringBuilder.h>
#include <AK/Utf32View.h>
#include <AK/Utf8View.h>
#include <ctype.h>
#include <stdio.h>
#include <sys/ioctl.h>
#include <sys/select.h>
#include <sys/time.h>
#include <unistd.h>

// #define SUGGESTIONS_DEBUG

namespace Line {

Editor::Editor(Configuration configuration)
    : m_configuration(move(configuration))
{
    m_always_refresh = configuration.refresh_behaviour == Configuration::RefreshBehaviour::Eager;
    m_pending_chars = ByteBuffer::create_uninitialized(0);
    struct winsize ws;
    if (ioctl(STDOUT_FILENO, TIOCGWINSZ, &ws) < 0) {
        m_num_columns = 80;
        m_num_lines = 25;
    } else {
        m_num_columns = ws.ws_col;
        m_num_lines = ws.ws_row;
    }
    m_suggestion_display = make<XtermSuggestionDisplay>(m_num_lines, m_num_columns);
}

Editor::~Editor()
{
    if (m_initialized)
        restore();
}

void Editor::add_to_history(const String& line)
{
    if ((m_history.size() + 1) > m_history_capacity)
        m_history.take_first();
    m_history.append(line);
}

void Editor::clear_line()
{
    for (size_t i = 0; i < m_cursor; ++i)
        fputc(0x8, stdout);
    fputs("\033[K", stdout);
    fflush(stdout);
    m_buffer.clear();
    m_cursor = 0;
    m_inline_search_cursor = m_cursor;
}

void Editor::insert(const Utf32View& string)
{
    for (size_t i = 0; i < string.length(); ++i)
        insert(string.codepoints()[i]);
}

void Editor::insert(const String& string)
{
    for (auto ch : Utf8View { string })
        insert(ch);
}

void Editor::insert(const u32 cp)
{
    StringBuilder builder;
    builder.append(Utf32View(&cp, 1));
    auto str = builder.build();
    m_pending_chars.append(str.characters(), str.length());

    readjust_anchored_styles(m_cursor, ModificationKind::Insertion);

    if (m_cursor == m_buffer.size()) {
        m_buffer.append(cp);
        m_cursor = m_buffer.size();
        m_inline_search_cursor = m_cursor;
        return;
    }

    m_buffer.insert(m_cursor, cp);
    ++m_chars_inserted_in_the_middle;
    ++m_cursor;
    m_inline_search_cursor = m_cursor;
}

void Editor::register_character_input_callback(char ch, Function<bool(Editor&)> callback)
{
    if (m_key_callbacks.contains(ch)) {
        dbg() << "Key callback registered twice for " << ch;
        ASSERT_NOT_REACHED();
    }
    m_key_callbacks.set(ch, make<KeyCallback>(move(callback)));
}

static size_t codepoint_length_in_utf8(u32 codepoint)
{
    if (codepoint <= 0x7f)
        return 1;
    if (codepoint <= 0x07ff)
        return 2;
    if (codepoint <= 0xffff)
        return 3;
    if (codepoint <= 0x10ffff)
        return 4;
    return 3;
}

// buffer [ 0 1 2 3 . . . A . . . B . . . M . . . N ]
//                        ^       ^       ^       ^
//                        |       |       |       +- end of buffer
//                        |       |       +- scan offset = M
//                        |       +- range end = M - B
//                        +- range start = M - A
// This method converts a byte range defined by [start_byte_offset, end_byte_offset] to a codepoint range [M - A, M - B] as shown in the diagram above.
// If `reverse' is true, A and B are before M, if not, A and B are after M.
Editor::CodepointRange Editor::byte_offset_range_to_codepoint_offset_range(size_t start_byte_offset, size_t end_byte_offset, size_t scan_codepoint_offset, bool reverse) const
{
    size_t byte_offset = 0;
    size_t codepoint_offset = scan_codepoint_offset + (reverse ? 1 : 0);
    CodepointRange range;

    for (;;) {
        if (!reverse) {
            if (codepoint_offset >= m_buffer.size())
                break;
        } else {
            if (codepoint_offset == 0)
                break;
        }

        if (byte_offset > end_byte_offset)
            break;

        if (byte_offset < start_byte_offset)
            ++range.start;

        if (byte_offset < end_byte_offset)
            ++range.end;

        byte_offset += codepoint_length_in_utf8(m_buffer[reverse ? --codepoint_offset : codepoint_offset++]);
    }

    return range;
}

void Editor::stylize(const Span& span, const Style& style)
{
    if (style.is_empty())
        return;

    auto start = span.beginning();
    auto end = span.end();

    if (span.mode() == Span::ByteOriented) {
        auto offsets = byte_offset_range_to_codepoint_offset_range(start, end, 0);

        start = offsets.start;
        end = offsets.end;
    }

    auto& spans_starting = style.is_anchored() ? m_anchored_spans_starting : m_spans_starting;
    auto& spans_ending = style.is_anchored() ? m_anchored_spans_ending : m_spans_ending;

    auto starting_map = spans_starting.get(start).value_or({});

    if (!starting_map.contains(end))
        m_refresh_needed = true;

    starting_map.set(end, style);

    spans_starting.set(start, starting_map);

    auto ending_map = spans_ending.get(end).value_or({});

    if (!ending_map.contains(start))
        m_refresh_needed = true;
    ending_map.set(start, style);

    spans_ending.set(end, ending_map);
}

void Editor::suggest(size_t invariant_offset, size_t static_offset, Span::Mode offset_mode) const
{
    auto internal_static_offset = static_offset;
    auto internal_invariant_offset = invariant_offset;
    if (offset_mode == Span::Mode::ByteOriented) {
        // FIXME: We're assuming that invariant_offset points to the end of the available data
        //        this is not necessarily true, but is true in most cases.
        auto offsets = byte_offset_range_to_codepoint_offset_range(internal_static_offset, internal_invariant_offset + internal_static_offset, m_cursor - 1, true);

        internal_static_offset = offsets.start;
        internal_invariant_offset = offsets.end - offsets.start;
    }
    m_suggestion_manager.set_suggestion_variants(internal_static_offset, internal_invariant_offset, 0);
}

String Editor::get_line(const String& prompt)
{
    initialize();
    m_is_editing = true;

    set_prompt(prompt);
    reset();
    set_origin();
    strip_styles(true);

    m_history_cursor = m_history.size();
    for (;;) {
        if (m_always_refresh)
            m_refresh_needed = true;
        refresh_display();
        if (m_finish) {
            m_finish = false;
            printf("\n");
            fflush(stdout);
            auto string = line();
            m_buffer.clear();
            m_is_editing = false;
            restore();
            return string;
        }
        char keybuf[16];
        ssize_t nread = 0;

        if (!m_incomplete_data.size())
            nread = read(0, keybuf, sizeof(keybuf));

        if (nread < 0) {
            if (errno == EINTR) {
                if (!m_was_interrupted) {
                    if (m_was_resized)
                        continue;

                    finish();
                    continue;
                }

                m_was_interrupted = false;

                if (!m_buffer.is_empty())
                    printf("^C");

                m_buffer.clear();
                m_cursor = 0;

                if (on_interrupt_handled)
                    on_interrupt_handled();

                m_refresh_needed = true;
                continue;
            }
            perror("read failed");
            // FIXME: exit()ing here is a bit off. Should communicate failure to caller somehow instead.
            exit(2);
        }

        m_incomplete_data.append(keybuf, nread);
        nread = m_incomplete_data.size();

        // FIXME: exit()ing here is a bit off. Should communicate failure to caller somehow instead.
        if (nread == 0)
            exit(0);

        auto reverse_tab = false;
        auto ctrl_held = false;

        // discard starting bytes until they make sense as utf-8
        size_t valid_bytes = 0;
        while (nread) {
            Utf8View { StringView { m_incomplete_data.data(), (size_t)nread } }.validate(valid_bytes);
            if (valid_bytes)
                break;
            m_incomplete_data.take_first();
            --nread;
        }

        Utf8View input_view { StringView { m_incomplete_data.data(), valid_bytes } };
        size_t consumed_codepoints = 0;

        for (auto codepoint : input_view) {
            if (m_finish)
                break;

            ++consumed_codepoints;

            if (codepoint == 0)
                continue;

            switch (m_state) {
            case InputState::ExpectBracket:
                if (codepoint == '[') {
                    m_state = InputState::ExpectFinal;
                    continue;
                } else {
                    m_state = InputState::Free;
                    break;
                }
            case InputState::ExpectFinal:
                switch (codepoint) {
                case 'O': // mod_ctrl
                    ctrl_held = true;
                    continue;
                case 'A': // up
                {
                    m_searching_backwards = true;
                    auto inline_search_cursor = m_inline_search_cursor;
                    StringBuilder builder;
                    builder.append(Utf32View { m_buffer.data(), inline_search_cursor });
                    String search_phrase = builder.to_string();
                    if (search(search_phrase, true, true)) {
                        ++m_search_offset;
                    } else {
                        insert(search_phrase);
                    }
                    m_inline_search_cursor = inline_search_cursor;
                    m_state = InputState::Free;
                    ctrl_held = false;
                    continue;
                }
                case 'B': // down
                {
                    auto inline_search_cursor = m_inline_search_cursor;
                    StringBuilder builder;
                    builder.append(Utf32View { m_buffer.data(), inline_search_cursor });
                    String search_phrase = builder.to_string();
                    auto search_changed_directions = m_searching_backwards;
                    m_searching_backwards = false;
                    if (m_search_offset > 0) {
                        m_search_offset -= 1 + search_changed_directions;
                        if (!search(search_phrase, true, true)) {
                            insert(search_phrase);
                        }
                    } else {
                        m_search_offset = 0;
                        m_cursor = 0;
                        m_buffer.clear();
                        insert(search_phrase);
                        m_refresh_needed = true;
                    }
                    m_inline_search_cursor = inline_search_cursor;
                    m_state = InputState::Free;
                    ctrl_held = false;
                    continue;
                }
                case 'D': // left
                    if (m_cursor > 0) {
                        if (ctrl_held) {
                            auto skipped_at_least_one_character = false;
                            for (;;) {
                                if (m_cursor == 0)
                                    break;
                                if (skipped_at_least_one_character && isspace(m_buffer[m_cursor - 1])) // stop *after* a space, but only if it changes the position
                                    break;
                                skipped_at_least_one_character = true;
                                --m_cursor;
                            }
                        } else {
                            --m_cursor;
                        }
                    }
                    m_inline_search_cursor = m_cursor;
                    m_state = InputState::Free;
                    ctrl_held = false;
                    continue;
                case 'C': // right
                    if (m_cursor < m_buffer.size()) {
                        if (ctrl_held) {
                            // temporarily put a space at the end of our buffer
                            // this greatly simplifies the logic below
                            m_buffer.append(' ');
                            for (;;) {
                                if (m_cursor >= m_buffer.size())
                                    break;
                                if (isspace(m_buffer[++m_cursor]))
                                    break;
                            }
                            m_buffer.take_last();
                        } else {
                            ++m_cursor;
                        }
                    }
                    m_inline_search_cursor = m_cursor;
                    m_search_offset = 0;
                    m_state = InputState::Free;
                    ctrl_held = false;
                    continue;
                case 'H':
                    m_cursor = 0;
                    m_inline_search_cursor = m_cursor;
                    m_search_offset = 0;
                    m_state = InputState::Free;
                    ctrl_held = false;
                    continue;
                case 'F':
                    m_cursor = m_buffer.size();
                    m_state = InputState::Free;
                    m_inline_search_cursor = m_cursor;
                    m_search_offset = 0;
                    ctrl_held = false;
                    continue;
                case 'Z': // shift+tab
                    reverse_tab = true;
                    m_state = InputState::Free;
                    ctrl_held = false;
                    break;
                case '3':
                    if (m_cursor == m_buffer.size()) {
                        fputc('\a', stdout);
                        fflush(stdout);
                        continue;
                    }
                    remove_at_index(m_cursor);
                    m_refresh_needed = true;
                    m_search_offset = 0;
                    m_state = InputState::ExpectTerminator;
                    ctrl_held = false;
                    continue;
                default:
                    dbgprintf("Shell: Unhandled final: %02x (%c)\r\n", codepoint, codepoint);
                    m_state = InputState::Free;
                    ctrl_held = false;
                    continue;
                }
                break;
            case InputState::ExpectTerminator:
                m_state = InputState::Free;
                continue;
            case InputState::Free:
                if (codepoint == 27) {
                    m_state = InputState::ExpectBracket;
                    continue;
                }
                break;
            }

            auto cb = m_key_callbacks.get(codepoint);
            if (cb.has_value()) {
                if (!cb.value()->callback(*this)) {
                    continue;
                }
            }

            m_search_offset = 0; // reset search offset on any key

            if (codepoint == '\t' || reverse_tab) {
                if (!on_tab_complete)
                    continue;

                // reverse tab can count as regular tab here
                m_times_tab_pressed++;

                int token_start = m_cursor;

                // ask for completions only on the first tab
                // and scan for the largest common prefix to display
                // further tabs simply show the cached completions
                if (m_times_tab_pressed == 1) {
                    m_suggestion_manager.set_suggestions(on_tab_complete(*this));
                    m_prompt_lines_at_suggestion_initiation = num_lines();
                    if (m_suggestion_manager.count() == 0) {
                        // there are no suggestions, beep~
                        putchar('\a');
                        fflush(stdout);
                    }
                }

                // Adjust already incremented / decremented index when switching tab direction
                if (reverse_tab && m_tab_direction != TabDirection::Backward) {
                    m_suggestion_manager.previous();
                    m_suggestion_manager.previous();
                    m_tab_direction = TabDirection::Backward;
                }
                if (!reverse_tab && m_tab_direction != TabDirection::Forward) {
                    m_suggestion_manager.next();
                    m_suggestion_manager.next();
                    m_tab_direction = TabDirection::Forward;
                }
                reverse_tab = false;

                auto completion_mode = m_times_tab_pressed == 1 ? SuggestionManager::CompletePrefix : m_times_tab_pressed == 2 ? SuggestionManager::ShowSuggestions : SuggestionManager::CycleSuggestions;

                auto completion_result = m_suggestion_manager.attempt_completion(completion_mode, token_start);

                auto new_cursor = m_cursor + completion_result.new_cursor_offset;
                for (size_t i = completion_result.offset_region_to_remove.start; i < completion_result.offset_region_to_remove.end; ++i)
                    remove_at_index(new_cursor);

                m_cursor = new_cursor;
                m_inline_search_cursor = new_cursor;
                m_refresh_needed = true;

                for (auto& view : completion_result.insert)
                    insert(view);

                if (completion_result.style_to_apply.has_value()) {
                    // Apply the style of the last suggestion
                    readjust_anchored_styles(m_suggestion_manager.current_suggestion().start_index, ModificationKind::ForcedOverlapRemoval);
                    stylize({ m_suggestion_manager.current_suggestion().start_index, m_cursor, Span::Mode::CodepointOriented }, completion_result.style_to_apply.value());
                }

                switch (completion_result.new_completion_mode) {
                case SuggestionManager::DontComplete:
                    m_times_tab_pressed = 0;
                    break;
                case SuggestionManager::CompletePrefix:
                    break;
                default:
                    ++m_times_tab_pressed;
                    break;
                }

                if (m_times_tab_pressed > 1) {
                    if (m_suggestion_manager.count() > 0) {
                        if (m_suggestion_display->cleanup())
                            reposition_cursor();

                        m_suggestion_display->set_initial_prompt_lines(m_prompt_lines_at_suggestion_initiation);

                        m_suggestion_display->display(m_suggestion_manager);

                        m_origin_x = m_suggestion_display->origin_x();
                    }
                }

                if (m_times_tab_pressed > 2) {
                    if (m_tab_direction == TabDirection::Forward)
                        m_suggestion_manager.next();
                    else
                        m_suggestion_manager.previous();
                }

                if (m_suggestion_manager.count() < 2) {
                    // we have none, or just one suggestion
                    // we should just commit that and continue
                    // after it, as if it were auto-completed
                    suggest(0, 0, Span::CodepointOriented);
                    m_times_tab_pressed = 0;
                    m_suggestion_manager.reset();
                }
                continue;
            }

            if (m_times_tab_pressed) {
                // Apply the style of the last suggestion
                readjust_anchored_styles(m_suggestion_manager.current_suggestion().start_index, ModificationKind::ForcedOverlapRemoval);
                stylize({ m_suggestion_manager.current_suggestion().start_index, m_cursor, Span::Mode::CodepointOriented }, m_suggestion_manager.current_suggestion().style);
                // we probably have some suggestions drawn
                // let's clean them up
                if (m_suggestion_display->cleanup()) {
                    reposition_cursor();
                    m_refresh_needed = true;
                }
                m_suggestion_manager.reset();
                suggest(0, 0, Span::CodepointOriented);
            }
            m_times_tab_pressed = 0; // Safe to say if we get here, the user didn't press TAB

            auto do_backspace = [&] {
                if (m_is_searching) {
                    return;
                }
                if (m_cursor == 0) {
                    fputc('\a', stdout);
                    fflush(stdout);
                    return;
                }
                remove_at_index(m_cursor - 1);
                --m_cursor;
                m_inline_search_cursor = m_cursor;
                // we will have to redraw :(
                m_refresh_needed = true;
            };

            if (codepoint == 8 || codepoint == m_termios.c_cc[VERASE]) {
                do_backspace();
                continue;
            }
            if (codepoint == m_termios.c_cc[VWERASE]) {
                bool has_seen_nonspace = false;
                while (m_cursor > 0) {
                    if (isspace(m_buffer[m_cursor - 1])) {
                        if (has_seen_nonspace)
                            break;
                    } else {
                        has_seen_nonspace = true;
                    }
                    do_backspace();
                }
                continue;
            }
            if (codepoint == m_termios.c_cc[VKILL]) {
                for (size_t i = 0; i < m_cursor; ++i)
                    remove_at_index(0);
                m_cursor = 0;
                m_refresh_needed = true;
                continue;
            }
            // ^L
            if (codepoint == 0xc) {
                printf("\033[3J\033[H\033[2J"); // Clear screen.
                VT::move_absolute(1, 1);
                set_origin(1, 1);
                m_refresh_needed = true;
                continue;
            }
            // ^A
            if (codepoint == 0x01) {
                m_cursor = 0;
                continue;
            }
            // ^R
            if (codepoint == 0x12) {
                if (m_is_searching) {
                    // how did we get here?
                    ASSERT_NOT_REACHED();
                } else {
                    m_is_searching = true;
                    m_search_offset = 0;
                    m_pre_search_buffer.clear();
                    for (auto codepoint : m_buffer)
                        m_pre_search_buffer.append(codepoint);
                    m_pre_search_cursor = m_cursor;
                    m_search_editor = make<Editor>(Configuration { Configuration::Eager, m_configuration.split_mechanism }); // Has anyone seen 'Inception'?
                    m_search_editor->on_display_refresh = [this](Editor& search_editor) {
                        StringBuilder builder;
                        builder.append(Utf32View { search_editor.buffer().data(), search_editor.buffer().size() });
                        search(builder.build());
                        refresh_display();
                        return;
                    };

                    // whenever the search editor gets a ^R, cycle between history entries
                    m_search_editor->register_character_input_callback(0x12, [this](Editor& search_editor) {
                        ++m_search_offset;
                        search_editor.m_refresh_needed = true;
                        return false; // Do not process this key event
                    });

                    // whenever the search editor gets a backspace, cycle back between history entries
                    // unless we're at the zeroth entry, in which case, allow the deletion
                    m_search_editor->register_character_input_callback(m_termios.c_cc[VERASE], [this](Editor& search_editor) {
                        if (m_search_offset > 0) {
                            --m_search_offset;
                            search_editor.m_refresh_needed = true;
                            return false; // Do not process this key event
                        }
                        return true;
                    });

                    // ^L - This is a source of issues, as the search editor refreshes first,
                    // and we end up with the wrong order of prompts, so we will first refresh
                    // ourselves, then refresh the search editor, and then tell him not to process
                    // this event
                    m_search_editor->register_character_input_callback(0x0c, [this](auto& search_editor) {
                        printf("\033[3J\033[H\033[2J"); // Clear screen.

                        // refresh our own prompt
                        set_origin(1, 1);
                        m_refresh_needed = true;
                        refresh_display();

                        // move the search prompt below ours
                        // and tell it to redraw itself
                        search_editor.set_origin(2, 1);
                        search_editor.m_refresh_needed = true;

                        return false;
                    });

                    // quit without clearing the current buffer
                    m_search_editor->register_character_input_callback('\t', [this](Editor& search_editor) {
                        search_editor.finish();
                        m_reset_buffer_on_search_end = false;
                        return false;
                    });

                    printf("\n");
                    fflush(stdout);

                    auto search_prompt = "\x1b[32msearch:\x1b[0m ";
                    auto search_string = m_search_editor->get_line(search_prompt);

                    m_search_editor = nullptr;
                    m_is_searching = false;
                    m_search_offset = 0;

                    // manually cleanup the search line
                    reposition_cursor();
                    auto search_string_codepoint_length = Utf8View { search_string }.length_in_codepoints();
                    VT::clear_lines(0, (search_string_codepoint_length + actual_rendered_string_length(search_prompt) + m_num_columns - 1) / m_num_columns);

                    reposition_cursor();

                    if (!m_reset_buffer_on_search_end || search_string_codepoint_length == 0) {
                        // if the entry was empty, or we purposely quit without a newline,
                        // do not return anything
                        // instead, just end the search
                        end_search();
                        continue;
                    }

                    // return the string
                    finish();
                    continue;
                }
                continue;
            }
            // Normally ^D
            if (codepoint == m_termios.c_cc[VEOF]) {
                if (m_buffer.is_empty()) {
                    printf("<EOF>\n");
                    if (!m_always_refresh) // this is a little off, but it'll do for now
                        exit(0);
                }
                continue;
            }
            // ^E
            if (codepoint == 0x05) {

                m_cursor = m_buffer.size();
                continue;
            }
            if (codepoint == '\n') {
                finish();
                continue;
            }

            insert(codepoint);
        }

        if (consumed_codepoints == m_incomplete_data.size()) {
            m_incomplete_data.clear();
        } else {
            for (size_t i = 0; i < consumed_codepoints; ++i)
                m_incomplete_data.take_first();
        }
    }
}

bool Editor::search(const StringView& phrase, bool allow_empty, bool from_beginning)
{

    int last_matching_offset = -1;

    // do not search for empty strings
    if (allow_empty || phrase.length() > 0) {
        size_t search_offset = m_search_offset;
        for (size_t i = m_history_cursor; i > 0; --i) {
            auto contains = from_beginning ? m_history[i - 1].starts_with(phrase) : m_history[i - 1].contains(phrase);
            if (contains) {
                last_matching_offset = i - 1;
                if (search_offset == 0)
                    break;
                --search_offset;
            }
        }

        if (last_matching_offset == -1) {
            fputc('\a', stdout);
            fflush(stdout);
        }
    }

    m_buffer.clear();
    m_cursor = 0;
    if (last_matching_offset >= 0) {
        insert(m_history[last_matching_offset]);
    }
    // always needed
    m_refresh_needed = true;
    return last_matching_offset >= 0;
}

void Editor::recalculate_origin()
{
    // changing the columns can affect our origin if
    // the new size is smaller than our prompt, which would
    // cause said prompt to take up more space, so we should
    // compensate for that
    if (m_cached_prompt_length >= m_num_columns) {
        auto added_lines = (m_cached_prompt_length + 1) / m_num_columns - 1;
        m_origin_x += added_lines;
    }

    // we also need to recalculate our cursor position
    // but that will be calculated and applied at the next
    // refresh cycle
}
void Editor::cleanup()
{
    VT::move_relative(0, m_pending_chars.size() - m_chars_inserted_in_the_middle);
    auto current_line = cursor_line();

    VT::clear_lines(current_line - 1, num_lines() - current_line);
    VT::move_relative(-num_lines() + 1, -offset_in_line() - m_old_prompt_length - m_pending_chars.size() + m_chars_inserted_in_the_middle);
};

void Editor::refresh_display()
{
    auto has_cleaned_up = false;
    // someone changed the window size, figure it out
    // and react to it, we might need to redraw
    if (m_was_resized) {
        auto previous_num_columns = m_num_columns;

        struct winsize ws;
        if (ioctl(STDOUT_FILENO, TIOCGWINSZ, &ws) < 0) {
            m_num_columns = 80;
            m_num_lines = 25;
        } else {
            m_num_columns = ws.ws_col;
            m_num_lines = ws.ws_row;
        }
        m_suggestion_display->set_vt_size(m_num_lines, m_num_columns);

        if (previous_num_columns != m_num_columns) {
            // we need to cleanup and redo everything
            m_cached_prompt_valid = false;
            m_refresh_needed = true;
            swap(previous_num_columns, m_num_columns);
            recalculate_origin();
            cleanup();
            swap(previous_num_columns, m_num_columns);
            has_cleaned_up = true;
        }
    }
    // do not call hook on pure cursor movement
    if (m_cached_prompt_valid && !m_refresh_needed && m_pending_chars.size() == 0) {
        // probably just moving around
        reposition_cursor();
        m_cached_buffer_size = m_buffer.size();
        return;
    }

    if (on_display_refresh)
        on_display_refresh(*this);

    if (m_cached_prompt_valid) {
        if (!m_refresh_needed && m_cursor == m_buffer.size()) {
            // just write the characters out and continue
            // no need to refresh the entire line
            char null = 0;
            m_pending_chars.append(&null, 1);
            fputs((char*)m_pending_chars.data(), stdout);
            m_pending_chars.clear();
            m_drawn_cursor = m_cursor;
            m_cached_buffer_size = m_buffer.size();
            fflush(stdout);
            return;
        }
    }

    // ouch, reflow entire line
    // FIXME: handle multiline stuff
    if (!has_cleaned_up) {
        cleanup();
    }
    VT::move_absolute(m_origin_x, m_origin_y);

    fputs(m_new_prompt.characters(), stdout);

    VT::clear_to_end_of_line();
    HashMap<u32, Style> empty_styles {};
    StringBuilder builder;
    for (size_t i = 0; i < m_buffer.size(); ++i) {
        auto ends = m_spans_ending.get(i).value_or(empty_styles);
        auto starts = m_spans_starting.get(i).value_or(empty_styles);

        auto anchored_ends = m_anchored_spans_ending.get(i).value_or(empty_styles);
        auto anchored_starts = m_anchored_spans_starting.get(i).value_or(empty_styles);

        if (ends.size() || anchored_ends.size()) {
            Style style;

            for (auto& applicable_style : ends)
                style.unify_with(applicable_style.value);

            for (auto& applicable_style : anchored_ends)
                style.unify_with(applicable_style.value);

            // Disable any style that should be turned off
            VT::apply_style(style, false);

            // go back to defaults
            style = find_applicable_style(i);
            VT::apply_style(style, true);
        }
        if (starts.size() || anchored_starts.size()) {
            Style style;

            for (auto& applicable_style : starts)
                style.unify_with(applicable_style.value);

            for (auto& applicable_style : anchored_starts)
                style.unify_with(applicable_style.value);

            // set new options
            VT::apply_style(style, true);
        }
        builder.clear();
        builder.append(Utf32View { &m_buffer[i], 1 });
        fputs(builder.to_string().characters(), stdout);
    }

    VT::apply_style(Style::reset_style()); // don't bleed to EOL

    m_pending_chars.clear();
    m_refresh_needed = false;
    m_cached_buffer_size = m_buffer.size();
    m_chars_inserted_in_the_middle = 0;
    if (!m_cached_prompt_valid) {
        m_cached_prompt_valid = true;
    }

    reposition_cursor();
    fflush(stdout);
}

void Editor::strip_styles(bool strip_anchored)
{
    m_spans_starting.clear();
    m_spans_ending.clear();

    if (strip_anchored) {
        m_anchored_spans_starting.clear();
        m_anchored_spans_ending.clear();
    }

    m_refresh_needed = true;
}

void Editor::reposition_cursor()
{
    m_drawn_cursor = m_cursor;

    auto line = cursor_line() - 1;
    auto column = offset_in_line();

    VT::move_absolute(line + m_origin_x, column + m_origin_y);
}

void VT::move_absolute(u32 x, u32 y)
{
    printf("\033[%d;%dH", x, y);
    fflush(stdout);
}

void VT::move_relative(int x, int y)
{
    char x_op = 'A', y_op = 'D';

    if (x > 0)
        x_op = 'B';
    else
        x = -x;
    if (y > 0)
        y_op = 'C';
    else
        y = -y;

    if (x > 0)
        printf("\033[%d%c", x, x_op);
    if (y > 0)
        printf("\033[%d%c", y, y_op);
}

Style Editor::find_applicable_style(size_t offset) const
{
    // walk through our styles and merge all that fit in the offset
    Style style;
    auto unify = [&](auto& entry) {
        if (entry.key >= offset)
            return;
        for (auto& style_value : entry.value) {
            if (style_value.key <= offset)
                return;
            style.unify_with(style_value.value);
        }
    };

    for (auto& entry : m_spans_starting) {
        unify(entry);
    }

    for (auto& entry : m_anchored_spans_starting) {
        unify(entry);
    }

    return style;
}

String Style::Background::to_vt_escape() const
{
    if (is_default())
        return "";

    if (m_is_rgb) {
        return String::format("\033[48;2;%d;%d;%dm", m_rgb_color[0], m_rgb_color[1], m_rgb_color[2]);
    } else {
        return String::format("\033[%dm", (u8)m_xterm_color + 40);
    }
}

String Style::Foreground::to_vt_escape() const
{
    if (is_default())
        return "";

    if (m_is_rgb) {
        return String::format("\033[38;2;%d;%d;%dm", m_rgb_color[0], m_rgb_color[1], m_rgb_color[2]);
    } else {
        return String::format("\033[%dm", (u8)m_xterm_color + 30);
    }
}

String Style::Hyperlink::to_vt_escape(bool starting) const
{
    if (is_empty())
        return "";

    return String::format("\033]8;;%s\033\\", starting ? m_link.characters() : "");
}

void Style::unify_with(const Style& other, bool prefer_other)
{
    // unify colors
    if (prefer_other || m_background.is_default())
        m_background = other.background();

    if (prefer_other || m_foreground.is_default())
        m_foreground = other.foreground();

    // unify graphic renditions
    if (other.bold())
        set(Bold);

    if (other.italic())
        set(Italic);

    if (other.underline())
        set(Underline);

    // unify links
    if (prefer_other || m_hyperlink.is_empty())
        m_hyperlink = other.hyperlink();
}

String Style::to_string() const
{
    StringBuilder builder;
    builder.append("Style { ");

    if (!m_foreground.is_default()) {
        builder.append("Foreground(");
        if (m_foreground.m_is_rgb) {
            builder.join(", ", m_foreground.m_rgb_color);
        } else {
            builder.appendf("(XtermColor) %d", m_foreground.m_xterm_color);
        }
        builder.append("), ");
    }

    if (!m_background.is_default()) {
        builder.append("Background(");
        if (m_background.m_is_rgb) {
            builder.join(' ', m_background.m_rgb_color);
        } else {
            builder.appendf("(XtermColor) %d", m_background.m_xterm_color);
        }
        builder.append("), ");
    }

    if (bold())
        builder.append("Bold, ");

    if (underline())
        builder.append("Underline, ");

    if (italic())
        builder.append("Italic, ");

    if (!m_hyperlink.is_empty())
        builder.appendf("Hyperlink(\"%s\"), ", m_hyperlink.m_link.characters());

    builder.append("}");

    return builder.build();
}

void VT::apply_style(const Style& style, bool is_starting)
{
    if (is_starting) {
        printf(
            "\033[%d;%d;%dm%s%s%s",
            style.bold() ? 1 : 22,
            style.underline() ? 4 : 24,
            style.italic() ? 3 : 23,
            style.background().to_vt_escape().characters(),
            style.foreground().to_vt_escape().characters(),
            style.hyperlink().to_vt_escape(true).characters());
    } else {
        printf("%s", style.hyperlink().to_vt_escape(false).characters());
    }
}

void VT::clear_lines(size_t count_above, size_t count_below)
{
    // go down count_below lines
    if (count_below > 0)
        printf("\033[%dB", (int)count_below);
    // then clear lines going upwards
    for (size_t i = count_below + count_above; i > 0; --i)
        fputs(i == 1 ? "\033[2K" : "\033[2K\033[A", stdout);
}

void VT::save_cursor()
{
    fputs("\033[s", stdout);
    fflush(stdout);
}

void VT::restore_cursor()
{
    fputs("\033[u", stdout);
    fflush(stdout);
}

void VT::clear_to_end_of_line()
{
    fputs("\033[K", stdout);
    fflush(stdout);
}

size_t Editor::actual_rendered_string_length(const StringView& string) const
{
    size_t length { 0 };
    enum VTState {
        Free = 1,
        Escape = 3,
        Bracket = 5,
        BracketArgsSemi = 7,
        Title = 9,
    } state { Free };
    Utf8View view { string };
    auto it = view.begin();

    for (size_t i = 0; i < view.length_in_codepoints(); ++i, ++it) {
        auto c = *it;
        switch (state) {
        case Free:
            if (c == '\x1b') {
                // escape
                state = Escape;
                continue;
            }
            if (c == '\r' || c == '\n') {
                // reset length to 0, since we either overwrite, or are on a newline
                length = 0;
                continue;
            }
            // FIXME: This will not support anything sophisticated
            ++length;
            break;
        case Escape:
            if (c == ']') {
                ++i;
                ++it;
                if (*it == '0')
                    state = Title;
                continue;
            }
            if (c == '[') {
                state = Bracket;
                continue;
            }
            // FIXME: This does not support non-VT (aside from set-title) escapes
            break;
        case Bracket:
            if (isdigit(c)) {
                state = BracketArgsSemi;
                continue;
            }
            break;
        case BracketArgsSemi:
            if (c == ';') {
                state = Bracket;
                continue;
            }
            if (!isdigit(c))
                state = Free;
            break;
        case Title:
            if (c == 7)
                state = Free;
            break;
        }
    }
    return length;
}

Vector<size_t, 2> Editor::vt_dsr()
{
    char buf[16];
    u32 length { 0 };

    // read whatever junk there is before talking to the terminal
    // and insert them later when we're reading user input
    bool more_junk_to_read { false };
    timeval timeout { 0, 0 };
    fd_set readfds;
    FD_ZERO(&readfds);
    FD_SET(0, &readfds);

    do {
        more_junk_to_read = false;
        (void)select(1, &readfds, nullptr, nullptr, &timeout);
        if (FD_ISSET(0, &readfds)) {
            auto nread = read(0, buf, 16);
            m_incomplete_data.append(buf, nread);
            more_junk_to_read = true;
        }
    } while (more_junk_to_read);

    fputs("\033[6n", stdout);
    fflush(stdout);

    do {
        auto nread = read(0, buf + length, 16 - length);
        if (nread < 0) {
            if (errno == 0) {
                // ????
                continue;
            }
            dbg() << "Error while reading DSR: " << strerror(errno);
            return { 1, 1 };
        }
        if (nread == 0) {
            dbg() << "Terminal DSR issue; received no response";
            return { 1, 1 };
        }
        length += nread;
    } while (buf[length - 1] != 'R' && length < 16);
    size_t x { 1 }, y { 1 };

    if (buf[0] == '\033' && buf[1] == '[') {
        auto parts = StringView(buf + 2, length - 3).split_view(';');
        bool ok;
        x = parts[0].to_int(ok);
        if (!ok) {
            dbg() << "Terminal DSR issue; received garbage x";
        }
        y = parts[1].to_int(ok);
        if (!ok) {
            dbg() << "Terminal DSR issue; received garbage y";
        }
    }
    return { x, y };
}

String Editor::line(size_t up_to_index) const
{
    StringBuilder builder;
    builder.append(Utf32View { m_buffer.data(), min(m_buffer.size(), up_to_index) });
    return builder.build();
}

bool Editor::should_break_token(Vector<u32, 1024>& buffer, size_t index)
{
    switch (m_configuration.split_mechanism) {
    case Configuration::TokenSplitMechanism::Spaces:
        return buffer[index] == ' ';
    case Configuration::TokenSplitMechanism::UnescapedSpaces:
        return buffer[index] == ' ' && (index == 0 || buffer[index - 1] != '\\');
    }

    ASSERT_NOT_REACHED();
    return true;
};

void Editor::remove_at_index(size_t index)
{
    // see if we have any anchored styles, and reposition them if needed
    readjust_anchored_styles(index, ModificationKind::Removal);
    m_buffer.remove(index);
}

void Editor::readjust_anchored_styles(size_t hint_index, ModificationKind modification)
{
    struct Anchor {
        Span old_span;
        Span new_span;
        Style style;
    };
    Vector<Anchor> anchors_to_relocate;
    auto index_shift = modification == ModificationKind::Insertion ? 1 : -1;
    auto forced_removal = modification == ModificationKind::ForcedOverlapRemoval;

    for (auto& start_entry : m_anchored_spans_starting) {
        for (auto& end_entry : start_entry.value) {
            if (forced_removal) {
                if (start_entry.key <= hint_index && end_entry.key > hint_index) {
                    // remove any overlapping regions
                    continue;
                }
            }
            if (start_entry.key >= hint_index) {
                if (start_entry.key == hint_index && end_entry.key == hint_index + 1 && modification == ModificationKind::Removal) {
                    // remove the anchor, as all its text was wiped
                    continue;
                }
                // shift everything
                anchors_to_relocate.append({ { start_entry.key, end_entry.key, Span::Mode::CodepointOriented }, { start_entry.key + index_shift, end_entry.key + index_shift, Span::Mode::CodepointOriented }, end_entry.value });
                continue;
            }
            if (end_entry.key > hint_index) {
                // shift just the end
                anchors_to_relocate.append({ { start_entry.key, end_entry.key, Span::Mode::CodepointOriented }, { start_entry.key, end_entry.key + index_shift, Span::Mode::CodepointOriented }, end_entry.value });
                continue;
            }
            anchors_to_relocate.append({ { start_entry.key, end_entry.key, Span::Mode::CodepointOriented }, { start_entry.key, end_entry.key, Span::Mode::CodepointOriented }, end_entry.value });
        }
    }

    m_anchored_spans_ending.clear();
    m_anchored_spans_starting.clear();
    // pass over the relocations and update the stale entries
    for (auto& relocation : anchors_to_relocate) {
        stylize(relocation.new_span, relocation.style);
    }
}
}
