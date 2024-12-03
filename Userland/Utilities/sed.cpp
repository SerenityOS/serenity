/*
 * Copyright (c) 2022, Eli Youngs <eli.m.youngs@gmail.com>
 * Copyright (c) 2023, Rodrigo Tobar <rtobarc@gmail.com>
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/Format.h>
#include <AK/GenericLexer.h>
#include <AK/LexicalPath.h>
#include <AK/Optional.h>
#include <AK/String.h>
#include <AK/Variant.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibFileSystem/FileSystem.h>
#include <LibFileSystem/TempFile.h>
#include <LibMain/Main.h>
#include <LibRegex/RegexMatcher.h>
#include <LibRegex/RegexOptions.h>

class SedError {
public:
    SedError() = default;
    SedError(String&& message)
        : m_message(move(message))
    {
    }

    SedError(Error const& error)
    {
        *this = formatted("Internal sed error: {}", error.string_literal());
    }

    String const& message() const { return m_message; }

    template<typename... Parameters>
    static SedError formatted(CheckedFormatString<Parameters...>&& fmtstr, Parameters const&... parameters)
    {
        return maybe_with_string(String::formatted(move(fmtstr), parameters...));
    }

    static SedError parsing_error(GenericLexer const& lexer, StringView message)
    {
        return parsing_error(lexer, "{}", message);
    }

    template<typename... Parameters>
    static SedError parsing_error(GenericLexer const& lexer, CheckedFormatString<Parameters...>&& fmtstr, Parameters const&... parameters)
    {
        StringBuilder builder;
        builder.appendff("Parsing error at position {}: ", lexer.tell());
        builder.appendff(move(fmtstr), parameters...);
        return maybe_with_string(String::from_utf8(builder.string_view()));
    }

    static SedError from_error(Error const& error)
    {
        return formatted("Internal sed error: {}", error.string_literal());
    }

private:
    String m_message;

    static SedError maybe_with_string(ErrorOr<String> maybe_string)
    {
        if (maybe_string.is_error())
            return SedError {};
        return SedError { maybe_string.release_value() };
    }
};

template<typename T>
using SedErrorOr = ErrorOr<T, SedError>;

// function, maximum addresses
#define ENUMERATE_FUNCTIONS(F) \
    F('a', 1)                  \
    F('b', 2)                  \
    F('c', 2)                  \
    F('d', 2)                  \
    F('D', 2)                  \
    F('g', 2)                  \
    F('G', 2)                  \
    F('h', 2)                  \
    F('H', 2)                  \
    F('i', 1)                  \
    F('l', 2)                  \
    F('n', 2)                  \
    F('N', 2)                  \
    F('p', 2)                  \
    F('P', 2)                  \
    F('q', 1)                  \
    F('r', 1)                  \
    F('s', 2)                  \
    F('t', 2)                  \
    F('w', 2)                  \
    F('x', 2)                  \
    F('y', 2)                  \
    F(':', 0)                  \
    F('=', 1)                  \
    F('#', 0)

enum class AddressType {
    Unset,
    Line,
    LastLine,
    ContextAddress,
};

class Address {
public:
    Address() = default;

    explicit Address(size_t line)
        : m_line_number(line)
        , m_address_type(AddressType::Line)
    {
    }

    explicit Address(AddressType address_type)
        : m_address_type(address_type)
    {
        VERIFY(address_type == AddressType::LastLine || address_type == AddressType::ContextAddress);
    }

    size_t line_number() const
    {
        VERIFY(m_address_type == AddressType::Line);
        return m_line_number;
    }

    AddressType address_type() const { return m_address_type; }

    bool matches([[maybe_unused]] StringView pattern_space, size_t line_number, bool is_last_line) const
    {
        switch (m_address_type) {
        case AddressType::Line:
            return line_number == m_line_number;
        case AddressType::LastLine:
            return is_last_line;
        default:
            warnln("Addressing type not implemented: {}", int(m_address_type));
            return false;
        }
    }

private:
    size_t m_line_number { 0 };
    AddressType m_address_type { AddressType::Unset };
};

namespace AK {
template<>
class Formatter<Address> : public StandardFormatter {
public:
    AK::ErrorOr<void> format(FormatBuilder& format_builder, Address address)
    {
        auto& builder = format_builder.builder();
        switch (address.address_type()) {
        case AddressType::Line:
            builder.appendff("{}", address.line_number());
            break;
        case AddressType::LastLine:
            builder.append('$');
            break;
        case AddressType::ContextAddress:
            VERIFY_NOT_REACHED();
        case AddressType::Unset:
            break;
        }
        return {};
    }
};
}

static bool is_command_separator(char c)
{
    return c == '\n' || c == ';';
}

template<typename ArgsT>
struct TextArgument {
    String text;

    static SedErrorOr<ArgsT> parse(GenericLexer& lexer)
    {
        auto original_text = lexer.consume_until([is_escape_sequence = false](char c) mutable {
            if (c == '\n' && !is_escape_sequence)
                return true;
            is_escape_sequence = c == '\\';
            return false;
        });
        if (!original_text.starts_with("\\\n"sv))
            return SedError::parsing_error(lexer, "Command should be followed by \\ + \\n"sv);
        auto text = TRY(String::from_utf8(original_text.substring_view(2)));
        return ArgsT { TRY(text.replace("\\\n"sv, "\n"sv, AK::ReplaceMode::All)) };
    }
};

template<typename ArgsT>
struct OptionalLabelArgument {
    Optional<StringView> label;

    static SedErrorOr<ArgsT> parse(GenericLexer& lexer)
    {
        auto blanks = lexer.consume_while(is_ascii_blank);
        if (blanks.is_empty())
            return SedError::parsing_error(lexer, "expected one or more blank characters"sv);
        if (lexer.next_is(is_command_separator))
            return ArgsT {};
        return ArgsT { lexer.consume_until(is_command_separator) };
    }
};

template<typename ArgsT>
struct FilepathArgument {
    static SedErrorOr<ArgsT> parse(GenericLexer& lexer)
    {
        lexer.consume_while(is_ascii_blank);
        auto filepath = lexer.consume_until(is_command_separator);
        if (filepath.is_empty())
            return SedError::parsing_error(lexer, "input filename expected, none found");
        return ArgsT { {}, filepath };
    }
};

struct AArguments : TextArgument<AArguments> { };

struct BArguments : OptionalLabelArgument<BArguments> { };

struct CArguments : TextArgument<CArguments> { };

struct IArguments : TextArgument<IArguments> { };

struct RArguments : FilepathArgument<RArguments> {
    StringView input_filepath;
};

struct SArguments {
    Regex<PosixExtended> regex;
    StringView replacement;
    PosixOptions options;
    bool print;
    Optional<StringView> output_filepath;

    static SedErrorOr<SArguments> parse(GenericLexer& lexer)
    {
        auto generic_error_message = "Incomplete substitution command"sv;

        if (lexer.is_eof())
            return SedError::parsing_error(lexer, generic_error_message);

        auto delimiter = lexer.consume();
        if (delimiter == '\n' || delimiter == '\\')
            return SedError::parsing_error(lexer, "\\n and \\ cannot be used as delimiters."sv);

        auto pattern = lexer.consume_until([is_escape_sequence = false, delimiter](char c) mutable {
            if (c == delimiter && !is_escape_sequence)
                return true;
            is_escape_sequence = c == '\\' && !is_escape_sequence;
            return false;
        });

        if (pattern.is_empty())
            return SedError::parsing_error(lexer, "Substitution patterns cannot be empty."sv);

        if (!lexer.consume_specific(delimiter))
            return SedError::parsing_error(lexer, generic_error_message);

        auto replacement = lexer.consume_until([is_escape_sequence = false, delimiter](char c) mutable {
            if (c == delimiter && !is_escape_sequence)
                return true;
            is_escape_sequence = c == '\\' && !is_escape_sequence;
            return false;
        });

        // According to Posix, "s/x/y" is an invalid substitution command.
        // It must have a closing delimiter: "s/x/y/"
        if (!lexer.consume_specific(delimiter))
            return SedError::parsing_error(lexer, "The substitution command was not properly terminated."sv);

        PosixOptions options = PosixOptions(PosixFlags::Global | PosixFlags::SingleMatch);
        bool print = false;
        Optional<StringView> output_filepath;

        auto flags = split_flags(lexer);
        for (auto const& flag : flags) {
            if (flag.starts_with('w')) {
                auto flag_filepath = flag.substring_view(1).trim_whitespace();
                if (flag_filepath.is_empty())
                    return SedError::parsing_error(lexer, "No filepath was provided for the 'w' flag."sv);
                output_filepath = flag_filepath;
            } else if (flag == "g"sv) {
                // Allow multiple matches per line by un-setting the SingleMatch flag
                options &= ~PosixFlags::SingleMatch;
            } else if (flag == "i"sv || flag == "I"sv) {
                options |= PosixFlags::Insensitive;
            } else if (flag == "p"sv) {
                print = true;
            } else {
                return SedError::parsing_error(lexer, "Unsupported flag for s command: {}", flag);
            }
        }

        return SArguments { Regex<PosixExtended> { pattern }, replacement, options, print, output_filepath };
    }

private:
    static Vector<StringView> split_flags(GenericLexer& lexer)
    {
        Vector<StringView> flags;

        while (!lexer.is_eof() && !lexer.next_is(is_command_separator)) {
            StringView flag;

            if (lexer.next_is(is_ascii_digit)) {
                flag = lexer.consume_while(is_ascii_digit);
            } else if (lexer.peek() == 'w') {
                flag = lexer.consume_until(is_command_separator);
            } else {
                flag = lexer.consume(1);
            }

            flags.append(flag);
        }

        return flags;
    }
};

struct TArguments : OptionalLabelArgument<TArguments> { };

struct WArguments : FilepathArgument<WArguments> {
    StringView output_filepath;
};

struct YArguments {
    StringView characters;
    StringView replacements;

    static SedErrorOr<YArguments> parse(GenericLexer& lexer)
    {
        auto generic_error_message = "Incomplete transform command"sv;

        if (lexer.is_eof())
            return SedError::parsing_error(lexer, generic_error_message);

        auto delimiter = lexer.consume();
        if (delimiter == '\\' || delimiter == '\n')
            return SedError::parsing_error(lexer, "\\n and \\ cannot be used as delimiters."sv);

        auto characters = lexer.consume_until([is_escape_sequence = false, delimiter](char c) mutable {
            if (c == delimiter && !is_escape_sequence)
                return true;
            is_escape_sequence = c == '\\' && !is_escape_sequence;
            return false;
        });

        if (!lexer.consume_specific(delimiter))
            return SedError::parsing_error(lexer, generic_error_message);

        auto replacements = lexer.consume_until([is_escape_sequence = false, delimiter](char c) mutable {
            if (c == delimiter && !is_escape_sequence)
                return true;
            is_escape_sequence = c == '\\' && !is_escape_sequence;
            return false;
        });

        if (characters.length() != replacements.length())
            return SedError::parsing_error(lexer, "Transform strings are not the same length.");

        if (!lexer.consume_specific(delimiter))
            return SedError::parsing_error(lexer, "The transform command was not properly terminated."sv);

        return YArguments {
            .characters = characters,
            .replacements = replacements
        };
    }
};

struct ColonArguments {
    StringView label;

    static SedErrorOr<ColonArguments> parse(GenericLexer& lexer)
    {
        ColonArguments args {};
        args.label = lexer.consume_until(is_command_separator);
        if (args.label.is_empty())
            return SedError::parsing_error(lexer, "label expected, none found");
        return args;
    }
};

struct Command {
    Address address1;
    Address address2;
    char function = '\0';
    Optional<Variant<AArguments, BArguments, CArguments, IArguments, RArguments, SArguments, TArguments, WArguments, YArguments, ColonArguments>> arguments;
    StringView arguments_view;

    void enable_for(StringView pattern_space, size_t line_number, bool is_last_line)
    {
        if (function == '#') {
            m_is_enabled = false;
            return;
        }
        m_is_enabled = selects(pattern_space, line_number, is_last_line);
    }

    bool is_enabled() const { return m_is_enabled; }

private:
    bool selects(StringView pattern_space, size_t line_number, bool is_last_line)
    {
        // no address set, all patterns match
        if (address1.address_type() == AddressType::Unset) {
            VERIFY(address2.address_type() == AddressType::Unset);
            return true;
        }

        // single address set
        if (address2.address_type() == AddressType::Unset)
            return address1.matches(pattern_space, line_number, is_last_line);

        // two addresses
        if (!m_is_selection_active && address1.matches(pattern_space, line_number, is_last_line)) {
            m_is_selection_active = true;
            return true;
        }
        if (m_is_selection_active && address2.matches(pattern_space, line_number, is_last_line)) {
            m_is_selection_active = false;
            return true;
        }
        return false;
    }

    bool m_is_enabled { false };
    bool m_is_selection_active { false };
};

namespace AK {
template<>
class Formatter<Command> : public StandardFormatter {
public:
    AK::ErrorOr<void> format(FormatBuilder& format_builder, Command const& command)
    {
        auto& builder = format_builder.builder();
        builder.appendff("{}", command.address1);
        if (command.address2.address_type() != AddressType::Unset) {
            builder.appendff(",{}", command.address2);
        }
        builder.append(command.function);
        builder.append(command.arguments_view);
        return {};
    }
};
}

struct AddressParsingResult {
    Optional<Address> address;
};

static Optional<Address> parse_address(GenericLexer& lexer)
{
    if (lexer.is_eof())
        return {};

    if (lexer.peek() == '$') {
        lexer.consume();
        return Address { AddressType::LastLine };
    }

    auto lineno = lexer.consume_while(AK::is_ascii_digit);
    if (lineno.is_empty())
        return {};
    return Address { AK::StringUtils::convert_to_uint<size_t>(lineno).release_value() };
}

template<int max_addresses>
static SedErrorOr<void> verify_number_of_addresses(Command const& command)
{
    if constexpr (max_addresses == 2) {
        return {};
    } else {
        static_assert(max_addresses == 0 || max_addresses == 1);
        auto c = command.function;
        if constexpr (max_addresses == 0) {
            if (command.address1.address_type() != AddressType::Unset) {
                return SedError::formatted("'{}' doesn't take any address, at least one given", c);
            }
        } else {
            if (command.address2.address_type() != AddressType::Unset) {
                return SedError::formatted("'{}' takes a single address, two given", c);
            }
        }
    }
    return {};
}

static SedErrorOr<Command> parse_command(GenericLexer& lexer)
{
    lexer.consume_while(is_ascii_blank);

    Command command;
    command.address1 = parse_address(lexer).value_or({});
    if (lexer.is_eof())
        return SedError::parsing_error(lexer, "Incomplete command"sv);
    if (lexer.peek() == ',') {
        lexer.consume();
        command.address2 = parse_address(lexer).value_or({});
    }
    if (lexer.is_eof())
        return SedError::parsing_error(lexer, "Incomplete command"sv);

    char command_char = lexer.consume();

#define HANDLE_FUNCTION_CASE(c, max_addresses)                   \
    case c:                                                      \
        command.function = c;                                    \
        TRY(verify_number_of_addresses<max_addresses>(command)); \
        break;

    switch (command_char) {
        ENUMERATE_FUNCTIONS(HANDLE_FUNCTION_CASE)
    default:
        return SedError::parsing_error(lexer, "Unknown function command '{}'", command_char);
    }
#undef HANDLE_FUNCTION_CASE

    auto args_start = lexer.tell();
    switch (command_char) {
    case 'a':
        command.arguments = TRY(AArguments::parse(lexer));
        break;
    case 'b':
        command.arguments = TRY(BArguments::parse(lexer));
        break;
    case 'c':
        command.arguments = TRY(CArguments::parse(lexer));
        break;
    case 'i':
        command.arguments = TRY(IArguments::parse(lexer));
        break;
    case 'r':
        command.arguments = TRY(RArguments::parse(lexer));
        break;
    case 's':
        command.arguments = TRY(SArguments::parse(lexer));
        break;
    case 't':
        command.arguments = TRY(TArguments::parse(lexer));
        break;
    case 'w':
        command.arguments = TRY(WArguments::parse(lexer));
        break;
    case 'y':
        command.arguments = TRY(YArguments::parse(lexer));
        break;
    case ':':
        command.arguments = TRY(ColonArguments::parse(lexer));
        break;
    case '#':
        lexer.consume_until('\n');
        break;
    default: {
        auto padding = lexer.consume_until(is_command_separator);
        if (!padding.is_whitespace()) {
            warnln("Command had arguments but none were expected, ignoring: '{}'", padding);
        }
    }
    }

    auto args_end = lexer.tell();
    VERIFY(args_end >= args_start);
    auto args_length = args_end - args_start;
    lexer.retreat(args_length);
    command.arguments_view = lexer.consume(args_length);
    return command;
}

class Script {
public:
    [[nodiscard]] bool add_script_part(StringView data)
    {
        auto last_pos = m_script.length();
        m_script.append(data);
        auto lexer = GenericLexer(m_script.string_view().substring_view(last_pos));
        while (!lexer.is_eof()) {
            if (lexer.is_eof())
                break;
            auto maybe_command = parse_command(lexer);
            if (maybe_command.is_error()) {
                warnln("Problem while parsing script part: {}", maybe_command.release_error().message());
                return false;
            };
            m_commands.append(maybe_command.release_value());
            lexer.consume_until(is_command_separator);
            if (lexer.is_eof())
                break;
            lexer.consume();
        }
        return true;
    }

    Vector<Command>& commands() { return m_commands; }

    ErrorOr<Vector<String>> output_filenames() const
    {
        Vector<String> output_filenames;
        for (auto const& command : m_commands) {
            if (!command.arguments.has_value())
                continue;
            if (command.arguments->has<SArguments>()) {
                auto const& s_arguments = command.arguments->get<SArguments>();
                if (s_arguments.output_filepath.has_value()) {
                    TRY(add(output_filenames, s_arguments.output_filepath.value()));
                }
            } else if (command.arguments->has<WArguments>()) {
                TRY(add(output_filenames, command.arguments->get<WArguments>().output_filepath));
            }
        }
        return output_filenames;
    }

    ErrorOr<Vector<String>> input_filenames() const
    {
        Vector<String> input_filenames;
        for (auto const& command : m_commands) {
            if (!command.arguments.has_value()) {
                continue;
            }
            if (command.arguments->has<RArguments>()) {
                TRY(add(input_filenames, command.arguments->get<RArguments>().input_filepath));
            }
        }
        return input_filenames;
    }

private:
    StringBuilder m_script;
    Vector<Command> m_commands;

    ErrorOr<void> add(Vector<String>& container, StringView element_sv) const
    {
        auto element = TRY(String::from_utf8(element_sv));
        TRY(container.try_append(move(element)));
        return {};
    }
};

enum class CycleDecision {
    None,
    Next,
    Quit
};

// In most cases, just an input to sed. However, files are also written to when the -i option is used.
class File {
    AK_MAKE_NONCOPYABLE(File);
    AK_MAKE_DEFAULT_MOVABLE(File);

public:
    // Used for -i mode.
    static ErrorOr<File> create_with_output_file(LexicalPath input_path, NonnullOwnPtr<Core::File>&& file)
    {
        auto buffered_file = TRY(Core::InputBufferedFile::create(move(file)));
        auto temp_file = TRY(FileSystem::TempFile::create_temp_file());
        // Open the file as read-write, since we need to later copy its contents to the original file.
        auto output_file = TRY(Core::File::open(temp_file->path(), Core::File::OpenMode::ReadWrite | Core::File::OpenMode::Truncate));
        return File { move(input_path), move(buffered_file), move(output_file), move(temp_file) };
    }

    // Used for non -i mode.
    static ErrorOr<File> create(LexicalPath input_path, NonnullOwnPtr<Core::File>&& file)
    {
        auto buffered_file = TRY(Core::InputBufferedFile::create(move(file)));
        return File { move(input_path), move(buffered_file), nullptr, nullptr };
    }

    static ErrorOr<File> create_from_stdin()
    {
        // While this path is correct, we don't ever use it since there's no output file to be copied over.
        return create(LexicalPath { "/proc/self/fd/0" }, TRY(Core::File::standard_input()));
    }

    static ErrorOr<File> create_from_stdout()
    {
        // We hack standard output into `File` to avoid having two versions of `write_pattern_space`.
        return File {
            LexicalPath { "/proc/self/fd/1" },
            TRY(Core::InputBufferedFile::create(TRY(Core::File::standard_input()))),
            TRY(Core::File::standard_output()),
            nullptr,
        };
    }

    bool has_next() const
    {
        return !m_file->is_eof();
    }

    ErrorOr<StringView> next()
    {
        VERIFY(has_next());
        m_current_line = TRY(m_file->read_line_with_resize(m_buffer));
        ++m_line_number;
        return m_current_line;
    }

    ErrorOr<void> write_until_depleted(ReadonlyBytes buffer)
    {
        // If we're not in -i mode, stdout, not us, is responsible for writing the output.
        if (!m_output)
            return {};
        return m_output->write_until_depleted(buffer);
    }

    size_t line_number() const { return m_line_number; }

    ErrorOr<void> copy_output_to_original_file()
    {
        if (!m_output)
            return {};
        VERIFY(m_output->is_open());

        TRY(m_output->seek(0, SeekMode::SetPosition));
        auto source_stat = TRY(Core::System::stat(m_input_file_path.string()));
        return FileSystem::copy_file(
            m_input_file_path.string(), m_output_temp_file->path(), source_stat, *m_output);
    }

private:
    File(LexicalPath input_file_path, NonnullOwnPtr<Core::InputBufferedFile>&& file, OwnPtr<Core::File>&& output, OwnPtr<FileSystem::TempFile>&& temp_file)
        : m_input_file_path(move(input_file_path))
        , m_file(move(file))
        , m_output(move(output))
        , m_output_temp_file(move(temp_file))
        , m_buffer(MUST(ByteBuffer::create_uninitialized(PAGE_SIZE)))
    {
    }

    LexicalPath m_input_file_path;
    NonnullOwnPtr<Core::InputBufferedFile> m_file;

    // Only in use if we're editing in place.
    OwnPtr<Core::File> m_output;
    OwnPtr<FileSystem::TempFile> m_output_temp_file;
    size_t m_line_number { 0 };
    ByteString m_current_line;
    ByteBuffer m_buffer;
};

static ErrorOr<void> write_pattern_space(File& output, StringBuilder& pattern_space)
{
    TRY(output.write_until_depleted(pattern_space.string_view().bytes()));
    TRY(output.write_until_depleted("\n"sv.bytes()));
    return {};
}

static void print_unambiguous(StringView pattern_space)
{
    auto find_fold_width = []() -> size_t {
        auto isatty = Core::System::isatty(STDOUT_FILENO);
        if (!isatty.is_error() && isatty.release_value()) {
            struct winsize ws;
            if (ioctl(STDOUT_FILENO, TIOCGWINSZ, &ws) == 0) {
                return ws.ws_col;
            }
        }

        return 70;
    };
    size_t fold_width = find_fold_width();

    AK::StringBuilder unambiguous_output;
    auto folded_append = [&unambiguous_output, current_line_length = size_t { 0 }, fold_width](auto const& value, size_t length) mutable {
        if (current_line_length + length < fold_width) {
            current_line_length += length;
        } else {
            unambiguous_output.append("\\\n"sv);
            current_line_length = length;
        }
        unambiguous_output.append(value);
    };
    for (auto const c : pattern_space) {
        if (c == '\\')
            folded_append("\\\\"sv, 2);
        else if (c == '\a')
            folded_append("\\a"sv, 2);
        else if (c == '\b')
            folded_append("\\b"sv, 2);
        else if (c == '\f')
            folded_append("\\f"sv, 2);
        else if (c == '\r')
            folded_append("\\r"sv, 2);
        else if (c == '\t')
            folded_append("\\t"sv, 2);
        else if (c == '\v')
            folded_append("\\v"sv, 2);
        else if (c == '\n')
            folded_append("$\n"sv, 1);
        else if (AK::is_ascii_printable(c))
            folded_append(c, 1);
        else
            folded_append(ByteString::formatted("\\{:3o}", (unsigned char)c), 4);
    }
    outln("{}$", unambiguous_output.string_view());
}

static ErrorOr<CycleDecision> apply(Command const& command, StringBuilder& pattern_space, StringBuilder& hold_space, StringBuilder& read_command_file_contents, File& input, File& stdout, bool suppress_default_output)
{
    auto cycle_decision = CycleDecision::None;

    switch (command.function) {
    case 'd':
        pattern_space.clear();
        cycle_decision = CycleDecision::Next;
        break;
    case 'g':
        pattern_space = hold_space;
        break;
    case 'G':
        pattern_space.append('\n');
        pattern_space.append(hold_space.string_view());
        break;
    case 'h':
        hold_space = pattern_space;
        break;
    case 'H':
        hold_space.append('\n');
        hold_space.append(pattern_space.string_view());
        break;
    case 'i':
        outln("{}", command.arguments->get<IArguments>().text);
        break;
    case 'l':
        print_unambiguous(pattern_space.string_view());
        break;
    case 'n':
        if (!suppress_default_output)
            TRY(write_pattern_space(stdout, pattern_space));
        TRY(write_pattern_space(input, pattern_space));
        if (input.has_next()) {
            pattern_space.clear();
            pattern_space.append(TRY(input.next()));
        }
        break;
    case 'p':
        TRY(write_pattern_space(stdout, pattern_space));
        break;
    case 'P': {
        auto pattern_sv = pattern_space.string_view();
        auto newline_position = pattern_sv.find('\n').value_or(pattern_sv.length() - 1);
        TRY(stdout.write_until_depleted(pattern_sv.substring_view(0, newline_position + 1).bytes()));
        break;
    }
    case 'q':
        cycle_decision = CycleDecision::Quit;
        break;
    case 's': {
        auto pattern_space_sv = pattern_space.string_view();
        auto const& s_args = command.arguments->get<SArguments>();
        auto result = s_args.regex.replace(pattern_space_sv, s_args.replacement, s_args.options);
        auto replacement_made = result != pattern_space_sv;
        pattern_space.clear();
        pattern_space.append(result);
        if (replacement_made && s_args.output_filepath.has_value()) {
            auto output_file = TRY(Core::File::open(s_args.output_filepath.value(), Core::File::OpenMode::Write | Core::File::OpenMode::Append));
            TRY(output_file->write_until_depleted(TRY(pattern_space.to_byte_buffer())));
            TRY(output_file->write_value('\n'));
        }
        if (replacement_made && s_args.print)
            TRY(write_pattern_space(stdout, pattern_space));
        TRY(write_pattern_space(input, pattern_space));
        break;
    }
    case 'y': {
        // FIXME: Escaping in the transform strings doesn't work properly
        auto pattern_space_sv = pattern_space.string_view();
        auto const& y_args = command.arguments->get<YArguments>();
        VERIFY(y_args.characters.length() == y_args.replacements.length());

        HashMap<char, char> replacement;
        for (size_t i = 0; i < y_args.characters.length(); i++) {
            TRY(replacement.try_set(y_args.characters[i], y_args.replacements[i]));
        }

        StringBuilder new_string;
        for (size_t i = 0; i < pattern_space.length(); i++) {
            if (replacement.contains(pattern_space_sv[i]))
                new_string.append(replacement.get(pattern_space_sv[i]).value());
            else
                new_string.append(pattern_space_sv[i]);
        }

        pattern_space.clear();
        pattern_space.append(new_string.to_byte_string());
        break;
    }
    case 'x':
        swap(pattern_space, hold_space);
        break;
    case '=':
        outln("{}", input.line_number());
        break;
    case '#':
        break;
    case 'w': {
        auto const& w_args = command.arguments->get<WArguments>();
        auto output_file = TRY(Core::File::open(w_args.output_filepath, Core::File::OpenMode::Write | Core::File::OpenMode::Append));
        TRY(output_file->write_until_depleted(TRY(pattern_space.to_byte_buffer())));
        TRY(output_file->write_value('\n'));
        break;
    }
    case 'r': {
        auto const& r_args = command.arguments->get<RArguments>();
        auto input_file_or_error = Core::File::open(r_args.input_filepath, Core::File::OpenMode::Read);
        if (!input_file_or_error.is_error()) {
            auto input_file = input_file_or_error.release_value();
            auto file_contents = TRY(input_file->read_until_eof());

            VERIFY(read_command_file_contents.is_empty());
            read_command_file_contents.append(file_contents);
        }
        break;
    }
    default:
        warnln("Command not implemented: {}", command.function);
        break;
    }

    return cycle_decision;
}

static ErrorOr<void> run(Vector<File>& inputs, Script& script, bool suppress_default_output)
{
    // TODO: verify all commands are valid

    StringBuilder pattern_space;
    StringBuilder hold_space;
    StringBuilder read_command_file_contents;

    // TODO: extend to multiple input files
    auto& input = inputs[0];
    auto stdout = TRY(File::create_from_stdout());

    // main loop
    while (input.has_next()) {
        if (!read_command_file_contents.is_empty() && !suppress_default_output)
            // FIXME: Should we use `out' instead?
            outln("{}", read_command_file_contents.string_view());
        read_command_file_contents.clear();

        // Avoid potential last, empty line
        auto line = TRY(input.next());
        auto is_last_line = !input.has_next();
        if (is_last_line && line.is_empty())
            break;

        // TODO: "Reading from input shall be skipped if a <newline> was in the pattern space prior to a D command ending the previous cycle"
        pattern_space.append(line);

        // Turn commands on/off depending on selection. We need
        for (auto& command : script.commands())
            command.enable_for(pattern_space.string_view(), input.line_number(), is_last_line);

        // Go, go, go!
        CycleDecision cycle_decision = CycleDecision::None;
        for (auto& command : script.commands()) {
            if (!command.is_enabled())
                continue;
            auto command_cycle_decision = TRY(apply(command, pattern_space, hold_space, read_command_file_contents, input, stdout, suppress_default_output));
            if (command_cycle_decision == CycleDecision::Next || command_cycle_decision == CycleDecision::Quit) {
                cycle_decision = command_cycle_decision;
                break;
            }
        }

        if (cycle_decision == CycleDecision::Next)
            continue;

        if (!suppress_default_output)
            TRY(write_pattern_space(stdout, pattern_space));
        pattern_space.clear();

        if (cycle_decision == CycleDecision::Quit)
            break;
    }
    return {};
}

ErrorOr<int> serenity_main(Main::Arguments args)
{
    TRY(Core::System::pledge("stdio cpath rpath wpath fattr chown tty"));

    bool suppress_default_output = false;
    bool edit_in_place = false;
    Core::ArgsParser arg_parser;
    Script script;
    Vector<StringView> pos_args;
    arg_parser.set_general_help("The Stream EDitor");
    arg_parser.add_option(suppress_default_output, "suppress default output", nullptr, 'n');
    arg_parser.add_option(Core::ArgsParser::Option {
        .argument_mode = Core::ArgsParser::OptionArgumentMode::Required,
        .help_string = "A file containing script commands",
        .short_name = 'f',
        .value_name = "script-file",
        .accept_value = [&script](StringView script_file) {
            auto maybe_file = Core::File::open(script_file, Core::File::OpenMode::Read);
            if (maybe_file.is_error()) {
                warnln("Failed to open script file: {}", maybe_file.release_error());
                return false;
            }
            auto maybe_file_contents = maybe_file.release_value()->read_until_eof(1);
            if (maybe_file_contents.is_error()) {
                warnln("Failed to read contents of script file {}: {}", script_file, maybe_file_contents.release_error());
                return false;
            }
            return script.add_script_part(StringView { maybe_file_contents.release_value().bytes() });
        },
    });
    arg_parser.add_option(Core::ArgsParser::Option {
        .argument_mode = Core::ArgsParser::OptionArgumentMode::Required,
        .help_string = "A script of commands",
        .short_name = 'e',
        .value_name = "script",
        .accept_value = [&script](StringView script_argument) {
            return script.add_script_part(script_argument);
        },
    });
    arg_parser.add_option(edit_in_place, "Edit file in place, implies -n", "in-place", 'i');
    arg_parser.add_positional_argument(pos_args, "script and/or file", "...", Core::ArgsParser::Required::No);
    arg_parser.parse(args);

    // When editing in-place, there's also no default output.
    suppress_default_output |= edit_in_place;

    // We only need fattr and chown for in-place editing.
    if (!edit_in_place)
        TRY(Core::System::pledge("stdio cpath rpath wpath tty"));

    if (script.commands().is_empty()) {
        if (pos_args.is_empty()) {
            warnln("No script specified, aborting");
            return 1;
        }
        if (!script.add_script_part(pos_args[0])) {
            return 1;
        }
        pos_args.remove(0);
    }

    HashMap<ByteString, String> paths_to_unveil;

    for (auto const& input_filename : TRY(script.input_filenames())) {
        TRY(paths_to_unveil.try_set(TRY(FileSystem::absolute_path(input_filename)), edit_in_place ? "rwc"_string : "r"_string));
    }
    for (auto const& output_filename : TRY(script.output_filenames())) {
        TRY(paths_to_unveil.try_set(TRY(FileSystem::absolute_path(output_filename)), "rwc"_string));
    }

    Vector<File> inputs;
    for (auto const& filename : pos_args) {
        if (filename == "-"sv) {
            inputs.empend(TRY(File::create_from_stdin()));
        } else {
            TRY(paths_to_unveil.try_set(TRY(FileSystem::absolute_path(filename)), edit_in_place ? "rwc"_string : "r"_string));
            auto file = TRY(Core::File::open(filename, Core::File::OpenMode::Read));
            if (edit_in_place)
                inputs.empend(TRY(File::create_with_output_file(LexicalPath { filename }, move(file))));
            else
                inputs.empend(TRY(File::create(LexicalPath { filename }, move(file))));
        }
    }

    for (auto const& bucket : paths_to_unveil) {
        TRY(Core::System::unveil(bucket.key, bucket.value));
    }
    TRY(Core::System::unveil("/tmp"sv, "rwc"sv));
    TRY(Core::System::unveil(nullptr, nullptr));

    if (inputs.is_empty()) {
        inputs.empend(TRY(File::create_from_stdin()));
    }

    TRY(run(inputs, script, suppress_default_output));

    for (auto& input : inputs)
        TRY(input.copy_output_to_original_file());

    return 0;
}
