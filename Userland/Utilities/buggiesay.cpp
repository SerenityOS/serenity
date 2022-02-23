/*
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Random.h>
#include <AK/Utf8View.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <LibVT/EscapeSequenceStripper.h>

struct Line {
    size_t length() const
    {
        return m_length;
    }

    bool is_empty() const
    {
        return m_words.is_empty();
    }

    void append(Utf8View word)
    {
        m_length += word.length();
        m_words.append(word.as_string());
    }

    Vector<StringView>::ConstIterator begin() const { return m_words.begin(); }
    Vector<StringView>::ConstIterator end() const { return m_words.end(); }

private:
    Vector<StringView> m_words;
    size_t m_length = 0;
};

static size_t max_word_len(Utf8View text)
{
    size_t len = 0;
    size_t max_len = 0;
    for (auto c : text) {
        if (c == ' ' || c == '\n' || c == '\t') {
            len = 0;
        } else {
            len += 1;
            max_len = max(len, max_len);
        }
    }
    return max_len;
}

static Vector<Line> wrap_text(Utf8View text, size_t max_width)
{
    // Expand the wrap width if there is one really long "word"
    max_width = max(max_word_len(text), max_width);

    Line current_line;
    Vector<Line> lines;

    u32 prev_c = 0;
    size_t blanks_in_row = 0;
    Optional<size_t> word_start;

    auto push_line = [&] {
        lines.append(move(current_line));
        current_line = Line {};
        blanks_in_row = 0;
    };

    auto append_to_line = [&](Utf8View word) {
        size_t new_length = current_line.length() + word.length();
        if (!current_line.is_empty()) {
            if (new_length + 1 <= max_width)
                current_line.append(Utf8View(StringView(" ", 1)));
            else
                push_line();
        }
        current_line.append(word);
    };

    auto append_last_word = [&](size_t word_end) {
        if (word_start.has_value())
            append_to_line(text.substring_view(*word_start, word_end - *word_start));
        word_start.clear();
    };

    auto it = text.begin();
    for (; it != text.end(); ++it) {
        u32 c = *it;
        size_t i = text.byte_offset_of(it);
        if (c == ' ') {
            append_last_word(i);
            blanks_in_row += 1;
        } else if (c == '\t') {
            append_last_word(i);
            blanks_in_row += 8;
        } else {
            // Always wrap if there is a newline followed by 8 spaces/a tab (new paragraph)
            if (prev_c == '\n' && blanks_in_row >= 8)
                push_line();

            // Always wrap if there is two newlines
            if (c == '\n') {
                append_last_word(i);
                if (prev_c == '\n')
                    push_line();
            } else if (!word_start.has_value()) {
                word_start = i;
            }
            // Always wrap is theres a '(' after newline
            // (Hack for better fortune formatting)
            if (c == '(' && prev_c == '\n' && i > 1) {
                push_line();
                push_line();
            }

            blanks_in_row = 0;
            prev_c = c;
        }
    }

    append_last_word(text.length());
    if (!current_line.is_empty())
        push_line();

    return lines;
}

static void output_boxed_lines(Vector<Line> const& lines)
{
    size_t max_line_length = 0;
    for (auto const& line : lines)
        max_line_length = max(line.length(), max_line_length);

    auto repeat_char = [](char c, size_t count) {
        for (size_t i = 0; i < count; i++)
            putchar(c);
    };

    auto output_top_bottom = [&](char line_char) {
        putchar(' ');
        repeat_char(line_char, max_line_length + 2);
        outln(" ");
    };

    auto output_line = [&](Line const& line) {
        for (auto const& word : line)
            out(word);
    };

    struct BoxEdge {
        char left, right;
    };

    auto get_box_edge = [&](size_t line_num) {
        if (line_num == 0)
            return BoxEdge { '/', '\\' };
        else if (line_num == lines.size() - 1)
            return BoxEdge { '\\', '/' };
        else
            return BoxEdge { '|', '|' };
    };

    output_top_bottom('_');
    if (lines.size() == 1) {
        out("< ");
        output_line(lines[0]);
        outln(" >");
    } else {
        for (size_t i = 0; i < lines.size(); i++) {
            auto box_egde = get_box_edge(i);
            auto const& line = lines[i];
            putchar(box_egde.left);
            putchar(' ');
            output_line(line);
            repeat_char(' ', max_line_length - line.length());
            putchar(' ');
            putchar(box_egde.right);
            putchar('\n');
        }
    }
    output_top_bottom('-');
}

static ErrorOr<size_t> read_stdin(Span<u8> buffer_span)
{
    size_t total_read = 0;
    for (;;) {
        auto nread = TRY(Core::System::read(STDIN_FILENO, buffer_span));
        if (nread == 0)
            break;
        buffer_span = buffer_span.slice(nread);
        total_read += nread;
    }
    return total_read;
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    char const* template_path = "/res/buggiesay_template.txt";
    TRY(Core::System::pledge("stdio rpath"));

    bool xd_eyes = false;
    bool hypnotised_eyes = false;
    bool dollar_eyes = false;
    bool confused_eyes = false;
    unsigned max_width = 60;

    Vector<StringView> message;
    Core::ArgsParser args_parser;
    args_parser.set_general_help("Speak as Buggie!");
    args_parser.add_option(xd_eyes, "XD eyes", nullptr, 'x');
    args_parser.add_option(hypnotised_eyes, "Hypnotised eyes", nullptr, 'a');
    args_parser.add_option(dollar_eyes, "Dollar eyes", nullptr, 'm');
    args_parser.add_option(confused_eyes, "Confused eyes", nullptr, 'O');
    args_parser.add_option(max_width, "Max message box width", "max-width", 'w', "max-width");
    args_parser.add_positional_argument(message, "Message to say", "message", Core::ArgsParser::Required::No);
    args_parser.set_stop_on_first_non_option(true);
    args_parser.parse(arguments);

    struct BuggieEyes {
        char left, right;
    };

    // Randomly pick from the from selected eyes
    Vector<BuggieEyes> chosen_eyes;
    chosen_eyes.ensure_capacity(4);
    if (xd_eyes)
        chosen_eyes.append(BuggieEyes { '>', '<' });
    if (hypnotised_eyes)
        chosen_eyes.append(BuggieEyes { '@', '@' });
    if (dollar_eyes)
        chosen_eyes.append(BuggieEyes { '$', '$' });
    if (confused_eyes)
        chosen_eyes.append(BuggieEyes { 'o', 'O' });

    auto eyes = chosen_eyes.size() > 0
        ? chosen_eyes.at(get_random_uniform(chosen_eyes.size()))
        : BuggieEyes { 'o', 'o' };

    Span<u8> message_span;
    Array<u8, 1024> message_buffer;
    if (message.is_empty()) {
        auto input_length = TRY(read_stdin(message_buffer.span()));
        message_span = message_buffer.span().trim(input_length);
    } else {
        // Join command-line message with spaces
        size_t buffer_pos = 0;
        for (size_t i = 0; i < message.size(); i++) {
            if (i != 0)
                message_buffer.at(buffer_pos++) = ' ';
            for (char c : message[i])
                message_buffer.at(buffer_pos++) = c;
        }
        message_span = message_buffer.span().trim(buffer_pos);
    }

    message_span = VT::EscapeSequenceStripper::strip_inplace(message_span);
    output_boxed_lines(wrap_text(Utf8View(StringView(message_span.data(), message_span.size())), max_width));

    auto buggie_template = TRY(Core::File::open(template_path, Core::OpenMode::ReadOnly))->read_all();
    out(buggie_template, eyes.left, eyes.right);
    return 0;
}
