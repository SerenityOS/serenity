/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <LibMarkdown/Document.h>
#include <LibMarkdown/Visitor.h>

class MarkdownTableFinder final : Markdown::Visitor {
public:
    ~MarkdownTableFinder() = default;

    static MarkdownTableFinder analyze(Markdown::Document const& document)
    {
        MarkdownTableFinder finder;
        document.walk(finder);
        return finder;
    }

    size_t table_count() const { return m_tables.size(); }
    Vector<Markdown::Table const*> const& tables() const { return m_tables; }

private:
    MarkdownTableFinder() { }

    virtual RecursionDecision visit(Markdown::Table const& table) override
    {
        if (m_tables.size() >= 1)
            return RecursionDecision::Break;
        m_tables.append(&table);
        return RecursionDecision::Recurse;
    }

    Vector<Markdown::Table const*> m_tables;
};
