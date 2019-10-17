#pragma once

#include <LibHTML/Layout/LayoutBlock.h>

class LayoutTable final : public LayoutBlock {
public:
    LayoutTable(const Element&, NonnullRefPtr<StyleProperties>);
    virtual ~LayoutTable() override;

    virtual void layout() override;

private:
    virtual bool is_table() const override { return true; }
    virtual const char* class_name() const override { return "LayoutTable"; }
};

template<>
inline bool is<LayoutTable>(const LayoutNode& node)
{
    return node.is_table();
}
