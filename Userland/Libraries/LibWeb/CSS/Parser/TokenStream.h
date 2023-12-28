/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>
#include <AK/Vector.h>
#include <LibWeb/CSS/Parser/ComponentValue.h>
#include <LibWeb/CSS/Parser/Tokenizer.h>

namespace Web::CSS::Parser {

template<typename T>
class TokenStream {
public:
    class StateTransaction {
    public:
        explicit StateTransaction(TokenStream<T>& token_stream)
            : m_token_stream(token_stream)
            , m_saved_iterator_offset(token_stream.m_iterator_offset)
        {
        }

        ~StateTransaction()
        {
            if (!m_commit)
                m_token_stream.m_iterator_offset = m_saved_iterator_offset;
        }

        StateTransaction create_child() { return StateTransaction(*this); }

        void commit()
        {
            m_commit = true;
            if (m_parent)
                m_parent->commit();
        }

    private:
        explicit StateTransaction(StateTransaction& parent)
            : m_parent(&parent)
            , m_token_stream(parent.m_token_stream)
            , m_saved_iterator_offset(parent.m_token_stream.m_iterator_offset)
        {
        }

        StateTransaction* m_parent { nullptr };
        TokenStream<T>& m_token_stream;
        int m_saved_iterator_offset { 0 };
        bool m_commit { false };
    };

    explicit TokenStream(Span<T const> tokens)
        : m_tokens(tokens)
        , m_eof(make_eof())
    {
    }

    explicit TokenStream(Vector<T> const& tokens)
        : m_tokens(tokens.span())
        , m_eof(make_eof())
    {
    }

    static TokenStream<T> of_single_token(T const& token)
    {
        return TokenStream(Span<T const> { &token, 1 });
    }

    TokenStream(TokenStream<T> const&) = delete;
    TokenStream(TokenStream<T>&&) = default;

    bool has_next_token()
    {
        return (size_t)(m_iterator_offset + 1) < m_tokens.size();
    }

    T const& next_token()
    {
        if (!has_next_token())
            return m_eof;

        ++m_iterator_offset;

        return m_tokens.at(m_iterator_offset);
    }

    T const& peek_token(int offset = 0)
    {
        if (!has_next_token())
            return m_eof;

        return m_tokens.at(m_iterator_offset + offset + 1);
    }

    T const& current_token()
    {
        if ((size_t)m_iterator_offset >= m_tokens.size())
            return m_eof;

        return m_tokens.at(m_iterator_offset);
    }

    void reconsume_current_input_token()
    {
        if (m_iterator_offset >= 0)
            --m_iterator_offset;
    }

    StateTransaction begin_transaction() { return StateTransaction(*this); }

    void skip_whitespace()
    {
        while (peek_token().is(Token::Type::Whitespace))
            next_token();
    }

    size_t token_count() const { return m_tokens.size(); }
    size_t remaining_token_count() const { return token_count() - m_iterator_offset - 1; }

    void dump_all_tokens()
    {
        dbgln("Dumping all tokens:");
        for (size_t i = 0; i < m_tokens.size(); ++i) {
            auto& token = m_tokens[i];
            if ((i - 1) == (size_t)m_iterator_offset)
                dbgln("-> {}", token.to_debug_string());
            else
                dbgln("   {}", token.to_debug_string());
        }
    }

    void copy_state(Badge<Parser>, TokenStream<T> const& other)
    {
        m_iterator_offset = other.m_iterator_offset;
    }

private:
    Span<T const> m_tokens;
    int m_iterator_offset { -1 };

    T make_eof()
    {
        if constexpr (IsSame<T, Token>) {
            return Tokenizer::create_eof_token();
        }
        if constexpr (IsSame<T, ComponentValue>) {
            return ComponentValue(Tokenizer::create_eof_token());
        }
    }

    T m_eof;
};

}
