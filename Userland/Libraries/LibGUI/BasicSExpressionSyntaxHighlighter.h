/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibSyntax/Highlighter.h>

namespace GUI {

class BasicSExpressionSyntaxHighlighter final : public Syntax::Highlighter {
public:
    BasicSExpressionSyntaxHighlighter() { }
    virtual ~BasicSExpressionSyntaxHighlighter() override;

    virtual bool is_identifier(u64) const override;

    virtual Syntax::Language language() const override { return Syntax::Language::GenericSExpression; }
    virtual void rehighlight(const Palette&) override;

protected:
    virtual Vector<MatchingTokenPair> matching_token_pairs_impl() const override;
    virtual bool token_types_equal(u64, u64) const override;
};

}
