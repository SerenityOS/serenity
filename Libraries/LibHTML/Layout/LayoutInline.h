#pragma once

#include <LibHTML/Layout/LayoutNode.h>

class Element;
class LayoutBlock;

class LayoutInline : public LayoutNode {
public:
    LayoutInline(const Node&, StyleProperties&&);
    virtual ~LayoutInline() override;

    virtual const char* class_name() const override { return "LayoutInline"; }
    virtual bool is_inline() const override { return true; }

    virtual void layout() override;

    virtual void split_into_lines(LayoutBlock& container);

private:
};
