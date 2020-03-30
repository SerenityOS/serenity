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

#include "LineEditor.h"
#include <ctype.h>
#include <stdio.h>
#include <sys/ioctl.h>
#include <unistd.h>

LineEditor::LineEditor(struct termios termios)
    : m_termios(termios)
    , m_initialized(true)
{
    struct winsize ws;
    if (ioctl(STDOUT_FILENO, TIOCGWINSZ, &ws) < 0)
        m_num_columns = 80;
    else
        m_num_columns = ws.ws_col;
}

LineEditor::LineEditor()
    : LineEditor(termios {})
{
}

LineEditor::~LineEditor()
{
}

void LineEditor::add_to_history(const String& line)
{
    if ((m_history.size() + 1) > m_history_capacity)
        m_history.take_first();
    m_history.append(line);
}

void LineEditor::clear_line()
{
    for (size_t i = 0; i < m_cursor; ++i)
        fputc(0x8, stdout);
    fputs("\033[K", stdout);
    fflush(stdout);
    m_buffer.clear();
    m_cursor = 0;
}

void LineEditor::insert(const String& string)
{
    fputs(string.characters(), stdout);
    fflush(stdout);

    if (m_cursor == m_buffer.size()) {
        m_buffer.append(string.characters(), string.length());
        m_cursor = m_buffer.size();
        return;
    }

    vt_save_cursor();
    vt_clear_to_end_of_line();
    for (size_t i = m_cursor; i < m_buffer.size(); ++i)
        fputc(m_buffer[i], stdout);
    vt_restore_cursor();

    m_buffer.ensure_capacity(m_buffer.size() + string.length());
    for (size_t i = 0; i < string.length(); ++i)
        m_buffer.insert(m_cursor + i, string[i]);
    m_cursor += string.length();
}

void LineEditor::insert(const char ch)
{
    putchar(ch);
    fflush(stdout);

    if (m_cursor == m_buffer.size()) {
        m_buffer.append(ch);
        m_cursor = m_buffer.size();
        return;
    }

    vt_save_cursor();
    vt_clear_to_end_of_line();
    for (size_t i = m_cursor; i < m_buffer.size(); ++i)
        fputc(m_buffer[i], stdout);
    vt_restore_cursor();

    m_buffer.insert(m_cursor, ch);
    ++m_cursor;
}

void LineEditor::on_char_input(char ch, Function<bool(LineEditor&)> callback)
{
    if (m_key_callbacks.contains(ch)) {
        dbg() << "Key callback registered twice for " << ch;
        ASSERT_NOT_REACHED();
    }
    m_key_callbacks.set(ch, make<KeyCallback>(move(callback)));
}

void LineEditor::cut_mismatching_chars(String& completion, const String& other, size_t start_compare)
{
    size_t i = start_compare;
    while (i < completion.length() && i < other.length() && completion[i] == other[i])
        ++i;
    completion = completion.substring(0, i);
}

String LineEditor::get_line(const String& prompt)
{
    fputs(prompt.characters(), stdout);
    fflush(stdout);

    m_history_cursor = m_history.size();
    m_cursor = 0;
    for (;;) {
        char keybuf[16];
        ssize_t nread = read(0, keybuf, sizeof(keybuf));
        // FIXME: exit()ing here is a bit off. Should communicate failure to caller somehow instead.
        if (nread == 0)
            exit(0);
        if (nread < 0) {
            if (errno == EINTR) {
                if (m_was_interrupted) {
                    m_was_interrupted = false;
                    if (!m_buffer.is_empty())
                        printf("^C");
                }
                if (m_was_resized) {
                    m_was_resized = false;
                    printf("\033[2K\r");
                    m_buffer.clear();

                    struct winsize ws;
                    int rc = ioctl(STDOUT_FILENO, TIOCGWINSZ, &ws);
                    ASSERT(rc == 0);
                    m_num_columns = ws.ws_col;

                    return String::empty();
                }
                m_buffer.clear();
                putchar('\n');
                return String::empty();
            }
            perror("read failed");
            // FIXME: exit()ing here is a bit off. Should communicate failure to caller somehow instead.
            exit(2);
        }

        auto do_delete = [&] {
            if (m_cursor == m_buffer.size()) {
                fputc('\a', stdout);
                fflush(stdout);
                return;
            }
            m_buffer.remove(m_cursor);
            fputs("\033[3~", stdout);
            fflush(stdout);
            vt_save_cursor();
            vt_clear_to_end_of_line();
            for (size_t i = m_cursor; i < m_buffer.size(); ++i)
                fputc(m_buffer[i], stdout);
            vt_restore_cursor();
        };

        for (ssize_t i = 0; i < nread; ++i) {
            char ch = keybuf[i];
            if (ch == 0)
                continue;

            switch (m_state) {
            case InputState::ExpectBracket:
                if (ch == '[') {
                    m_state = InputState::ExpectFinal;
                    continue;
                } else {
                    m_state = InputState::Free;
                    break;
                }
            case InputState::ExpectFinal:
                switch (ch) {
                case 'A': // up
                    if (m_history_cursor > 0)
                        --m_history_cursor;
                    clear_line();
                    if (m_history_cursor < m_history.size())
                        insert(m_history[m_history_cursor]);
                    m_state = InputState::Free;
                    continue;
                case 'B': // down
                    if (m_history_cursor < m_history.size())
                        ++m_history_cursor;
                    clear_line();
                    if (m_history_cursor < m_history.size())
                        insert(m_history[m_history_cursor]);
                    m_state = InputState::Free;
                    continue;
                case 'D': // left
                    if (m_cursor > 0) {
                        --m_cursor;
                        fputs("\033[D", stdout);
                        fflush(stdout);
                    }
                    m_state = InputState::Free;
                    continue;
                case 'C': // right
                    if (m_cursor < m_buffer.size()) {
                        ++m_cursor;
                        fputs("\033[C", stdout);
                        fflush(stdout);
                    }
                    m_state = InputState::Free;
                    continue;
                case 'H':
                    if (m_cursor > 0) {
                        fprintf(stdout, "\033[%zuD", m_cursor);
                        fflush(stdout);
                        m_cursor = 0;
                    }
                    m_state = InputState::Free;
                    continue;
                case 'F':
                    if (m_cursor < m_buffer.size()) {
                        fprintf(stdout, "\033[%zuC", m_buffer.size() - m_cursor);
                        fflush(stdout);
                        m_cursor = m_buffer.size();
                    }
                    m_state = InputState::Free;
                    continue;
                case '3':
                    do_delete();
                    m_state = InputState::ExpectTerminator;
                    continue;
                default:
                    dbgprintf("Shell: Unhandled final: %b (%c)\n", ch, ch);
                    m_state = InputState::Free;
                    continue;
                }
                break;
            case InputState::ExpectTerminator:
                m_state = InputState::Free;
                continue;
            case InputState::Free:
                if (ch == 27) {
                    m_state = InputState::ExpectBracket;
                    continue;
                }
                break;
            }

            auto cb = m_key_callbacks.get(ch);
            if (cb.has_value()) {
                if (!cb.value()->callback(*this)) {
                    continue;
                }
            }

            if (ch == '\t') {
                if (!on_tab_complete_first_token || !on_tab_complete_other_token)
                    continue;

                bool is_empty_token = m_cursor == 0 || m_buffer[m_cursor - 1] == ' ';
                m_times_tab_pressed++;

                int token_start = m_cursor - 1;
                if (!is_empty_token) {
                    while (token_start >= 0 && m_buffer[token_start] != ' ')
                        --token_start;
                    ++token_start;
                }

                bool is_first_token = true;
                for (int i = token_start - 1; i >= 0; --i) {
                    if (m_buffer[i] != ' ') {
                        is_first_token = false;
                        break;
                    }
                }

                String token = is_empty_token ? String() : String(&m_buffer[token_start], m_cursor - token_start);
                Vector<String> suggestions;

                if (is_first_token)
                    suggestions = on_tab_complete_first_token(token);
                else
                    suggestions = on_tab_complete_other_token(token);

                if (m_times_tab_pressed > 1 && !suggestions.is_empty()) {
                    size_t longest_suggestion_length = 0;

                    for (auto& suggestion : suggestions)
                        longest_suggestion_length = max(longest_suggestion_length, suggestion.length());

                    size_t num_printed = 0;
                    putchar('\n');
                    for (auto& suggestion : suggestions) {
                        size_t next_column = num_printed + suggestion.length() + longest_suggestion_length + 2;

                        if (next_column > m_num_columns) {
                            putchar('\n');
                            num_printed = 0;
                        }

                        num_printed += fprintf(stderr, "%-*s", static_cast<int>(longest_suggestion_length) + 2, suggestion.characters());
                    }

                    putchar('\n');
                    write(STDOUT_FILENO, prompt.characters(), prompt.length());
                    write(STDOUT_FILENO, m_buffer.data(), m_cursor);
                    // Prevent not printing characters in case the user has moved the cursor and then pressed tab
                    write(STDOUT_FILENO, m_buffer.data() + m_cursor, m_buffer.size() - m_cursor);
                    m_cursor = m_buffer.size(); // bash doesn't do this, but it makes a little bit more sense
                }

                suggestions.clear_with_capacity();
                continue;
            }

            m_times_tab_pressed = 0; // Safe to say if we get here, the user didn't press TAB

            auto do_backspace = [&] {
                if (m_cursor == 0) {
                    fputc('\a', stdout);
                    fflush(stdout);
                    return;
                }
                m_buffer.remove(m_cursor - 1);
                --m_cursor;
                putchar(8);
                vt_save_cursor();
                vt_clear_to_end_of_line();
                for (size_t i = m_cursor; i < m_buffer.size(); ++i)
                    fputc(m_buffer[i], stdout);
                vt_restore_cursor();
            };

            if (ch == 8 || ch == m_termios.c_cc[VERASE]) {
                do_backspace();
                continue;
            }
            if (ch == m_termios.c_cc[VWERASE]) {
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
            if (ch == m_termios.c_cc[VKILL]) {
                while (m_cursor > 0)
                    do_backspace();
                continue;
            }
            if (ch == 0xc) {                    // ^L
                printf("\033[3J\033[H\033[2J"); // Clear screen.
                fputs(prompt.characters(), stdout);
                for (size_t i = 0; i < m_buffer.size(); ++i)
                    fputc(m_buffer[i], stdout);
                if (m_cursor < m_buffer.size())
                    printf("\033[%zuD", m_buffer.size() - m_cursor); // Move cursor N steps left.
                fflush(stdout);
                continue;
            }
            if (ch == 0x01) { // ^A
                if (m_cursor > 0) {
                    printf("\033[%zuD", m_cursor);
                    fflush(stdout);
                    m_cursor = 0;
                }
                continue;
            }
            if (ch == m_termios.c_cc[VEOF]) { // Normally ^D
                if (m_buffer.is_empty()) {
                    printf("<EOF>\n");
                    exit(0);
                }
                continue;
            }
            if (ch == 0x05) { // ^E
                if (m_cursor < m_buffer.size()) {
                    printf("\033[%zuC", m_buffer.size() - m_cursor);
                    fflush(stdout);
                    m_cursor = m_buffer.size();
                }
                continue;
            }
            if (ch == '\n') {
                putchar('\n');
                fflush(stdout);
                auto string = String::copy(m_buffer);
                m_buffer.clear();
                return string;
            }

            insert(ch);
        }
    }
}

void LineEditor::vt_save_cursor()
{
    fputs("\033[s", stdout);
    fflush(stdout);
}

void LineEditor::vt_restore_cursor()
{
    fputs("\033[u", stdout);
    fflush(stdout);
}

void LineEditor::vt_clear_to_end_of_line()
{
    fputs("\033[K", stdout);
    fflush(stdout);
}
