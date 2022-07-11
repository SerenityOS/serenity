/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2021, Peter Elliott <pelliott@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <LibMarkdown/Document.h>
#include <LibMarkdown/LineIterator.h>
#include <LibMarkdown/Visitor.h>

namespace Markdown {

String Document::render_to_html() const
{
    StringBuilder builder;

    builder.append("<!DOCTYPE html>\n"sv);
    builder.append("<html>\n"sv);
    builder.append("<head>\n"sv);
    builder.append("<style>\n"sv);
    builder.append("code { white-space: pre; }\n"sv);
    builder.append("</style>\n"sv);
    builder.append("</head>\n"sv);
    builder.append("<body>\n"sv);

    builder.append(render_to_inline_html());

    builder.append("</body>\n"sv);
    builder.append("</html>\n"sv);
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

RecursionDecision Document::walk(Visitor& visitor) const
{
    RecursionDecision rd = visitor.visit(*this);
    if (rd != RecursionDecision::Recurse)
        return rd;

    return m_container->walk(visitor);
}

OwnPtr<Document> Document::parse(StringView str)
{
    Vector<StringView> const lines_vec = str.lines();
    LineIterator lines(lines_vec.begin());
    return make<Document>(ContainerBlock::parse(lines));
}

}
