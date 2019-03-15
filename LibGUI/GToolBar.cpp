#include <LibGUI/GToolBar.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GAction.h>
#include <SharedGraphics/Painter.h>

GToolBar::GToolBar(GWidget* parent)
    : GWidget(parent)
{
    set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    set_preferred_size({ 0, 30 });
    set_layout(make<GBoxLayout>(Orientation::Horizontal));
    layout()->set_spacing(0);
    layout()->set_margins({ 3, 3, 3, 3 });
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
    ASSERT(button->size_policy(Orientation::Horizontal) == SizePolicy::Fixed);
    ASSERT(button->size_policy(Orientation::Vertical) == SizePolicy::Fixed);
    button->set_preferred_size({ 24, 24 });

    m_items.append(move(item));
}

class SeparatorWidget final : public GWidget {
public:
    SeparatorWidget(GWidget* parent)
        : GWidget(parent)
    {
        set_size_policy(SizePolicy::Fixed, SizePolicy::Fixed);
        set_background_color(Color::White);
        set_preferred_size({ 8, 20 });
    }
    virtual ~SeparatorWidget() override { }

    virtual void paint_event(GPaintEvent& event) override
    {
        Painter painter(*this);
        painter.set_clip_rect(event.rect());
        painter.translate(rect().center().x() - 1, 0);
        painter.draw_line({ 0, 0 }, { 0, rect().bottom() }, Color::DarkGray);
        painter.draw_line({ 1, 0 }, { 1, rect().bottom() }, Color::White);
    }

private:
    virtual const char* class_name() const override { return "SeparatorWidget"; }
};

void GToolBar::add_separator()
{
    auto item = make<Item>();
    item->type = Item::Separator;
    new SeparatorWidget(this);
    m_items.append(move(item));
}

void GToolBar::paint_event(GPaintEvent& event)
{
    Painter painter(*this);
    painter.set_clip_rect(event.rect());
    GStyle::the().paint_surface(painter, rect());
}
