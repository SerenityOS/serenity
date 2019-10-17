#pragma once

#include <LibHTML/Layout/LayoutBlock.h>

class LayoutTable final : public LayoutBlock {
public:
    LayoutTable(const Element&, NonnullRefPtr<StyleProperties>);
    virtual ~LayoutTable() override;

    virtual void layout() override;

private:
    virtual const char* class_name() const override { return "LayoutTable"; }
};
