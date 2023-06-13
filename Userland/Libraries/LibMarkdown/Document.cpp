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

ErrorOr<String> Document::render_to_html(StringView extra_head_contents) const
{
    StringBuilder builder;
    TRY(builder.try_append(R"~~~(<!DOCTYPE html>
<html>
<head>
    <style>
        code { white-space: pre; }
    </style>
)~~~"sv));
    if (!extra_head_contents.is_empty())
        TRY(builder.try_append(extra_head_contents));
    TRY(builder.try_append(R"~~~(
</head>
<body>
)~~~"sv));

    auto inline_html = TRY(render_to_inline_html());
    TRY(builder.try_append(inline_html));

    TRY(builder.try_append(R"~~~(
</body>
</html>)~~~"sv));

    return builder.to_string();
}

ErrorOr<String> Document::render_to_inline_html() const
{
    return String::from_deprecated_string(m_container->render_to_html());
}

ErrorOr<String> Document::render_for_terminal(size_t view_width) const
{
    StringBuilder builder;
    for (auto& line : m_container->render_lines_for_terminal(view_width)) {
        TRY(builder.try_append(line));
        TRY(builder.try_append("\n"sv));
    }

    return builder.to_string();
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
