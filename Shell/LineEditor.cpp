#include "LineEditor.h"
#include "GlobalState.h"
#include <ctype.h>
#include <stdio.h>
#include <unistd.h>

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
    for (int i = 0; i < m_cursor; ++i)
        fputc(0x8, stdout);
    fputs("\033[K", stdout);
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

void LineEditor::tab_complete_first_token()
{
    auto input = String::copy(m_buffer);

    String path = getenv("PATH");
    if (path.is_empty())
        return;
    auto directories = path.split(':');

    String match;

    // Go through the files in PATH.
    for (const auto& directory : directories) {
        CDirIterator programs(directory.characters(), CDirIterator::SkipDots);
        while (programs.has_next()) {
            String program = programs.next_path();
            if (!program.starts_with(input))
                continue;

            // Check that the file is an executable program.
            struct stat program_status;
            StringBuilder program_path;
            program_path.append(directory.characters());
            program_path.append('/');
            program_path.append(program.characters());
            int stat_error = stat(program_path.to_string().characters(), &program_status);
            if (stat_error || !(program_status.st_mode & S_IXUSR))
                continue;

            // Set `match` to the first one that starts with `input`.
            if (match.is_empty()) {
                match = program;
            } else {
                // Remove characters from the end of `match` if they're
                // different from another `program` starting with `input`.
                int i = input.length();
                while (i < match.length() && i < program.length() && match[i] == program[i])
                    ++i;
                match = match.substring(0, i);
            }

            if (match.length() == input.length())
                return;
        }
    }

    if (match.is_empty())
        return;

    // Then append `match` to the buffer, excluding the `input` part which is
    // already in the buffer.
    append(match.substring(input.length(), match.length() - input.length()).characters());
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
                if (g.was_interrupted) {
                    g.was_interrupted = false;
                    if (!m_buffer.is_empty())
                        printf("^C");
                }
                if (g.was_resized) {
                    g.was_resized = false;
                    printf("\033[2K\r");
                    m_buffer.clear();
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
            m_buffer.remove(m_cursor - 1);
            fputs("\033[3~", stdout);
            fflush(stdout);
            vt_save_cursor();
            vt_clear_to_end_of_line();
            for (int i = m_cursor; i < m_buffer.size(); ++i)
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
                case 'H':
                    if (m_cursor > 0) {
                        fprintf(stdout, "\033[%dD", m_cursor);
                        fflush(stdout);
                        m_cursor = 0;
                    }
                    m_state = InputState::Free;
                    continue;
                case 'F':
                    if (m_cursor < m_buffer.size()) {
                        fprintf(stdout, "\033[%dC", m_buffer.size() - m_cursor);
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

            if (ch == '\t') {
                if (m_buffer.is_empty())
                    continue;

                bool is_first_token = true;
                for (const auto& character : m_buffer) {
                    if (isspace(character)) {
                        is_first_token = false;
                        break;
                    }
                }

                // FIXME: Implement tab-completion for other tokens (paths).
                if (is_first_token)
                    tab_complete_first_token();

                continue;
            }

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
                for (int i = m_cursor; i < m_buffer.size(); ++i)
                    fputc(m_buffer[i], stdout);
                vt_restore_cursor();
            };

            if (ch == 8 || ch == g.termios.c_cc[VERASE]) {
                do_backspace();
                continue;
            }
            if (ch == g.termios.c_cc[VWERASE]) {
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
            if (ch == g.termios.c_cc[VKILL]) {
                while (m_cursor > 0)
                    do_backspace();
                continue;
            }
            if (ch == 0xc) { // ^L
                printf("\033[3J\033[H\033[2J"); // Clear screen.
                fputs(prompt.characters(), stdout);
                for (int i = 0; i < m_buffer.size(); ++i)
                    fputc(m_buffer[i], stdout);
                if (m_cursor < m_buffer.size())
                    printf("\033[%dD", m_buffer.size() - m_cursor); // Move cursor N steps left.
                fflush(stdout);
                continue;
            }
            if (ch == 0x01) { // ^A
                if (m_cursor > 0) {
                    printf("\033[%dD", m_cursor);
                    fflush(stdout);
                    m_cursor = 0;
                }
                continue;
            }
            if (ch == 0x05) { // ^E
                if (m_cursor < m_buffer.size()) {
                    printf("\033[%dC", m_buffer.size() - m_cursor);
                    fflush(stdout);
                    m_cursor = m_buffer.size();
                }
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
            vt_save_cursor();
            vt_clear_to_end_of_line();
            for (int i = m_cursor; i < m_buffer.size(); ++i)
                fputc(m_buffer[i], stdout);
            vt_restore_cursor();
            m_buffer.insert(m_cursor, move(ch));
            ++m_cursor;
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
