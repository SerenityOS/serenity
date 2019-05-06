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

String LineEditor::get_line()
{
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
            } else {
                perror("read failed");
                // FIXME: exit()ing here is a bit off. Should communicate failure to caller somehow instead.
                exit(2);
            }
        }

        for (ssize_t i = 0; i < nread; ++i) {
            char ch = keybuf[i];
            if (ch == 0)
                continue;
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
