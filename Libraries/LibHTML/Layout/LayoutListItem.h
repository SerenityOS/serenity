#pragma once

#include <LibHTML/DOM/Element.h>
#include <LibHTML/Layout/LayoutBlock.h>

class LayoutListItemMarker;

class LayoutListItem final : public LayoutBlock {
public:
    LayoutListItem(const Element&, NonnullRefPtr<StyleProperties>);
    virtual ~LayoutListItem() override;

    virtual void layout() override;

private:
    virtual const char* class_name() const override { return "LayoutListItem"; }

    RefPtr<LayoutListItemMarker> m_marker;
};
