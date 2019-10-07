#pragma once

#include <LibHTML/Layout/LayoutNode.h>

class LayoutBlock;

class LayoutInline : public LayoutNodeWithStyle {
public:
    LayoutInline(const Element&, NonnullRefPtr<StyleProperties>);
    virtual ~LayoutInline() override;

    virtual const char* class_name() const override { return "LayoutInline"; }

private:
};
