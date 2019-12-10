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

    if (m_cursor == (size_t)m_buffer.size()) {
        m_buffer.append(string.characters(), (int)string.length());
        m_cursor = (size_t)m_buffer.size();
        return;
    }

    vt_save_cursor();
    vt_clear_to_end_of_line();
    for (size_t i = m_cursor; i < (size_t)m_buffer.size(); ++i)
        fputc(m_buffer[(int)i], stdout);
    vt_restore_cursor();

    m_buffer.ensure_capacity(m_buffer.size() + (int)string.length());
    for (size_t i = 0; i < string.length(); ++i)
        m_buffer.insert((int)m_cursor + (int)i, string[i]);
    m_cursor += string.length();
}

void LineEditor::insert(const char ch)
{
    putchar(ch);
    fflush(stdout);

    if (m_cursor == (size_t)m_buffer.size()) {
        m_buffer.append(ch);
        m_cursor = (size_t)m_buffer.size();
        return;
    }

    vt_save_cursor();
    vt_clear_to_end_of_line();
    for (size_t i = m_cursor; i < (size_t)m_buffer.size(); ++i)
        fputc(m_buffer[(int)i], stdout);
    vt_restore_cursor();

    m_buffer.insert((int)m_cursor, ch);
    ++m_cursor;
}

void LineEditor::cache_path()
{
    if (!m_path.is_empty())
        m_path.clear_with_capacity();

    String path = getenv("PATH");
    if (path.is_empty())
        return;

    auto directories = path.split(':');
    for (const auto& directory : directories) {
        CDirIterator programs(directory.characters(), CDirIterator::SkipDots);
        while (programs.has_next()) {
            auto program = programs.next_path();
            String program_path = String::format("%s/%s", directory.characters(), program.characters());
            struct stat program_status;
            int stat_error = stat(program_path.characters(), &program_status);
            if (!stat_error && (program_status.st_mode & S_IXUSR))
                m_path.append(program.characters());
        }
    }

    quick_sort(m_path.begin(), m_path.end(), AK::is_less_than<String>);
}

void LineEditor::cut_mismatching_chars(String& completion, const String& program, size_t token_length)
{
    size_t i = token_length;
    while (i < completion.length() && i < program.length() && completion[i] == program[i])
        ++i;
    completion = completion.substring(0, i);
}

void LineEditor::tab_complete_first_token(const String& token)
{
    auto match = binary_search(m_path.data(), m_path.size(), token, [](const String& token, const String& program) -> int {
        return strncmp(token.characters(), program.characters(), token.length());
    });
    if (!match)
        return;

    String completion = *match;

    // Now that we have a program name starting with our token, we look at
    // other program names starting with our token and cut off any mismatching
    // characters.

    bool seen_others = false;
    int index = match - m_path.data();
    for (int i = index - 1; i >= 0 && m_path[i].starts_with(token); --i) {
        cut_mismatching_chars(completion, m_path[i], token.length());
        seen_others = true;
    }
    for (int i = index + 1; i < m_path.size() && m_path[i].starts_with(token); ++i) {
        cut_mismatching_chars(completion, m_path[i], token.length());
        seen_others = true;
    }

    // If we have characters to add, add them.
    if (completion.length() > token.length())
        insert(completion.substring(token.length(), completion.length() - token.length()));
    // If we have a single match, we add a space, unless we already have one.
    if (!seen_others && (m_cursor == (size_t)m_buffer.size() || m_buffer[(int)m_cursor] != ' '))
        insert(' ');
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
            if (m_cursor == (size_t)m_buffer.size()) {
                fputc('\a', stdout);
                fflush(stdout);
                return;
            }
            m_buffer.remove((int)m_cursor - 1);
            fputs("\033[3~", stdout);
            fflush(stdout);
            vt_save_cursor();
            vt_clear_to_end_of_line();
            for (size_t i = m_cursor; i < (size_t)m_buffer.size(); ++i)
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
                    if (m_cursor < (size_t)m_buffer.size()) {
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
                    if (m_cursor < (size_t)m_buffer.size()) {
                        fprintf(stdout, "\033[%zuC", (size_t)m_buffer.size() - m_cursor);
                        fflush(stdout);
                        m_cursor = (size_t)m_buffer.size();
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
                bool is_empty_token = m_cursor == 0 || m_buffer[(int)m_cursor - 1] == ' ';

                int token_start = (int)m_cursor - 1;
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

                String token = is_empty_token ? String() : String(&m_buffer[token_start], m_cursor - (size_t)token_start);

                // FIXME: Implement tab-completion for other tokens (paths).
                if (is_first_token)
                    tab_complete_first_token(token);

                continue;
            }

            auto do_backspace = [&] {
                if (m_cursor == 0) {
                    fputc('\a', stdout);
                    fflush(stdout);
                    return;
                }
                m_buffer.remove((int)m_cursor - 1);
                --m_cursor;
                putchar(8);
                vt_save_cursor();
                vt_clear_to_end_of_line();
                for (size_t i = m_cursor; i < (size_t)m_buffer.size(); ++i)
                    fputc(m_buffer[(int)i], stdout);
                vt_restore_cursor();
            };

            if (ch == 8 || ch == g.termios.c_cc[VERASE]) {
                do_backspace();
                continue;
            }
            if (ch == g.termios.c_cc[VWERASE]) {
                bool has_seen_nonspace = false;
                while (m_cursor > 0) {
                    if (isspace(m_buffer[(int)m_cursor - 1])) {
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
                if (m_cursor < (size_t)m_buffer.size())
                    printf("\033[%zuD", (size_t)m_buffer.size() - m_cursor); // Move cursor N steps left.
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
            if (ch == g.termios.c_cc[VEOF]) { // Normally ^D
                if (m_buffer.is_empty()) {
                    printf("<EOF>\n");
                    exit(0);
                }
                continue;
            }
            if (ch == 0x05) { // ^E
                if (m_cursor < (size_t)m_buffer.size()) {
                    printf("\033[%zuC", (size_t)m_buffer.size() - m_cursor);
                    fflush(stdout);
                    m_cursor = (size_t)m_buffer.size();
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
