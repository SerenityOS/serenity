#pragma once

#include <LibHTML/Layout/LayoutBlock.h>

class LayoutTableCell final : public LayoutBlock {
public:
    LayoutTableCell(const Element&, NonnullRefPtr<StyleProperties>);
    virtual ~LayoutTableCell() override;

private:
    virtual bool is_table_cell() const override { return true; }
    virtual const char* class_name() const override { return "LayoutTableCell"; }
};

template<>
inline bool is<LayoutTableCell>(const LayoutNode& node)
{
    return node.is_table_cell();
}
