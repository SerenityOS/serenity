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
}

void LineEditor::append(const String& string)
{
    m_buffer.append(string.characters(), string.length());
    fputs(string.characters(), stdout);
    fflush(stdout);
}

String LineEditor::get_line()
{
    m_history_cursor = m_history.size();
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
                    m_state = InputState::Free;
                    continue;
                case 'C': // right
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
                if (m_buffer.is_empty())
                    continue;
                m_buffer.take_last();
                putchar(8);
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
            if (ch != '\n') {
                m_buffer.append(ch);
            } else {
                auto string = String::copy(m_buffer);
                m_buffer.clear();
                return string;
            }
        }
    }
}
