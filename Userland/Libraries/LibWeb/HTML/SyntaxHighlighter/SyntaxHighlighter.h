/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibSyntax/Highlighter.h>

namespace Web::HTML {

class SyntaxHighlighter : public Syntax::Highlighter {
public:
    SyntaxHighlighter() = default;
    virtual ~SyntaxHighlighter() override = default;

    virtual bool is_identifier(void*) const override;
    virtual bool is_navigatable(void*) const override;

    virtual Syntax::Language language() const override { return Syntax::Language::HTML; }
    virtual void rehighlight(Palette const&) override;

protected:
    virtual Vector<MatchingTokenPair> matching_token_pairs() const override;
    virtual bool token_types_equal(void*, void*) const override;

    size_t m_line { 1 };
    size_t m_column { 0 };
};

}
