/*
 * Copyright (c) 2024, Nils Ollrogge <nils-ollrogge@outlook.de>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/AllOf.h>
#include <AK/CharacterTypes.h>
#include <AK/StdLibExtras.h>
#include <AK/String.h>
#include <AK/StringUtils.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

static constexpr size_t BYTES_PER_LINE_HEX = 16;
static constexpr size_t BYTES_PER_LINE_C = 12;
static constexpr size_t BYTES_PER_LINE_BITS = 6;
static constexpr size_t BYTES_PER_LINE_PLAIN_HEX = 30;
static constexpr size_t BYTES_PER_LINE_MAX = 256;

static constexpr size_t GROUP_SIZE_HEX = 2;
static constexpr size_t GROUP_SIZE_HEX_LITTLE_ENDIAN = 4;
static constexpr size_t GROUP_SIZE_BITS = 1;
static constexpr size_t GROUP_SIZE_PLAIN_HEX = 0;

enum class DisplayStyle {
    Hex,
    PlainHex,
    HexLittleEndian,
    CStyle,
    Bits
};

enum class Color {
    Black = 30,
    Red,
    Green,
    Yellow,
    Blue,
    Purple,
    Cyan,
    White
};

enum class ColorizeOutput {
    No,
    Yes
};

static void color_on(Color const& color)
{
    out("\e[0;{}m", AK::to_underlying(color));
}

static void color_off()
{
    out("\e[0m");
}

static bool is_tab_or_linebreak(u8 const& byte)
{
    return byte == '\t' || byte == '\n' || byte == '\r';
}

static bool is_printable(u8 const& byte)
{
    return byte >= 0x20 && byte <= 0x7E;
}

static Color choose_color(u8 const& byte)
{
    if (byte == 0x00) {
        return Color::White;
    } else if (byte == 0xFF) {
        return Color::Blue;
    } else if (is_printable(byte)) {
        return Color::Green;
    } else if (is_tab_or_linebreak(byte)) {
        return Color::Yellow;
    }

    return Color::Red;
}

static void print_ascii(Bytes line, ColorizeOutput const colorize_output)
{
    for (auto const& byte : line) {
        if (colorize_output == ColorizeOutput::Yes)
            color_on(choose_color(byte));

        if (is_ascii_printable(byte)) {
            putchar(byte);
        } else {
            putchar('.');
        }

        if (colorize_output == ColorizeOutput::Yes)
            color_off();
    }
}

static void print_line_hex(Bytes line, size_t line_length_config, size_t group_size, bool uppercase, ColorizeOutput const colorize_output)
{
    for (size_t i = 0; i < line_length_config; ++i) {
        if (i < line.size()) {
            if (colorize_output == ColorizeOutput::Yes)
                color_on(choose_color(line[i]));

            if (uppercase) {
                out("{:02X}", line[i]);
            } else {
                out("{:02x}", line[i]);
            }

            if (colorize_output == ColorizeOutput::Yes)
                color_off();
        } else {
            out("  ");
        }

        if (group_size != 0 && (i + 1) % group_size == 0) {
            out(" ");
        }
    }

    out(" ");
}

static void print_line_little_endian_hex(Bytes line, size_t line_length_config, size_t group_size, bool uppercase, ColorizeOutput const colorize_output)
{
    if (group_size == 1) {
        print_line_hex(line, line_length_config, group_size, uppercase, colorize_output);
        return;
    }

    if (group_size == 0 || group_size > BYTES_PER_LINE_HEX) {
        group_size = BYTES_PER_LINE_HEX;
    }

    for (size_t i = 0; i < line_length_config; i += group_size) {
        if (i < line.size()) {
            size_t size = i + group_size < line.size() ? group_size : line.size() - i;
            auto group = line.slice(i, size);
            if (size < group_size) {
                for (size_t i = 0; i < group_size - size; ++i) {
                    out("  ");
                }
            }
            for (ssize_t i = group.size() - 1; i >= 0; --i) {
                if (colorize_output == ColorizeOutput::Yes)
                    color_on(choose_color(group[i]));

                if (uppercase) {
                    out("{:02X}", group[i]);
                } else {
                    out("{:02x}", group[i]);
                }

                if (colorize_output == ColorizeOutput::Yes)
                    color_off();
            }
        } else {
            for (size_t i = 0; i < group_size; ++i) {
                out("  ");
            }
        }

        out(" ");
    }

    out(" ");
}

static void print_line_bits(Bytes line, size_t line_length_config, size_t group_size, ColorizeOutput const colorize_output)
{
    auto print_byte = [](u8 byte) {
        for (ssize_t i = 7; i >= 0; --i) {
            out("{}", (byte >> i) & 1 ? '1' : '0');
        }
    };

    for (size_t i = 0; i < line_length_config; ++i) {
        if (i < line.size()) {
            if (colorize_output == ColorizeOutput::Yes)
                color_on(choose_color(line[i]));

            print_byte(line[i]);

            if (colorize_output == ColorizeOutput::Yes)
                color_off();
        } else {
            out("         ");
        }

        if (group_size > 0 && (i + 1) % group_size == 0) {
            out(" ");
        }
    }

    out(" ");
}

static void print_line_c_style(Bytes line)
{
    out("  ");
    for (size_t i = 0; i < line.size() - 1; ++i) {
        out("{:#02x}, ", line[i]);
    }
    out("{:#02x}", line[line.size() - 1]);
}

static ErrorOr<String> path_to_variable_name(StringView path)
{
    auto work = path.to_byte_string();

    work = work.replace("."sv, "_"sv, ReplaceMode::All);
    work = work.replace("/"sv, "_"sv, ReplaceMode::All);

    return TRY(String::from_byte_string(work));
}

ErrorOr<int> serenity_main(Main::Arguments args)
{
    TRY(Core::System::pledge("stdio rpath"));

    Core::ArgsParser args_parser;
    StringView path;
    bool autoskip = false;
    bool c_include_file_style = false;
    bool capitalize_c_include_file_style = false;
    bool binary_digit_formatting = false;
    bool little_endian_hexdump = false;
    bool offset_in_decimal = false;
    bool plain_hexdump_style = false;
    bool uppercase_hex = false;
    bool revert = false;
    Optional<size_t> line_length_option;
    Optional<size_t> group_size_option;
    Optional<size_t> max_bytes;
    Optional<size_t> position_offset;
    Optional<off_t> seek_to;
    String c_include_file_style_variable_name;
    StringView colorize_output_option;

    args_parser.add_positional_argument(path, "Input file", "input", Core::ArgsParser::Required::No);
    args_parser.add_option(autoskip, "Replace nul-lines with '*'", "autoskip", 'a');
    args_parser.add_option(binary_digit_formatting, "Binary digit formatting", "bits", 'b');
    args_parser.add_option(capitalize_c_include_file_style, "Capitalize C include file style (-i).", "capitalize", 'C');
    args_parser.add_option(line_length_option, "Amount of bytes shown per line (max 256)", "cols", 'c', "cols");
    args_parser.add_option(offset_in_decimal, "Show file offset in decimal", "decimal", 'd');
    args_parser.add_option(little_endian_hexdump, "Little-endian hex dump", nullptr, 'e');
    args_parser.add_option(group_size_option, "Separate the output of every amount bytes", "groupsize", 'g', "amount");
    args_parser.add_option(c_include_file_style, "Output in C include file style", "include", 'i');
    args_parser.add_option(max_bytes, "Truncate to fixed number of bytes", "len", 'l', "bytes");
    args_parser.add_option(c_include_file_style_variable_name, "Set variable name used in C include ouput (-i)", "name", 'n', "include_style");
    args_parser.add_option(position_offset, "Add offset to displayed file position", nullptr, 'o', "offset");
    args_parser.add_option(plain_hexdump_style, "Output in plain hex dump style", "plain", 'p');
    args_parser.add_option(revert, "Patch hex dump into binary", "revert", 'r');
    args_parser.add_option(colorize_output_option, "Colorize output", nullptr, 'R', "when");
    args_parser.add_option(seek_to, "Seek to a byte offset", "seek", 's', "[-]offset");
    args_parser.add_option(uppercase_hex, "Use upper case hex letters", nullptr, 'u');

    args_parser.parse(args);

    auto file = TRY(Core::File::open_file_or_standard_stream(path, Core::File::OpenMode::Read));

    auto display_style = DisplayStyle::Hex;
    size_t line_length_config = BYTES_PER_LINE_HEX;
    size_t group_size = GROUP_SIZE_HEX;

    auto formatting_options_provided = 0x0;

    if (c_include_file_style) {
        formatting_options_provided++;
        display_style = DisplayStyle::CStyle;
        line_length_config = BYTES_PER_LINE_C;

        if (c_include_file_style_variable_name.is_empty()) {
            c_include_file_style_variable_name = TRY(path_to_variable_name(path));
        }

        if (capitalize_c_include_file_style) {
            c_include_file_style_variable_name = TRY(c_include_file_style_variable_name.to_uppercase());
        }

        if (file->fd() != STDIN_FILENO) {
            outln("unsigned char {}[] = {{", c_include_file_style_variable_name);
        }
    }

    if (little_endian_hexdump) {
        formatting_options_provided++;
        display_style = DisplayStyle::HexLittleEndian;
        group_size = GROUP_SIZE_HEX_LITTLE_ENDIAN;
    }

    if (plain_hexdump_style) {
        formatting_options_provided++;
        display_style = DisplayStyle::PlainHex;
        line_length_config = BYTES_PER_LINE_PLAIN_HEX;
        group_size = GROUP_SIZE_PLAIN_HEX;
    }

    if (binary_digit_formatting) {
        formatting_options_provided++;
        display_style = DisplayStyle::Bits;
        group_size = GROUP_SIZE_BITS;
        line_length_config = BYTES_PER_LINE_BITS;
    }

    if (formatting_options_provided > 1) {
        warnln("Only one of the following flags can be used at a time: -i, -e, -p, -b");
        return 1;
    }

    if (line_length_option.has_value() && line_length_option.value() > 0) {
        line_length_config = line_length_option.value();

        if (line_length_config > BYTES_PER_LINE_MAX && !plain_hexdump_style) {
            warnln("Invalid number of columns (max is 256).");
            return 1;
        }
    }

    if (group_size_option.has_value()) {
        group_size = group_size_option.value();

        if (little_endian_hexdump) {
            if (group_size != 0 && !is_power_of_two(group_size)) {
                warnln("Group size must be a power of 2 with -e");
                return 1;
            }
        }
    }

    ColorizeOutput colorize_output = ColorizeOutput::No;
    if (!colorize_output_option.is_null()) {
        if (colorize_output_option == "always") {
            colorize_output = ColorizeOutput::Yes;
        } else if (colorize_output_option == "auto") {
            if (TRY(Core::System::isatty(STDOUT_FILENO))) {
                colorize_output = ColorizeOutput::Yes;
            } else {
                colorize_output = ColorizeOutput::No;
            }
        } else if (colorize_output_option == "never") {
            colorize_output = ColorizeOutput::No;
        } else {
            warnln("Unknown value '{}' for -R, should be one of 'always', 'auto', or 'never'", colorize_output_option);
            return 1;
        }
    }

    if (revert) {
        warnln("Patching is not supported");
        return 1;
    }

    Array<u8, BUFSIZ> contents;
    Bytes bytes;
    size_t total_bytes_read = 0x0;
    size_t const max_read_size = contents.size() - (contents.size() % line_length_config);
    bool is_input_remaining = true;

    // TODO: seek relative to current stdin file position
    if (seek_to.has_value()) {
        auto file_size = 0x0;
        if (auto size = file->size(); !size.is_error()) {
            file_size = size.value();
        }

        auto offset = seek_to.value();
        total_bytes_read = offset < 0 ? file_size + offset : offset;
        TRY(file->seek(total_bytes_read, SeekMode::SetPosition));
    }

    while (is_input_remaining) {
        auto bytes_to_read = max_read_size - bytes.size();

        bytes = contents.span().slice(0, bytes_to_read);
        bytes = TRY(file->read_some(bytes));

        if (bytes.size() < bytes_to_read && file->fd() != STDIN_FILENO) {
            is_input_remaining = false;
        }

        while (bytes.size() > 0) {
            auto line_length = bytes.size() > line_length_config ? line_length_config : bytes.size();

            if (max_bytes.has_value()) {
                auto bytes_remaining = max_bytes.value() - total_bytes_read;
                if (bytes_remaining < line_length) {
                    line_length = bytes_remaining;
                }
            }

            auto current_line = bytes.slice(0, line_length);
            bytes = bytes.slice(line_length);

            if (autoskip && all_of(bytes, [](auto& b) { return b == 0x0; })) {
                outln("*");
                continue;
            }

            if (display_style != DisplayStyle::CStyle && display_style != DisplayStyle::PlainHex) {
                auto offset = 0;
                if (position_offset.has_value()) {
                    offset = position_offset.value();
                }

                if (offset_in_decimal) {
                    out("{:08}: ", total_bytes_read + offset);
                } else {
                    out("{:08x}: ", total_bytes_read + offset);
                }
            }

            switch (display_style) {
            case DisplayStyle::Hex:
                print_line_hex(current_line, line_length_config, group_size, uppercase_hex, colorize_output);
                print_ascii(current_line, colorize_output);
                break;
            case DisplayStyle::PlainHex:
                print_line_hex(current_line, line_length_config, group_size, uppercase_hex, colorize_output);
                break;
            case DisplayStyle::HexLittleEndian:
                print_line_little_endian_hex(current_line, line_length_config, group_size, uppercase_hex, colorize_output);
                print_ascii(current_line, colorize_output);
                break;
            case DisplayStyle::Bits:
                print_line_bits(current_line, line_length_config, group_size, colorize_output);
                print_ascii(current_line, colorize_output);
                break;
            case DisplayStyle::CStyle:
                print_line_c_style(current_line);
                break;
            }

            putchar('\n');

            total_bytes_read += line_length;

            if (max_bytes.has_value() && total_bytes_read >= max_bytes.value()) {
                is_input_remaining = false;
                break;
            }
        }
    }

    if (display_style == DisplayStyle::CStyle) {
        outln("}};");
        auto postfix = capitalize_c_include_file_style ? "LEN" : "len";
        outln("unsigned int {}_{} = {};", c_include_file_style_variable_name, postfix, total_bytes_read);
    }

    return 0;
}
