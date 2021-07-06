/*
 * Copyright (c) 2021, Peter Elliott <pelliott@ualberta.ca>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
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

static void setup_tty()
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

    // Save cursor and switch to alternate buffer.
    out("\e[s\e[?1047h");
}

static void teardown_tty()
{
    if (tcsetattr(STDOUT_FILENO, TCSAFLUSH, &g_save) == -1) {
        perror("tcsetattr(3)");
    }

    out("\e[?1047l\e[u");
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

        while (n - (m_lines.size() - m_line) > 0) {
            if (!read_line())
                break;
        }
        m_line += write_range(min(m_line + m_height - 1, m_line + (m_lines.size() - m_line)), n);
        status_line();

        fflush(m_tty);
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
        out(m_tty, "\e[7m -- less -- (line {})\e[27m", m_line + 1);
    }

    bool read_line()
    {
        char* line = nullptr;
        size_t n = 0;
        ssize_t size = getline(&line, &n, m_file);

        if (size == -1)
            return false;

        m_lines.extend(wrap_line(Utf8View(line), m_width));
        free(line);
        return true;
    }

private:
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

int main(int argc, char** argv)
{
    const char* filename = "-";
    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(filename, "The paged file", "file", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    FILE* file;
    if (String("-") == filename) {
        file = stdin;
    } else {
        file = fopen(filename, "r");
    }

    setup_tty();

    Pager pager(file, stdout, g_wsize.ws_col, g_wsize.ws_row);

    pager.init();

    for (String sequence;; sequence = get_key_sequence()) {
        if (sequence == "" || sequence == "q") {
            break;
        } else if (sequence == "j" || sequence == "\e[B" || sequence == "\n") {
            pager.down();
        } else if (sequence == "k" || sequence == "\e[A") {
            pager.up();
        } else if (sequence == " ") {
            pager.down_page();
        }
    }

    teardown_tty();
    return 0;
}
