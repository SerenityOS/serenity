/*
 * Copyright (c) 2021, Peter Elliott <pelliott@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/LexicalPath.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/Utf8View.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <ctype.h>
#include <fcntl.h>
#include <stdio.h>
#include <sys/ioctl.h>
#include <termios.h>
#include <unistd.h>

static struct termios g_save;
static struct winsize g_wsize;

static void setup_tty(bool switch_buffer)
{
    // Save previous tty settings.
    if (tcgetattr(STDOUT_FILENO, &g_save) == -1) {
        perror("tcgetattr(3)");
    }

    // Get the window size.
    if (ioctl(STDOUT_FILENO, TIOCGWINSZ, &g_wsize) == -1) {
        perror("ioctl(2)");
    }

    struct termios raw = g_save;
    raw.c_lflag &= ~(ECHO | ICANON);

    // Disable echo and line buffering
    if (tcsetattr(STDOUT_FILENO, TCSAFLUSH, &raw) == -1) {
        perror("tcsetattr(3)");
    }

    if (switch_buffer) {
        // Save cursor and switch to alternate buffer.
        out("\e[s\e[?1047h");
    }
}

static void teardown_tty(bool switch_buffer)
{
    if (tcsetattr(STDOUT_FILENO, TCSAFLUSH, &g_save) == -1) {
        perror("tcsetattr(3)");
    }

    if (switch_buffer) {
        out("\e[?1047l\e[u");
    }
}

static Vector<String> wrap_line(Utf8View const& string, size_t width)
{
    Vector<String> lines;

    StringBuilder builder;
    size_t offset = 0;

    bool in_ansi = false;
    for (auto codepoint : string) {
        if (offset >= width) {
            builder.append('\n');
            lines.append(builder.build());
            builder.clear();
            offset = 0;
        }

        builder.append(codepoint);

        if (codepoint == '\e')
            in_ansi = true;

        if (!in_ansi)
            // FIXME: calcuate the printed width of the character.
            offset++;

        if (isalpha(codepoint))
            in_ansi = false;
    }

    if (builder.length() > 0)
        lines.append(builder.build());

    return lines;
}

class Pager {
public:
    Pager(FILE* file, FILE* tty, size_t width, size_t height)
        : m_file(file)
        , m_tty(tty)
        , m_width(width)
        , m_height(height)
    {
    }

    void up()
    {
        up_n(1);
    }

    void down()
    {
        down_n(1);
    }

    void up_page()
    {
        up_n(m_height - 1);
    }

    void down_page()
    {
        down_n(m_height - 1);
    }

    void up_n(size_t n)
    {
        if (m_line == 0)
            return;

        m_line = (m_line > n) ? m_line - n : 0;

        // Clear screen and reset cursor position.
        out("\e[2J\e[0G\e[0d");
        write_range(m_line, m_height - 1);
        status_line();
        fflush(m_tty);
    }

    void down_n(size_t n)
    {
        clear_status();

        while (n - (m_lines.size() - m_line) + m_height - 1 > 0) {
            if (!read_line())
                break;
        }
        m_line += write_range(min(m_line + m_height - 1, m_line + (m_lines.size() - m_line)), n);
        status_line();

        fflush(m_tty);
    }

    void top()
    {
        up_n(m_line);
    }

    void bottom()
    {
        while (read_line())
            ;
        down_n(m_lines.size() - m_line);
    }

    void up_half_page()
    {
        up_n(m_height / 2);
    }

    void down_half_page()
    {
        down_n(m_height / 2);
    }

    void go_to_line(size_t line_num)
    {
        if (line_num < m_line) {
            up_n(m_line - line_num);
        } else {
            down_n(line_num - m_line);
        }
    }

    void init()
    {
        while (m_lines.size() < m_height) {
            if (!read_line())
                break;
        }
        write_range(0, m_height - 1);
        status_line();
        m_line = 0;
        fflush(m_tty);
    }

    size_t write_range(size_t start, size_t length)
    {
        size_t lines = min(length, m_lines.size() - start);
        for (size_t i = 0; i < lines; ++i) {
            out(m_tty, "{}", m_lines[start + i]);
        }
        return lines;
    }

    void clear_status()
    {
        out(m_tty, "\e[2K\r");
    }

    void status_line()
    {
        out(m_tty, "\e[7m ");
        render_status_line(m_prompt);
        out(m_tty, " \e[27m");
    }

    void set_filename(StringView const& filename)
    {
        m_filename = filename;
    }

    void set_prompt(StringView const& prompt)
    {
        m_prompt = prompt;
    }

    bool read_line()
    {
        char* line = nullptr;
        size_t n = 0;
        ssize_t size = getline(&line, &n, m_file);

        if (size == -1)
            return false;

        m_lines.extend(wrap_line(Utf8View { StringView { line } }, m_width));
        free(line);
        return true;
    }

    bool at_end()
    {
        return (m_line + m_height - 1) >= m_lines.size() && feof(m_file);
    }

private:
    size_t render_status_line(StringView const& prompt, size_t off = 0, char end = '\0', bool ignored = false)
    {
        for (; prompt[off] != end && off < prompt.length(); ++off) {
            if (ignored)
                continue;

            if (off + 1 >= prompt.length()) {
                // Don't parse any multi-character sequences if we are at the end of input.
                out(m_tty, "{}", prompt[off]);
                continue;
            }

            switch (prompt[off]) {
            case '?':
                switch (prompt[++off]) {
                case 'f':
                    off = render_status_line(prompt, off + 1, ':', m_file == stdin);
                    off = render_status_line(prompt, off + 1, '.', m_file != stdin);
                    break;
                case 'e':
                    off = render_status_line(prompt, off + 1, ':', !at_end());
                    off = render_status_line(prompt, off + 1, '.', at_end());
                    break;
                default:
                    // Unknown flags are never true.
                    off = render_status_line(prompt, off + 1, ':', true);
                    off = render_status_line(prompt, off + 1, '.', false);
                }
                break;
            case '%':
                switch (prompt[++off]) {
                case 'f':
                    out(m_tty, "{}", m_filename);
                    break;
                case 'l':
                    out(m_tty, "{}", m_line);
                    break;
                default:
                    out(m_tty, "?");
                }
                break;
            case '\\':
                ++off;
                [[fallthrough]];
            default:
                out(m_tty, "{}", prompt[off]);
            }
        }
        return off;
    }

    // FIXME: Don't save scrollback when emulating more.
    Vector<String> m_lines;
    size_t m_line { 0 };
    FILE* m_file;
    FILE* m_tty;

    size_t m_width;
    size_t m_height;

    String m_filename;
    String m_prompt;
};

static String get_key_sequence()
{
    // We need a buffer to handle ansi sequences.
    char buff[8];
    ssize_t n = read(STDOUT_FILENO, buff, sizeof(buff));
    return String(buff, n);
}

static void cat_file(FILE* file)
{
    Array<u8, 4096> buffer;
    while (!feof(file)) {
        size_t n = fread(buffer.data(), 1, buffer.size(), file);
        if (n == 0 && ferror(file)) {
            perror("fread");
            exit(1);
        }

        n = fwrite(buffer.data(), 1, n, stdout);
        if (n == 0 && ferror(stdout)) {
            perror("fwrite");
            exit(1);
        }
    }
}

int main(int argc, char** argv)
{
    if (pledge("stdio rpath tty", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    char const* filename = "-";
    char const* prompt = "?f%f :.(line %l)?e (END):.";
    bool dont_switch_buffer = false;
    bool quit_at_eof = false;
    bool emulate_more = false;

    if (LexicalPath::basename(argv[0]) == "more"sv)
        emulate_more = true;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(filename, "The paged file", "file", Core::ArgsParser::Required::No);
    args_parser.add_option(prompt, "Prompt line", "prompt", 'P', "Prompt");
    args_parser.add_option(dont_switch_buffer, "Don't use xterm alternate buffer", "no-init", 'X');
    args_parser.add_option(quit_at_eof, "Exit when the end of the file is reached", "quit-at-eof", 'e');
    args_parser.add_option(emulate_more, "Pretend that we are more(1)", "emulate-more", 'm');
    args_parser.parse(argc, argv);

    FILE* file;
    if (String("-") == filename) {
        file = stdin;
    } else if ((file = fopen(filename, "r")) == nullptr) {
        perror("fopen");
        exit(1);
    }

    if (pledge("stdio tty", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (emulate_more) {
        // Configure options that match more's behavior
        dont_switch_buffer = true;
        quit_at_eof = true;
        prompt = "--More--";
    }

    if (!isatty(STDOUT_FILENO)) {
        cat_file(file);
        return 0;
    }

    setup_tty(!dont_switch_buffer);

    Pager pager(file, stdout, g_wsize.ws_col, g_wsize.ws_row);
    pager.set_filename(filename);
    pager.set_prompt(prompt);

    pager.init();

    StringBuilder modifier_buffer = StringBuilder(10);
    for (String sequence;; sequence = get_key_sequence()) {
        if (sequence.to_uint().has_value()) {
            modifier_buffer.append(sequence);
        } else {
            if (sequence == "" || sequence == "q") {
                break;
            } else if (sequence == "j" || sequence == "\e[B" || sequence == "\n") {
                if (!emulate_more) {
                    if (!modifier_buffer.is_empty())
                        pager.down_n(modifier_buffer.build().to_uint().value_or(1));
                    else
                        pager.down();
                }
            } else if (sequence == "k" || sequence == "\e[A") {
                if (!emulate_more) {
                    if (!modifier_buffer.is_empty())
                        pager.up_n(modifier_buffer.build().to_uint().value_or(1));
                    else
                        pager.up();
                }
            } else if (sequence == "g") {
                if (!emulate_more) {
                    if (!modifier_buffer.is_empty())
                        pager.go_to_line(modifier_buffer.build().to_uint().value());
                    else
                        pager.top();
                }
            } else if (sequence == "G") {
                if (!emulate_more) {
                    if (!modifier_buffer.is_empty())
                        pager.go_to_line(modifier_buffer.build().to_uint().value());
                    else
                        pager.bottom();
                }
            } else if (sequence == " " || sequence == "\e[6~") {
                pager.down_page();
            } else if (sequence == "\e[5~" && !emulate_more) {
                pager.up_page();
            } else if (sequence == "d") {
                pager.down_half_page();
            } else if (sequence == "u" && !emulate_more) {
                pager.up_half_page();
            }

            modifier_buffer.clear();
        }

        if (quit_at_eof && pager.at_end())
            break;
    }

    pager.clear_status();

    teardown_tty(!dont_switch_buffer);
    return 0;
}
