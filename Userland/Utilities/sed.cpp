/*
 * Copyright (c) 2021, Rodrigo Tobar <rtobarc@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/Format.h>
#include <AK/Optional.h>
#include <AK/Tuple.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>

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
            Core::File::standard_error()->write(String::formatted("Addressing type not implemented: {}", int(m_address_type)));
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

struct Command {
    Address address1;
    Address address2;
    char function = '\0';
    StringView arguments;

    void enable_for(StringView pattern_space, size_t line_number, bool is_last_line)
    {
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
        } else if (m_is_selection_active) {
            if (address2.matches(pattern_space, line_number, is_last_line))
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
    AK::ErrorOr<void> format(FormatBuilder& format_builder, Command command)
    {
        auto& builder = format_builder.builder();
        builder.appendff("{}", command.address1);
        if (command.address2.address_type() != AddressType::Unset) {
            builder.appendff(",{}", command.address2);
        }
        builder.append(command.function);
        builder.append(command.arguments);
        return {};
    }
};
}

struct AddressParsingResult {
    Optional<Address> address;
    size_t end_of_address;
};

static AddressParsingResult parse_address(StringView sv)
{
    if (sv.is_empty())
        return { {}, 0 };

    if (sv[0] == '$') {
        return { Address { AddressType::LastLine }, 1 };
    }

    size_t index = 0;
    for (; index < sv.length() && '0' <= sv[index] && sv[index] <= '9'; index++)
        ;
    if (index == 0)
        return { {}, 0 };
    return {
        Address { AK::StringUtils::convert_to_uint<size_t>(sv.substring_view(0, index)).release_value() },
        index
    };
}

template<int max_addresses>
static bool verify_number_of_addresses(Command const& command)
{
    if constexpr (max_addresses == 2) {
        return true;
    } else {
        static_assert(max_addresses == 0 || max_addresses == 1);
        auto c = command.function;
        if constexpr (max_addresses == 0) {
            if (command.address1.address_type() != AddressType::Unset) {
                Core::File::standard_error()->write(String::formatted("'{}' doesn't take any address\n", c));
                return false;
            }
        } else {
            if (command.address2.address_type() != AddressType::Unset) {
                Core::File::standard_error()->write(String::formatted("'{}' takes a single address\n", c));
                return false;
            }
        }
    }
    return true;
}

struct CommandParsingResult {
    Optional<Command> command;
    size_t last_index;
};

static CommandParsingResult parse_command(StringView to_parse)
{
    Command command;
    size_t end_of_command_index = 0;

    auto [maybe_address, end_of_address] = parse_address(to_parse);
    end_of_command_index += end_of_address;
    command.address1 = maybe_address.value_or({});
    if (end_of_address >= to_parse.length())
        return { {}, end_of_command_index };
    to_parse = to_parse.substring_view(end_of_address);

    if (to_parse.starts_with(',')) {
        to_parse = to_parse.substring_view(1);
        auto [maybe_address, end_of_address] = parse_address(to_parse);
        end_of_command_index += end_of_address + 1;
        to_parse = to_parse.substring_view(end_of_address);
        command.address2 = maybe_address.value_or({});
    }

    if (to_parse.is_empty()) {
        return { command, end_of_command_index };
    }

    char command_char = to_parse[0];
    to_parse = to_parse.substring_view(1);
    end_of_command_index += 1;

#define HANDLE_FUNCTION_CASE(c, max_addresses)                     \
    case c:                                                        \
        command.function = c;                                      \
        if (!verify_number_of_addresses<max_addresses>(command)) { \
            return { {}, end_of_command_index };                   \
        }                                                          \
        break;

    switch (command_char) {
        ENUMERATE_FUNCTIONS(HANDLE_FUNCTION_CASE)
    default:
        Core::File::standard_error()->write(String::formatted("Unknown function command '{}'\n", to_parse[0]));
        return { command, end_of_command_index };
    }
#undef HANDLE_FUNCTION_CASE

    // TODO: improve argument parsing
    if (to_parse.is_empty())
        return { command, end_of_command_index };
    auto command_separator_index = to_parse.find(';').value_or(to_parse.length() - 1);
    command.arguments = to_parse.substring_view(0, command_separator_index);
    end_of_command_index += command_separator_index + 1;
    return { command, end_of_command_index };
}

class Script {
public:
    [[nodiscard]] bool add_script_part(StringView data)
    {
        StringView command_sv = data.trim_whitespace();
        while (!command_sv.is_empty()) {
            auto [command, end_of_command_index] = parse_command(command_sv);
            if (!command.has_value())
                return false;
            m_commands.append(command.release_value());
            command_sv = command_sv.substring_view(end_of_command_index).trim_whitespace();
        }
        return true;
    }

    Vector<Command>& commands() { return m_commands; }

private:
    Vector<Command> m_commands;
};

enum class CycleDecision {
    None,
    Next,
    Quit
};

class InputFile {
    AK_MAKE_NONCOPYABLE(InputFile);

public:
    InputFile(RefPtr<Core::File> file)
        : m_file(move(file))
        , m_line_iterator(m_file->line_begin())
    {
    }
    InputFile(InputFile&&) = default;
    InputFile& operator=(InputFile&&) = default;

    bool has_next() const
    {
        return !m_line_iterator.at_end();
    }

    StringView next()
    {
        VERIFY(has_next());
        if (!m_started)
            m_started = true;
        else
            ++m_line_iterator;
        auto line = *m_line_iterator;
        ++m_line_number;
        return line;
    }

    size_t line_number() const { return m_line_number; }

private:
    RefPtr<Core::File> m_file;
    Core::LineIterator m_line_iterator;
    size_t m_line_number { 0 };
    bool m_started { false };
};

static void write_pattern_space(Core::File& output, StringBuilder& pattern_space)
{
    output.write(pattern_space.string_view());
    output.write("\n");
}

static void print_unambiguous(Core::File& output, StringView pattern_space)
{
    // TODO: find out the terminal width, folding width should be less than that
    // to make it clear that folding is happening
    constexpr size_t fold_width = 70;

    AK::StringBuilder unambiguous_output;
    auto folded_append = [&unambiguous_output, current_line_length = size_t { 0 }](const auto& value, size_t length) mutable {
        if (current_line_length + length < fold_width) {
            current_line_length += length;
        } else {
            unambiguous_output.append("\\\n");
            current_line_length = length;
        }
        unambiguous_output.append(value);
    };
    for (const auto c : pattern_space) {
        if (c == '\\')
            folded_append("\\\\", 2);
        else if (c == '\a')
            folded_append("\\a", 2);
        else if (c == '\b')
            folded_append("\\b", 2);
        else if (c == '\f')
            folded_append("\\f", 2);
        else if (c == '\r')
            folded_append("\\r", 2);
        else if (c == '\t')
            folded_append("\\t", 2);
        else if (c == '\v')
            folded_append("\\v", 2);
        else if (c == '\n')
            folded_append("$\n", 1);
        else if (AK::is_ascii_printable(c))
            folded_append(c, 1);
        else
            folded_append(String::formatted("\\{:3o}", (unsigned char)c), 4);
    }
    output.write(unambiguous_output.string_view());
    output.write("$\n");
}

static CycleDecision apply(Command const& command, StringBuilder& pattern_space, StringBuilder& hold_space, InputFile& input, bool suppress_default_output)
{
    auto stdout = Core::File::standard_output();
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
    case 'l':
        print_unambiguous(stdout, pattern_space.string_view());
        break;
    case 'n':
        if (!suppress_default_output)
            write_pattern_space(*stdout, pattern_space);
        if (input.has_next()) {
            pattern_space.clear();
            pattern_space.append(input.next());
        }
        break;
    case 'p':
        write_pattern_space(*stdout, pattern_space);
        break;
    case 'P': {
        auto pattern_sv = pattern_space.string_view();
        auto newline_position = pattern_sv.find('\n').value_or(pattern_sv.length() - 1);
        stdout->write(pattern_sv.substring_view(0, newline_position + 1));
        break;
    }
    case 'q':
        cycle_decision = CycleDecision::Quit;
        break;
    case 'x':
        swap(pattern_space, hold_space);
        break;
    case '=':
        stdout->write(String::formatted("{}\n", input.line_number()));
        break;
    case '#':
        break;
    default:
        warnln("Command not implemented: {}", command.function);
        break;
    }

    return cycle_decision;
}

static bool run(Vector<InputFile>& inputs, Script& script, bool suppress_default_output)
{
    // TODO: verify all commands are valid

    StringBuilder pattern_space;
    StringBuilder hold_space;
    auto stdout = Core::File::standard_output();

    // TODO: extend to multiple input files
    auto& input = inputs[0];
    // main loop
    while (input.has_next()) {

        // TODO: "Reading from input shall be skipped if a <newline> was in the pattern space prior to a D command ending the previous cycle"
        pattern_space.append(input.next());
        auto is_last_line = !input.has_next();

        // Turn commands on/off depending on selection. We need
        for (auto& command : script.commands())
            command.enable_for(pattern_space.string_view(), input.line_number(), is_last_line);

        // Go, go, go!
        CycleDecision cycle_decision = CycleDecision::None;
        for (auto& command : script.commands()) {
            if (!command.is_enabled())
                continue;
            auto command_cycle_decision = apply(command, pattern_space, hold_space, input, suppress_default_output);
            if (command_cycle_decision == CycleDecision::Next || command_cycle_decision == CycleDecision::Quit) {
                cycle_decision = command_cycle_decision;
                break;
            }
        }

        if (cycle_decision == CycleDecision::Next)
            continue;
        if (cycle_decision == CycleDecision::Quit)
            break;

        if (!suppress_default_output)
            write_pattern_space(*stdout, pattern_space);
        pattern_space.clear();
    }

    return true;
}

int main(int argc, char* argv[])
{
    bool suppress_default_output = false;
    Core::ArgsParser arg_parser;
    Script script;
    Vector<const char*> pos_args;
    arg_parser.set_general_help("The Stream EDitor");
    arg_parser.add_option(suppress_default_output, "suppress default output", nullptr, 'n');
    arg_parser.add_option(Core::ArgsParser::Option {
        .requires_argument = true,
        .help_string = "A file containing script commands",
        .short_name = 'f',
        .value_name = "script-file",
        .accept_value = [&script](const char* script_file) {
            auto file = Core::File::open(script_file, Core::OpenMode::ReadOnly);
            if (file.is_error()) {
                warnln("Failed to open script file: {}", file.error());
                return false;
            }
            return script.add_script_part(file.value()->read_all());
        },
    });
    arg_parser.add_option(Core::ArgsParser::Option {
        .requires_argument = true,
        .help_string = "A script of commands",
        .short_name = 'e',
        .value_name = "script",
        .accept_value = [&script](const char* script_argument) {
            return script.add_script_part(script_argument);
        },
    });
    arg_parser.add_positional_argument(pos_args, "script and/or file", "...", Core::ArgsParser::Required::No);
    arg_parser.parse(argc, argv);

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
    Vector<InputFile> inputs;
    for (const auto& filename : pos_args) {
        auto file = Core::File::open(filename, Core::OpenMode::ReadOnly);
        if (file.is_error()) {
            warnln("Cannot open input file for reading: {}", file.error());
            return 1;
        }
        inputs.empend(file.release_value());
    }
    if (inputs.is_empty()) {
        inputs.empend(Core::File::standard_input());
    }

    return !run(inputs, script, suppress_default_output);
}
