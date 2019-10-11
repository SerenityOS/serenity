#pragma once

#include <LibHTML/Layout/LayoutNode.h>

class LayoutListItemMarker final : public LayoutNode {
public:
    LayoutListItemMarker();
    virtual ~LayoutListItemMarker() override;

    virtual void render(RenderingContext&) override;

private:
    virtual const char* class_name() const override { return "LayoutListItemMarker"; }
};
