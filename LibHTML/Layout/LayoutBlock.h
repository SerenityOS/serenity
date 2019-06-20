#pragma once

#include <LibHTML/Layout/LayoutNode.h>

class Element;

class LayoutBlock : public LayoutNode {
public:
    explicit LayoutBlock(Element&);
    virtual ~LayoutBlock() override;

    virtual const char* class_name() const override { return "LayoutBlock"; }

    virtual void layout() override;

private:
};
