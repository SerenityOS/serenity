/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace Markdown {

class Block {
public:
    virtual ~Block() { }

    virtual String render_to_html() const = 0;
    virtual String render_for_terminal(size_t view_width = 0) const = 0;
};

}
