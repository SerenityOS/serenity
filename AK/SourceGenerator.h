/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/GenericLexer.h>
#include <AK/HashMap.h>
#include <AK/StringBuilder.h>

namespace AK {

class SourceGenerator {
    AK_MAKE_NONCOPYABLE(SourceGenerator);

public:
    using MappingType = HashMap<StringView, DeprecatedString>;

    explicit SourceGenerator(StringBuilder& builder, char opening = '@', char closing = '@')
        : m_builder(builder)
        , m_opening(opening)
        , m_closing(closing)
    {
    }
    explicit SourceGenerator(StringBuilder& builder, MappingType const& mapping, char opening = '@', char closing = '@')
        : m_builder(builder)
        , m_mapping(mapping)
        , m_opening(opening)
        , m_closing(closing)
    {
    }

    SourceGenerator(SourceGenerator&&) = default;

    SourceGenerator fork() { return SourceGenerator { m_builder, m_mapping, m_opening, m_closing }; }

    void set(StringView key, DeprecatedString value)
    {
        if (key.contains(m_opening) || key.contains(m_closing)) {
            warnln("SourceGenerator keys cannot contain the opening/closing delimiters `{}` and `{}`. (Keys are only wrapped in these when using them, not when setting them.)", m_opening, m_closing);
            VERIFY_NOT_REACHED();
        }
        m_mapping.set(key, move(value));
    }

    DeprecatedString get(StringView key) const
    {
        auto result = m_mapping.get(key);
        if (!result.has_value()) {
            warnln("No key named `{}` set on SourceGenerator", key);
            VERIFY_NOT_REACHED();
        }
        return result.release_value();
    }

    StringView as_string_view() const { return m_builder.string_view(); }
    DeprecatedString as_string() const { return m_builder.build(); }

    void append(StringView pattern)
    {
        GenericLexer lexer { pattern };

        while (!lexer.is_eof()) {
            // FIXME: It is a bit inconvenient, that 'consume_until' also consumes the 'stop' character, this makes
            //        the method less generic because there is no way to check if the 'stop' character ever appeared.
            auto const consume_until_without_consuming_stop_character = [&](char stop) {
                return lexer.consume_while([&](char ch) { return ch != stop; });
            };

            m_builder.append(consume_until_without_consuming_stop_character(m_opening));

            if (lexer.consume_specific(m_opening)) {
                auto const placeholder = consume_until_without_consuming_stop_character(m_closing);

                if (!lexer.consume_specific(m_closing))
                    VERIFY_NOT_REACHED();

                m_builder.append(get(placeholder));
            } else {
                VERIFY(lexer.is_eof());
            }
        }
    }

    void appendln(StringView pattern)
    {
        append(pattern);
        m_builder.append('\n');
    }

    template<size_t N>
    DeprecatedString get(char const (&key)[N])
    {
        return get(StringView { key, N - 1 });
    }

    template<size_t N>
    void set(char const (&key)[N], DeprecatedString value)
    {
        set(StringView { key, N - 1 }, value);
    }

    template<size_t N>
    void append(char const (&pattern)[N])
    {
        append(StringView { pattern, N - 1 });
    }

    template<size_t N>
    void appendln(char const (&pattern)[N])
    {
        appendln(StringView { pattern, N - 1 });
    }

private:
    StringBuilder& m_builder;
    MappingType m_mapping;
    char m_opening, m_closing;
};

}

#if USING_AK_GLOBALLY
using AK::SourceGenerator;
#endif
