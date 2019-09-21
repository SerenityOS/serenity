#pragma once

#include <LibHTML/DOM/Text.h>
#include <LibHTML/Layout/LayoutNode.h>

class LayoutText : public LayoutNode {
public:
    LayoutText(const Text&, StyleProperties&&);
    virtual ~LayoutText() override;

    const Text& node() const { return static_cast<const Text&>(*LayoutNode::node()); }

    const String& text() const;

    virtual const char* class_name() const override { return "LayoutText"; }
    virtual bool is_text() const final { return true; }
    virtual void layout() override;

    struct Run {
        Point pos;
        String text;
    };

    const Vector<Run>& runs() const { return m_runs; }

private:
    void compute_runs();

    Vector<Run> m_runs;
};
