#include "LineEditor.h"
#include "GlobalState.h"
#include <stdio.h>
#include <unistd.h>
#include <ctype.h>

LineEditor::LineEditor()
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
    for (int i = 0; i < m_buffer.size(); ++i)
        fputc(0x8, stdout);
    fflush(stdout);
    m_buffer.clear();
    m_cursor = 0;
}

void LineEditor::append(const String& string)
{
    m_buffer.append(string.characters(), string.length());
    fputs(string.characters(), stdout);
    fflush(stdout);
    m_cursor = m_buffer.size();
}

String LineEditor::get_line()
{
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
                if (g.was_interrupted) {
                    if (!m_buffer.is_empty())
                        printf("^C");
                }
                g.was_interrupted = false;
                m_buffer.clear();
                putchar('\n');
                return String::empty();
            }
            perror("read failed");
            // FIXME: exit()ing here is a bit off. Should communicate failure to caller somehow instead.
            exit(2);
        }

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
                        append(m_history[m_history_cursor]);
                    m_state = InputState::Free;
                    continue;
                case 'B': // down
                    if (m_history_cursor < m_history.size())
                        ++m_history_cursor;
                    clear_line();
                    if (m_history_cursor < m_history.size())
                        append(m_history[m_history_cursor]);
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
                default:
                    dbgprintf("Shell: Unhandled final: %b (%c)\n", ch, ch);
                    m_state = InputState::Free;
                    continue;
                }
                break;
            case InputState::Free:
                if (ch == 27) {
                    m_state = InputState::ExpectBracket;
                    continue;
                }
                break;
            }

            if (ch == '\t') {
                // FIXME: Implement tab-completion.
                continue;
            }

            if (ch == 8 || ch == g.termios.c_cc[VERASE]) {
                if (m_cursor == 0)
                    continue;
                m_buffer.remove(m_cursor - 1);
                --m_cursor;
                putchar(8);
                fputs("\033[s", stdout);
                fputs("\033[K", stdout);
                for (int i = m_cursor; i < m_buffer.size(); ++i)
                    fputc(m_buffer[i], stdout);
                fputs("\033[u", stdout);
                fflush(stdout);
                continue;
            }
            if (ch == g.termios.c_cc[VWERASE]) {
                bool has_seen_nonspace = false;
                while (!m_buffer.is_empty()) {
                    if (isspace(m_buffer.last())) {
                        if (has_seen_nonspace)
                            break;
                    } else {
                        has_seen_nonspace = true;
                    }
                    putchar(0x8);
                    m_buffer.take_last();
                }
                fflush(stdout);
                continue;
            }
            if (ch == g.termios.c_cc[VKILL]) {
                if (m_buffer.is_empty())
                    continue;
                for (int i = 0; i < m_buffer.size(); ++i)
                    putchar(0x8);
                m_buffer.clear();
                fflush(stdout);
                continue;
            }
            putchar(ch);
            fflush(stdout);
            if (ch == '\n') {
                auto string = String::copy(m_buffer);
                m_buffer.clear();
                return string;
            }

            if (m_cursor == m_buffer.size()) {
                m_buffer.append(ch);
                ++m_cursor;
                continue;
            }
            fputs("\033[s", stdout);
            fputs("\033[K", stdout);
            for (int i = m_cursor; i < m_buffer.size(); ++i)
                fputc(m_buffer[i], stdout);
            fputs("\033[u", stdout);
            fflush(stdout);
            m_buffer.insert(m_cursor, move(ch));
            ++m_cursor;

        }
    }
}
