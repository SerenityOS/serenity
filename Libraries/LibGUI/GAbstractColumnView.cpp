#include <AK/StringBuilder.h>
#include <LibGUI/GAbstractColumnView.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GMenu.h>
#include <LibGUI/GPainter.h>
#include <LibGUI/GScrollBar.h>

static const int minimum_column_width = 2;

GAbstractColumnView::GAbstractColumnView(GWidget* parent)
    : GAbstractView(parent)
{
    set_frame_shape(FrameShape::Container);
    set_frame_shadow(FrameShadow::Sunken);
    set_frame_thickness(2);

    set_should_hide_unnecessary_scrollbars(true);
}

GAbstractColumnView::~GAbstractColumnView()
{
}

void GAbstractColumnView::update_column_sizes()
{
    if (!m_size_columns_to_fit_content)
        return;

    if (!model())
        return;

    auto& model = *this->model();
    int column_count = model.column_count();
    int row_count = model.row_count();

    for (int column = 0; column < column_count; ++column) {
        if (is_column_hidden(column))
            continue;
        int header_width = header_font().width(model.column_name(column));
        int column_width = header_width;
        for (int row = 0; row < row_count; ++row) {
            auto cell_data = model.data(model.index(row, column));
            int cell_width = 0;
            if (cell_data.is_bitmap()) {
                cell_width = cell_data.as_bitmap().width();
            } else {
                cell_width = font().width(cell_data.to_string());
            }
            column_width = max(column_width, cell_width);
        }
        auto& column_data = this->column_data(column);
        column_data.width = max(column_data.width, column_width);
        column_data.has_initialized_width = true;
    }
}

void GAbstractColumnView::update_content_size()
{
    if (!model())
        return set_content_size({});

    int content_width = 0;
    int column_count = model()->column_count();
    for (int i = 0; i < column_count; ++i) {
        if (!is_column_hidden(i))
            content_width += column_width(i) + horizontal_padding() * 2;
    }
    int content_height = item_count() * item_height();

    set_content_size({ content_width, content_height });
    set_size_occupied_by_fixed_elements({ 0, header_height() });
}

Rect GAbstractColumnView::header_rect(int column_index) const
{
    if (!model())
        return {};
    if (is_column_hidden(column_index))
        return {};
    int x_offset = 0;
    for (int i = 0; i < column_index; ++i) {
        if (is_column_hidden(i))
            continue;
        x_offset += column_width(i) + horizontal_padding() * 2;
    }
    return { x_offset, 0, column_width(column_index) + horizontal_padding() * 2, header_height() };
}

void GAbstractColumnView::set_hovered_header_index(int index)
{
    if (m_hovered_column_header_index == index)
        return;
    m_hovered_column_header_index = index;
    update_headers();
}

void GAbstractColumnView::paint_headers(GPainter& painter)
{
    if (!headers_visible())
        return;
    int exposed_width = max(content_size().width(), width());
    painter.fill_rect({ 0, 0, exposed_width, header_height() }, Color::WarmGray);
    painter.draw_line({ 0, 0 }, { exposed_width - 1, 0 }, Color::White);
    painter.draw_line({ 0, header_height() - 1 }, { exposed_width - 1, header_height() - 1 }, Color::MidGray);
    int x_offset = 0;
    int column_count = model()->column_count();
    for (int column_index = 0; column_index < column_count; ++column_index) {
        if (is_column_hidden(column_index))
            continue;
        int column_width = this->column_width(column_index);
        bool is_key_column = model()->key_column() == column_index;
        Rect cell_rect(x_offset, 0, column_width + horizontal_padding() * 2, header_height());
        bool pressed = column_index == m_pressed_column_header_index && m_pressed_column_header_is_pressed;
        bool hovered = column_index == m_hovered_column_header_index && model()->column_metadata(column_index).sortable == GModel::ColumnMetadata::Sortable::True;
        StylePainter::paint_button(painter, cell_rect, ButtonStyle::Normal, pressed, hovered);
        String text;
        if (is_key_column) {
            StringBuilder builder;
            builder.append(model()->column_name(column_index));
            auto sort_order = model()->sort_order();
            if (sort_order == GSortOrder::Ascending)
                builder.append(" \xc3\xb6");
            else if (sort_order == GSortOrder::Descending)
                builder.append(" \xc3\xb7");
            text = builder.to_string();
        } else {
            text = model()->column_name(column_index);
        }
        auto text_rect = cell_rect.translated(horizontal_padding(), 0);
        if (pressed)
            text_rect.move_by(1, 1);
        painter.draw_text(text_rect, text, header_font(), TextAlignment::CenterLeft, Color::Black);
        x_offset += column_width + horizontal_padding() * 2;
    }
}

bool GAbstractColumnView::is_column_hidden(int column) const
{
    return !column_data(column).visibility;
}

void GAbstractColumnView::set_column_hidden(int column, bool hidden)
{
    auto& column_data = this->column_data(column);
    if (column_data.visibility == !hidden)
        return;
    column_data.visibility = !hidden;
    update_content_size();
    update();
}

GMenu& GAbstractColumnView::ensure_header_context_menu()
{
    // FIXME: This menu needs to be rebuilt if the model is swapped out,
    //        or if the column count/names change.
    if (!m_header_context_menu) {
        ASSERT(model());
        m_header_context_menu = GMenu::construct();

        for (int column = 0; column < model()->column_count(); ++column) {
            auto& column_data = this->column_data(column);
            auto name = model()->column_name(column);
            column_data.visibility_action = GAction::create(name, [this, column](GAction& action) {
                action.set_checked(!action.is_checked());
                set_column_hidden(column, !action.is_checked());
            });
            column_data.visibility_action->set_checkable(true);
            column_data.visibility_action->set_checked(true);

            m_header_context_menu->add_action(*column_data.visibility_action);
        }
    }
    return *m_header_context_menu;
}

const Font& GAbstractColumnView::header_font()
{
    return Font::default_bold_font();
}

void GAbstractColumnView::set_cell_painting_delegate(int column, OwnPtr<GTableCellPaintingDelegate>&& delegate)
{
    column_data(column).cell_painting_delegate = move(delegate);
}

void GAbstractColumnView::update_headers()
{
    Rect rect { 0, 0, frame_inner_rect().width(), header_height() };
    rect.move_by(frame_thickness(), frame_thickness());
    update(rect);
}

GAbstractColumnView::ColumnData& GAbstractColumnView::column_data(int column) const
{
    if (column >= m_column_data.size())
        m_column_data.resize(column + 1);
    return m_column_data.at(column);
}

Rect GAbstractColumnView::column_resize_grabbable_rect(int column) const
{
    if (!model())
        return {};
    auto header_rect = this->header_rect(column);
    return { header_rect.right() - 1, header_rect.top(), 4, header_rect.height() };
}

int GAbstractColumnView::column_width(int column_index) const
{
    if (!model())
        return 0;
    auto& column_data = this->column_data(column_index);
    if (!column_data.has_initialized_width) {
        ASSERT(!m_size_columns_to_fit_content);
        column_data.has_initialized_width = true;
        column_data.width = model()->column_metadata(column_index).preferred_width;
    }
    return column_data.width;
}

void GAbstractColumnView::mousemove_event(GMouseEvent& event)
{
    if (!model())
        return;

    if (m_in_column_resize) {
        auto delta = event.position() - m_column_resize_origin;
        int new_width = m_column_resize_original_width + delta.x();
        if (new_width <= minimum_column_width)
            new_width = minimum_column_width;
        ASSERT(m_resizing_column >= 0 && m_resizing_column < model()->column_count());
        auto& column_data = this->column_data(m_resizing_column);
        if (column_data.width != new_width) {
            column_data.width = new_width;
            dbg() << "New column width: " << new_width;
            update_content_size();
            update();
        }
        return;
    }

    if (m_pressed_column_header_index != -1) {
        auto header_rect = this->header_rect(m_pressed_column_header_index);
        if (header_rect.contains(event.position())) {
            if (!m_pressed_column_header_is_pressed)
                update_headers();
            m_pressed_column_header_is_pressed = true;
        } else {
            if (m_pressed_column_header_is_pressed)
                update_headers();
            m_pressed_column_header_is_pressed = false;
        }
        return;
    }

    if (event.buttons() == 0) {
        int column_count = model()->column_count();
        bool found_hovered_header = false;
        for (int i = 0; i < column_count; ++i) {
            if (column_resize_grabbable_rect(i).contains(event.position())) {
                window()->set_override_cursor(GStandardCursor::ResizeHorizontal);
                set_hovered_header_index(-1);
                return;
            }
            if (header_rect(i).contains(event.position())) {
                set_hovered_header_index(i);
                found_hovered_header = true;
            }
        }
        if (!found_hovered_header)
            set_hovered_header_index(-1);
    }
    window()->set_override_cursor(GStandardCursor::None);
}

void GAbstractColumnView::mouseup_event(GMouseEvent& event)
{
    auto adjusted_position = this->adjusted_position(event.position());
    if (event.button() == GMouseButton::Left) {
        if (m_in_column_resize) {
            if (!column_resize_grabbable_rect(m_resizing_column).contains(adjusted_position))
                window()->set_override_cursor(GStandardCursor::None);
            m_in_column_resize = false;
        }
        if (m_pressed_column_header_index != -1) {
            auto header_rect = this->header_rect(m_pressed_column_header_index);
            if (header_rect.contains(event.position())) {
                auto new_sort_order = GSortOrder::Ascending;
                if (model()->key_column() == m_pressed_column_header_index)
                    new_sort_order = model()->sort_order() == GSortOrder::Ascending
                        ? GSortOrder::Descending
                        : GSortOrder::Ascending;
                model()->set_key_column_and_sort_order(m_pressed_column_header_index, new_sort_order);
            }
            m_pressed_column_header_index = -1;
            m_pressed_column_header_is_pressed = false;
            update_headers();
        }
    }
}

void GAbstractColumnView::mousedown_event(GMouseEvent& event)
{
    if (!model())
        return;

    if (event.button() != GMouseButton::Left)
        return;

    if (event.y() < header_height()) {
        int column_count = model()->column_count();
        for (int i = 0; i < column_count; ++i) {
            if (column_resize_grabbable_rect(i).contains(event.position())) {
                m_resizing_column = i;
                m_in_column_resize = true;
                m_column_resize_original_width = column_width(i);
                m_column_resize_origin = event.position();
                return;
            }
            auto header_rect = this->header_rect(i);
            auto column_metadata = model()->column_metadata(i);
            if (header_rect.contains(event.position()) && column_metadata.sortable == GModel::ColumnMetadata::Sortable::True) {
                m_pressed_column_header_index = i;
                m_pressed_column_header_is_pressed = true;
                update_headers();
                return;
            }
        }
        return;
    }

    bool is_toggle;
    auto index = index_at_event_position(event.position(), is_toggle);
    if (!index.is_valid()) {
        selection().clear();
        return;
    }
    if (is_toggle && model()->row_count(index)) {
        toggle_index(index);
        return;
    }

    if (event.modifiers() & Mod_Ctrl)
        selection().toggle(index);
    else
        selection().set(index);
}

GModelIndex GAbstractColumnView::index_at_event_position(const Point& position, bool&) const
{
    if (!model())
        return {};

    auto adjusted_position = this->adjusted_position(position);
    for (int row = 0, row_count = model()->row_count(); row < row_count; ++row) {
        if (!row_rect(row).contains(adjusted_position))
            continue;
        for (int column = 0, column_count = model()->column_count(); column < column_count; ++column) {
            if (!content_rect(row, column).contains(adjusted_position))
                continue;
            return model()->index(row, column);
        }
        return model()->index(row, 0);
    }
    return {};
}

int GAbstractColumnView::item_count() const
{
    if (!model())
        return 0;
    return model()->row_count();
}

void GAbstractColumnView::keydown_event(GKeyEvent& event)
{
    if (!model())
        return;
    auto& model = *this->model();
    if (event.key() == KeyCode::Key_Return) {
        selection().for_each_index([this](auto& index) {
            activate(index);
        });
        return;
    }
    if (event.key() == KeyCode::Key_Up) {
        GModelIndex new_index;
        if (!selection().is_empty()) {
            auto old_index = selection().first();
            new_index = model.index(old_index.row() - 1, old_index.column());
        } else {
            new_index = model.index(0, 0);
        }
        if (model.is_valid(new_index)) {
            selection().set(new_index);
            scroll_into_view(new_index, Orientation::Vertical);
            update();
        }
        return;
    }
    if (event.key() == KeyCode::Key_Down) {
        GModelIndex new_index;
        if (!selection().is_empty()) {
            auto old_index = selection().first();
            new_index = model.index(old_index.row() + 1, old_index.column());
        } else {
            new_index = model.index(0, 0);
        }
        if (model.is_valid(new_index)) {
            selection().set(new_index);
            scroll_into_view(new_index, Orientation::Vertical);
            update();
        }
        return;
    }
    if (event.key() == KeyCode::Key_PageUp) {
        int items_per_page = visible_content_rect().height() / item_height();
        auto old_index = selection().first();
        auto new_index = model.index(max(0, old_index.row() - items_per_page), old_index.column());
        if (model.is_valid(new_index)) {
            selection().set(new_index);
            scroll_into_view(new_index, Orientation::Vertical);
            update();
        }
        return;
    }
    if (event.key() == KeyCode::Key_PageDown) {
        int items_per_page = visible_content_rect().height() / item_height();
        auto old_index = selection().first();
        auto new_index = model.index(min(model.row_count() - 1, old_index.row() + items_per_page), old_index.column());
        if (model.is_valid(new_index)) {
            selection().set(new_index);
            scroll_into_view(new_index, Orientation::Vertical);
            update();
        }
        return;
    }
    return GWidget::keydown_event(event);
}

void GAbstractColumnView::scroll_into_view(const GModelIndex& index, Orientation orientation)
{
    auto rect = row_rect(index.row()).translated(0, -header_height());
    GScrollableWidget::scroll_into_view(rect, orientation);
}

void GAbstractColumnView::doubleclick_event(GMouseEvent& event)
{
    if (!model())
        return;
    if (event.button() == GMouseButton::Left) {
        if (event.y() < header_height())
            return;
        if (!selection().is_empty()) {
            if (is_editable()) {
                begin_editing(selection().first());
            } else {
                selection().for_each_index([this](auto& index) {
                    activate(index);
                });
            }
        }
    }
}

void GAbstractColumnView::context_menu_event(GContextMenuEvent& event)
{
    if (!model())
        return;
    if (event.position().y() < header_height()) {
        ensure_header_context_menu().popup(event.screen_position());
        return;
    }

    bool is_toggle;
    auto index = index_at_event_position(event.position(), is_toggle);
    if (index.is_valid()) {
        if (!selection().contains(index))
            selection().set(index);
    } else {
        selection().clear();
    }
    if (on_context_menu_request)
        on_context_menu_request(index, event);
}

void GAbstractColumnView::leave_event(CEvent&)
{
    window()->set_override_cursor(GStandardCursor::None);
    set_hovered_header_index(-1);
}

Rect GAbstractColumnView::content_rect(int row, int column) const
{
    auto row_rect = this->row_rect(row);
    int x = 0;
    for (int i = 0; i < column; ++i)
        x += column_width(i) + horizontal_padding() * 2;

    return { row_rect.x() + x, row_rect.y(), column_width(column) + horizontal_padding() * 2, item_height() };
}

Rect GAbstractColumnView::content_rect(const GModelIndex& index) const
{
    return content_rect(index.row(), index.column());
}

Rect GAbstractColumnView::row_rect(int item_index) const
{
    return { 0, header_height() + (item_index * item_height()), max(content_size().width(), width()), item_height() };
}

Point GAbstractColumnView::adjusted_position(const Point& position) const
{
    return position.translated(horizontal_scrollbar().value() - frame_thickness(), vertical_scrollbar().value() - frame_thickness());
}

void GAbstractColumnView::did_update_model()
{
    GAbstractView::did_update_model();
    update_column_sizes();
    update_content_size();
    update();
}
