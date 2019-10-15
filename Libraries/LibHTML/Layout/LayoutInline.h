#pragma once

#include <LibHTML/Layout/LayoutBox.h>

class LayoutBlock;

class LayoutInline : public LayoutNodeWithStyleAndBoxModelMetrics {
public:
    LayoutInline(const Element&, NonnullRefPtr<StyleProperties>);
    virtual ~LayoutInline() override;
    virtual const char* class_name() const override { return "LayoutInline"; }
};
