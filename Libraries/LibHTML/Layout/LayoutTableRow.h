#pragma once

#include <LibHTML/Layout/LayoutBox.h>

class LayoutTableRow final : public LayoutBox {
public:
    LayoutTableRow(const Element&, NonnullRefPtr<StyleProperties>);
    virtual ~LayoutTableRow() override;

    virtual void layout() override;

private:
    virtual bool is_table_row() const override { return true; }
    virtual const char* class_name() const override { return "LayoutTableRow"; }
};

template<>
inline bool is<LayoutTableRow>(const LayoutNode& node)
{
    return node.is_table_row();
}
