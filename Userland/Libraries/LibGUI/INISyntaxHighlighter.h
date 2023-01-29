/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibSyntax/Highlighter.h>

namespace GUI {

class IniSyntaxHighlighter final : public Syntax::Highlighter {
public:
    IniSyntaxHighlighter() = default;
    virtual ~IniSyntaxHighlighter() override = default;

    virtual bool is_identifier(u64) const override;

    virtual Syntax::Language language() const override { return Syntax::Language::INI; }
    virtual Optional<StringView> comment_prefix() const override { return ";"sv; }
    virtual Optional<StringView> comment_suffix() const override { return {}; }
    virtual void rehighlight(Palette const&) override;

protected:
    virtual Vector<MatchingTokenPair> matching_token_pairs_impl() const override;
    virtual bool token_types_equal(u64, u64) const override;
};

}
