#pragma once

#include <LibHTML/LayoutNode.h>
#include <LibHTML/Text.h>

class LayoutText : public LayoutNode {
public:
    explicit LayoutText(const Text&);
    virtual ~LayoutText() override;

    const Text& node() const { return static_cast<const Text&>(*LayoutNode::node()); }

    const String& text() const;

    virtual const char* class_name() const override { return "LayoutText"; }
    virtual bool is_text() const final { return true; }

private:
};
