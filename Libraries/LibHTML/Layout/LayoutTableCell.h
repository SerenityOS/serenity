#pragma once

#include <LibHTML/Layout/LayoutBlock.h>

class LayoutTableCell final : public LayoutBlock {
public:
    LayoutTableCell(const Element&, NonnullRefPtr<StyleProperties>);
    virtual ~LayoutTableCell() override;

    LayoutTableCell* next_cell() { return next_sibling_of_type<LayoutTableCell>(); }
    const LayoutTableCell* next_cell() const { return next_sibling_of_type<LayoutTableCell>(); }

private:
    virtual bool is_table_cell() const override { return true; }
    virtual const char* class_name() const override { return "LayoutTableCell"; }
};

template<>
inline bool is<LayoutTableCell>(const LayoutNode& node)
{
    return node.is_table_cell();
}
