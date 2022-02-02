/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibSyntax/Highlighter.h>

namespace GUI::GML {

class SyntaxHighlighter final : public Syntax::Highlighter {
public:
    SyntaxHighlighter() { }
    virtual ~SyntaxHighlighter() override;

    virtual bool is_identifier(u64) const override;

    virtual Syntax::Language language() const override { return Syntax::Language::GML; }
    virtual void rehighlight(const Palette&) override;

protected:
    virtual Vector<MatchingTokenPair> matching_token_pairs_impl() const override;
    virtual bool token_types_equal(u64, u64) const override;
};

}
