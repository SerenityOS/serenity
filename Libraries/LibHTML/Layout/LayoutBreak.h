#pragma once

#include <LibHTML/DOM/HTMLBRElement.h>
#include <LibHTML/Layout/LayoutNode.h>

class LayoutBreak final : public LayoutNode {
public:
    explicit LayoutBreak(const HTMLBRElement&);
    virtual ~LayoutBreak() override;

    const HTMLBRElement& node() const { return to<HTMLBRElement>(*LayoutNode::node()); }

private:
    virtual const char* class_name() const override { return "LayoutBreak"; }
    virtual void split_into_lines(LayoutBlock&) override;
};
