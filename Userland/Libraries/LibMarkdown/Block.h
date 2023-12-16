/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RecursionDecision.h>
#include <AK/StringView.h>
#include <AK/Vector.h>
#include <LibMarkdown/Forward.h>

namespace Markdown {

class Block {
public:
    virtual ~Block() = default;

    virtual ByteString render_to_html(bool tight = false) const = 0;
    virtual Vector<ByteString> render_lines_for_terminal(size_t view_width = 0) const = 0;
    virtual RecursionDecision walk(Visitor&) const = 0;
};

}
