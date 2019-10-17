#pragma once

#include <LibHTML/Layout/LayoutBox.h>

class LayoutTableRow final : public LayoutBox {
public:
    LayoutTableRow(const Element&, NonnullRefPtr<StyleProperties>);
    virtual ~LayoutTableRow() override;

    virtual void layout() override;

private:
    virtual const char* class_name() const override { return "LayoutTableRow"; }
};
