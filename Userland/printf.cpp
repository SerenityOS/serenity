/*
 * Copyright (c) 2020, the SerenityOS developers.
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

#include <AK/PrintfImplementation.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/Types.h>
#include <stdio.h>
#include <unistd.h>

[[gnu::noreturn]] static void fail(const char* message)
{
    fputs("\e[31m", stderr);
    fputs(message, stderr);
    fputs("\e[0m\n", stderr);
    exit(1);
}

template<typename PutChFunc, typename ArgumentListRefT, template<typename T, typename U = ArgumentListRefT> typename NextArgument>
struct PrintfImpl : public PrintfImplementation::PrintfImpl<PutChFunc, ArgumentListRefT, NextArgument> {
    ALWAYS_INLINE PrintfImpl(PutChFunc& putch, char*& bufptr, const int& nwritten)
        : PrintfImplementation::PrintfImpl<PutChFunc, ArgumentListRefT, NextArgument>(putch, bufptr, nwritten)
    {
    }

    ALWAYS_INLINE int format_b(const PrintfImplementation::ModifierState&, ArgumentListRefT&) const
    {
        fail("format specifier 'b' is not supported");
    }
    ALWAYS_INLINE int format_q(const PrintfImplementation::ModifierState& state, ArgumentListRefT& ap) const
    {
        auto state_copy = state;
        auto str = NextArgument<const char*>()(ap);
        if (!str)
            str = "(null)";

        constexpr auto make_len_or_escape = [](auto str, bool mk_len, size_t field_width, auto putc) {
            unsigned len = 2;
            if (!mk_len)
                putc('"');

            for (size_t i = 0; str[i] && (mk_len ? true : (field_width >= len)); ++i) {
                auto ch = str[i];
                switch (ch) {
                case '"':
                case '$':
                case '\\':
                    ++len;
                    if (!mk_len)
                        putc('\\');
                }
                ++len;
                if (!mk_len)
                    putc(ch);
            }

            if (!mk_len)
                putc('"');

            return len;
        };

        auto len = make_len_or_escape(str, true, state_copy.field_width, [&](auto c) { this->m_putch(this->m_bufptr, c); });

        if (!state_copy.dot && (!state_copy.field_width || state_copy.field_width < len))
            state_copy.field_width = len;
        size_t pad_amount = state_copy.field_width > len ? state_copy.field_width - len : 0;

        if (!state_copy.left_pad) {
            for (size_t i = 0; i < pad_amount; ++i)
                this->m_putch(this->m_bufptr, ' ');
        }

        make_len_or_escape(str, false, state_copy.field_width, [&](auto c) { this->m_putch(this->m_bufptr, c); });

        if (state_copy.left_pad) {
            for (size_t i = 0; i < pad_amount; ++i)
                this->m_putch(this->m_bufptr, ' ');
        }

        return state_copy.field_width;
    }
};

template<typename T, typename V>
struct ArgvNextArgument {
    ALWAYS_INLINE T operator()(V) const
    {
        static_assert(sizeof(V) != sizeof(V), "Base instantiated");
        return declval<T>();
    }
};

template<typename V>
struct ArgvNextArgument<char*, V> {
    ALWAYS_INLINE char* operator()(V arg) const
    {
        if (arg.argc == 0)
            fail("Not enough arguments");

        auto result = *arg.argv++;
        --arg.argc;
        return result;
    }
};

template<typename V>
struct ArgvNextArgument<const char*, V> {
    ALWAYS_INLINE const char* operator()(V arg) const
    {
        if (arg.argc == 0)
            return "";

        auto result = *arg.argv++;
        --arg.argc;
        return result;
    }
};

template<typename V>
struct ArgvNextArgument<int, V> {
    ALWAYS_INLINE int operator()(V arg) const
    {
        if (arg.argc == 0)
            return 0;

        auto result = *arg.argv++;
        --arg.argc;
        return atoi(result);
    }
};

template<typename V>
struct ArgvNextArgument<unsigned, V> {
    ALWAYS_INLINE unsigned operator()(V arg) const
    {
        if (arg.argc == 0)
            return 0;

        auto result = *arg.argv++;
        --arg.argc;
        return strtoul(result, nullptr, 10);
    }
};

template<typename V>
struct ArgvNextArgument<i64, V> {
    ALWAYS_INLINE i64 operator()(V arg) const
    {
        if (arg.argc == 0)
            return 0;

        auto result = *arg.argv++;
        --arg.argc;
        return strtoll(result, nullptr, 10);
    }
};

template<typename V>
struct ArgvNextArgument<u64, V> {
    ALWAYS_INLINE u64 operator()(V arg) const
    {
        if (arg.argc == 0)
            return 0;

        auto result = *arg.argv++;
        --arg.argc;
        return strtoull(result, nullptr, 10);
    }
};

template<typename V>
struct ArgvNextArgument<double, V> {
    ALWAYS_INLINE double operator()(V arg) const
    {
        if (arg.argc == 0)
            return 0;

        auto result = *arg.argv++;
        --arg.argc;
        return strtod(result, nullptr);
    }
};

template<typename V>
struct ArgvNextArgument<int*, V> {
    ALWAYS_INLINE int* operator()(V) const
    {
        ASSERT_NOT_REACHED();
        return nullptr;
    }
};

struct ArgvWithCount {
    char**& argv;
    int& argc;
};

static String handle_escapes(const char* string)
{
    StringBuilder builder;
    for (auto c = *string; c; c = *++string) {
        if (c == '\\') {
            if (string[1]) {
                switch (c = *++string) {
                case '\\':
                case '"':
                    builder.append(c);
                    break;
                case 'a':
                    builder.append('\a');
                    break;
                case 'b':
                    builder.append('\b');
                    break;
                case 'c':
                    return builder.build();
                case 'e':
                    builder.append('\e');
                    break;
                case 'f':
                    builder.append('\f');
                    break;
                case 'n':
                    builder.append('\n');
                    break;
                case 'r':
                    builder.append('\r');
                    break;
                case 't':
                    builder.append('\t');
                    break;
                case 'v':
                    builder.append('\v');
                    break;
                case 'x':
                    fail("Unsupported escape '\\x'");
                case 'u':
                    fail("Unsupported escape '\\u'");
                case 'U':
                    fail("Unsupported escape '\\U'");
                default:
                    builder.append(c);
                }
            } else {
                builder.append(c);
            }
        } else {
            builder.append(c);
        }
    }

    return builder.build();
}

int main(int argc, char** argv)
{
    if (argc < 2)
        return 1;

    ++argv;
    String format = handle_escapes(*(argv++));
    auto format_string = format.characters();

    argc -= 2;

    ArgvWithCount arg { argv, argc };

    auto putch = [](auto*, auto ch) {
        putchar(ch);
    };

    auto previous_argc = 0;
    do {
        previous_argc = argc;
        PrintfImplementation::printf_internal<decltype(putch), PrintfImpl, ArgvWithCount, ArgvNextArgument>(putch, nullptr, format_string, arg);
    } while (argc && previous_argc != argc);

    return 0;
}
