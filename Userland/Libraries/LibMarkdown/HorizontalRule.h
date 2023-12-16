/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/StringView.h>
#include <AK/Vector.h>
#include <LibMarkdown/Block.h>
#include <LibMarkdown/LineIterator.h>

namespace Markdown {

class HorizontalRule final : public Block {
public:
    HorizontalRule() = default;
    virtual ~HorizontalRule() override = default;

    virtual ByteString render_to_html(bool tight = false) const override;
    virtual Vector<ByteString> render_lines_for_terminal(size_t view_width = 0) const override;
    virtual RecursionDecision walk(Visitor&) const override;
    static OwnPtr<HorizontalRule> parse(LineIterator& lines);
};

}
