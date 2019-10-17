#pragma once

#include <LibHTML/Layout/LayoutBlock.h>

class LayoutTableCell final : public LayoutBlock {
public:
    LayoutTableCell(const Element&, NonnullRefPtr<StyleProperties>);
    virtual ~LayoutTableCell() override;

private:
    virtual const char* class_name() const override { return "LayoutTableCell"; }
};
