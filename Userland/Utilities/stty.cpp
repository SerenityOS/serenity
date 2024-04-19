/*
 * Copyright (c) 2021, Daniel Bertalan <dani@danielbertalan.dev>
 * Copyright (c) 2022, Alex Major
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#define __USE_MISC
#define TTYDEFCHARS
#include <AK/ByteString.h>
#include <AK/CharacterTypes.h>
#include <AK/Optional.h>
#include <AK/Result.h>
#include <AK/ScopeGuard.h>
#include <AK/StringView.h>
#include <AK/Vector.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <ctype.h>
#include <fcntl.h>
#include <getopt.h>
#include <stdio.h>
#include <sys/ioctl.h>
#include <sys/ttydefaults.h>
#include <termios.h>
#include <unistd.h>

constexpr option long_options[] = {
    { "all", no_argument, 0, 'a' },
    { "save", no_argument, 0, 'g' },
    { "file", required_argument, 0, 'F' },
    { 0, 0, 0, 0 }
};

struct TermiosFlag {
    StringView name;
    tcflag_t value;
    tcflag_t mask;
};

struct BaudRate {
    speed_t speed;
    unsigned long numeric_value;
};

struct ControlCharacter {
    StringView name;
    unsigned index;
};

constexpr TermiosFlag all_iflags[] = {
    { "ignbrk"sv, IGNBRK, IGNBRK },
    { "brkint"sv, BRKINT, BRKINT },
    { "ignpar"sv, IGNPAR, IGNPAR },
    { "parmer"sv, PARMRK, PARMRK },
    { "inpck"sv, INPCK, INPCK },
    { "istrip"sv, ISTRIP, ISTRIP },
    { "inlcr"sv, INLCR, INLCR },
    { "igncr"sv, IGNCR, IGNCR },
    { "icrnl"sv, ICRNL, ICRNL },
    { "iuclc"sv, IUCLC, IUCLC },
    { "ixon"sv, IXON, IXON },
    { "ixany"sv, IXANY, IXANY },
    { "ixoff"sv, IXOFF, IXOFF },
    { "imaxbel"sv, IMAXBEL, IMAXBEL },
    { "iutf8"sv, IUTF8, IUTF8 }
};

constexpr TermiosFlag all_oflags[] = {
    { "opost"sv, OPOST, OPOST },
    { "olcuc"sv, OLCUC, OPOST },
    { "onlcr"sv, ONLCR, ONLCR },
    { "onlret"sv, ONLRET, ONLRET },
    { "ofill"sv, OFILL, OFILL },
    { "ofdel"sv, OFDEL, OFDEL },
};

constexpr TermiosFlag all_cflags[] = {
    { "cs5"sv, CS5, CSIZE },
    { "cs6"sv, CS6, CSIZE },
    { "cs7"sv, CS7, CSIZE },
    { "cs8"sv, CS8, CSIZE },
    { "cstopb"sv, CSTOPB, CSTOPB },
    { "cread"sv, CREAD, CREAD },
    { "parenb"sv, PARENB, PARENB },
    { "parodd"sv, PARODD, PARODD },
    { "hupcl"sv, HUPCL, HUPCL },
    { "clocal"sv, CLOCAL, CLOCAL },
};

constexpr TermiosFlag all_lflags[] = {
    { "isig"sv, ISIG, ISIG },
    { "icanon"sv, ICANON, ICANON },
    { "echo"sv, ECHO, ECHO },
    { "echoe"sv, ECHOE, ECHOE },
    { "echok"sv, ECHOK, ECHOK },
    { "echonl"sv, ECHONL, ECHONL },
    { "noflsh"sv, NOFLSH, NOFLSH },
    { "tostop"sv, TOSTOP, TOSTOP },
    { "iexten"sv, IEXTEN, IEXTEN }
};

constexpr BaudRate baud_rates[] = {
    { B0, 0 },
    { B50, 50 },
    { B75, 75 },
    { B110, 110 },
    { B134, 134 },
    { B150, 150 },
    { B200, 200 },
    { B300, 300 },
    { B600, 600 },
    { B1200, 1200 },
    { B1800, 1800 },
    { B2400, 2400 },
    { B4800, 4800 },
    { B9600, 9600 },
    { B19200, 19200 },
    { B38400, 38400 },
    { B57600, 57600 },
    { B115200, 115200 },
    { B230400, 230400 },
    { B460800, 460800 },
    { B500000, 500000 },
    { B576000, 576000 },
    { B921600, 921600 },
    { B1000000, 1000000 },
    { B1152000, 1152000 },
    { B1500000, 1500000 },
    { B2000000, 2000000 },
    { B2500000, 2500000 },
    { B3000000, 3000000 },
    { B3500000, 3500000 },
    { B4000000, 4000000 }
};

constexpr ControlCharacter control_characters[] = {
    { "intr"sv, VINTR },
    { "quit"sv, VQUIT },
    { "erase"sv, VERASE },
    { "kill"sv, VKILL },
    { "eof"sv, VEOF },
    /* time and min are handled separately */
    { "swtc"sv, VSWTC },
    { "start"sv, VSTART },
    { "stop"sv, VSTOP },
    { "susp"sv, VSUSP },
    { "eol"sv, VEOL },
    { "reprint"sv, VREPRINT },
    { "discard"sv, VDISCARD },
    { "werase"sv, VWERASE },
    { "lnext"sv, VLNEXT },
    { "eol2"sv, VEOL2 }
};

Optional<speed_t> numeric_value_to_speed(unsigned long);
Optional<unsigned long> speed_to_numeric_value(speed_t);

void print_stty_readable(termios const&);
void print_human_readable(termios const&, winsize const&, bool);
Result<void, int> apply_stty_readable_modes(StringView, termios&);
Result<void, int> apply_modes(size_t, char**, termios&, winsize&);

Optional<speed_t> numeric_value_to_speed(unsigned long numeric_value)
{
    for (auto rate : baud_rates) {
        if (rate.numeric_value == numeric_value)
            return rate.speed;
    }
    return {};
}

Optional<unsigned long> speed_to_numeric_value(speed_t speed)
{
    for (auto rate : baud_rates) {
        if (rate.speed == speed)
            return rate.numeric_value;
    }
    return {};
}

void print_stty_readable(termios const& modes)
{
    out("{:x}:{:x}:{:x}:{:x}", modes.c_iflag, modes.c_oflag, modes.c_cflag, modes.c_lflag);
    for (size_t i = 0; i < NCCS; ++i)
        out(":{:x}", modes.c_cc[i]);
    out(":{:x}:{:x}\n", modes.c_ispeed, modes.c_ospeed);
}

void print_human_readable(termios const& modes, winsize const& ws, bool verbose_mode)
{
    auto print_speed = [&] {
        if (verbose_mode && modes.c_ispeed != modes.c_ospeed) {
            out("ispeed {} baud; ospeed {} baud;", speed_to_numeric_value(modes.c_ispeed).value(), speed_to_numeric_value(modes.c_ospeed).value());

        } else {
            out("speed {} baud;", speed_to_numeric_value(modes.c_ispeed).value());
        }
    };

    auto print_winsize = [&] {
        out("rows {}; columns {};", ws.ws_row, ws.ws_col);
    };

    auto escape_character = [&](u8 ch) {
        StringBuilder sb;
        if (ch <= 0x20) {
            sb.append('^');
            sb.append(ch + 0x40);
        } else if (ch == 0x7f) {
            sb.append("^?"sv);
        } else {
            sb.append(ch);
        }
        return sb.to_byte_string();
    };

    auto print_control_characters = [&] {
        bool first_in_line = true;
        for (auto cc : control_characters) {
            if (verbose_mode || modes.c_cc[cc.index] != ttydefchars[cc.index]) {
                out("{}{} = {};", (first_in_line) ? "" : " ", cc.name, escape_character(modes.c_cc[cc.index]));
                first_in_line = false;
            }
        }
        if (!first_in_line)
            out("\n");
    };

    auto print_flags_of_type = [&](TermiosFlag const flags[], size_t flag_count, tcflag_t field_value, tcflag_t field_default) {
        bool first_in_line = true;
        for (size_t i = 0; i < flag_count; ++i) {
            auto& flag = flags[i];
            if (verbose_mode || (field_value & flag.mask) != (field_default & flag.mask)) {
                bool set = (field_value & flag.mask) == flag.value;
                out("{}{}{}", first_in_line ? "" : " ", set ? "" : "-", flag.name);
                first_in_line = false;
            }
        }
        if (!first_in_line)
            out("\n");
    };

    auto print_flags = [&] {
        print_flags_of_type(all_cflags, sizeof(all_cflags) / sizeof(TermiosFlag), modes.c_cflag, TTYDEF_CFLAG);
        print_flags_of_type(all_oflags, sizeof(all_oflags) / sizeof(TermiosFlag), modes.c_oflag, TTYDEF_OFLAG);
        print_flags_of_type(all_iflags, sizeof(all_iflags) / sizeof(TermiosFlag), modes.c_iflag, TTYDEF_IFLAG);
        print_flags_of_type(all_lflags, sizeof(all_lflags) / sizeof(TermiosFlag), modes.c_lflag, TTYDEF_LFLAG);
    };

    print_speed();
    out(" ");
    print_winsize();
    out("\n");
    print_control_characters();
    print_flags();
}

Result<void, int> apply_stty_readable_modes(StringView mode_string, termios& t)
{
    auto split = mode_string.split_view(':');
    if (split.size() != 4 + NCCS + 2) {
        warnln("Save string has an incorrect number of parameters");
        return 1;
    }
    auto parse_hex = [&](StringView v) {
        tcflag_t ret = 0;
        for (auto c : v) {
            c = to_ascii_lowercase(c);
            ret *= 16;
            if (is_ascii_digit(c)) {
                ret += c - '0';
            } else {
                VERIFY(c >= 'a' && c <= 'f');
                ret += c - 'a';
            }
        }
        return ret;
    };

    t.c_iflag = parse_hex(split[0]);
    t.c_oflag = parse_hex(split[1]);
    t.c_cflag = parse_hex(split[2]);
    t.c_lflag = parse_hex(split[3]);
    for (size_t i = 0; i < NCCS; ++i) {
        t.c_cc[i] = (cc_t)parse_hex(split[4 + i]);
    }
    t.c_ispeed = parse_hex(split[4 + NCCS]);
    t.c_ospeed = parse_hex(split[4 + NCCS + 1]);
    return {};
}

Result<void, int> apply_modes(size_t parameter_count, char** raw_parameters, termios& t, winsize& w)
{
    Vector<StringView> parameters;
    parameters.ensure_capacity(parameter_count);
    for (size_t i = 0; i < parameter_count; ++i)
        parameters.append(StringView { raw_parameters[i], strlen(raw_parameters[i]) });

    auto parse_baud = [&](size_t idx) -> Optional<speed_t> {
        auto maybe_numeric_value = parameters[idx].to_number<uint32_t>();
        if (maybe_numeric_value.has_value())
            return numeric_value_to_speed(maybe_numeric_value.value());
        return {};
    };

    auto parse_number = [&](size_t idx) -> Optional<cc_t> {
        return parameters[idx].to_number<cc_t>();
    };

    auto looks_like_stty_readable = [&](size_t idx) {
        bool contains_colon = false;
        for (auto c : parameters[idx]) {
            c = to_ascii_lowercase(c);
            if (!is_ascii_digit(c) && !(c >= 'a' && c <= 'f') && c != ':')
                return false;
            if (c == ':')
                contains_colon = true;
        }
        return contains_colon;
    };

    auto parse_control_character = [&](size_t idx) -> Optional<cc_t> {
        VERIFY(!parameters[idx].is_empty());
        if (parameters[idx] == "^-" || parameters[idx] == "undef") {
            // FIXME: disabling characters is a bit wonky right now in TTY.
            // We should add the _POSIX_VDISABLE macro.
            return 0;
        } else if (parameters[idx][0] == '^' && parameters[idx].length() == 2) {
            return to_ascii_uppercase(parameters[idx][1]) - 0x40;
        } else if (parameters[idx].starts_with("0x"sv)) {
            cc_t value = 0;
            if (parameters[idx].length() == 2) {
                warnln("Invalid hexadecimal character code {}", parameters[idx]);
                return {};
            }
            for (size_t i = 2; i < parameters[idx].length(); ++i) {
                char ch = to_ascii_lowercase(parameters[idx][i]);
                if (!is_ascii_digit(ch) && !(ch >= 'a' && ch <= 'f')) {
                    warnln("Invalid hexadecimal character code {}", parameters[idx]);
                    return {};
                }
                value = 16 * value + (is_ascii_digit(ch) ? (ch - '0') : (ch - 'a'));
            }
            return value;
        } else if (parameters[idx].starts_with("0"sv)) {
            cc_t value = 0;
            for (size_t i = 1; i < parameters[idx].length(); ++i) {
                char ch = parameters[idx][i];
                if (!(ch >= '0' && ch <= '7')) {
                    warnln("Invalid octal character code {}", parameters[idx]);
                    return {};
                }
                value = 8 * value + (ch - '0');
            }
            return value;
        } else if (is_ascii_digit(parameters[idx][0])) {
            auto maybe_value = parameters[idx].to_number<cc_t>();
            if (!maybe_value.has_value()) {
                warnln("Invalid decimal character code {}", parameters[idx]);
                return {};
            }
            return maybe_value.value();
        } else if (parameters[idx].length() == 1) {
            return parameters[idx][0];
        }
        warnln("Invalid control character {}", parameters[idx]);
        return {};
    };

    size_t parameter_idx = 0;

    auto parse_flag_or_char = [&]() -> Result<void, int> {
        if (parameters[parameter_idx][0] != '-') {
            if (parameters[parameter_idx] == "min") {
                auto maybe_number = parse_number(++parameter_idx);
                if (!maybe_number.has_value()) {
                    warnln("Error parsing min: {} is not a number", parameters[parameter_idx]);
                    return 1;
                }
                return {};
            } else if (parameters[parameter_idx] == "time") {
                auto maybe_number = parse_number(++parameter_idx);
                if (!maybe_number.has_value()) {
                    warnln("Error parsing time: {} is not a number", parameters[parameter_idx]);
                    return 1;
                }
                return {};
            } else {
                for (auto cc : control_characters) {
                    if (cc.name == parameters[parameter_idx]) {
                        if (parameter_idx == parameter_count - 1) {
                            warnln("No control character specified for {}", cc.name);
                            return 1;
                        }
                        auto maybe_control_character = parse_control_character(++parameter_idx);
                        if (!maybe_control_character.has_value())
                            return 1;
                        t.c_cc[cc.index] = maybe_control_character.value();
                        return {};
                    }
                }
            }
        }

        // We fall through to here if what we are setting is not a control character.
        bool negate = false;
        if (parameters[parameter_idx][0] == '-') {
            negate = true;
            parameters[parameter_idx] = parameters[parameter_idx].substring_view(1);
        }

        auto perform_masking = [&](tcflag_t value, tcflag_t mask, tcflag_t& dest) {
            if (negate)
                dest &= ~mask;
            else
                dest = (dest & (~mask)) | value;
        };

        for (auto flag : all_iflags) {
            if (flag.name == parameters[parameter_idx]) {
                perform_masking(flag.value, flag.mask, t.c_iflag);
                return {};
            }
        }
        for (auto flag : all_oflags) {
            if (flag.name == parameters[parameter_idx]) {
                perform_masking(flag.value, flag.mask, t.c_oflag);
                return {};
            }
        }
        for (auto flag : all_cflags) {
            if (flag.name == parameters[parameter_idx]) {
                perform_masking(flag.value, flag.mask, t.c_cflag);
                return {};
            }
        }
        for (auto flag : all_lflags) {
            if (flag.name == parameters[parameter_idx]) {
                perform_masking(flag.value, flag.mask, t.c_lflag);
                return {};
            }
        }
        warnln("Invalid control flag or control character name {}", parameters[parameter_idx]);
        return 1;
    };

    while (parameter_idx < parameter_count) {
        if (looks_like_stty_readable(parameter_idx)) {
            auto maybe_error = apply_stty_readable_modes(parameters[parameter_idx], t);
            if (maybe_error.is_error())
                return maybe_error.error();
        } else if (is_ascii_digit(parameters[parameter_idx][0])) {
            auto new_baud = parse_baud(parameter_idx);
            if (!new_baud.has_value()) {
                warnln("Invalid baud rate {}", parameters[parameter_idx]);
                return 1;
            }
            t.c_ispeed = t.c_ospeed = new_baud.value();
        } else if (parameters[parameter_idx] == "ispeed") {
            if (parameter_idx == parameter_count - 1) {
                warnln("No baud rate specified for ispeed");
                return 1;
            }
            auto new_baud = parse_baud(++parameter_idx);
            if (!new_baud.has_value()) {
                warnln("Invalid input baud rate {}", parameters[parameter_idx]);
                return 1;
            }
            t.c_ispeed = new_baud.value();
        } else if (parameters[parameter_idx] == "ospeed") {
            if (parameter_idx == parameter_count - 1) {
                warnln("No baud rate specified for ospeed");
                return 1;
            }
            auto new_baud = parse_baud(++parameter_idx);
            if (!new_baud.has_value()) {
                warnln("Invalid output baud rate {}", parameters[parameter_idx]);
                return 1;
            }
            t.c_ospeed = new_baud.value();
        } else if (parameters[parameter_idx] == "columns" || parameters[parameter_idx] == "cols") {
            auto maybe_number = parse_number(++parameter_idx);
            if (!maybe_number.has_value()) {
                warnln("Invalid column count {}", parameters[parameter_idx]);
                return 1;
            }
            w.ws_col = maybe_number.value();
        } else if (parameters[parameter_idx] == "rows") {
            auto maybe_number = parse_number(++parameter_idx);
            if (!maybe_number.has_value()) {
                warnln("Invalid row count {}", parameters[parameter_idx]);
                return 1;
            }
            w.ws_row = maybe_number.value();
        } else if (parameters[parameter_idx] == "evenp" || parameters[parameter_idx] == "parity") {
            t.c_cflag &= ~(CSIZE | PARODD);
            t.c_cflag |= CS7 | PARENB;
        } else if (parameters[parameter_idx] == "oddp") {
            t.c_cflag &= ~(CSIZE);
            t.c_cflag |= CS7 | PARENB | PARODD;
        } else if (parameters[parameter_idx] == "-parity" || parameters[parameter_idx] == "-evenp" || parameters[parameter_idx] == "-oddp") {
            t.c_cflag &= ~(PARENB | CSIZE);
            t.c_cflag |= CS8;
        } else if (parameters[parameter_idx] == "raw") {
            cfmakeraw(&t);
        } else if (parameters[parameter_idx] == "nl") {
            t.c_iflag &= ~ICRNL;
        } else if (parameters[parameter_idx] == "-nl") {
            t.c_cflag &= ~(INLCR & IGNCR);
            t.c_iflag |= ICRNL;
        } else if (parameters[parameter_idx] == "ek") {
            t.c_cc[VERASE] = CERASE;
            t.c_cc[VKILL] = CKILL;
        } else if (parameters[parameter_idx] == "sane") {
            t.c_iflag = TTYDEF_IFLAG;
            t.c_oflag = TTYDEF_OFLAG;
            t.c_cflag = TTYDEF_CFLAG;
            t.c_lflag = TTYDEF_LFLAG;
            for (size_t i = 0; i < NCCS; ++i)
                t.c_cc[i] = ttydefchars[i];
            t.c_ispeed = t.c_ospeed = TTYDEF_SPEED;
        } else {
            auto maybe_error = parse_flag_or_char();
            if (maybe_error.is_error())
                return maybe_error.error();
        }
        ++parameter_idx;
    }
    return {};
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio tty rpath"));
    TRY(Core::System::unveil("/dev", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    ByteString device_file;
    bool stty_readable = false;
    bool all_settings = false;

    // Core::ArgsParser can't handle the weird syntax of stty, so we use getopt_long instead.
    int argc = arguments.argc;
    char** argv = arguments.argv;
    opterr = 0; // We handle unknown flags gracefully by starting to parse the arguments in `apply_modes`.
    int optc;
    bool should_quit = false;
    while (!should_quit && ((optc = getopt_long(argc, argv, "-agF:", long_options, nullptr)) != -1)) {
        switch (optc) {
        case 'a':
            all_settings = true;
            break;
        case 'g':
            stty_readable = true;
            break;
        case 'F':
            if (!device_file.is_empty()) {
                warnln("Only one device may be specified");
                exit(1);
            }
            device_file = optarg;
            break;
        default:
            should_quit = true;
            break;
        }
    }

    if (stty_readable && all_settings) {
        warnln("Save mode and all-settings mode are mutually exclusive");
        exit(1);
    }

    int terminal_fd = STDIN_FILENO;
    if (!device_file.is_empty()) {
        if ((terminal_fd = open(device_file.characters(), O_RDONLY, 0)) < 0) {
            perror("open");
            exit(1);
        }
    }

    ScopeGuard file_close_guard = [&] { close(terminal_fd); };

    termios initial_termios = TRY(Core::System::tcgetattr(terminal_fd));

    winsize initial_winsize;
    TRY(Core::System::ioctl(terminal_fd, TIOCGWINSZ, &initial_winsize));

    if (optind < argc) {
        if (stty_readable || all_settings) {
            warnln("Modes cannot be set when printing settings");
            exit(1);
        }

        auto result = apply_modes(argc - optind, argv + optind, initial_termios, initial_winsize);
        if (result.is_error())
            return result.error();

        TRY(Core::System::tcsetattr(terminal_fd, TCSADRAIN, initial_termios));
        TRY(Core::System::ioctl(terminal_fd, TIOCSWINSZ, &initial_winsize));

    } else if (stty_readable) {
        print_stty_readable(initial_termios);
    } else {
        print_human_readable(initial_termios, initial_winsize, all_settings);
    }
    return 0;
}
