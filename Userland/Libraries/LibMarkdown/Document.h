/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/String.h>
#include <LibMarkdown/Block.h>
#include <LibMarkdown/ContainerBlock.h>

namespace Markdown {

class Document final {
public:
    Document(OwnPtr<ContainerBlock> container)
        : m_container(move(container))
    {
    }
    String render_to_html() const;
    String render_to_inline_html() const;
    String render_for_terminal(size_t view_width = 0) const;

    static OwnPtr<Document> parse(const StringView&);

private:
    OwnPtr<ContainerBlock> m_container;
};

}
