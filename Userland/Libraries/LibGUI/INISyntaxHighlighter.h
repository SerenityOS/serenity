/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibSyntax/Highlighter.h>

namespace GUI {

class IniSyntaxHighlighter final : public Syntax::Highlighter {
public:
    IniSyntaxHighlighter() { }
    virtual ~IniSyntaxHighlighter() override;

    virtual bool is_identifier(void*) const override;

    virtual Syntax::Language language() const override { return Syntax::Language::INI; }
    virtual void rehighlight(const Palette&) override;

protected:
    virtual Vector<MatchingTokenPair> matching_token_pairs() const override;
    virtual bool token_types_equal(void*, void*) const override;
};

}
