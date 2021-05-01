/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibSyntax/Highlighter.h>

namespace JS {

class SyntaxHighlighter : public Syntax::Highlighter {
public:
    SyntaxHighlighter() { }
    virtual ~SyntaxHighlighter() override;

    virtual bool is_identifier(void*) const override;
    virtual bool is_navigatable(void*) const override;

    virtual Syntax::Language language() const override { return Syntax::Language::JavaScript; }
    virtual void rehighlight(const Palette&) override;

protected:
    virtual Vector<MatchingTokenPair> matching_token_pairs() const override;
    virtual bool token_types_equal(void*, void*) const override;
};

}
