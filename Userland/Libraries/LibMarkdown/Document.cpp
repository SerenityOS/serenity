/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2021, Peter Elliott <pelliott@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <LibMarkdown/CodeBlock.h>
#include <LibMarkdown/Document.h>
#include <LibMarkdown/Heading.h>
#include <LibMarkdown/HorizontalRule.h>
#include <LibMarkdown/List.h>
#include <LibMarkdown/Paragraph.h>
#include <LibMarkdown/Table.h>

namespace Markdown {

String Document::render_to_html() const
{
    StringBuilder builder;

    builder.append("<!DOCTYPE html>\n");
    builder.append("<html>\n");
    builder.append("<head>\n");
    builder.append("<style>\n");
    builder.append("code { white-space: pre; }\n");
    builder.append("</style>\n");
    builder.append("</head>\n");
    builder.append("<body>\n");

    builder.append(render_to_inline_html());

    builder.append("</body>\n");
    builder.append("</html>\n");
    return builder.build();
}

String Document::render_to_inline_html() const
{
    return m_container->render_to_html();
}

String Document::render_for_terminal(size_t view_width) const
{
    return m_container->render_for_terminal(view_width);
}

OwnPtr<Document> Document::parse(const StringView& str)
{
    const Vector<StringView> lines_vec = str.lines();
    auto lines = lines_vec.begin();
    return make<Document>(ContainerBlock::parse(lines));
}

}
