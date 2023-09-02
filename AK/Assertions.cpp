/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/Format.h>
#include <AK/Platform.h>
#include <AK/StringView.h>

#if defined(AK_OS_LINUX) || defined(AK_OS_BSD_GENERIC) || defined(AK_OS_SOLARIS)
#    define EXECINFO_BACKTRACE
#endif

#if defined(EXECINFO_BACKTRACE)
#    include <cxxabi.h>
#    include <execinfo.h>
#endif

#if !defined(KERNEL)

#    if defined(EXECINFO_BACKTRACE)
namespace {
ALWAYS_INLINE void dump_backtrace()
{
    // Grab symbols and dso name for up to 256 frames
    void* trace[256] = {};
    int const num_frames = backtrace(trace, sizeof(trace));
    char** syms = backtrace_symbols(trace, num_frames);

    for (auto i = 0; i < num_frames; ++i) {
        // If there is a C++ symbol name in the line of the backtrace, demangle it
        StringView sym(syms[i], strlen(syms[i]));
        if (auto idx = sym.find("_Z"sv); idx.has_value()) {
            // Play C games with the original string so we can print before and after the mangled symbol with a C API
            // We don't want to call dbgln() here on substring StringView because we might VERIFY() within AK::Format
            syms[i][idx.value() - 1] = '\0';
            (void)fprintf(stderr, "%s ", syms[i]);

            auto sym_substring = sym.substring_view(idx.value());
            auto end_of_sym = sym_substring.find_any_of("+ "sv).value_or(sym_substring.length() - 1);
            syms[i][idx.value() + end_of_sym] = '\0';

            size_t buf_size = 128u;
            char* buf = static_cast<char*>(malloc(buf_size));
            auto* raw_str = &syms[i][idx.value()];
            buf = abi::__cxa_demangle(raw_str, buf, &buf_size, nullptr);

            (void)fputs(buf ? buf : raw_str, stderr);
            free(buf);

            (void)fprintf(stderr, " %s", &syms[i][idx.value() + end_of_sym + 1]);
        } else {
            (void)fputs(sym.characters_without_null_termination(), stderr);
        }
        (void)fputs("\n", stderr);
    }
    free(syms);
}
}
#    endif

extern "C" {

void ak_verification_failed(char const* message)
{
    dbgln("VERIFICATION FAILED: {}", message);
#    if defined(EXECINFO_BACKTRACE)
    dump_backtrace();
#    endif
    __builtin_trap();
}
}

#endif
