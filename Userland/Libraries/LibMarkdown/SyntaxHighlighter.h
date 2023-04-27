/*
 * Copyright (c) 2023, Maciej <sppmacd@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibSyntax/Highlighter.h>

namespace Markdown {

class SyntaxHighlighter : public Syntax::Highlighter {

    virtual Syntax::Language language() const override;
    virtual Optional<StringView> comment_prefix() const override;
    virtual Optional<StringView> comment_suffix() const override;
    virtual void rehighlight(Palette const&) override;
    virtual Vector<MatchingTokenPair> matching_token_pairs_impl() const override;
    virtual bool token_types_equal(u64, u64) const override;
};

}
