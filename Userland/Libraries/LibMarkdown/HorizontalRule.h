/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/OwnPtr.h>
#include <YAK/StringView.h>
#include <YAK/Vector.h>
#include <LibMarkdown/Block.h>

namespace Markdown {

class HorizontalRule final : public Block {
public:
    HorizontalRule()
    {
    }
    virtual ~HorizontalRule() override { }

    virtual String render_to_html() const override;
    virtual String render_for_terminal(size_t view_width = 0) const override;
    static OwnPtr<HorizontalRule> parse(Vector<StringView>::ConstIterator& lines);
};

}
