/*
 * Copyright (c) 2022, Brian Gianforcaro <bgianf@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibSyntax/Highlighter.h>

namespace GUI {

class GitCommitSyntaxHighlighter final : public Syntax::Highlighter {
public:
    GitCommitSyntaxHighlighter() = default;
    virtual ~GitCommitSyntaxHighlighter() override = default;

    virtual Syntax::Language language() const override { return Syntax::Language::GitCommit; }
    virtual Optional<StringView> comment_prefix() const override { return {}; }
    virtual Optional<StringView> comment_suffix() const override { return {}; }
    virtual void rehighlight(Palette const&) override;

protected:
    virtual Vector<MatchingTokenPair> matching_token_pairs_impl() const override;
    virtual bool token_types_equal(u64, u64) const override;
};

}
