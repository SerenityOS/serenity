#pragma once

#include <LibGUI/GAbstractView.h>

class GTreeView : public GAbstractView {
    C_OBJECT(GTreeView)
public:
    explicit GTreeView(GWidget*);
    virtual ~GTreeView() override;

    virtual void scroll_into_view(const GModelIndex&, Orientation);

protected:
    virtual void paint_event(GPaintEvent&) override;
    virtual void mousedown_event(GMouseEvent&) override;
    virtual void keydown_event(GKeyEvent&) override;
    virtual void did_update_selection() override;
    virtual void did_update_model() override;

private:
    GModelIndex index_at_content_position(const Point&, bool& is_toggle) const;
    int item_height() const { return 16; }
    int max_item_width() const { return frame_inner_rect().width(); }
    int indent_width_in_pixels() const { return 16; }
    int icon_size() const { return 16; }
    int icon_spacing() const { return 2; }
    int toggle_size() const { return 9; }
    int text_padding() const { return 2; }
    void update_content_size();

    template<typename Callback>
    void traverse_in_paint_order(Callback) const;

    struct MetadataForIndex;

    MetadataForIndex& ensure_metadata_for_index(const GModelIndex&) const;

    mutable HashMap<void*, NonnullOwnPtr<MetadataForIndex>> m_view_metadata;

    RefPtr<GraphicsBitmap> m_expand_bitmap;
    RefPtr<GraphicsBitmap> m_collapse_bitmap;
};
