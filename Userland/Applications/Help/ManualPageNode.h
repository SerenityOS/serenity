/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "ManualNode.h"

class ManualSectionNode;

class ManualPageNode : public ManualNode {
public:
    virtual ~ManualPageNode() override = default;

    ManualPageNode(const ManualSectionNode& section, StringView page)
        : m_section(section)
        , m_page(page)
    {
    }

    virtual NonnullOwnPtrVector<ManualNode>& children() const override;
    virtual const ManualNode* parent() const override;
    virtual String name() const override { return m_page; };
    virtual bool is_page() const override { return true; }

    String path() const;

private:
    const ManualSectionNode& m_section;
    String m_page;
};
