/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Alex Major
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/Debug.h>
#include <AK/Iterator.h>
#include <AK/Vector.h>
#include <Kernel/API/SyscallString.h>
#include <LibCore/ArgsParser.h>
#include <LibMain/Main.h>
#include <errno_codes.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
#include <syscall.h>

#define SC_NARG 4

FlatPtr arg[SC_NARG];
char outbuf[BUFSIZ];

using Arguments = Vector<ByteString>;
using ArgIter = Arguments::Iterator;

static FlatPtr parse_from(ArgIter&);

template<>
struct AK::Formatter<Syscall::Function> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, Syscall::Function function)
    {
        return Formatter<StringView>::format(builder, to_string(function));
    }
};

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    bool output_buffer = false;
    bool list_syscalls = false;
    Arguments syscall_arguments;

    Core::ArgsParser args_parser;
    args_parser.set_general_help(
        "Enables you to do a direct syscall, even those that use a 'SC_*_params' buffer.\n"
        "Arguments can be literal strings, numbers, the output buffer, or parameter buffers:\n"
        " - Arguments that begin with a comma are stripped of the comma and treated as string arguments, for example ',0x0' or ',['.\n"
        " - 'buf' is replaced by a pointer to the output buffer.\n"
        " - Numbers can be written like 1234 or 0xDEADC0DE.\n"
        " - Parameter buffer (e.g. SC_realpath_params) can be passed by wrapping them in '[' and ']'. Note that '[' and ']' must be separate arguments to syscall(1). Buffers can be used recursively.\n"
        " - The first argument may also be any syscall function name. Run 'syscall -l' to see the list.\n"
        " - Arguments that cannot be interpreted are treated as string arguments, for example 'Hello, friends!'.\n"
        "\n"
        "Full example: syscall -o realpath [ /usr/share/man/man2/getgid.md 1024 buf 1024 ]");
    args_parser.add_option(list_syscalls, "List all existing syscalls, and exit", "list-syscalls", 'l');
    args_parser.add_option(output_buffer, "Output the contents of the buffer (beware of stray zero bytes!)", "output-buffer", 'o');
    args_parser.add_positional_argument(syscall_arguments, "Syscall arguments; see general help.", "syscall-arguments", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    if (list_syscalls) {
        outln("syscall list:");
        for (int sc = 0; sc < Syscall::Function::__Count; ++sc) {
            outln("  \033[33;1m{}\033[0m - {}", sc, static_cast<Syscall::Function>(sc));
        }
        exit(0);
    }

    if (syscall_arguments.is_empty()) {
        args_parser.print_usage(stderr, arguments.strings[0]);
        exit(1);
    }

    ArgIter iter = syscall_arguments.begin();
    for (size_t i = 0; i < SC_NARG && !iter.is_end(); i++) {
        arg[i] = parse_from(iter);
    }
    if (!iter.is_end()) {
        warnln("Too many arguments (did you want to use '[ parameter buffers ]'?)");
        return -1;
    }

    if (arg[0] > Syscall::Function::__Count) {
        for (int sc = 0; sc < Syscall::Function::__Count; ++sc) {
            if (Syscall::to_string((Syscall::Function)sc) == (char const*)arg[0]) {
                arg[0] = sc;
                break;
            }
        }
        if (arg[0] > Syscall::Function::__Count) {
            warnln("Invalid syscall entry {}", (char*)arg[0]);
            return -1;
        }
    }

    dbgln_if(SYSCALL_1_DEBUG, "Calling {} {:p} {:p} {:p}\n", arg[0], arg[1], arg[2], arg[3]);
    int rc = syscall(arg[0], arg[1], arg[2], arg[3]);
    if (output_buffer)
        fwrite(outbuf, 1, sizeof(outbuf), stdout);

    if (-rc >= 0 && -rc < EMAXERRNO) {
        warnln("Syscall return: {} ({})", rc, strerror(-rc));
    } else {
        warnln("Syscall return: {} (?)", rc);
    }
    return 0;
}

static FlatPtr as_buf(Vector<FlatPtr> params_vec)
{
    size_t params_size = sizeof(FlatPtr) * params_vec.size();
    size_t buf_size = round_up_to_power_of_two(params_size + 1, PAGE_SIZE);
    void* buf = mmap(nullptr, buf_size, PROT_READ | PROT_WRITE, MAP_ANON | MAP_PRIVATE, 0, 0);
    if (buf == MAP_FAILED) {
        fprintf(stderr, "Warning: Could not allocate buffer of size %zu (low memory?)\n", buf_size);
        exit(1);
    }
    // It's probably good to ensure zero-initialization.
    memset(buf, 0, buf_size);
    memcpy(buf, params_vec.data(), params_size);

    if constexpr (SYSCALL_1_DEBUG) {
        StringBuilder builder;
        builder.append("Prepared ["sv);
        for (size_t i = 0; i < params_vec.size(); ++i) {
            builder.appendff(" {:p}", params_vec[i]);
        }
        builder.appendff(" ] at {:p}", (FlatPtr)buf);
        dbgln("{}", builder.to_byte_string());
    }

    // Leak the buffer here. We need to keep it until the special syscall happens,
    // and we terminate immediately afterwards anyway.
    return (FlatPtr)buf;
}

static FlatPtr parse_parameter_buffer(ArgIter& iter)
{
    Vector<FlatPtr> params_vec;
    while (!iter.is_end()) {
        if (*iter == "]"sv) {
            ++iter;
            return as_buf(params_vec);
        }

        params_vec.append(parse_from(iter));
    }

    fprintf(stderr, "Error: Unmatched '['?!\n");
    exit(1);
    VERIFY_NOT_REACHED();
}

static FlatPtr parse_from(ArgIter& iter)
{
    auto const& this_arg_string = *iter;
    auto* this_arg = this_arg_string.characters();
    ++iter;

    // Is it a forced literal?
    if (this_arg[0] == ',') {
        this_arg += 1;
        dbgln_if(SYSCALL_1_DEBUG, "Using (forced) string >>{}<< at {:p}", this_arg_string, (FlatPtr)this_arg);
        return (FlatPtr)this_arg;
    }

    // Is it the output buffer?
    if (this_arg_string == "buf"sv)
        return (FlatPtr)outbuf;

    // Is it a parameter buffer?
    if (this_arg_string == "["sv)
        return parse_parameter_buffer(iter);

    // Is it a number?
    if (auto l = this_arg_string.to_number<unsigned>(); l.has_value())
        return *l;

    // Then it must be a string:
    if (this_arg_string == "]"sv)
        fprintf(stderr, "Warning: Treating unmatched ']' as literal string\n");

    dbgln_if(SYSCALL_1_DEBUG, "Using (detected) string >>{}<< at {:p}", this_arg_string, (FlatPtr)this_arg);

    return (FlatPtr)this_arg;
}
