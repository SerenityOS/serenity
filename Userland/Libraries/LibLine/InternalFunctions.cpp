/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/FileStream.h>
#include <AK/ScopeGuard.h>
#include <AK/ScopedValueRollback.h>
#include <AK/StringBuilder.h>
#include <AK/TemporaryChange.h>
#include <LibCore/File.h>
#include <LibLine/Editor.h>
#include <stdio.h>
#include <sys/wait.h>
#include <unistd.h>

namespace {
constexpr u32 ctrl(char c) { return c & 0x3f; }
}

namespace Line {

Function<bool(Editor&)> Editor::find_internal_function(StringView name)
{
#define __ENUMERATE(internal_name) \
    if (name == #internal_name)    \
        return EDITOR_INTERNAL_FUNCTION(internal_name);

    ENUMERATE_EDITOR_INTERNAL_FUNCTIONS(__ENUMERATE)

    return {};
}

void Editor::search_forwards()
{
    ScopedValueRollback inline_search_cursor_rollback { m_inline_search_cursor };
    StringBuilder builder;
    builder.append(Utf32View { m_buffer.data(), m_inline_search_cursor });
    String search_phrase = builder.to_string();
    if (m_search_offset_state == SearchOffsetState::Backwards)
        --m_search_offset;
    if (m_search_offset > 0) {
        ScopedValueRollback search_offset_rollback { m_search_offset };
        --m_search_offset;
        if (search(search_phrase, true)) {
            m_search_offset_state = SearchOffsetState::Forwards;
            search_offset_rollback.set_override_rollback_value(m_search_offset);
        } else {
            m_search_offset_state = SearchOffsetState::Unbiased;
        }
    } else {
        m_search_offset_state = SearchOffsetState::Unbiased;
        m_chars_touched_in_the_middle = buffer().size();
        m_cursor = 0;
        m_buffer.clear();
        insert(search_phrase);
        m_refresh_needed = true;
    }
}

void Editor::search_backwards()
{
    ScopedValueRollback inline_search_cursor_rollback { m_inline_search_cursor };
    StringBuilder builder;
    builder.append(Utf32View { m_buffer.data(), m_inline_search_cursor });
    String search_phrase = builder.to_string();
    if (m_search_offset_state == SearchOffsetState::Forwards)
        ++m_search_offset;
    if (search(search_phrase, true)) {
        m_search_offset_state = SearchOffsetState::Backwards;
        ++m_search_offset;
    } else {
        m_search_offset_state = SearchOffsetState::Unbiased;
        --m_search_offset;
    }
}

void Editor::cursor_left_word()
{
    if (m_cursor > 0) {
        auto skipped_at_least_one_character = false;
        for (;;) {
            if (m_cursor == 0)
                break;
            if (skipped_at_least_one_character && !is_ascii_alphanumeric(m_buffer[m_cursor - 1])) // stop *after* a non-alnum, but only if it changes the position
                break;
            skipped_at_least_one_character = true;
            --m_cursor;
        }
    }
    m_inline_search_cursor = m_cursor;
}

void Editor::cursor_left_character()
{
    if (m_cursor > 0)
        --m_cursor;
    m_inline_search_cursor = m_cursor;
}

void Editor::cursor_right_word()
{
    if (m_cursor < m_buffer.size()) {
        // Temporarily put a space at the end of our buffer,
        // doing this greatly simplifies the logic below.
        m_buffer.append(' ');
        for (;;) {
            if (m_cursor >= m_buffer.size())
                break;
            if (!is_ascii_alphanumeric(m_buffer[++m_cursor]))
                break;
        }
        m_buffer.take_last();
    }
    m_inline_search_cursor = m_cursor;
    m_search_offset = 0;
}

void Editor::cursor_right_character()
{
    if (m_cursor < m_buffer.size()) {
        ++m_cursor;
    }
    m_inline_search_cursor = m_cursor;
    m_search_offset = 0;
}

void Editor::erase_character_backwards()
{
    if (m_is_searching) {
        return;
    }
    if (m_cursor == 0) {
        fputc('\a', stderr);
        fflush(stderr);
        return;
    }
    remove_at_index(m_cursor - 1);
    --m_cursor;
    m_inline_search_cursor = m_cursor;
    // We will have to redraw :(
    m_refresh_needed = true;
}

void Editor::erase_character_forwards()
{
    if (m_cursor == m_buffer.size()) {
        fputc('\a', stderr);
        fflush(stderr);
        return;
    }
    remove_at_index(m_cursor);
    m_refresh_needed = true;
}

void Editor::finish_edit()
{
    fprintf(stderr, "<EOF>\n");
    if (!m_always_refresh) {
        m_input_error = Error::Eof;
        finish();
        really_quit_event_loop();
    }
}

void Editor::kill_line()
{
    for (size_t i = 0; i < m_cursor; ++i)
        remove_at_index(0);
    m_cursor = 0;
    m_inline_search_cursor = m_cursor;
    m_refresh_needed = true;
}

void Editor::erase_word_backwards()
{
    // A word here is space-separated. `foo=bar baz` is two words.
    bool has_seen_nonspace = false;
    while (m_cursor > 0) {
        if (is_ascii_space(m_buffer[m_cursor - 1])) {
            if (has_seen_nonspace)
                break;
        } else {
            has_seen_nonspace = true;
        }
        erase_character_backwards();
    }
}

void Editor::erase_to_end()
{
    while (m_cursor < m_buffer.size())
        erase_character_forwards();
}

void Editor::erase_to_beginning()
{
}

void Editor::transpose_characters()
{
    if (m_cursor > 0 && m_buffer.size() >= 2) {
        if (m_cursor < m_buffer.size())
            ++m_cursor;
        swap(m_buffer[m_cursor - 1], m_buffer[m_cursor - 2]);
        // FIXME: Update anchored styles too.
        m_refresh_needed = true;
        m_chars_touched_in_the_middle += 2;
    }
}

void Editor::enter_search()
{
    if (m_is_searching) {
        // How did we get here?
        VERIFY_NOT_REACHED();
    } else {
        m_is_searching = true;
        m_search_offset = 0;
        m_pre_search_buffer.clear();
        for (auto code_point : m_buffer)
            m_pre_search_buffer.append(code_point);
        m_pre_search_cursor = m_cursor;

        ensure_free_lines_from_origin(1 + num_lines());

        // Disable our own notifier so as to avoid interfering with the search editor.
        m_notifier->set_enabled(false);

        m_search_editor = Editor::construct(Configuration { Configuration::Eager, Configuration::NoSignalHandlers }); // Has anyone seen 'Inception'?
        m_search_editor->initialize();
        add_child(*m_search_editor);

        m_search_editor->on_display_refresh = [this](Editor& search_editor) {
            // Remove the search editor prompt before updating ourselves (this avoids artifacts when we move the search editor around).
            search_editor.cleanup();

            StringBuilder builder;
            builder.append(Utf32View { search_editor.buffer().data(), search_editor.buffer().size() });
            if (!search(builder.build(), false, false)) {
                m_chars_touched_in_the_middle = m_buffer.size();
                m_refresh_needed = true;
                m_buffer.clear();
                m_cursor = 0;
            }

            refresh_display();

            // Move the search prompt below ours and tell it to redraw itself.
            auto prompt_end_line = current_prompt_metrics().lines_with_addition(m_cached_buffer_metrics, m_num_columns);
            search_editor.set_origin(prompt_end_line + m_origin_row, 1);
            search_editor.m_refresh_needed = true;
        };

        // Whenever the search editor gets a ^R, cycle between history entries.
        m_search_editor->register_key_input_callback(ctrl('R'), [this](Editor& search_editor) {
            ++m_search_offset;
            search_editor.m_refresh_needed = true;
            return false; // Do not process this key event
        });

        // ^C should cancel the search.
        m_search_editor->register_key_input_callback(ctrl('C'), [this](Editor& search_editor) {
            search_editor.finish();
            m_reset_buffer_on_search_end = true;
            search_editor.end_search();
            search_editor.deferred_invoke([&search_editor] { search_editor.really_quit_event_loop(); });
            return false;
        });

        // Whenever the search editor gets a backspace, cycle back between history entries
        // unless we're at the zeroth entry, in which case, allow the deletion.
        m_search_editor->register_key_input_callback(m_termios.c_cc[VERASE], [this](Editor& search_editor) {
            if (m_search_offset > 0) {
                --m_search_offset;
                search_editor.m_refresh_needed = true;
                return false; // Do not process this key event
            }

            search_editor.erase_character_backwards();
            return false;
        });

        // ^L - This is a source of issues, as the search editor refreshes first,
        // and we end up with the wrong order of prompts, so we will first refresh
        // ourselves, then refresh the search editor, and then tell him not to process
        // this event.
        m_search_editor->register_key_input_callback(ctrl('L'), [this](auto& search_editor) {
            fprintf(stderr, "\033[3J\033[H\033[2J"); // Clear screen.

            // refresh our own prompt
            {
                TemporaryChange refresh_change { m_always_refresh, true };
                set_origin(1, 1);
                m_refresh_needed = true;
                refresh_display();
            }

            // move the search prompt below ours
            // and tell it to redraw itself
            auto prompt_end_line = current_prompt_metrics().lines_with_addition(m_cached_buffer_metrics, m_num_columns);
            search_editor.set_origin(prompt_end_line + 1, 1);
            search_editor.m_refresh_needed = true;

            return false;
        });

        // quit without clearing the current buffer
        m_search_editor->register_key_input_callback('\t', [this](Editor& search_editor) {
            search_editor.finish();
            m_reset_buffer_on_search_end = false;
            return false;
        });

        auto search_prompt = "\x1b[32msearch:\x1b[0m ";

        // While the search editor is active, we do not want editing events.
        m_is_editing = false;

        auto search_string_result = m_search_editor->get_line(search_prompt);

        // Grab where the search origin last was, anything up to this point will be cleared.
        auto search_end_row = m_search_editor->m_origin_row;

        remove_child(*m_search_editor);
        m_search_editor = nullptr;
        m_is_searching = false;
        m_is_editing = true;
        m_search_offset = 0;

        // Re-enable the notifier after discarding the search editor.
        m_notifier->set_enabled(true);

        if (search_string_result.is_error()) {
            // Somethine broke, fail
            m_input_error = search_string_result.error();
            finish();
            return;
        }

        auto& search_string = search_string_result.value();

        // Manually cleanup the search line.
        OutputFileStream stderr_stream { stderr };
        reposition_cursor(stderr_stream);
        auto search_metrics = actual_rendered_string_metrics(search_string);
        auto metrics = actual_rendered_string_metrics(search_prompt);
        VT::clear_lines(0, metrics.lines_with_addition(search_metrics, m_num_columns) + search_end_row - m_origin_row - 1, stderr_stream);

        reposition_cursor(stderr_stream);

        m_refresh_needed = true;
        m_cached_prompt_valid = false;
        m_chars_touched_in_the_middle = 1;

        if (!m_reset_buffer_on_search_end || search_metrics.total_length == 0) {
            // If the entry was empty, or we purposely quit without a newline,
            // do not return anything; instead, just end the search.
            end_search();
            return;
        }

        // Return the string,
        finish();
    }
}

void Editor::transpose_words()
{
    // A word here is contiguous alnums. `foo=bar baz` is three words.

    // 'abcd,.:efg...' should become 'efg...,.:abcd' if caret is after
    // 'efg...'. If it's in 'efg', it should become 'efg,.:abcd...'
    // with the caret after it, which then becomes 'abcd...,.:efg'
    // when alt-t is pressed a second time.

    // Move to end of word under (or after) caret.
    size_t cursor = m_cursor;
    while (cursor < m_buffer.size() && !is_ascii_alphanumeric(m_buffer[cursor]))
        ++cursor;
    while (cursor < m_buffer.size() && is_ascii_alphanumeric(m_buffer[cursor]))
        ++cursor;

    // Move left over second word and the space to its right.
    size_t end = cursor;
    size_t start = cursor;
    while (start > 0 && !is_ascii_alphanumeric(m_buffer[start - 1]))
        --start;
    while (start > 0 && is_ascii_alphanumeric(m_buffer[start - 1]))
        --start;
    size_t start_second_word = start;

    // Move left over space between the two words.
    while (start > 0 && !is_ascii_alphanumeric(m_buffer[start - 1]))
        --start;
    size_t start_gap = start;

    // Move left over first word.
    while (start > 0 && is_ascii_alphanumeric(m_buffer[start - 1]))
        --start;

    if (start != start_gap) {
        // To swap the two words, swap each word (and the gap) individually, and then swap the whole range.
        auto swap_range = [this](auto from, auto to) {
            for (size_t i = 0; i < (to - from) / 2; ++i)
                swap(m_buffer[from + i], m_buffer[to - 1 - i]);
        };
        swap_range(start, start_gap);
        swap_range(start_gap, start_second_word);
        swap_range(start_second_word, end);
        swap_range(start, end);
        m_cursor = cursor;
        // FIXME: Update anchored styles too.
        m_refresh_needed = true;
        m_chars_touched_in_the_middle += end - start;
    }
}

void Editor::go_home()
{
    m_cursor = 0;
    m_inline_search_cursor = m_cursor;
    m_search_offset = 0;
}

void Editor::go_end()
{
    m_cursor = m_buffer.size();
    m_inline_search_cursor = m_cursor;
    m_search_offset = 0;
}

void Editor::clear_screen()
{
    warn("\033[3J\033[H\033[2J");
    OutputFileStream stream { stderr };
    VT::move_absolute(1, 1, stream);
    set_origin(1, 1);
    m_refresh_needed = true;
    m_cached_prompt_valid = false;
}

void Editor::insert_last_words()
{
    if (!m_history.is_empty()) {
        // FIXME: This isn't quite right: if the last arg was `"foo bar"` or `foo\ bar` (but not `foo\\ bar`), we should insert that whole arg as last token.
        if (auto last_words = m_history.last().entry.split_view(' '); !last_words.is_empty())
            insert(last_words.last());
    }
}

void Editor::erase_alnum_word_backwards()
{
    // A word here is contiguous alnums. `foo=bar baz` is three words.
    bool has_seen_alnum = false;
    while (m_cursor > 0) {
        if (!is_ascii_alphanumeric(m_buffer[m_cursor - 1])) {
            if (has_seen_alnum)
                break;
        } else {
            has_seen_alnum = true;
        }
        erase_character_backwards();
    }
}

void Editor::erase_alnum_word_forwards()
{
    // A word here is contiguous alnums. `foo=bar baz` is three words.
    bool has_seen_alnum = false;
    while (m_cursor < m_buffer.size()) {
        if (!is_ascii_alphanumeric(m_buffer[m_cursor])) {
            if (has_seen_alnum)
                break;
        } else {
            has_seen_alnum = true;
        }
        erase_character_forwards();
    }
}

void Editor::case_change_word(Editor::CaseChangeOp change_op)
{
    // A word here is contiguous alnums. `foo=bar baz` is three words.
    while (m_cursor < m_buffer.size() && !is_ascii_alphanumeric(m_buffer[m_cursor]))
        ++m_cursor;
    size_t start = m_cursor;
    while (m_cursor < m_buffer.size() && is_ascii_alphanumeric(m_buffer[m_cursor])) {
        if (change_op == CaseChangeOp::Uppercase || (change_op == CaseChangeOp::Capital && m_cursor == start)) {
            m_buffer[m_cursor] = to_ascii_uppercase(m_buffer[m_cursor]);
        } else {
            VERIFY(change_op == CaseChangeOp::Lowercase || (change_op == CaseChangeOp::Capital && m_cursor > start));
            m_buffer[m_cursor] = to_ascii_lowercase(m_buffer[m_cursor]);
        }
        ++m_cursor;
        m_refresh_needed = true;
    }
}

void Editor::capitalize_word()
{
    case_change_word(CaseChangeOp::Capital);
}

void Editor::lowercase_word()
{
    case_change_word(CaseChangeOp::Lowercase);
}

void Editor::uppercase_word()
{
    case_change_word(CaseChangeOp::Uppercase);
}

void Editor::edit_in_external_editor()
{
    const auto* editor_command = getenv("EDITOR");
    if (!editor_command)
        editor_command = m_configuration.m_default_text_editor.characters();

    char file_path[] = "/tmp/line-XXXXXX";
    auto fd = mkstemp(file_path);

    if (fd < 0) {
        perror("mktemp");
        return;
    }

    {
        auto* fp = fdopen(fd, "rw");
        if (!fp) {
            perror("fdopen");
            return;
        }

        OutputFileStream stream { fp };

        StringBuilder builder;
        builder.append(Utf32View { m_buffer.data(), m_buffer.size() });
        auto bytes = builder.string_view().bytes();
        while (!bytes.is_empty()) {
            auto nwritten = stream.write(bytes);
            bytes = bytes.slice(nwritten);
        }
    }

    ScopeGuard remove_temp_file_guard {
        [fd, file_path] {
            close(fd);
            unlink(file_path);
        }
    };

    Vector<const char*> args { editor_command, file_path, nullptr };
    auto pid = fork();

    if (pid == -1) {
        perror("fork");
        return;
    }

    if (pid == 0) {
        execvp(editor_command, const_cast<char* const*>(args.data()));
        perror("execv");
        _exit(126);
    } else {
        int wstatus = 0;
        do {
            waitpid(pid, &wstatus, 0);
        } while (errno == EINTR);

        if (!(WIFEXITED(wstatus) && WEXITSTATUS(wstatus) == 0))
            return;
    }

    {
        auto file_or_error = Core::File::open(file_path, Core::OpenMode::ReadOnly);
        if (file_or_error.is_error())
            return;

        auto file = file_or_error.release_value();
        auto contents = file->read_all();
        StringView data { contents };
        while (data.ends_with('\n'))
            data = data.substring_view(0, data.length() - 1);

        m_cursor = 0;
        m_chars_touched_in_the_middle = m_buffer.size();
        m_buffer.clear_with_capacity();
        m_refresh_needed = true;

        Utf8View view { data };
        if (view.validate()) {
            for (auto cp : view)
                insert(cp);
        } else {
            for (auto ch : data)
                insert(ch);
        }
    }
}
}
