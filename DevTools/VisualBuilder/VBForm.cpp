#include "VBForm.h"
#include "VBProperty.h"
#include "VBWidget.h"
#include "VBWidgetRegistry.h"
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/StringBuilder.h>
#include <LibCore/CFile.h>
#include <LibDraw/PNGLoader.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GMenu.h>
#include <LibGUI/GMessageBox.h>
#include <LibGUI/GPainter.h>

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
    set_greedy_for_hits(true);

    m_context_menu = GMenu::construct();
    m_context_menu->add_action(GCommonActions::make_move_to_front_action([this](auto&) {
        if (auto* widget = single_selected_widget())
            widget->gwidget()->move_to_front();
    }));
    m_context_menu->add_action(GCommonActions::make_move_to_back_action([this](auto&) {
        if (auto* widget = single_selected_widget())
            widget->gwidget()->move_to_back();
    }));
    m_context_menu->add_separator();
    m_context_menu->add_action(GAction::create("Lay out horizontally", load_png("/res/icons/16x16/layout-horizontally.png"), [this](auto&) {
        if (auto* widget = single_selected_widget()) {
            dbg() << "Giving " << *widget->gwidget() << " a horizontal box layout";
            widget->gwidget()->set_layout(make<GBoxLayout>(Orientation::Horizontal));
        }
    }));
    m_context_menu->add_action(GAction::create("Lay out vertically", load_png("/res/icons/16x16/layout-vertically.png"), [this](auto&) {
        if (auto* widget = single_selected_widget()) {
            dbg() << "Giving " << *widget->gwidget() << " a vertical box layout";
            widget->gwidget()->set_layout(make<GBoxLayout>(Orientation::Vertical));
        }
    }));
    m_context_menu->add_separator();
    m_context_menu->add_action(GCommonActions::make_delete_action([this](auto&) {
        delete_selected_widgets();
    }));
}

void VBForm::context_menu_event(GContextMenuEvent& event)
{
    m_context_menu->popup(event.screen_position());
}

void VBForm::insert_widget(VBWidgetType type)
{
    auto* insertion_parent = single_selected_widget();
    auto widget = VBWidget::create(type, *this, insertion_parent);
    Point insertion_position = m_next_insertion_position;
    if (insertion_parent)
        insertion_position.move_by(insertion_parent->gwidget()->window_relative_rect().location());
    widget->set_rect({ insertion_position, { m_grid_size * 10 + 1, m_grid_size * 5 + 1 } });
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
        if (widget.is_selected()) {
            for_each_direction([&](auto direction) {
                bool in_layout = widget.is_in_layout();
                auto grabber_rect = widget.grabber_rect(direction);
                painter.fill_rect(grabber_rect, in_layout ? Color::White : Color::Black);
                if (in_layout)
                    painter.draw_rect(grabber_rect, Color::Black);
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
    auto result = hit_test(position, GWidget::ShouldRespectGreediness::No);
    if (!result.widget)
        return nullptr;
    auto* gwidget = result.widget;
    while (gwidget) {
        if (auto* widget = m_gwidget_map.get(gwidget).value_or(nullptr))
            return widget;
        gwidget = gwidget->parent_widget();
    }
    return nullptr;
}

void VBForm::grabber_mousedown_event(GMouseEvent& event, Direction grabber)
{
    m_transform_event_origin = event.position();
    for_each_selected_widget([](auto& widget) { widget.capture_transform_origin_rect(); });
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
            set_single_selected_widget(&m_widgets.first());
            update();
            return;
        }
        int selected_widget_index = 0;
        for (; selected_widget_index < m_widgets.size(); ++selected_widget_index) {
            if (&m_widgets[selected_widget_index] == *m_selected_widgets.begin())
                break;
        }
        ++selected_widget_index;
        if (selected_widget_index == m_widgets.size())
            selected_widget_index = 0;
        set_single_selected_widget(&m_widgets[selected_widget_index]);
        update();
        return;
    }
    if (!m_selected_widgets.is_empty()) {
        switch (event.key()) {
        case KeyCode::Key_Up:
            update();
            for_each_selected_widget([this](auto& widget) {
                if (widget.is_in_layout())
                    return;
                widget.gwidget()->move_by(0, -m_grid_size);
            });
            break;
        case KeyCode::Key_Down:
            update();
            for_each_selected_widget([this](auto& widget) {
                if (widget.is_in_layout())
                    return;
                widget.gwidget()->move_by(0, m_grid_size);
            });
            break;
        case KeyCode::Key_Left:
            update();
            for_each_selected_widget([this](auto& widget) {
                if (widget.is_in_layout())
                    return;
                widget.gwidget()->move_by(-m_grid_size, 0);
            });
            break;
        case KeyCode::Key_Right:
            update();
            for_each_selected_widget([this](auto& widget) {
                if (widget.is_in_layout())
                    return;
                widget.gwidget()->move_by(m_grid_size, 0);
            });
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
        for_each_selected_widget([&](auto& widget) {
            if (widget.is_in_layout())
                return;
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
        for_each_selected_widget([](auto& widget) { widget.capture_transform_origin_rect(); });
        on_widget_selected(single_selected_widget());
    }
}

void VBForm::mousemove_event(GMouseEvent& event)
{
    if (event.buttons() & GMouseButton::Left) {
        if (m_resize_direction == Direction::None) {
            update();
            auto delta = event.position() - m_transform_event_origin;
            for_each_selected_widget([&](auto& widget) {
                if (widget.is_in_layout())
                    return;
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
        for_each_selected_widget([&](auto& widget) {
            if (widget.is_in_layout())
                return;
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

        set_cursor_type_from_grabber(m_resize_direction);
    } else {
        for (auto& widget : m_selected_widgets) {
            if (widget->is_in_layout())
                continue;
            auto grabber_at = widget->grabber_at(event.position());
            set_cursor_type_from_grabber(grabber_at);
            if (grabber_at != Direction::None)
                break;
        }
    }
}

void VBForm::load_from_file(const String& path)
{
    auto file = CFile::construct(path);
    if (!file->open(CIODevice::ReadOnly)) {
        GMessageBox::show(String::format("Could not open '%s' for reading", path.characters()), "Error", GMessageBox::Type::Error, GMessageBox::InputType::OK, window());
        return;
    }

    auto file_contents = file->read_all();
    auto form_json = JsonValue::from_string(file_contents);

    if (!form_json.is_object()) {
        GMessageBox::show(String::format("Could not parse '%s'", path.characters()), "Error", GMessageBox::Type::Error, GMessageBox::InputType::OK, window());
        return;
    }

    m_name = form_json.as_object().get("name").to_string();
    auto widgets = form_json.as_object().get("widgets").as_array();

    widgets.for_each([&](const JsonValue& widget_value) {
        auto& widget_object = widget_value.as_object();
        auto widget_class = widget_object.get("class").as_string();
        auto widget_type = widget_type_from_class_name(widget_class);
        // FIXME: Construct VBWidget within the right parent..
        auto vbwidget = VBWidget::create(widget_type, *this, nullptr);
        widget_object.for_each_member([&](auto& property_name, const JsonValue& property_value) {
            (void)property_name;
            (void)property_value;
            VBProperty& property = vbwidget->property(property_name);
            dbgprintf("Set property %s.%s to '%s'\n", widget_class.characters(), property_name.characters(), property_value.to_string().characters());
            property.set_value(property_value);
        });
        m_widgets.append(vbwidget);
    });
}

void VBForm::write_to_file(const String& path)
{
    auto file = CFile::construct(path);
    if (!file->open(CIODevice::WriteOnly)) {
        GMessageBox::show(String::format("Could not open '%s' for writing", path.characters()), "Error", GMessageBox::Type::Error, GMessageBox::InputType::OK, window());
        return;
    }

    JsonObject form_object;
    form_object.set("name", m_name);
    JsonArray widget_array;
    for (auto& widget : m_widgets) {
        JsonObject widget_object;
        widget.for_each_property([&](auto& property) {
            if (property.value().is_bool())
                widget_object.set(property.name(), property.value().to_bool());
            else if (property.value().is_int())
                widget_object.set(property.name(), property.value().to_int());
            else
                widget_object.set(property.name(), property.value().to_string());
        });
        widget_array.append(widget_object);
    }
    form_object.set("widgets", widget_array);
    file->write(form_object.to_string());
}

void VBForm::dump()
{
    dbgprintf("[Form]\n");
    dbgprintf("Name=%s\n", m_name.characters());
    dbgprintf("\n");
    int i = 0;
    for (auto& widget : m_widgets) {
        dbgprintf("[Widget %d]\n", i++);
        widget.for_each_property([](auto& property) {
            dbgprintf("%s=%s\n", property.name().characters(), property.value().to_string().characters());
        });
        dbgprintf("\n");
    }
}

void VBForm::mouseup_event(GMouseEvent& event)
{
    if (event.button() == GMouseButton::Left) {
        m_transform_event_origin = {};
        m_resize_direction = Direction::None;
    }
}

void VBForm::delete_selected_widgets()
{
    Vector<VBWidget*> to_delete;
    for_each_selected_widget([&](auto& widget) {
        to_delete.append(&widget);
    });
    if (to_delete.is_empty())
        return;
    for (auto& widget : to_delete)
        m_widgets.remove_first_matching([&widget](auto& entry) { return entry == widget; });
    on_widget_selected(single_selected_widget());
    update();
}

template<typename Callback>
void VBForm::for_each_selected_widget(Callback callback)
{
    for (auto& widget : m_selected_widgets)
        callback(*widget);
}

void VBForm::set_cursor_type_from_grabber(Direction grabber)
{
    if (grabber == m_mouse_direction_type)
        return;

    switch (grabber) {
    case Direction::Up:
    case Direction::Down:
        window()->set_override_cursor(GStandardCursor::ResizeVertical);
        break;
    case Direction::Left:
    case Direction::Right:
        window()->set_override_cursor(GStandardCursor::ResizeHorizontal);
        break;
    case Direction::UpLeft:
    case Direction::DownRight:
        window()->set_override_cursor(GStandardCursor::ResizeDiagonalTLBR);
        break;
    case Direction::UpRight:
    case Direction::DownLeft:
        window()->set_override_cursor(GStandardCursor::ResizeDiagonalBLTR);
        break;
    case Direction::None:
        window()->set_override_cursor(GStandardCursor::None);
        break;
    }

    m_mouse_direction_type = grabber;
}

VBWidget* VBForm::single_selected_widget()
{
    if (m_selected_widgets.size() != 1)
        return nullptr;
    return *m_selected_widgets.begin();
}
