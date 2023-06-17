/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/GenericLexer.h>
#include <AK/HashMap.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>

namespace AK {

class SourceGenerator {
    AK_MAKE_NONCOPYABLE(SourceGenerator);

public:
    using MappingType = HashMap<StringView, String>;

    explicit SourceGenerator(StringBuilder& builder, char opening = '@', char closing = '@')
        : m_builder(builder)
        , m_opening(opening)
        , m_closing(closing)
    {
    }
    explicit SourceGenerator(StringBuilder& builder, MappingType&& mapping, char opening = '@', char closing = '@')
        : m_builder(builder)
        , m_mapping(move(mapping))
        , m_opening(opening)
        , m_closing(closing)
    {
    }

    SourceGenerator(SourceGenerator&&) = default;

    ErrorOr<SourceGenerator> fork()
    {
        return SourceGenerator { m_builder, TRY(m_mapping.clone()), m_opening, m_closing };
    }

    ErrorOr<void> set(StringView key, String value)
    {
        if (key.contains(m_opening) || key.contains(m_closing)) {
            warnln("SourceGenerator keys cannot contain the opening/closing delimiters `{}` and `{}`. (Keys are only wrapped in these when using them, not when setting them.)", m_opening, m_closing);
            VERIFY_NOT_REACHED();
        }
        TRY(m_mapping.try_set(key, move(value)));
        return {};
    }

    String get(StringView key) const
    {
        auto result = m_mapping.get(key);
        if (!result.has_value()) {
            warnln("No key named `{}` set on SourceGenerator", key);
            VERIFY_NOT_REACHED();
        }
        return result.release_value();
    }

    StringView as_string_view() const { return m_builder.string_view(); }

    ErrorOr<void> try_append(StringView pattern)
    {
        GenericLexer lexer { pattern };

        while (!lexer.is_eof()) {
            TRY(m_builder.try_append(lexer.consume_until(m_opening)));

            if (lexer.consume_specific(m_opening)) {
                auto const placeholder = lexer.consume_until(m_closing);

                if (!lexer.consume_specific(m_closing))
                    VERIFY_NOT_REACHED();

                TRY(m_builder.try_append(get(placeholder)));
            } else {
                VERIFY(lexer.is_eof());
            }
        }
        return {};
    }

    ErrorOr<void> try_appendln(StringView pattern)
    {
        TRY(try_append(pattern));
        TRY(m_builder.try_append('\n'));
        return {};
    }

    template<size_t N>
    String get(char const (&key)[N])
    {
        return get(StringView { key, N - 1 });
    }

    template<size_t N>
    ErrorOr<void> set(char const (&key)[N], String value)
    {
        return set(StringView { key, N - 1 }, value);
    }

    template<size_t N>
    ErrorOr<void> try_append(char const (&pattern)[N])
    {
        return try_append(StringView { pattern, N - 1 });
    }

    template<size_t N>
    ErrorOr<void> try_appendln(char const (&pattern)[N])
    {
        return try_appendln(StringView { pattern, N - 1 });
    }

    // FIXME: These are deprecated.
    void set(StringView key, DeprecatedString value)
    {
        MUST(set(key, MUST(String::from_deprecated_string(value))));
    }
    template<size_t N>
    void set(char const (&key)[N], DeprecatedString value)
    {
        set(StringView { key, N - 1 }, value);
    }
    void append(StringView pattern) { MUST(try_append(pattern)); }
    void appendln(StringView pattern) { MUST(try_appendln(pattern)); }
    template<size_t N>
    void append(char const (&pattern)[N]) { MUST(try_append(pattern)); }
    template<size_t N>
    void appendln(char const (&pattern)[N]) { MUST(try_appendln(pattern)); }

private:
    StringBuilder& m_builder;
    MappingType m_mapping;
    char m_opening, m_closing;
};

}

#if USING_AK_GLOBALLY
using AK::SourceGenerator;
#endif
