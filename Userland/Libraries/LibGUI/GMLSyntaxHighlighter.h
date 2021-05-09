/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibSyntax/Highlighter.h>

namespace GUI {

class GMLSyntaxHighlighter final : public Syntax::Highlighter {
public:
    GMLSyntaxHighlighter() { }
    virtual ~GMLSyntaxHighlighter() override;

    virtual bool is_identifier(void*) const override;

    virtual Syntax::Language language() const override { return Syntax::Language::GML; }
    virtual void rehighlight(const Palette&) override;

protected:
    virtual Vector<MatchingTokenPair> matching_token_pairs() const override;
    virtual bool token_types_equal(void*, void*) const override;
};

}
