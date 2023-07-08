/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <LibManual/Node.h>

namespace Manual {

class SectionNode;

class PageNode : public Node {
public:
    virtual ~PageNode() override = default;

    PageNode(NonnullRefPtr<SectionNode const> section, String page)
        : m_section(move(section))
        , m_page(move(page))
    {
    }

    virtual ErrorOr<Span<NonnullRefPtr<Node const>>> children() const override;
    virtual Node const* parent() const override;
    virtual ErrorOr<String> name() const override { return m_page; }
    virtual bool is_page() const override { return true; }
    virtual PageNode const* document() const override { return this; }
    virtual unsigned section_number() const override;

    virtual ErrorOr<String> path() const override;

    static ErrorOr<NonnullRefPtr<PageNode>> help_index_page();

private:
    NonnullRefPtr<SectionNode const> m_section;
    String m_page;
};

}
