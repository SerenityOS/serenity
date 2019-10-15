#pragma once

#include <LibHTML/Layout/LayoutBox.h>

class LayoutListItemMarker final : public LayoutBox {
public:
    LayoutListItemMarker();
    virtual ~LayoutListItemMarker() override;

    virtual void render(RenderingContext&) override;

private:
    virtual const char* class_name() const override { return "LayoutListItemMarker"; }
};
