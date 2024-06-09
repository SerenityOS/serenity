/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/Format.h>
#include <AK/Platform.h>
#include <AK/StringBuilder.h>
#include <AK/StringView.h>

#if defined(AK_OS_LINUX) || defined(AK_LIBC_GLIBC) || defined(AK_OS_BSD_GENERIC) || defined(AK_OS_SOLARIS) || defined(AK_OS_HAIKU)
#    define EXECINFO_BACKTRACE
#endif

#define PRINT_ERROR(s) (void)fputs((s), stderr)

#if defined(EXECINFO_BACKTRACE)
#    include <cxxabi.h>
#    include <execinfo.h>
#endif

#if defined(AK_OS_SERENITY)
#    define ERRORLN dbgln
#else
#    define ERRORLN warnln
#endif

#if !defined(KERNEL)

#    if defined(EXECINFO_BACKTRACE)
namespace {
ALWAYS_INLINE void dump_backtrace()
{
    // Grab symbols and dso name for up to 256 frames
    void* trace[256] = {};
    int const num_frames = backtrace(trace, array_size(trace));
    char** syms = backtrace_symbols(trace, num_frames);

    for (auto i = 0; i < num_frames; ++i) {
        // If there is a C++ symbol name in the line of the backtrace, demangle it
        StringView sym(syms[i], strlen(syms[i]));
        StringBuilder error_builder;
        if (auto idx = sym.find("_Z"sv); idx.has_value()) {
            // Play C games with the original string so we can print before and after the mangled symbol with a C API
            // We don't want to call dbgln() here on substring StringView because we might VERIFY() within AK::Format
            syms[i][idx.value() - 1] = '\0';
            error_builder.append(syms[i], strlen(syms[i]));
            error_builder.append(' ');

            auto sym_substring = sym.substring_view(idx.value());
            auto end_of_sym = sym_substring.find_any_of("+ "sv).value_or(sym_substring.length() - 1);
            syms[i][idx.value() + end_of_sym] = '\0';

            size_t buf_size = 128u;
            char* buf = static_cast<char*>(malloc(buf_size));
            auto* raw_str = &syms[i][idx.value()];
            buf = abi::__cxa_demangle(raw_str, buf, &buf_size, nullptr);

            auto* buf_to_print = buf ? buf : raw_str;
            error_builder.append(buf_to_print, strlen(buf_to_print));
            free(buf);

            error_builder.append(' ');
            auto* end_of_line = &syms[i][idx.value() + end_of_sym + 1];
            error_builder.append(end_of_line, strlen(end_of_line));
        } else {
            error_builder.append(sym);
        }
        error_builder.append('\n');
        error_builder.append('\0');
        PRINT_ERROR(error_builder.string_view().characters_without_null_termination());
    }
    free(syms);
}
}
#    endif

extern "C" {

void ak_verification_failed(char const* message)
{
#    if defined(AK_OS_SERENITY)
    bool colorize_output = true;
#    elif defined(AK_OS_WINDOWS)
    bool colorize_output = false;
#    else
    bool colorize_output = isatty(STDERR_FILENO) == 1;
#    endif

    if (colorize_output)
        ERRORLN("\033[31;1mVERIFICATION FAILED\033[0m: {}", message);
    else
        ERRORLN("VERIFICATION FAILED: {}", message);

#    if defined(EXECINFO_BACKTRACE)
    dump_backtrace();
#    endif
    __builtin_trap();
}
}

#endif
