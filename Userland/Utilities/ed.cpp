/*
 * Copyright (c) 2021, the SerenityOS developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/FileStream.h>
#include <AK/ScopeGuard.h>
#include <AK/TemporaryChange.h>
#include <AK/Types.h>
#include <LibCore/ArgsParser.h>
#include <LibLine/Editor.h>
#include <LibRegex/Regex.h>
#include <ctype.h>
#include <math.h>

struct Address {
    enum class Kind {
        CurrentLine,
        LastLine,
        FirstLine,
        MatchingRegex,
        Mark,
    } kind { Kind::CurrentLine };
    off_t offset { 0 };
    char mark { 0 };
    String regex {};
};

struct Range {
    Address from { Address::Kind::FirstLine, 0, 0, {} };
    Address to { Address::Kind::LastLine, 0, 0, {} };
};

class Editor;

static Optional<Address> parse_address(GenericLexer& lexer)
{
    Address base;
    switch (lexer.peek()) {
    case '.':
        lexer.consume();
        base.kind = Address::Kind::CurrentLine;
        break;
    case '$':
        lexer.consume();
        base.kind = Address::Kind::LastLine;
        break;
    case '-':
        lexer.consume();
        base.kind = Address::Kind::CurrentLine;
        base.offset = -1;
        break;
    case '+':
        lexer.consume();
        base.kind = Address::Kind::CurrentLine;
        base.offset = lexer.consume_while([](auto ch) { return !!isdigit(ch); }).to_int().value_or(1);
        break;
    case '^':
        lexer.consume();
        base.kind = Address::Kind::CurrentLine;
        base.offset = -lexer.consume_while([](auto ch) { return !!isdigit(ch); }).to_int().value();
        break;
    case '0':
        lexer.consume();
        base.kind = Address::Kind::FirstLine;
        break;
    case ' ':
    case '\t':
        lexer.consume_while(is_any_of(" \t"));
        base.kind = Address::Kind::CurrentLine;
        base.offset = -lexer.consume_while([](auto ch) { return !!isdigit(ch); }).to_int().value();
        break;
    case '/':
        // FIXME: '?' should search backwards.
    case '?': {
        auto starting = lexer.consume();
        base.kind = Address::Kind::MatchingRegex;
        base.regex = lexer.consume_while([starting, last = '\0'](auto ch) {
            ScopeGuard set_last { [&] {
                const_cast<char&>(last) = ch;
            } };
            return ch != starting && last != '\\';
        });
        if (!lexer.consume_specific(starting)) {
            warnln("Expected {} but got {}", starting, lexer.peek());
            return {};
        }
        break;
    }
    case '\'':
        lexer.consume();
        base.kind = Address::Kind::Mark;
        base.mark = lexer.consume();
        if (!islower(base.mark)) {
            warnln("Expected a lowercase letter, '{}' is not a lowercase letter", base.mark);
            return {};
        }
        break;
    default: {
        base.kind = Address::Kind::FirstLine;
        auto offset = lexer.consume_while([](auto ch) { return !!isdigit(ch); }).to_int();
        if (!offset.has_value())
            return {};
        base.offset = offset.value();
        break;
    }
    }

    return base;
}

static Optional<Range> parse_range(GenericLexer& lexer)
{
    if (lexer.consume_specific(','))
        return Range {};

    if (lexer.consume_specific(';'))
        return Range { { Address::Kind::CurrentLine }, { Address::Kind::LastLine } };

    Vector<Address, 2> addresses;
    do {
        auto address = parse_address(lexer);
        if (!address.has_value())
            break;
        addresses.append(address.release_value());
    } while (lexer.consume_specific(',') || lexer.consume_specific(';'));

    if (addresses.is_empty())
        return {};
    if (addresses.size() < 2)
        return Range { {}, addresses.last() };

    return Range { addresses[addresses.size() - 2], addresses[addresses.size() - 1] };
}

struct EditorCommand {
    virtual ~EditorCommand() { }

    virtual void execute(Editor&) const
    {
        auto& id = typeid(*this);
        outln("Command {}", id.name());
    }

    void parse_suffix(GenericLexer& lexer)
    {
        switch (lexer.peek()) {
        case 'p':
            lexer.consume();
            print_suffix = PrintSuffix::Print;
            break;
        case 'l':
            lexer.consume();
            print_suffix = PrintSuffix::List;
            break;
        case 'n':
            lexer.consume();
            print_suffix = PrintSuffix::Enumerate;
            break;
        case '\n':
            lexer.consume();
            break;
        default:
            break;
        }
    }

    Range range;
    enum class PrintSuffix {
        Print,
        List,
        Enumerate,
        None,
    } print_suffix { PrintSuffix::None };
};

class Editor final {
public:
    explicit Editor(const StringView& filename)
    {
        if (!filename.is_null())
            read_from_file(filename, {});
    }

    explicit Editor(FILE* fp)
    {
        InputFileStream stream { fp };
        read_from_file(stream, {});
    }

    Result<NonnullOwnPtr<EditorCommand>, String> parse(String command);

    void parse_and_run(String command)
    {
        dbgln("Received command '{}'", command);
        auto parsed_command = parse(move(command));
        if (parsed_command.is_error()) {
            warnln("Error: {}", parsed_command.error());
            return;
        }
        parsed_command.value()->execute(*this);
    }

    Address address(size_t line)
    {
        return { Address::Kind::FirstLine, (off_t)line };
    }

    bool has_line(size_t index) const
    {
        return m_lines.size() > index;
    }

    size_t resolve(const Address& address)
    {
        Optional<size_t> base;
        off_t offset = 0;
        switch (address.kind) {
        case Address::Kind::CurrentLine:
            base = m_current_line;
            offset = address.offset;
            break;
        case Address::Kind::FirstLine:
            base = 0;
            offset = address.offset;
            break;
        case Address::Kind::LastLine:
            base = m_lines.size() - 1;
            offset = address.offset;
            break;
        case Address::Kind::Mark:
            base = mark(address.mark);
            offset = address.offset;
            break;
        case Address::Kind::MatchingRegex:
            base = find_matching(address.regex.is_empty() ? m_last_lookup_regex : address.regex);
            offset = address.offset;
            break;
        default:
            ASSERT_NOT_REACHED();
        }

        if (!base.has_value()) {
            warnln("Address did not resolve");
            return 0;
        }

        return base.value() + offset;
    }

    bool matches(size_t line, Regex<PosixExtended>& regex) const
    {
        if (!has_line(line))
            return false;
        m_last_lookup_regex = regex.pattern_value;
        RegexResult result;
        return regex.match(m_lines[line], result, PosixFlags::Global);
    }

    Optional<size_t> find_matching(StringView regex) const
    {
        m_last_lookup_regex = regex;
        Regex<PosixExtended> re(regex);
        if (re.parser_result.error != regex::Error::NoError) {
            warnln(re.error_string());
            return {};
        }

        for (size_t i = 0; i < m_lines.size(); ++i) {
            auto j = (i + m_current_line) % m_lines.size();
            if (matches(j, re))
                return j;
        }
        return {};
    }

    void insert(size_t line, String data)
    {
        m_lines.insert(line, move(data));
    }

    String take_line(size_t line)
    {
        if (line >= m_lines.size())
            return m_the_empty_line;

        for (auto& mark : m_marks) {
            if (!mark.has_value())
                continue;
            if (mark.value() == line)
                mark.clear();
            else if (mark.value() > line)
                mark = mark.value() - 1;
        }

        if (m_current_line >= line)
            --m_current_line;

        return m_lines.take(line);
    }

    void yank(Range range)
    {
        auto start = resolve(range.from);
        auto end = resolve(range.to);

        m_cut_buffer.clear();
        for (size_t i = start; i <= end; ++i)
            m_cut_buffer.append(line(i));
    }

    size_t put(Address address)
    {
        auto start = resolve(address);
        for (auto& line : m_cut_buffer)
            m_lines.insert(start++, line);
        return start;
    }

    String input(String prompt = "? ")
    {
        ASSERT(m_current_editor);
        auto result = m_current_editor->get_line(m_should_show_prompts ? prompt : m_the_empty_line);
        if (result.is_error())
            return {};
        return result.release_value();
    }

    void repl(Line::Editor& line_editor)
    {
        TemporaryChange change { m_current_editor, &line_editor };
        while (!m_should_quit) {
            auto result = line_editor.get_line(m_should_show_prompts ? m_prompt : m_the_empty_line);
            if (result.is_error())
                return;

            parse_and_run(result.release_value());
        }
    }

    void read_from_file(String filename, Optional<size_t> upto, bool replace = true, Optional<size_t> target = {})
    {
        auto fp = fopen(filename.characters(), "r");
        if (!fp) {
            perror("fopen");
            return;
        }
        {
            InputFileStream stream { fp };
            read_from_file(stream, upto, replace, target);
        }
        fclose(fp);
    }

    void read_from_file(InputStream& stream, Optional<size_t> upto, bool replace = true, Optional<size_t> target = {})
    {
        if (replace)
            m_lines.clear();

        if (target.has_value())
            ASSERT(!replace);

        char buffer[256];
        StringBuilder builder;
        bool should_stop = false;
        for (size_t i = 0; !should_stop; ++i) {
            if (upto.has_value() && i >= upto.value())
                break;

            for (;;) {
                auto length = stream.read({ buffer, sizeof(buffer) });
                if (length == 0) {
                    should_stop = true;
                    break;
                }
                StringView data { buffer, length };
                auto split = data.split_view('\n', true);
                auto last = split.take_last();
                for (auto& entry : split) {
                    builder.append(entry);
                    if (target.has_value()) {
                        m_lines.insert(target.value(), builder.build());
                        target = target.value() + 1;
                    } else {
                        m_lines.append(builder.build());
                    }
                    builder.clear();
                }
                builder.append(last);
            }

            if (builder.length() > 0) {
                if (target.has_value()) {
                    m_lines.insert(target.value(), builder.build());
                    target = target.value() + 1;
                } else {
                    m_lines.append(builder.build());
                }
            }
        }
    }

    void write_to_file(String filename, Range range, bool should_append = false)
    {
        auto fp = fopen(filename.characters(), should_append ? "a+" : "w+");
        if (!fp) {
            perror("fopen");
            return;
        }
        {
            OutputFileStream stream { fp };
            write_to_file(stream, range);
        }
        fclose(fp);
    }

    void write_to_file(OutputStream& stream, Range range)
    {
        auto start = resolve(range.from);
        auto end = resolve(range.to);
        for (size_t i = start; i <= end; ++i) {
            if (!stream.write_or_error(m_lines[i].bytes()))
                return;
            if (!stream.write_or_error({ "\n", 1 }))
                return;
        }
    }

    void set_prompt(String prompt) { m_prompt = move(prompt); }
    void set_address(size_t new_address) { m_current_line = new_address; }

    void quit() { m_should_quit = true; }
    void toggle_prompt() { m_should_show_prompts = !m_should_show_prompts; }

    void set_line(size_t i, String line) { m_lines[i] = move(line); }
    const String& line(size_t i) const { return i < m_lines.size() ? m_lines[i] : m_the_empty_line; }
    const String& last_regex() const { return m_last_lookup_regex; }
    void mark(char mark, size_t address) { m_marks[mark - 'a'] = address; }
    Optional<size_t> mark(char mark) { return m_marks[mark - 'a']; }

private:
    Vector<String> m_lines;
    size_t m_current_line { 0 };
    Vector<String> m_cut_buffer;
    String m_prompt;
    Line::Editor* m_current_editor { nullptr };
    Array<Optional<size_t>, 26> m_marks;
    mutable String m_last_lookup_regex;
    String m_the_empty_line { String::empty() };
    bool m_should_quit { false };
    bool m_should_show_prompts { true };
};

static void print_line(Editor&, size_t current_line, size_t line_index);

struct SetAddressCommand final : public EditorCommand {
    virtual ~SetAddressCommand() { }
    SetAddressCommand(Address address)
    {
        range.to = address;
    }

    virtual void execute(Editor& editor) const override
    {
        auto address = editor.resolve(range.to);
        editor.set_address(address);
    }
};

struct AppendCommand final : public EditorCommand {
    virtual ~AppendCommand() { }
    static Result<NonnullOwnPtr<EditorCommand>, String> parse(GenericLexer& lexer, Optional<Range> range, Address current_address)
    {
        auto cmd = make<AppendCommand>();
        cmd->parse_suffix(lexer);
        if (!range.has_value())
            range = Range { current_address, current_address };
        cmd->range = range.release_value();
        return static_cast<NonnullOwnPtr<EditorCommand>&&>(move(cmd));
    }

    virtual void execute(Editor& editor) const override
    {
        auto address = editor.resolve(range.to);
        auto value = editor.input();
        editor.insert(address, value);
        editor.set_address(address + 1);
    }
};

struct ChangeCommand final : public EditorCommand {
    virtual ~ChangeCommand() { }
    static Result<NonnullOwnPtr<EditorCommand>, String> parse(GenericLexer& lexer, Optional<Range> range, Address current_address)
    {
        auto cmd = make<ChangeCommand>();
        cmd->parse_suffix(lexer);
        if (!range.has_value())
            range = Range { current_address, current_address };
        cmd->range = range.release_value();
        return static_cast<NonnullOwnPtr<EditorCommand>&&>(move(cmd));
    }

    virtual void execute(Editor& editor) const override
    {
        auto start = editor.resolve(range.from);
        auto end = editor.resolve(range.to);
        for (size_t i = start; i <= end; ++i)
            editor.take_line(i);

        auto value = editor.input();
        editor.insert(start, value);
        editor.set_address(start + 1);
    }
};

struct DeleteCommand final : public EditorCommand {
    virtual ~DeleteCommand() { }
    static Result<NonnullOwnPtr<EditorCommand>, String> parse(GenericLexer& lexer, Optional<Range> range, Address current_address)
    {
        auto cmd = make<DeleteCommand>();
        cmd->parse_suffix(lexer);
        if (!range.has_value())
            range = Range { current_address, current_address };
        cmd->range = range.release_value();
        return static_cast<NonnullOwnPtr<EditorCommand>&&>(move(cmd));
    }

    virtual void execute(Editor& editor) const override
    {
        auto start = editor.resolve(range.from);
        auto end = editor.resolve(range.to);
        for (size_t i = start; i <= end; ++i)
            editor.take_line(i);
        if (start == 0 || editor.has_line(start))
            editor.set_address(start);
        else if (start > 0)
            editor.set_address(start - 1);
    }
};

struct EditCommand final : public EditorCommand {
    virtual ~EditCommand() { }
    static Result<NonnullOwnPtr<EditorCommand>, String> parse(GenericLexer& lexer, Optional<Range>, Address, bool force)
    {
        auto cmd = make<EditCommand>();
        lexer.consume_while(is_any_of(" \t"));
        cmd->filename = lexer.consume_until(is_any_of("\n"));
        cmd->force = force;
        return static_cast<NonnullOwnPtr<EditorCommand>&&>(move(cmd));
    }

    virtual void execute(Editor& editor) const override
    {
        if (!force) {
            if (!editor.input("The current buffer will be lost, continue? [y/N] ").to_lowercase().starts_with('y'))
                return;
        }

        editor.read_from_file(filename, {});
    }

    bool force { false };
    String filename;
};

struct FileNameCommand final : public EditorCommand {
    virtual ~FileNameCommand() { }
    static Result<NonnullOwnPtr<EditorCommand>, String> parse(GenericLexer& lexer)
    {
        auto cmd = make<FileNameCommand>();
        lexer.consume_while(is_any_of(" \t"));
        cmd->filename = lexer.consume_until(is_any_of("\n"));
        return static_cast<NonnullOwnPtr<EditorCommand>&&>(move(cmd));
    }

    virtual void execute(Editor&) const override
    {
        warnln("Not yet implemented!");
        return;
    }

    String filename;
};

struct GlobalCommand final : public EditorCommand {
    virtual ~GlobalCommand() { }
    static Vector<String> parse_command_list(GenericLexer& lexer)
    {
        Vector<String> command_list;
        do {
            command_list.append(lexer.consume_until("\\\n"));
        } while (lexer.consume_specific("\\\n"));
        return command_list;
    }
    static Result<NonnullOwnPtr<EditorCommand>, String> parse(GenericLexer& lexer, Optional<Range> range, Address, bool interactive, bool reverse)
    {
        auto cmd = make<GlobalCommand>();
        if (!lexer.consume_specific('/'))
            return String { "Expected '/'" };
        cmd->pattern = lexer.consume_until(is_any_of("/\n"));
        if (!lexer.consume_specific('/'))
            return String { "Expected '/'" };

        if (!interactive)
            cmd->command_list = parse_command_list(lexer);

        if (!range.has_value())
            range = Range { { Address::Kind::FirstLine }, { Address::Kind::LastLine } };
        cmd->range = range.release_value();
        cmd->interactive = interactive;
        cmd->reverse = reverse;

        return static_cast<NonnullOwnPtr<EditorCommand>&&>(move(cmd));
    }

    virtual void execute(Editor& editor) const override
    {
        auto start = editor.resolve(range.from);
        auto end = editor.resolve(range.to);

        Regex<PosixExtended> re(pattern.is_empty() ? editor.last_regex() : pattern);
        if (re.parser_result.error != regex::Error::NoError) {
            warnln(re.error_string());
            return;
        }

        for (size_t i = start; i <= end; ++i) {
            if (!(editor.matches(i, re) ^ reverse))
                continue;

            editor.set_address(i);

            Vector<String> commands;
            if (interactive) {
                print_line(editor, i, i);
                StringBuilder builder;
                for (;;) {
                    auto cmd = editor.input("? ");
                    builder.append(cmd);
                    if (!cmd.ends_with("\\"))
                        break;
                }
                GenericLexer lexer(builder.string_view());
                commands = parse_command_list(lexer);
            } else {
                commands = command_list;
            }

            for (auto& cmd : commands)
                editor.parse_and_run(cmd);
        }
    }

    String pattern;
    Vector<String> command_list;
    bool interactive { false };
    bool reverse { false };
};

struct ToggleHelpCommand final : public EditorCommand {
    virtual ~ToggleHelpCommand() { }
    static Result<NonnullOwnPtr<EditorCommand>, String> parse(GenericLexer& lexer)
    {
        auto cmd = make<ToggleHelpCommand>();
        cmd->parse_suffix(lexer);
        return static_cast<NonnullOwnPtr<EditorCommand>&&>(move(cmd));
    }

    virtual void execute(Editor&) const override { }
};

struct PrintHelpCommand final : public EditorCommand {
    virtual ~PrintHelpCommand() { }
    static Result<NonnullOwnPtr<EditorCommand>, String> parse(GenericLexer& lexer)
    {
        auto cmd = make<PrintHelpCommand>();
        cmd->parse_suffix(lexer);
        return static_cast<NonnullOwnPtr<EditorCommand>&&>(move(cmd));
    }

    virtual void execute(Editor&) const override
    {
    }
};

struct InsertCommand final : public EditorCommand {
    virtual ~InsertCommand() { }
    static Result<NonnullOwnPtr<EditorCommand>, String> parse(GenericLexer& lexer, Optional<Range> range, Address current_address)
    {
        auto cmd = make<InsertCommand>();
        if (!range.has_value())
            range = Range { current_address, current_address };
        cmd->range = range.release_value();
        cmd->parse_suffix(lexer);
        return static_cast<NonnullOwnPtr<EditorCommand>&&>(move(cmd));
    }

    virtual void execute(Editor& editor) const override
    {
        auto address = editor.resolve(range.to);
        if (address > 0)
            --address;
        auto value = editor.input();
        editor.insert(address, value);
        editor.set_address(address + 1);
    }
};

struct JoinCommand final : public EditorCommand {
    virtual ~JoinCommand() { }
    static Result<NonnullOwnPtr<EditorCommand>, String> parse(GenericLexer& lexer, Optional<Range> range, Address current_address)
    {
        auto cmd = make<JoinCommand>();
        if (!range.has_value())
            range = Range { current_address, { current_address.kind, current_address.offset + 1, current_address.mark, current_address.regex } };
        cmd->range = range.release_value();
        cmd->parse_suffix(lexer);
        return static_cast<NonnullOwnPtr<EditorCommand>&&>(move(cmd));
    }

    virtual void execute(Editor& editor) const override
    {
        auto start = editor.resolve(range.from);
        auto end = editor.resolve(range.to);

        StringBuilder builder;
        auto first = true;
        auto count = end - start;
        for (size_t i = 0; i <= count; ++i) {
            if (first)
                first = false;
            else
                builder.append(' ');
            auto line = editor.take_line(start);
            builder.append(line);
            dbgln("Took line {} end={}", line, end);
        }

        editor.insert(start, builder.to_string());
        editor.set_address(start);
    }
};

struct MarkCommand final : public EditorCommand {
    virtual ~MarkCommand() { }
    static Result<NonnullOwnPtr<EditorCommand>, String> parse(GenericLexer& lexer, Optional<Range> range, Address current_address)
    {
        auto cmd = make<MarkCommand>();
        if (!range.has_value())
            range = Range { current_address, current_address };
        cmd->range = range.release_value();
        cmd->mark = lexer.consume();
        if (!islower(cmd->mark))
            return String::formatted("Expected a lowercase letter, '{}' is not a lowercase letter", cmd->mark);
        cmd->parse_suffix(lexer);
        return static_cast<NonnullOwnPtr<EditorCommand>&&>(move(cmd));
    }

    virtual void execute(Editor& editor) const override
    {
        auto address = editor.resolve(range.to);
        editor.mark(mark, address);
    }

    char mark { 0 };
};

struct MoveCommand final : public EditorCommand {
    virtual ~MoveCommand() { }
    static Result<NonnullOwnPtr<EditorCommand>, String> parse(GenericLexer& lexer, Optional<Range> range, Address current_address)
    {
        auto cmd = make<MoveCommand>();
        if (!range.has_value())
            range = Range { current_address, current_address };
        cmd->range = range.release_value();
        auto address = parse_address(lexer);
        if (!address.has_value())
            return String::formatted("Expected an address but got {}", lexer.peek());
        cmd->target_address = address.release_value();
        cmd->parse_suffix(lexer);
        return static_cast<NonnullOwnPtr<EditorCommand>&&>(move(cmd));
    }

    virtual void execute(Editor& editor) const override
    {
        auto start = editor.resolve(range.from);
        auto end = editor.resolve(range.to);

        Vector<String> lines;
        auto count = end - start;
        for (size_t i = 0; i <= count; ++i) {
            auto line = editor.take_line(start);
            lines.append(move(line));
        }

        auto target = editor.resolve(target_address);
        for (auto& line : lines)
            editor.insert(target++, line);
        editor.set_address(target);
    }

    Address target_address;
};

struct PrintCommand final : public EditorCommand {
    virtual ~PrintCommand() { }
    enum class Mode {
        Unambiguous,
        WithLineNumbers,
        Normal
    } mode { Mode::Normal };

    static Result<NonnullOwnPtr<EditorCommand>, String> parse(GenericLexer& lexer, Optional<Range> range, Address current_address, Mode mode)
    {
        auto cmd = make<PrintCommand>();
        cmd->mode = mode;
        if (!range.has_value())
            range = Range { current_address, current_address };
        cmd->range = range.release_value();
        cmd->parse_suffix(lexer);
        return static_cast<NonnullOwnPtr<EditorCommand>&&>(move(cmd));
    }

    virtual void execute(Editor& editor) const override
    {
        auto start = editor.resolve(range.from);
        auto end = editor.resolve(range.to);
        for (size_t i = start; i <= end; ++i) {
            switch (mode) {
            case Mode::Unambiguous: // FIXME: what even is this?
            case Mode::Normal:
                outln("{}", editor.line(i));
                break;
            case Mode::WithLineNumbers:
                outln("{: >{1}}{2}", i, (size_t)log10(end) + 1, editor.line(i));
                break;
            default:
                ASSERT_NOT_REACHED();
            }
        }
        editor.set_address(end);
    }
};

struct ToggleCommandPrompt final : public EditorCommand {
    virtual ~ToggleCommandPrompt() { }
    static Result<NonnullOwnPtr<EditorCommand>, String> parse(GenericLexer& lexer)
    {
        auto cmd = make<ToggleCommandPrompt>();
        cmd->parse_suffix(lexer);
        return static_cast<NonnullOwnPtr<EditorCommand>&&>(move(cmd));
    }

    virtual void execute(Editor& editor) const override
    {
        editor.toggle_prompt();
    }
};

struct QuitCommand final : public EditorCommand {
    virtual ~QuitCommand() { }
    static Result<NonnullOwnPtr<EditorCommand>, String> parse(GenericLexer& lexer, bool force)
    {
        auto cmd = make<QuitCommand>();
        cmd->force = force;
        cmd->parse_suffix(lexer);
        return static_cast<NonnullOwnPtr<EditorCommand>&&>(move(cmd));
    }

    virtual void execute(Editor& editor) const override
    {
        if (!force) {
            if (!editor.input("Really quit? [y/N] ").to_lowercase().starts_with('y'))
                return;
        }
        editor.quit();
    }

    bool force { false };
};

struct ReadCommand final : public EditorCommand {
    virtual ~ReadCommand() { }
    static Result<NonnullOwnPtr<EditorCommand>, String> parse(GenericLexer& lexer, Optional<Range> range, Address)
    {
        auto cmd = make<ReadCommand>();
        if (!range.has_value())
            range = Range { { Address::Kind::LastLine }, { Address::Kind::LastLine } };
        cmd->range = range.release_value();

        lexer.consume_while(is_any_of(" \t"));
        cmd->filename = lexer.consume_until('\n');
        lexer.consume_specific('\n');
        return static_cast<NonnullOwnPtr<EditorCommand>&&>(move(cmd));
    }

    virtual void execute(Editor& editor) const override
    {
        editor.read_from_file(filename, {}, false, editor.resolve(range.to));
    }

    String filename;
};

struct SubstituteCommand final : public EditorCommand {
    virtual ~SubstituteCommand() { }
    static Result<NonnullOwnPtr<EditorCommand>, String> parse(GenericLexer& lexer, Optional<Range> range, Address current_address)
    {
        // FIXME: Implement s/re/repl/<n>
        // FIXME: Implement s
        auto cmd = make<SubstituteCommand>();
        if (!lexer.consume_specific('/'))
            return String { "Expected '/'" };
        cmd->pattern = lexer.consume_until(is_any_of("/\n"));
        if (!lexer.consume_specific('/'))
            return String { "Expected '/'" };

        cmd->replacement = lexer.consume_until(is_any_of("/\n"));
        if (!lexer.consume_specific('/'))
            return String { "Expected '/'" };

        for (bool stop = false; !stop;) {
            switch (lexer.peek()) {
            case 'g':
                lexer.consume();
                cmd->flags |= PosixFlags::Global;
                break;
            case 'i':
                lexer.consume();
                cmd->flags |= PosixFlags::Insensitive;
                break;
            default:
                stop = true;
                break;
            }
        }

        if (!range.has_value())
            range = Range { current_address, current_address };
        cmd->range = range.release_value();
        cmd->parse_suffix(lexer);

        return static_cast<NonnullOwnPtr<EditorCommand>&&>(move(cmd));
    }

    virtual void execute(Editor& editor) const override
    {
        auto start = editor.resolve(range.from);
        auto end = editor.resolve(range.to);

        Regex<PosixExtended> re(pattern.is_empty() ? editor.last_regex() : pattern, flags);
        if (re.parser_result.error != regex::Error::NoError) {
            warnln(re.error_string());
            return;
        }

        for (size_t i = start; i <= end; ++i) {
            if (!editor.matches(i, re))
                continue;

            editor.set_address(i);
            editor.set_line(i, re.replace(editor.line(i), replacement));
        }
    }

    String pattern;
    String replacement;
    PosixOptions flags;
};

struct TransferCommand final : public EditorCommand {
    virtual ~TransferCommand() { }
    static Result<NonnullOwnPtr<EditorCommand>, String> parse(GenericLexer& lexer, Optional<Range> range, Address current_address)
    {
        auto cmd = make<TransferCommand>();
        if (!range.has_value())
            range = Range { current_address, current_address };
        cmd->range = range.release_value();
        auto address = parse_address(lexer);
        if (!address.has_value())
            return String::formatted("Expected an address but got {}", lexer.peek());
        cmd->target_address = address.release_value();
        cmd->parse_suffix(lexer);
        return static_cast<NonnullOwnPtr<EditorCommand>&&>(move(cmd));
    }

    virtual void execute(Editor& editor) const override
    {
        auto start = editor.resolve(range.from);
        auto end = editor.resolve(range.to);

        Vector<String> lines;
        auto count = end - start;
        for (size_t i = 0; i <= count; ++i) {
            auto line = editor.line(start);
            lines.append(move(line));
        }

        auto target = editor.resolve(target_address);
        for (auto& line : lines)
            editor.insert(target++, line);
        editor.set_address(target);
    }

    Address target_address;
};

struct WriteCommand final : public EditorCommand {
    virtual ~WriteCommand() { }
    static Result<NonnullOwnPtr<EditorCommand>, String> parse(GenericLexer& lexer, Optional<Range> range, Address, bool append)
    {
        auto cmd = make<WriteCommand>();
        if (!range.has_value())
            range = Range { { Address::Kind::FirstLine }, { Address::Kind::LastLine } };
        cmd->range = range.release_value();
        lexer.consume_while(is_any_of("\t "));
        cmd->filename = lexer.consume_until('\n');
        cmd->append = append;
        return static_cast<NonnullOwnPtr<EditorCommand>&&>(move(cmd));
    }

    virtual void execute(Editor& editor) const override
    {
        editor.write_to_file(filename, range, append);
    }

    String filename;
    bool append { false };
};

struct PutCommand final : public EditorCommand {
    virtual ~PutCommand() { }
    static Result<NonnullOwnPtr<EditorCommand>, String> parse(GenericLexer& lexer, Optional<Range> range, Address current_address)
    {
        auto cmd = make<PutCommand>();
        if (!range.has_value())
            range = Range { current_address, current_address };
        cmd->range = range.release_value();
        cmd->parse_suffix(lexer);
        return static_cast<NonnullOwnPtr<EditorCommand>&&>(move(cmd));
    }

    virtual void execute(Editor& editor) const override
    {
        auto last_line = editor.put(range.to);
        editor.set_address(last_line);
    }
};

struct YankCommand final : public EditorCommand {
    virtual ~YankCommand() { }
    static Result<NonnullOwnPtr<EditorCommand>, String> parse(GenericLexer& lexer, Optional<Range> range, Address current_address)
    {
        auto cmd = make<YankCommand>();
        if (!range.has_value())
            range = Range { current_address, current_address };
        cmd->range = range.release_value();
        cmd->parse_suffix(lexer);
        return static_cast<NonnullOwnPtr<EditorCommand>&&>(move(cmd));
    }

    virtual void execute(Editor& editor) const override
    {
        editor.yank(range);
    }
};

struct ResolveCommand final : public EditorCommand {
    virtual ~ResolveCommand() { }
    static Result<NonnullOwnPtr<EditorCommand>, String> parse(GenericLexer& lexer, Optional<Range> range, Address current_address)
    {
        auto cmd = make<ResolveCommand>();
        if (!range.has_value())
            range = Range { current_address, { Address::Kind::LastLine } };
        cmd->range = range.release_value();
        cmd->parse_suffix(lexer);
        return static_cast<NonnullOwnPtr<EditorCommand>&&>(move(cmd));
    }

    virtual void execute(Editor& editor) const override
    {
        auto address = editor.resolve(range.to);
        outln("{}", address);
    }
};

struct NewlineCommand final : public EditorCommand {
    virtual ~NewlineCommand() { }
    static Result<NonnullOwnPtr<EditorCommand>, String> parse(GenericLexer&, Optional<Range> range, Address current_address)
    {
        auto cmd = make<ResolveCommand>();
        if (!range.has_value())
            range = Range { current_address, { current_address.kind, current_address.offset + 1, current_address.mark, current_address.regex } };
        cmd->range = range.release_value();
        return static_cast<NonnullOwnPtr<EditorCommand>&&>(move(cmd));
    }

    virtual void execute(Editor& editor) const override
    {
        auto line = editor.resolve(range.to);
        print_line(editor, editor.resolve(range.from), line);
        editor.set_address(line);
    }
};

Result<NonnullOwnPtr<EditorCommand>, String> Editor::parse(String command)
{
    GenericLexer lexer { command };
try_again:;
    auto range_option = parse_range(lexer);
    if (lexer.is_eof()) {
        if (range_option.has_value())
            return static_cast<NonnullOwnPtr<EditorCommand>>(make<SetAddressCommand>(range_option.release_value().to));
        else
            return String { "Expected at least an address" };
    }
    char c;
    switch (c = lexer.consume()) {
    case 'a':
        return AppendCommand::parse(lexer, move(range_option), address(m_current_line));
    case 'c':
        return ChangeCommand::parse(lexer, move(range_option), address(m_current_line));
    case 'd':
        return DeleteCommand::parse(lexer, move(range_option), address(m_current_line));
    case 'e':
        return EditCommand::parse(lexer, move(range_option), address(m_current_line), false);
    case 'E':
        return EditCommand::parse(lexer, move(range_option), address(m_current_line), true);
    case 'f':
        return FileNameCommand::parse(lexer);
    case 'g':
        return GlobalCommand::parse(lexer, move(range_option), address(m_current_line), false, false);
    case 'G':
        return GlobalCommand::parse(lexer, move(range_option), address(m_current_line), true, false);
    case 'H':
        return ToggleHelpCommand::parse(lexer);
    case 'h':
        return PrintHelpCommand::parse(lexer);
    case 'i':
        return InsertCommand::parse(lexer, move(range_option), address(m_current_line));
    case 'j':
        return JoinCommand::parse(lexer, move(range_option), address(m_current_line));
    case 'k':
        return MarkCommand::parse(lexer, move(range_option), address(m_current_line));
    case 'l':
        return PrintCommand::parse(lexer, move(range_option), address(m_current_line), PrintCommand::Mode::Unambiguous);
    case 'm':
        return MoveCommand::parse(lexer, move(range_option), address(m_current_line));
    case 'n':
        return PrintCommand::parse(lexer, move(range_option), address(m_current_line), PrintCommand::Mode::WithLineNumbers);
    case 'p':
        return PrintCommand::parse(lexer, move(range_option), address(m_current_line), PrintCommand::Mode::Normal);
    case 'P':
        return ToggleCommandPrompt::parse(lexer);
    case 'q':
        return QuitCommand::parse(lexer, false);
    case 'Q':
        return QuitCommand::parse(lexer, true);
    case 'r':
        return ReadCommand::parse(lexer, move(range_option), address(m_current_line));
    case 's':
        return SubstituteCommand::parse(lexer, move(range_option), address(m_current_line));
    case 't':
        return TransferCommand::parse(lexer, move(range_option), address(m_current_line));
    case 'v':
        return GlobalCommand::parse(lexer, move(range_option), address(m_current_line), false, true);
    case 'V':
        return GlobalCommand::parse(lexer, move(range_option), address(m_current_line), true, true);
    case 'w':
        return WriteCommand::parse(lexer, move(range_option), address(m_current_line), false);
    case 'W':
        return WriteCommand::parse(lexer, move(range_option), address(m_current_line), true);
    case 'x':
        return PutCommand::parse(lexer, move(range_option), address(m_current_line));
    case 'y':
        return YankCommand::parse(lexer, move(range_option), address(m_current_line));
    case '#':
        lexer.consume_until(is_any_of("\n"));
        lexer.consume_specific('\n');
        goto try_again;
    case '=':
        return ResolveCommand::parse(lexer, move(range_option), address(m_current_line));
    case '\n':
        return NewlineCommand::parse(lexer, move(range_option), address(m_current_line));
    default:
        return String::formatted("Unknown command '{}'", c);
    }
}

void print_line(Editor& editor, size_t current_line, size_t line)
{
    auto cmd = make<PrintCommand>();
    cmd->mode = PrintCommand::Mode::Normal;
    cmd->range = { { Address::Kind::FirstLine, (off_t)line }, { Address::Kind::FirstLine, (off_t)line } };
    cmd->execute(editor);
    editor.set_address(current_line);
}

int main(int argc, char** argv)
{
    // Might need to fork()+exec() for 'r !cmd'.
    if (pledge("stdio rpath wpath cpath unix fattr tty sigaction", nullptr) < 0)
        return 0;

    const char* prompt = "> ";
    const char* file_to_edit = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_option(prompt, "Override command prompt", "prompt", 'p', "prompt");
    args_parser.add_positional_argument(file_to_edit, "File to edit", "file", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    auto libline_config = Line::Configuration::from_config();

    Editor ed { file_to_edit };
    ed.set_prompt(prompt);

    if (!isatty(STDIN_FILENO))
        libline_config.set(Line::Configuration::NonInteractive);

    Core::EventLoop loop;

    auto line_editor = Line::Editor::construct(move(libline_config));
    ed.repl(*line_editor);

    return 0;
}
