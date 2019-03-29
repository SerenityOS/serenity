#pragma once

#include <LibGUI/GAbstractView.h>

class GTreeView : public GAbstractView {
public:
    explicit GTreeView(GWidget*);
    virtual ~GTreeView() override;

    virtual const char* class_name() const override { return "GTreeView"; }

    GModelIndex index_at_content_position(const Point&) const;

protected:
    virtual void paint_event(GPaintEvent&) override;
    virtual void mousedown_event(GMouseEvent&) override;

private:
    int item_height() const { return 16; }
    int max_item_width() const { return frame_inner_rect().width(); }
    int indent_width_in_pixels() const { return 12; }
    int icon_size() const { return 16; }
    int icon_spacing() const { return 4; }

    struct MetadataForIndex;

    MetadataForIndex& ensure_metadata_for_index(const GModelIndex&) const;

    mutable HashMap<void*, OwnPtr<MetadataForIndex>> m_view_metadata;
};
