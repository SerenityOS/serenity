/*
 * Copyright (c) 2021, Daniel Bertalan <dani@danielbertalan.dev>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#define __USE_MISC
#define TTYDEFCHARS
#include <AK/Array.h>
#include <AK/Optional.h>
#include <AK/Result.h>
#include <AK/ScopeGuard.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Vector.h>
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
    { "ignbrk", IGNBRK, IGNBRK },
    { "brkint", BRKINT, BRKINT },
    { "ignpar", IGNPAR, IGNPAR },
    { "parmer", PARMRK, PARMRK },
    { "inpck", INPCK, INPCK },
    { "istrip", ISTRIP, ISTRIP },
    { "inlcr", INLCR, INLCR },
    { "igncr", IGNCR, IGNCR },
    { "icrnl", ICRNL, ICRNL },
    { "iuclc", IUCLC, IUCLC },
    { "ixon", IXON, IXON },
    { "ixany", IXANY, IXANY },
    { "ixoff", IXOFF, IXOFF },
    { "imaxbel", IMAXBEL, IMAXBEL },
    { "iutf8", IUTF8, IUTF8 }
};

constexpr TermiosFlag all_oflags[] = {
    { "opost", OPOST, OPOST },
    { "olcuc", OLCUC, OPOST },
    { "onlcr", ONLCR, ONLCR },
    { "onlret", ONLRET, ONLRET },
    { "ofill", OFILL, OFILL },
    { "ofdel", OFDEL, OFDEL },
};

constexpr TermiosFlag all_cflags[] = {
    { "cs5", CS5, CSIZE },
    { "cs6", CS6, CSIZE },
    { "cs7", CS7, CSIZE },
    { "cs8", CS8, CSIZE },
    { "cstopb", CSTOPB, CSTOPB },
    { "cread", CREAD, CREAD },
    { "parenb", PARENB, PARENB },
    { "parodd", PARODD, PARODD },
    { "hupcl", HUPCL, HUPCL },
    { "clocal", CLOCAL, CLOCAL },
};

constexpr TermiosFlag all_lflags[] = {
    { "isig", ISIG, ISIG },
    { "icanon", ICANON, ICANON },
    { "echo", ECHO, ECHO },
    { "echoe", ECHOE, ECHOE },
    { "echok", ECHOK, ECHOK },
    { "echonl", ECHONL, ECHONL },
    { "noflsh", NOFLSH, NOFLSH },
    { "tostop", TOSTOP, TOSTOP },
    { "iexten", IEXTEN, IEXTEN }
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
    { "intr", VINTR },
    { "quit", VQUIT },
    { "erase", VERASE },
    { "kill", VKILL },
    { "eof", VEOF },
    /* time and min are handled separately */
    { "swtc", VSWTC },
    { "start", VSTART },
    { "stop", VSTOP },
    { "susp", VSUSP },
    { "eol", VEOL },
    { "reprint", VREPRINT },
    { "discard", VDISCARD },
    { "werase", VWERASE },
    { "lnext", VLNEXT },
    { "eol2", VEOL2 }
};

Optional<speed_t> numeric_value_to_speed(unsigned long);
Optional<unsigned long> speed_to_numeric_value(speed_t);

void print_stty_readable(const termios&);
void print_human_readable(const termios&, const winsize&, bool);
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

void print_stty_readable(const termios& modes)
{
    out("{:x}:{:x}:{:x}:{:x}", modes.c_iflag, modes.c_oflag, modes.c_cflag, modes.c_lflag);
    for (size_t i = 0; i < NCCS; ++i)
        out(":{:x}", modes.c_cc[i]);
    out(":{:x}:{:x}\n", modes.c_ispeed, modes.c_ospeed);
}

void print_human_readable(const termios& modes, const winsize& ws, bool verbose_mode)
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
            sb.append("^");
            sb.append(ch + 0x40);
        } else if (ch == 0x7f) {
            sb.append("^?");
        } else {
            sb.append(ch);
        }
        return sb.to_string();
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

    auto print_flags_of_type = [&](const TermiosFlag flags[], size_t flag_count, tcflag_t field_value, tcflag_t field_default) {
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
            c = tolower(c);
            ret *= 16;
            if (isdigit(c)) {
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
        parameters.append(StringView(raw_parameters[i]));

    auto parse_baud = [&](size_t idx) -> Optional<speed_t> {
        auto maybe_numeric_value = parameters[idx].to_uint<uint32_t>();
        if (maybe_numeric_value.has_value())
            return numeric_value_to_speed(maybe_numeric_value.value());
        return {};
    };

    auto parse_number = [&](size_t idx) -> Optional<cc_t> {
        return parameters[idx].to_uint<cc_t>();
    };

    auto looks_like_stty_readable = [&](size_t idx) {
        bool contains_colon = false;
        for (auto c : parameters[idx]) {
            c = tolower(c);
            if (!isdigit(c) && !(c >= 'a' && c <= 'f') && c != ':')
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
            return toupper(parameters[idx][1]) - 0x40;
        } else if (parameters[idx].starts_with("0x")) {
            cc_t value = 0;
            if (parameters[idx].length() == 2) {
                warnln("Invalid hexadecimal character code {}", parameters[idx]);
                return {};
            }
            for (size_t i = 2; i < parameters[idx].length(); ++i) {
                char ch = tolower(parameters[idx][i]);
                if (!isdigit(ch) && !(ch >= 'a' && ch <= 'f')) {
                    warnln("Invalid hexadecimal character code {}", parameters[idx]);
                    return {};
                }
                value = 16 * value + (isdigit(ch)) ? (ch - '0') : (ch - 'a');
            }
            return value;
        } else if (parameters[idx].starts_with("0")) {
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
        } else if (isdigit(parameters[idx][0])) {
            auto maybe_value = parameters[idx].to_uint<cc_t>();
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
        } else if (isdigit(parameters[parameter_idx][0])) {
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

int main(int argc, char** argv)
{
    if (pledge("stdio tty rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/dev", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil");
        return 1;
    }

    String device_file;
    bool stty_readable = false;
    bool all_settings = false;

    // Core::ArgsParser can't handle the weird syntax of stty, so we use getopt_long instead.
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

    termios initial_termios;
    if (tcgetattr(terminal_fd, &initial_termios) < 0) {
        perror("tcgetattr");
        exit(1);
    }

    winsize initial_winsize;
    if (ioctl(terminal_fd, TIOCGWINSZ, &initial_winsize) < 0) {
        perror("ioctl(TIOCGWINSZ)");
        exit(1);
    }

    if (optind < argc) {
        if (stty_readable || all_settings) {
            warnln("Modes cannot be set when printing settings");
            exit(1);
        }
        auto result = apply_modes(argc - optind, argv + optind, initial_termios, initial_winsize);
        if (result.is_error())
            return result.error();

        if (tcsetattr(terminal_fd, TCSADRAIN, &initial_termios) < 0) {
            perror("tcsetattr");
            exit(1);
        }
        if (ioctl(terminal_fd, TIOCSWINSZ, &initial_winsize) < 0) {
            perror("ioctl(TIOCSWINSZ)");
            exit(1);
        }
    } else if (stty_readable) {
        print_stty_readable(initial_termios);
    } else {
        print_human_readable(initial_termios, initial_winsize, all_settings);
    }
    return 0;
}
