/*
 * Copyright (c) 2021, Peter Elliott <pelliott@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibLine/Editor.h>
#include <LibMain/Main.h>
#include <math.h>
#include <stdio.h>
#include <termios.h>
#include <unistd.h>

static struct termios g_save;

// Flag set by a SIGWINCH signal handler to notify the main loop that the window has been resized.
static Atomic<bool> g_resized { false };

static Atomic<bool> g_restore_buffer_on_close { false };

constexpr size_t line_number_column_padding = 7;
static constexpr StringView line_number_column_separator = " "sv;

static ErrorOr<void> setup_tty(bool switch_buffer)
{
    // Save previous tty settings.
    g_save = TRY(Core::System::tcgetattr(STDOUT_FILENO));

    struct termios raw = g_save;
    raw.c_lflag &= ~(ECHO | ICANON);

    // Disable echo and line buffering
    TRY(Core::System::tcsetattr(STDOUT_FILENO, TCSAFLUSH, raw));

    if (switch_buffer) {
        // Save cursor and switch to alternate buffer.
        out("\e[s\e[?1047h");
        g_restore_buffer_on_close = true;
    }

    return {};
}

static void teardown_tty()
{
    auto maybe_error = Core::System::tcsetattr(STDOUT_FILENO, TCSAFLUSH, g_save);
    if (maybe_error.is_error())
        warnln("Failed to reset original terminal state: {}", strerror(maybe_error.error().code()));

    if (g_restore_buffer_on_close.exchange(false))
        out("\e[?1047l\e[u");
}

static Vector<StringView> wrap_line(ByteString const& string, size_t width)
{
    auto const result = Line::Editor::actual_rendered_string_metrics(string, {}, width);

    Vector<StringView> spans;
    size_t span_start = 0;
    for (auto const& line_metric : result.line_metrics) {
        VERIFY(line_metric.bit_length.has_value());
        auto const bit_length = line_metric.bit_length.value();
        spans.append(string.substring_view(span_start, bit_length));
        span_start += bit_length;
    }

    return spans;
}

class Pager {
public:
    Pager(StringView filename, FILE* file, FILE* tty, StringView prompt, bool show_line_numbers)
        : m_file(file)
        , m_tty(tty)
        , m_filename(filename)
        , m_prompt(prompt)
        , m_show_line_numbers(show_line_numbers)
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
        if (m_line == 0 && m_subline == 0)
            return;

        line_subline_add(m_line, m_subline, -n);

        full_redraw();
    }

    void down_n(size_t n)
    {
        if (at_end())
            return;

        clear_status();

        read_enough_for_line(m_line + n);

        size_t real_n = line_subline_add(m_line, m_subline, n);

        // If we are moving less than a screen down, just draw the extra lines
        // for efficiency and more(1) compatibility.
        if (n < m_height - 1) {
            size_t line = m_line;
            size_t subline = m_subline;
            line_subline_add(line, subline, (m_height - 1) - real_n, false);
            write_range(line, subline, real_n);
        } else {
            write_range(m_line, m_subline, m_height - 1);
        }

        status_line();

        fflush(m_tty);
    }

    void top()
    {
        m_line = 0;
        m_subline = 0;
        full_redraw();
    }

    void bottom()
    {
        while (read_line())
            ;

        m_line = end_line();
        m_subline = end_subline();
        full_redraw();
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
        read_enough_for_line(line_num);

        m_line = line_num;
        m_subline = 0;
        bound_cursor();
        full_redraw();
    }

    void init()
    {
        resize(false);
    }

    void populate_line_buffer()
    {
        // First, we get the current size of the window.
        struct winsize window;
        if (auto maybe_error = Core::System::ioctl(STDOUT_FILENO, TIOCGWINSZ, &window); maybe_error.is_error()) {
            warnln("ioctl(2): {}", strerror(maybe_error.error().code()));
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
    }

    void resize(bool clear = true)
    {
        populate_line_buffer();

        reflow();
        bound_cursor();

        // Next, we repaint the whole screen. We need to figure out what line was at the top
        // of the screen, and seek there and re-display everything again.
        if (clear) {
            full_redraw();
        } else {
            redraw();
        }
    }

    static size_t count_digits_in_number(size_t line_numbers)
    {
        return line_numbers > 0 ? static_cast<size_t>(log10(static_cast<double>(line_numbers))) + 1 : 1;
    }

    size_t write_range(size_t line, size_t subline, size_t length)
    {
        size_t lines = 0;
        for (size_t i = line; i < m_lines.size(); ++i) {
            auto digits_count_for_max_line_number = count_digits_in_number(i + 1);
            auto column_width = 0;
            if (m_width > line_number_column_padding + line_number_column_separator.length())
                column_width = max(line_number_column_padding, digits_count_for_max_line_number);

            for (auto string : sublines(i)) {
                if (subline > 0) {
                    --subline;
                    continue;
                }
                if (lines >= length)
                    return lines;

                if (m_show_line_numbers)
                    out(m_tty, "\e[1m{:>{}}\e[22m{}", i + 1, column_width, line_number_column_separator);

                outln(m_tty, "{}", string);
                ++lines;
            }
        }

        return lines;
    }

    void clear_status()
    {
        out(m_tty, "\e[2K\r");
    }

    void status_line()
    {
        out(m_tty, "\e[0;7m ");
        render_status_line(m_prompt);
        out(m_tty, " \e[0m");
    }

    bool read_line()
    {
        char* line = nullptr;
        size_t n = 0;
        ssize_t size = getline(&line, &n, m_file);
        ScopeGuard guard([line] {
            free(line);
        });

        if (size == -1)
            return false;

        // Strip trailing newline.
        if (line[size - 1] == '\n')
            --size;

        m_lines.append(ByteString(line, size));
        return true;
    }

    bool at_end()
    {
        return feof(m_file) && m_line == end_line() && m_subline == end_subline();
    }

private:
    void redraw()
    {
        write_range(m_line, m_subline, m_height - 1);
        status_line();
        fflush(m_tty);
    }

    void full_redraw()
    {
        out("\e[2J\e[0G\e[0d");
        redraw();
    }

    void read_enough_for_line(size_t line)
    {
        // This might read a bounded number of extra lines.
        while (m_lines.size() < line + m_height) {
            if (!read_line())
                break;
        }
    }

    size_t render_status_line(StringView prompt, size_t off = 0, char end = '\0', bool ignored = false)
    {
        for (; off < prompt.length() && prompt[off] != end; ++off) {
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
                    out(m_tty, "{}", m_line + 1);
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

    Vector<StringView> const& sublines(size_t line)
    {
        return m_subline_cache.ensure(line, [&]() {
            auto width = m_width;
            if (m_show_line_numbers) {
                auto line_number_column_width = max(line_number_column_padding, count_digits_in_number(line)) + line_number_column_separator.length();
                if (width > line_number_column_width)
                    width -= line_number_column_width;
            }
            return wrap_line(m_lines[line], width);
        });
    }

    size_t line_subline_add(size_t& line, size_t& subline, int delta, bool bounded = true)
    {
        int unit = delta / AK::abs(delta);
        size_t i;
        for (i = 0; i < (size_t)AK::abs(delta); ++i) {
            if (subline == 0 && unit == -1) {
                if (line == 0)
                    return i;

                line--;
                subline = sublines(line).size() - 1;
            } else if (subline == sublines(line).size() - 1 && unit == 1) {
                if (bounded && feof(m_file) && line == end_line() && subline == end_subline())
                    return i;

                if (line >= m_lines.size() - 1)
                    return i;

                line++;
                subline = 0;
            } else {
                subline += unit;
            }
        }
        return i;
    }

    void bound_cursor()
    {
        if (!feof(m_file))
            return;

        if (m_line == end_line() && m_subline >= end_subline()) {
            m_subline = end_subline();
        } else if (m_line > end_line()) {
            m_line = end_line();
            m_subline = end_subline();
        }
    }

    void calculate_end()
    {
        if (m_lines.is_empty()) {
            m_end_line = 0;
            m_end_subline = 0;
            return;
        }
        size_t end_line = m_lines.size() - 1;
        size_t end_subline = sublines(end_line).size() - 1;
        line_subline_add(end_line, end_subline, -(m_height - 1), false);
        m_end_line = end_line;
        m_end_subline = end_subline;
    }

    // Only valid after all lines are read.
    size_t end_line()
    {
        if (!m_end_line.has_value())
            calculate_end();

        return m_end_line.value();
    }

    // Only valid after all lines are read.
    size_t end_subline()
    {
        if (!m_end_subline.has_value())
            calculate_end();

        return m_end_subline.value();
    }

    void reflow()
    {
        m_subline_cache.clear();
        m_end_line = {};
        m_end_subline = {};

        m_subline = 0;
    }

    // FIXME: Don't save scrollback when emulating more.
    Vector<ByteString> m_lines;

    size_t m_line { 0 };
    size_t m_subline { 0 };

    HashMap<size_t, Vector<StringView>> m_subline_cache;
    Optional<size_t> m_end_line;
    Optional<size_t> m_end_subline;

    FILE* m_file;
    FILE* m_tty;

    size_t m_width { 0 };
    size_t m_height { 0 };

    ByteString m_filename;
    ByteString m_prompt;

    bool m_show_line_numbers { false };
};

/// Return the next key sequence, or nothing if a signal is received while waiting
/// to read the next sequence.
static Optional<ByteString> get_key_sequence()
{
    // We need a buffer to handle ansi sequences.
    char buff[8];

    ssize_t n = read(STDOUT_FILENO, buff, sizeof(buff));
    if (n > 0) {
        return ByteString(buff, n);
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

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath tty sigaction"));

    // FIXME: Make these into StringViews once we stop using fopen below.
    ByteString filename = "-";
    ByteString prompt = "?f%f :.(line %l)?e (END):.";
    bool dont_switch_buffer = false;
    bool quit_at_eof = false;
    bool quit_if_one_screen = false;
    bool emulate_more = false;
    bool show_line_numbers = false;

    if (LexicalPath::basename(arguments.strings[0]) == "more"sv)
        emulate_more = true;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(filename, "The paged file", "file", Core::ArgsParser::Required::No);
    args_parser.add_option(prompt, "Prompt line", "prompt", 'P', "Prompt");
    args_parser.add_option(dont_switch_buffer, "Don't use xterm alternate buffer", "no-init", 'X');
    args_parser.add_option(show_line_numbers, "Show line numbers", "line-numbers", 'N');
    args_parser.add_option(quit_at_eof, "Exit when the end of the file is reached", "quit-at-eof", 'e');
    args_parser.add_option(quit_if_one_screen, "Exit immediately if the entire file can be displayed on one screen", "quit-if-one-screen", 'F');
    args_parser.add_option(emulate_more, "Pretend that we are more(1)", "emulate-more", 'm');
    args_parser.parse(arguments);

    FILE* file;
    if (ByteString("-") == filename) {
        file = stdin;
    } else if ((file = fopen(filename.characters(), "r")) == nullptr) {
        perror("fopen");
        exit(1);
    }

    // On SIGWINCH set this flag so that the main-loop knows when the terminal
    // has been resized.
    struct sigaction resize_action;
    resize_action.sa_handler = [](auto) {
        g_resized = true;
    };
    TRY(Core::System::sigaction(SIGWINCH, &resize_action, nullptr));

    TRY(Core::System::pledge("stdio tty sigaction"));

    if (emulate_more) {
        // Configure options that match more's behavior
        dont_switch_buffer = true;
        quit_at_eof = true;
        prompt = "--More--";
    }

    if (!TRY(Core::System::isatty(STDOUT_FILENO))) {
        cat_file(file);
        return 0;
    }

    Pager pager(filename, file, stdout, prompt, show_line_numbers);
    pager.populate_line_buffer();

    if (quit_if_one_screen && pager.at_end()) {
        pager.init();
        pager.clear_status();
        return 0;
    }

    TRY(setup_tty(!dont_switch_buffer));

    ScopeGuard teardown_guard([] {
        teardown_tty();
    });

    auto teardown_sigaction_handler = [](auto) {
        teardown_tty();
        exit(1);
    };
    struct sigaction teardown_action;
    teardown_action.sa_handler = teardown_sigaction_handler;
    TRY(Core::System::sigaction(SIGTERM, &teardown_action, nullptr));

    struct sigaction ignore_action;
    ignore_action.sa_handler = { SIG_IGN };
    TRY(Core::System::sigaction(SIGINT, &ignore_action, nullptr));

    pager.init();

    StringBuilder modifier_buffer = StringBuilder(10);
    for (Optional<ByteString> sequence_value;; sequence_value = get_key_sequence()) {
        if (g_resized) {
            g_resized = false;
            pager.resize();
        }

        if (!sequence_value.has_value()) {
            continue;
        }

        auto const& sequence = sequence_value.value();

        if (sequence.to_number<unsigned>().has_value()) {
            modifier_buffer.append(sequence);
        } else {
            if (sequence == "" || sequence == "q" || sequence == "Q") {
                break;
            } else if (sequence == "j" || sequence == "\e[B" || sequence == "\n") {
                if (!emulate_more) {
                    if (!modifier_buffer.is_empty())
                        pager.down_n(modifier_buffer.to_byte_string().to_number<unsigned>().value_or(1));
                    else
                        pager.down();
                }
            } else if (sequence == "k" || sequence == "\e[A") {
                if (!emulate_more) {
                    if (!modifier_buffer.is_empty())
                        pager.up_n(modifier_buffer.to_byte_string().to_number<unsigned>().value_or(1));
                    else
                        pager.up();
                }
            } else if (sequence == "g") {
                if (!emulate_more) {
                    if (!modifier_buffer.is_empty())
                        pager.go_to_line(modifier_buffer.to_byte_string().to_number<unsigned>().value());
                    else
                        pager.top();
                }
            } else if (sequence == "G") {
                if (!emulate_more) {
                    if (!modifier_buffer.is_empty())
                        pager.go_to_line(modifier_buffer.to_byte_string().to_number<unsigned>().value());
                    else
                        pager.bottom();
                }
            } else if (sequence == " " || sequence == "f" || sequence == "\e[6~") {
                pager.down_page();
            } else if ((sequence == "\e[5~" || sequence == "b") && !emulate_more) {
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
    return 0;
}
