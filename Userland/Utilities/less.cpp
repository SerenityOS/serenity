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
#include <csignal>
#include <ctype.h>
#include <fcntl.h>
#include <stdio.h>
#include <sys/ioctl.h>
#include <termios.h>
#include <unistd.h>

static struct termios g_save;

// Flag set by a SIGWINCH signal handler to notify the main loop that the window has been resized.
static Atomic<bool> g_resized { false };

static void setup_tty(bool switch_buffer)
{
    // Save previous tty settings.
    if (tcgetattr(STDOUT_FILENO, &g_save) == -1) {
        perror("tcgetattr(3)");
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
            // FIXME: calculate the printed width of the character.
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
    Pager(StringView const& filename, FILE* file, FILE* tty, StringView const& prompt)
        : m_file(file)
        , m_tty(tty)
        , m_filename(filename)
        , m_prompt(prompt)
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

        while (m_lines.size() < m_line + n + m_height - 1) {
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
        resize();
    }

    void resize()
    {
        // First, we get the current size of the window.
        struct winsize window;
        if (ioctl(STDOUT_FILENO, TIOCGWINSZ, &window) == -1) {
            perror("ioctl(2)");
            return;
        }

        auto original_height = m_height;

        m_width = window.ws_col;
        m_height = window.ws_row;

        // If the window is now larger than it was before, read more lines of
        // the file so that there is enough data to fill the whole screen.
        //
        // m_height is initialized to 0, so if the terminal was 80x25 when
        // this is called for the first time, then additional_lines will be 80
        // and 80 lines of text will be buffered.
        auto additional_lines = m_height - original_height;
        while (additional_lines > 0) {
            if (!read_line()) {
                // End of file has been reached.
                break;
            }
            --additional_lines;
        }

        // Next, we repaint the whole screen. We need to figure out what line was at the top
        // of the screen, and seek there and re-display everything again.
        clear_status();
        out("\e[2J\e[0G\e[0d");
        write_range(m_line, m_height - 1);
        status_line();
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

    size_t m_width { 0 };
    size_t m_height { 0 };

    String m_filename;
    String m_prompt;
};

/// Return the next key sequence, or nothing if a signal is received while waiting
/// to read the next sequence.
static Optional<String> get_key_sequence()
{
    // We need a buffer to handle ansi sequences.
    char buff[8];

    ssize_t n = read(STDOUT_FILENO, buff, sizeof(buff));
    if (n > 0) {
        return String(buff, n);
    } else {
        return {};
    }
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
    if (pledge("stdio rpath tty sigaction", nullptr) < 0) {
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

    // On SIGWINCH set this flag so that the main-loop knows when the terminal
    // has been resized.
    signal(SIGWINCH, [](auto) {
        g_resized = true;
    });

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

    Pager pager(filename, file, stdout, prompt);
    pager.init();

    StringBuilder modifier_buffer = StringBuilder(10);
    for (Optional<String> sequence_value;; sequence_value = get_key_sequence()) {
        if (g_resized) {
            g_resized = false;
            pager.resize();
        }

        if (!sequence_value.has_value()) {
            continue;
        }

        const auto& sequence = sequence_value.value();

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
