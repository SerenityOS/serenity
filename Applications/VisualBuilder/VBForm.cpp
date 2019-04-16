#include "VBForm.h"
#include "VBWidget.h"
#include <LibGUI/GPainter.h>
#include <LibGUI/GMenu.h>
#include <LibGUI/GAction.h>

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

    auto context_menu = make<GMenu>("Context menu");
    context_menu->add_action(GAction::create("Move to front", [this] (auto&) {
        if (m_selected_widget)
            m_selected_widget->gwidget()->move_to_front();
    }));
    context_menu->add_action(GAction::create("Move to back", [this] (auto&) {
        if (m_selected_widget)
            m_selected_widget->gwidget()->move_to_back();
    }));
    set_context_menu(move(context_menu), GWidget::ContextMenuMode::PassthroughMouseEvent);
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
            painter.set_pixel({ x, y }, Color::Black);
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
    return &widget == m_selected_widget;
}

VBWidget* VBForm::widget_at(const Point& position)
{
    auto* gwidget = child_at(position);
    if (!gwidget)
        return nullptr;
    return m_gwidget_map.get(gwidget);
}

void VBForm::grabber_mousedown_event(GMouseEvent& event, VBWidget& widget, Direction grabber)
{
    m_transform_event_origin = event.position();
    m_transform_widget_origin_rect = widget.rect();
    m_resize_direction = grabber;
}

void VBForm::mousedown_event(GMouseEvent& event)
{
    if (m_selected_widget && m_resize_direction == Direction::None) {
        auto grabber = m_selected_widget->grabber_at(event.position());
        if (grabber != Direction::None)
            return grabber_mousedown_event(event, *m_selected_widget, grabber);
    }
    auto* widget = widget_at(event.position());
    if (!widget) {
        if (m_selected_widget) {
            m_selected_widget = nullptr;
            if (on_widget_selected)
                on_widget_selected(nullptr);
            update();
        }
        return;
    }
    if (event.button() == GMouseButton::Left || event.button() == GMouseButton::Right) {
        m_selected_widget = widget->make_weak_ptr();
        m_transform_event_origin = event.position();
        m_transform_widget_origin_rect = widget->rect();
        if (on_widget_selected)
            on_widget_selected(widget);
        update();
    }
}

void VBForm::mousemove_event(GMouseEvent& event)
{
    if (event.buttons() & GMouseButton::Left && m_selected_widget) {
        if (m_resize_direction == Direction::None) {
            auto delta = event.position() - m_transform_event_origin;
            auto new_rect = m_transform_widget_origin_rect.translated(delta);
            new_rect.set_x(new_rect.x() - (new_rect.x() % m_grid_size));
            new_rect.set_y(new_rect.y() - (new_rect.y() % m_grid_size));
            m_selected_widget->set_rect(new_rect);
            update();
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

        auto new_rect = m_transform_widget_origin_rect;
        Size minimum_size { 5, 5 };

        new_rect.set_x(new_rect.x() + change_x);
        new_rect.set_y(new_rect.y() + change_y);
        new_rect.set_width(max(minimum_size.width(), new_rect.width() + change_w));
        new_rect.set_height(max(minimum_size.height(), new_rect.height() + change_h));

        new_rect.set_x(new_rect.x() - (new_rect.x() % m_grid_size));
        new_rect.set_y(new_rect.y() - (new_rect.y() % m_grid_size));
        new_rect.set_width(new_rect.width() - (new_rect.width() % m_grid_size) + 1);
        new_rect.set_height(new_rect.height() - (new_rect.height() % m_grid_size) + 1);

        m_selected_widget->set_rect(new_rect);
        update();
    }
}

void VBForm::mouseup_event(GMouseEvent& event)
{
    if (event.button() == GMouseButton::Left) {
        m_transform_event_origin = { };
        m_transform_widget_origin_rect = { };
        m_resize_direction = Direction::None;
    }
}
