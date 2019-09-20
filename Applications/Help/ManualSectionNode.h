#pragma once

#include "ManualNode.h"

class ManualSectionNode : public ManualNode {
public:
    virtual ~ManualSectionNode() override {}

    ManualSectionNode(String section, String name)
        : m_section(section)
        , m_full_name(String::format("%s. %s", section.characters(), name.characters()))
    {
    }

    virtual NonnullOwnPtrVector<ManualNode>& children() const override
    {
        reify_if_needed();
        return m_children;
    }

    virtual const ManualNode* parent() const override { return nullptr; }
    virtual String name() const override { return m_full_name; }

    const String& section_name() const { return m_section; }
    String path() const;

private:
    void reify_if_needed() const;

    String m_section;
    String m_full_name;
    mutable NonnullOwnPtrVector<ManualNode> m_children;
    mutable bool m_reified { false };
};
