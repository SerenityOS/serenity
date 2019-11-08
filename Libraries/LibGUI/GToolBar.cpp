#include <LibGUI/GAction.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GPainter.h>
#include <LibGUI/GToolBar.h>

GToolBar::GToolBar(GWidget* parent)
    : GToolBar(Orientation::Horizontal, 16, parent)
{
}

GToolBar::GToolBar(Orientation orientation, int button_size, GWidget* parent)
    : GWidget(parent)
    , m_button_size(button_size)
{
    if (orientation == Orientation::Horizontal) {
        set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
        set_preferred_size(0, button_size + 12);
    } else {
        set_size_policy(SizePolicy::Fixed, SizePolicy::Fill);
        set_preferred_size(button_size + 12, 0);
    }
    set_layout(make<GBoxLayout>(orientation));
    layout()->set_spacing(0);
    layout()->set_margins({ 2, 2, 2, 2 });
}

GToolBar::~GToolBar()
{
}

void GToolBar::add_action(GAction& action)
{
    auto item = make<Item>();
    item->type = Item::Action;
    item->action = action;

    auto button = GButton::construct(this);
    button->set_action(*item->action);
    button->set_tooltip(item->action->text());
    if (item->action->icon())
        button->set_icon(item->action->icon());
    else
        button->set_text(item->action->text());

    button->set_button_style(ButtonStyle::CoolBar);
    button->set_size_policy(SizePolicy::Fixed, SizePolicy::Fixed);
    ASSERT(button->size_policy(Orientation::Horizontal) == SizePolicy::Fixed);
    ASSERT(button->size_policy(Orientation::Vertical) == SizePolicy::Fixed);
    button->set_preferred_size(m_button_size + 8, m_button_size + 8);

    m_items.append(move(item));
}

class SeparatorWidget final : public GWidget {
    C_OBJECT(SeparatorWidget)
public:
    SeparatorWidget(GWidget* parent)
        : GWidget(parent)
    {
        set_size_policy(SizePolicy::Fixed, SizePolicy::Fixed);
        set_background_color(Color::White);
        set_preferred_size(8, 22);
    }
    virtual ~SeparatorWidget() override {}

    virtual void paint_event(GPaintEvent& event) override
    {
        GPainter painter(*this);
        painter.add_clip_rect(event.rect());
        painter.translate(rect().center().x() - 1, 0);
        painter.draw_line({ 0, 0 }, { 0, rect().bottom() }, Color::MidGray);
        painter.draw_line({ 1, 0 }, { 1, rect().bottom() }, Color::White);
    }
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
    GPainter painter(*this);
    painter.add_clip_rect(event.rect());

    if (m_has_frame)
        StylePainter::paint_surface(painter, rect(), x() != 0, y() != 0);
    else
        painter.fill_rect(event.rect(), Color::WarmGray);
}
