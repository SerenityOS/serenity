/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "ManualNode.h"

class ManualSectionNode : public ManualNode {
public:
    virtual ~ManualSectionNode() override = default;

    ManualSectionNode(String section, String name)
        : m_section(section)
        , m_full_name(String::formatted("{}. {}", section, name))
    {
    }

    virtual NonnullOwnPtrVector<ManualNode>& children() const override
    {
        reify_if_needed();
        return m_children;
    }

    virtual const ManualNode* parent() const override { return nullptr; }
    virtual String name() const override { return m_full_name; }
    virtual bool is_open() const override { return m_open; }
    void set_open(bool open);

    const String& section_name() const { return m_section; }
    String path() const;

private:
    void reify_if_needed() const;

    String m_section;
    String m_full_name;
    mutable NonnullOwnPtrVector<ManualNode> m_children;
    mutable bool m_reified { false };
    bool m_open { false };
};
