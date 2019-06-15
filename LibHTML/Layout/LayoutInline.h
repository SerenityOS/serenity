#pragma once

#include <LibHTML/Layout/LayoutNode.h>

class Element;

class LayoutInline : public LayoutNode {
public:
    explicit LayoutInline(Element&);
    virtual ~LayoutInline() override;

    virtual const char* class_name() const override { return "LayoutInline"; }

private:
};
