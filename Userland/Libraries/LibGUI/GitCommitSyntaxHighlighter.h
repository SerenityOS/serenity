/*
 * Copyright (c) 2022, Brian Gianforcaro <bgianf@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibSyntax/Highlighter.h>

namespace GUI {

class GitCommitSyntaxHighlighter final : public Syntax::Highlighter {
public:
    GitCommitSyntaxHighlighter() { }
    virtual ~GitCommitSyntaxHighlighter() override;

    virtual Syntax::Language language() const override { return Syntax::Language::GitCommit; }
    virtual void rehighlight(Palette const&) override;

protected:
    virtual Vector<MatchingTokenPair> matching_token_pairs_impl() const override;
    virtual bool token_types_equal(u64, u64) const override;
};

}
