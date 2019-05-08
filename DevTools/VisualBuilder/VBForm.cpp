#include "VBForm.h"
#include "VBWidget.h"
#include "VBProperty.h"
#include <LibGUI/GPainter.h>
#include <LibGUI/GMenu.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GMessageBox.h>
#include <LibCore/CFile.h>

static VBForm* s_current;
VBForm* VBForm::current()
{
    return s_current;
}

VBForm::VBForm(const String& name, GWidget* parent)
    : GWidget(parent)
    , m_name(name)
{
    s_current = this;
    set_fill_with_background_color(true);
    set_background_color(Color::LightGray);
    set_greedy_for_hits(true);

    auto box1 = VBWidget::create(VBWidgetType::GSpinBox, *this);
    box1->set_rect({ 10, 10, 81, 21 });
    m_widgets.append(move(box1));

    auto box2 = VBWidget::create(VBWidgetType::GTextEditor, *this);
    box2->set_rect({ 100, 100, 161, 161 });
    m_widgets.append(move(box2));

    auto button1 = VBWidget::create(VBWidgetType::GButton, *this);
    button1->set_rect({ 200, 50, 81, 21 });
    m_widgets.append(move(button1));

    auto groupbox1 = VBWidget::create(VBWidgetType::GGroupBox, *this);
    groupbox1->set_rect({ 300, 150, 161, 51 });
    m_widgets.append(move(groupbox1));

    m_context_menu = make<GMenu>("Context menu");
    m_context_menu->add_action(GAction::create("Move to front", [this] (auto&) {
        if (auto* widget = single_selected_widget())
            widget->gwidget()->move_to_front();
    }));
    m_context_menu->add_action(GAction::create("Move to back", [this] (auto&) {
        if (auto* widget = single_selected_widget())
            widget->gwidget()->move_to_back();
    }));
    m_context_menu->add_action(GAction::create("Delete", [this] (auto&) {
        delete_selected_widgets();
    }));
}

void VBForm::context_menu_event(GContextMenuEvent& event)
{
    m_context_menu->popup(event.screen_position());
}

void VBForm::insert_widget(VBWidgetType type)
{
    auto widget = VBWidget::create(type, *this);
    widget->set_rect({ m_next_insertion_position, { m_grid_size * 10 + 1, m_grid_size * 5 + 1 } });
    m_next_insertion_position.move_by(m_grid_size, m_grid_size);
    m_widgets.append(move(widget));
}

VBForm::~VBForm()
{
}

void VBForm::paint_event(GPaintEvent& event)
{
    GPainter painter(*this);
    painter.add_clip_rect(event.rect());

    for (int y = 0; y < height(); y += m_grid_size) {
        for (int x = 0; x < width(); x += m_grid_size) {
            painter.set_pixel({ x, y }, Color::from_rgb(0x404040));
        }
    }
}

void VBForm::second_paint_event(GPaintEvent& event)
{
    GPainter painter(*this);
    painter.add_clip_rect(event.rect());

    for (auto& widget : m_widgets) {
        if (widget->is_selected()) {
            for_each_direction([&] (Direction direction) {
                painter.fill_rect(widget->grabber_rect(direction), Color::Black);
            });
        }
    }
}

bool VBForm::is_selected(const VBWidget& widget) const
{
    // FIXME: Fix HashTable and remove this const_cast.
    return m_selected_widgets.contains(const_cast<VBWidget*>(&widget));
}

VBWidget* VBForm::widget_at(const Point& position)
{
    auto* gwidget = child_at(position);
    if (!gwidget)
        return nullptr;
    return m_gwidget_map.get(gwidget);
}

void VBForm::grabber_mousedown_event(GMouseEvent& event, Direction grabber)
{
    m_transform_event_origin = event.position();
    for_each_selected_widget([] (auto& widget) { widget.capture_transform_origin_rect(); });
    m_resize_direction = grabber;
}

void VBForm::keydown_event(GKeyEvent& event)
{
    if (event.key() == KeyCode::Key_Delete) {
        delete_selected_widgets();
        return;
    }
    if (event.key() == KeyCode::Key_Tab) {
        if (m_widgets.is_empty())
            return;
        if (m_selected_widgets.is_empty()) {
            set_single_selected_widget(m_widgets.first());
            update();
            return;
        }
        int selected_widget_index = 0;
        for (; selected_widget_index < m_widgets.size(); ++selected_widget_index) {
            if (m_widgets[selected_widget_index] == *m_selected_widgets.begin())
                break;
        }
        ++selected_widget_index;
        if (selected_widget_index == m_widgets.size())
            selected_widget_index = 0;
        set_single_selected_widget(m_widgets[selected_widget_index]);
        update();
        return;
    }
    if (!m_selected_widgets.is_empty()) {
        switch (event.key()) {
        case KeyCode::Key_Up:
            update();
            for_each_selected_widget([this] (auto& widget) { widget.gwidget()->move_by(0, -m_grid_size); });
            break;
        case KeyCode::Key_Down:
            update();
            for_each_selected_widget([this] (auto& widget) { widget.gwidget()->move_by(0, m_grid_size); });
            break;
        case KeyCode::Key_Left:
            update();
            for_each_selected_widget([this] (auto& widget) { widget.gwidget()->move_by(-m_grid_size, 0); });
            break;
        case KeyCode::Key_Right:
            update();
            for_each_selected_widget([this] (auto& widget) { widget.gwidget()->move_by(m_grid_size, 0); });
            break;
        }
        return;
    }
}

void VBForm::set_single_selected_widget(VBWidget* widget)
{
    if (!widget) {
        if (!m_selected_widgets.is_empty()) {
            m_selected_widgets.clear();
            on_widget_selected(nullptr);
            update();
        }
        return;
    }
    m_selected_widgets.clear();
    m_selected_widgets.set(widget);
    on_widget_selected(m_selected_widgets.size() == 1 ? widget : nullptr);
    update();
}

void VBForm::add_to_selection(VBWidget& widget)
{
    m_selected_widgets.set(&widget);
    update();
}

void VBForm::remove_from_selection(VBWidget& widget)
{
    m_selected_widgets.remove(&widget);
    update();
}

void VBForm::mousedown_event(GMouseEvent& event)
{
    if (m_resize_direction == Direction::None) {
        bool hit_grabber = false;
        for_each_selected_widget([&] (auto& widget) {
            auto grabber = widget.grabber_at(event.position());
            if (grabber != Direction::None) {
                hit_grabber = true;
                return grabber_mousedown_event(event, grabber);
            }
        });
        if (hit_grabber)
            return;
    }
    auto* widget = widget_at(event.position());
    if (!widget) {
        set_single_selected_widget(nullptr);
        return;
    }
    if (event.button() == GMouseButton::Left || event.button() == GMouseButton::Right) {
        m_transform_event_origin = event.position();
        if (event.modifiers() == Mod_Ctrl)
            remove_from_selection(*widget);
        else if (event.modifiers() == Mod_Shift)
            add_to_selection(*widget);
        else if (!m_selected_widgets.contains(widget))
            set_single_selected_widget(widget);
        for_each_selected_widget([] (auto& widget) { widget.capture_transform_origin_rect(); });
        on_widget_selected(single_selected_widget());
    }
}

void VBForm::mousemove_event(GMouseEvent& event)
{
    if (event.buttons() & GMouseButton::Left) {
        if (m_resize_direction == Direction::None) {
            update();
            auto delta = event.position() - m_transform_event_origin;
            for_each_selected_widget([&] (auto& widget) {
                auto new_rect = widget.transform_origin_rect().translated(delta);
                new_rect.set_x(new_rect.x() - (new_rect.x() % m_grid_size));
                new_rect.set_y(new_rect.y() - (new_rect.y() % m_grid_size));
                widget.set_rect(new_rect);
            });
            return;
        }
        int diff_x = event.x() - m_transform_event_origin.x();
        int diff_y = event.y() - m_transform_event_origin.y();

        int change_x = 0;
        int change_y = 0;
        int change_w = 0;
        int change_h = 0;

        switch (m_resize_direction) {
        case Direction::DownRight:
            change_w = diff_x;
            change_h = diff_y;
            break;
        case Direction::Right:
            change_w = diff_x;
            break;
        case Direction::UpRight:
            change_w = diff_x;
            change_y = diff_y;
            change_h = -diff_y;
            break;
        case Direction::Up:
            change_y = diff_y;
            change_h = -diff_y;
            break;
        case Direction::UpLeft:
            change_x = diff_x;
            change_w = -diff_x;
            change_y = diff_y;
            change_h = -diff_y;
            break;
        case Direction::Left:
            change_x = diff_x;
            change_w = -diff_x;
            break;
        case Direction::DownLeft:
            change_x = diff_x;
            change_w = -diff_x;
            change_h = diff_y;
            break;
        case Direction::Down:
            change_h = diff_y;
            break;
        default:
            ASSERT_NOT_REACHED();
        }

        update();
        for_each_selected_widget([&] (auto& widget) {
            auto new_rect = widget.transform_origin_rect();
            Size minimum_size { 5, 5 };
            new_rect.set_x(new_rect.x() + change_x);
            new_rect.set_y(new_rect.y() + change_y);
            new_rect.set_width(max(minimum_size.width(), new_rect.width() + change_w));
            new_rect.set_height(max(minimum_size.height(), new_rect.height() + change_h));
            new_rect.set_x(new_rect.x() - (new_rect.x() % m_grid_size));
            new_rect.set_y(new_rect.y() - (new_rect.y() % m_grid_size));
            new_rect.set_width(new_rect.width() - (new_rect.width() % m_grid_size) + 1);
            new_rect.set_height(new_rect.height() - (new_rect.height() % m_grid_size) + 1);
            widget.set_rect(new_rect);
        });
    }
}

void VBForm::write_to_file(const String& path)
{
    CFile file(path);
    if (!file.open(CIODevice::WriteOnly)) {
        GMessageBox::show(String::format("Could not open '%s' for writing", path.characters()), "Error", GMessageBox::Type::Error, window());
        return;
    }
    file.printf("[Form]\n");
    file.printf("Name=%s\n", m_name.characters());
    file.printf("\n");
    int i = 0;
    for (auto& widget : m_widgets) {
        file.printf("[Widget %d]\n", i++);
        widget->for_each_property([&] (auto& property) {
            file.printf("%s=%s\n", property.name().characters(), property.value().to_string().characters());
        });
        file.printf("\n");
    }
}

void VBForm::dump()
{
    dbgprintf("[Form]\n");
    dbgprintf("Name=%s\n", m_name.characters());
    dbgprintf("\n");
    int i = 0;
    for (auto& widget : m_widgets) {
        dbgprintf("[Widget %d]\n", i++);
        widget->for_each_property([] (auto& property) {
            dbgprintf("%s=%s\n", property.name().characters(), property.value().to_string().characters());
        });
        dbgprintf("\n");
    }
}

void VBForm::mouseup_event(GMouseEvent& event)
{
    if (event.button() == GMouseButton::Left) {
        m_transform_event_origin = { };
        m_resize_direction = Direction::None;
    }
}

void VBForm::delete_selected_widgets()
{
    Vector<VBWidget*> to_delete;
    for_each_selected_widget([&] (auto& widget) {
        to_delete.append(&widget);
    });
    for (auto& widget : to_delete)
        m_widgets.remove_first_matching([&widget] (auto& entry) { return entry == widget; } );
    on_widget_selected(single_selected_widget());
}

template<typename Callback>
void VBForm::for_each_selected_widget(Callback callback)
{
    for (auto& widget : m_selected_widgets)
        callback(*widget);
}

VBWidget* VBForm::single_selected_widget()
{
    if (m_selected_widgets.size() != 1)
        return nullptr;
    return *m_selected_widgets.begin();
}
