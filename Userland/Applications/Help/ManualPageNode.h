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

    ManualPageNode(ManualSectionNode const& section, StringView page)
        : m_section(section)
        , m_page(page)
    {
    }

    virtual NonnullOwnPtrVector<ManualNode>& children() const override;
    virtual ManualNode const* parent() const override;
    virtual DeprecatedString name() const override { return m_page; };
    virtual bool is_page() const override { return true; }

    DeprecatedString path() const;

private:
    ManualSectionNode const& m_section;
    DeprecatedString m_page;
};
