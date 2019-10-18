#pragma once

#include <LibHTML/Layout/LayoutBlock.h>

class LayoutTableRow;

class LayoutTable final : public LayoutBlock {
public:
    LayoutTable(const Element&, NonnullRefPtr<StyleProperties>);
    virtual ~LayoutTable() override;

    virtual void layout() override;

    LayoutTableRow* first_row();
    const LayoutTableRow* first_row() const;

private:
    virtual bool is_table() const override { return true; }
    virtual const char* class_name() const override { return "LayoutTable"; }
};

template<>
inline bool is<LayoutTable>(const LayoutNode& node)
{
    return node.is_table();
}
