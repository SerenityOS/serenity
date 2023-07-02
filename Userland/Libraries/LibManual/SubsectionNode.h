/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibManual/PageNode.h>
#include <LibManual/SectionNode.h>

namespace Manual {

// A non-toplevel (i.e. not numbered) manual section.
class SubsectionNode : public SectionNode {
public:
    SubsectionNode(NonnullRefPtr<SectionNode const> parent, StringView name, RefPtr<PageNode> page = {});
    virtual ~SubsectionNode() = default;

    virtual Node const* parent() const override;
    virtual ErrorOr<String> path() const override;
    virtual ErrorOr<String> name() const override;
    virtual PageNode const* document() const override;
    virtual unsigned section_number() const override { return m_parent->section_number(); }

protected:
    NonnullRefPtr<SectionNode const> m_parent;

private:
    RefPtr<PageNode> m_page;
};

}
