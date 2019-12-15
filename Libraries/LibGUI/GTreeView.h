#pragma once

#include <LibGUI/GAbstractColumnView.h>

class GTreeView : public GAbstractColumnView {
    C_OBJECT(GTreeView)
public:
    virtual ~GTreeView() override;

    virtual void scroll_into_view(const GModelIndex&, Orientation);

    virtual int item_count() const override;

protected:
    explicit GTreeView(GWidget*);

    virtual void paint_event(GPaintEvent&) override;
    virtual void doubleclick_event(GMouseEvent&) override;
    virtual void keydown_event(GKeyEvent&) override;
    virtual void did_update_selection() override;
    virtual void did_update_model() override;

private:
    virtual GModelIndex index_at_event_position(const Point&, bool& is_toggle) const override;

    int item_height() const { return 16; }
    int max_item_width() const { return frame_inner_rect().width(); }
    int indent_width_in_pixels() const { return 16; }
    int icon_size() const { return 16; }
    int icon_spacing() const { return 2; }
    int toggle_size() const { return 9; }
    int text_padding() const { return 2; }
    virtual void toggle_index(const GModelIndex&) override;
    virtual void update_column_sizes() override;

    template<typename Callback>
    void traverse_in_paint_order(Callback) const;

    struct MetadataForIndex;

    MetadataForIndex& ensure_metadata_for_index(const GModelIndex&) const;

    mutable HashMap<void*, NonnullOwnPtr<MetadataForIndex>> m_view_metadata;

    RefPtr<GraphicsBitmap> m_expand_bitmap;
    RefPtr<GraphicsBitmap> m_collapse_bitmap;
};
