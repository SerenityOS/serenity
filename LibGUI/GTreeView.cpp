#include <LibGUI/GTreeView.h>
#include <LibGUI/GPainter.h>
#include <LibGUI/GScrollBar.h>

//#define DEBUG_ITEM_RECTS

struct GTreeView::MetadataForIndex {
    bool open { false };
};

GTreeView::MetadataForIndex& GTreeView::ensure_metadata_for_index(const GModelIndex& index) const
{
    ASSERT(index.is_valid());
    auto it = m_view_metadata.find(index.internal_data());
    if (it != m_view_metadata.end())
        return *it->value;
    auto new_metadata = make<MetadataForIndex>();
    auto& new_metadata_ref = *new_metadata;
    m_view_metadata.set(index.internal_data(), move(new_metadata));
    return new_metadata_ref;
}

GTreeView::GTreeView(GWidget* parent)
    : GAbstractView(parent)
{
    set_frame_shape(GFrame::Shape::Container);
    set_frame_shadow(GFrame::Shadow::Sunken);
    set_frame_thickness(2);

    m_expand_bitmap = GraphicsBitmap::load_from_file("/res/icons/treeview-expand.png");
    m_collapse_bitmap = GraphicsBitmap::load_from_file("/res/icons/treeview-collapse.png");
}

GTreeView::~GTreeView()
{
}

GModelIndex GTreeView::index_at_content_position(const Point& position, bool& is_toggle) const
{
    is_toggle = false;
    if (!model())
        return { };
    GModelIndex result;
    traverse_in_paint_order([&] (const GModelIndex& index, const Rect& rect, const Rect& toggle_rect, int) {
        if (rect.contains(position)) {
            result = index;
            return IterationDecision::Abort;
        }
        if (toggle_rect.contains(position)) {
            result = index;
            is_toggle = true;
            return IterationDecision::Abort;
        }
        return IterationDecision::Continue;
    });
    return result;
}

void GTreeView::mousedown_event(GMouseEvent& event)
{
    if (!model())
        return;
    auto& model = *this->model();
    auto adjusted_position = event.position().translated(horizontal_scrollbar().value() - frame_thickness(), vertical_scrollbar().value() - frame_thickness());
    bool is_toggle;
    auto index = index_at_content_position(adjusted_position, is_toggle);
    if (!index.is_valid())
        return;

    if (model.selected_index() != index) {
        model.set_selected_index(index);
        update();
    }

    if (is_toggle && model.row_count(index)) {
        auto& metadata = ensure_metadata_for_index(index);
        metadata.open = !metadata.open;
        update();
    }
}

template<typename Callback>
void GTreeView::traverse_in_paint_order(Callback callback) const
{
    ASSERT(model());
    auto& model = *this->model();
    int indent_level = 0;
    int y_offset = 0;
    auto visible_content_rect = this->visible_content_rect();

    Function<IterationDecision(const GModelIndex&)> traverse_index = [&] (const GModelIndex& index) {
        int row_count_at_index = model.row_count(index);
        if (index.is_valid()) {
            auto& metadata = ensure_metadata_for_index(index);
            int x_offset = indent_level * indent_width_in_pixels();
            auto node_text = model.data(index, GModel::Role::Display).to_string();
            Rect rect = {
                x_offset, y_offset,
                icon_size() + icon_spacing() + text_padding() + font().width(node_text) + text_padding(), item_height()
            };
            Rect toggle_rect;
            if (row_count_at_index > 0) {
                int toggle_x = indent_width_in_pixels() * indent_level - icon_size() / 2 - 4;
                toggle_rect = { toggle_x, rect.y(), toggle_size(), toggle_size() };
                toggle_rect.center_vertically_within(rect);
            }
            if (rect.intersects(visible_content_rect)) {
                if (callback(index, rect, toggle_rect, indent_level) == IterationDecision::Abort)
                    return IterationDecision::Abort;
            }
            y_offset += item_height();
            // NOTE: Skip traversing children if this index is closed!
            if (!metadata.open)
                return IterationDecision::Continue;
        }

        ++indent_level;
        int row_count = model.row_count(index);
        for (int i = 0; i < row_count; ++i) {
            if (traverse_index(model.index(i, 0, index)) == IterationDecision::Abort)
                return IterationDecision::Abort;
        }
        --indent_level;
        return IterationDecision::Continue;
    };
    traverse_index(model.index(0, 0, GModelIndex()));
}

void GTreeView::paint_event(GPaintEvent& event)
{
    GFrame::paint_event(event);
    GPainter painter(*this);
    painter.add_clip_rect(frame_inner_rect());
    painter.add_clip_rect(event.rect());
    painter.fill_rect(event.rect(), Color::White);
    painter.translate(frame_inner_rect().location());

    if (!model())
        return;
    auto& model = *this->model();

    traverse_in_paint_order([&] (const GModelIndex& index, const Rect& rect, const Rect& toggle_rect, int indent_level) {
#ifdef DEBUG_ITEM_RECTS
        painter.fill_rect(rect, Color::LightGray);
#endif

        Color background_color = Color::from_rgb(0xffffff);
        Color text_color = Color::from_rgb(0x000000);

        Rect icon_rect = { rect.x(), rect.y(), icon_size(), icon_size() };
        auto icon = model.data(index, GModel::Role::Icon);
        if (icon.is_icon()) {
            if (auto* bitmap = icon.as_icon().bitmap_for_size(icon_size()))
                painter.blit(icon_rect.location(), *bitmap, bitmap->rect());
        }
        Rect text_rect = {
            icon_rect.right() + 1 + icon_spacing(), rect.y(),
            rect.width() - icon_size() - icon_spacing(), rect.height()
        };
        if (index == model.selected_index()) {
            background_color = is_focused() ? Color::from_rgb(0x84351a) : Color::from_rgb(0x606060);
            text_color = Color::from_rgb(0xffffff);
            painter.fill_rect(text_rect, background_color);
        }
        auto node_text = model.data(index, GModel::Role::Display).to_string();
        painter.draw_text(text_rect, node_text, TextAlignment::Center, text_color);
        auto index_at_indent = index;
        for (int i = indent_level; i >= 0; --i) {
            auto parent_of_index_at_indent = index_at_indent.parent();
            bool index_at_indent_is_last_in_parent = index_at_indent.row() == model.row_count(parent_of_index_at_indent) - 1;
            Point a { indent_width_in_pixels() * i - icon_size() / 2, rect.y() - 2 };
            Point b { a.x(), a.y() + item_height() - 1 };
            if (index_at_indent_is_last_in_parent)
                b.set_y(rect.center().y());
            if (!(i != indent_level && index_at_indent_is_last_in_parent))
                painter.draw_line(a, b, Color::MidGray);

            if (i == indent_level) {
                Point c { a.x(), rect.center().y() };
                Point d { c.x() + icon_size() / 2, c.y() };
                painter.draw_line(c, d, Color::MidGray);
            }
            index_at_indent = parent_of_index_at_indent;
        }

        if (!toggle_rect.is_empty()) {
            auto& metadata = ensure_metadata_for_index(index);
            if (metadata.open)
                painter.blit(toggle_rect.location(), *m_collapse_bitmap, m_collapse_bitmap->rect());
            else
                painter.blit(toggle_rect.location(), *m_expand_bitmap, m_expand_bitmap->rect());
        }

        return IterationDecision::Continue;
    });
}

void GTreeView::scroll_into_view(const GModelIndex& a_index, Orientation orientation)
{
    if (!a_index.is_valid())
        return;
    Rect found_rect;
    traverse_in_paint_order([&] (const GModelIndex& index, const Rect& rect, const Rect&, int) {
        if (index == a_index) {
            found_rect = rect;
            return IterationDecision::Abort;
        }
        return IterationDecision::Continue;
    });
    GScrollableWidget::scroll_into_view(found_rect, orientation);
}

void GTreeView::did_update_selection()
{
    ASSERT(model());
    auto& model = *this->model();
    auto index = model.selected_index();
    if (!index.is_valid())
        return;
    ensure_metadata_for_index(index).open = true;
    auto parent = index.parent();
    while (parent.is_valid()) {
        ensure_metadata_for_index(parent).open = true;
        parent = parent.parent();
    }
}
