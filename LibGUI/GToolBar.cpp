#include <LibGUI/GToolBar.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GAction.h>
#include <SharedGraphics/Painter.h>

GToolBar::GToolBar(GWidget* parent)
    : GWidget(parent)
{
    set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    set_preferred_size({ 0, 29 });
    set_layout(make<GBoxLayout>(Orientation::Horizontal));
    layout()->set_spacing(0);
    layout()->set_margins({1, 1, 1, 1});
}

GToolBar::~GToolBar()
{
}

void GToolBar::add_action(Retained<GAction>&& action)
{
    GAction* raw_action_ptr = action.ptr();
    auto item = make<Item>();
    item->type = Item::Action;
    item->action = move(action);

    auto* button = new GButton(this);
    if (item->action->icon())
        button->set_icon(item->action->icon());
    else
        button->set_caption(item->action->text());
    button->on_click = [raw_action_ptr] (const GButton&) {
        raw_action_ptr->activate();
    };

    button->set_button_style(GButtonStyle::CoolBar);
    button->set_size_policy(SizePolicy::Fixed, SizePolicy::Fixed);
    button->set_preferred_size({ 26, 26 });

    m_items.append(move(item));
}

void GToolBar::add_separator()
{
    auto item = make<Item>();
    item->type = Item::Separator;
    m_items.append(move(item));
}

void GToolBar::paint_event(GPaintEvent& event)
{
    Painter painter(*this);
    painter.set_clip_rect(event.rect());
    painter.fill_rect({ 0, 0, width(), height() - 1 }, Color::LightGray);
    painter.draw_line({ 0, 0 }, { width() - 1, 0 }, Color::White);
    painter.draw_line({ 0, rect().bottom() }, { width() - 1, rect().bottom() }, Color::DarkGray);
}
