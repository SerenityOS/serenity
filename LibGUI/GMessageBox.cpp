#include <LibGUI/GMessageBox.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GButton.h>

GMessageBox::GMessageBox(const String& text, const String& title, CObject* parent)
    : GDialog(parent)
    , m_text(text)
{
    set_title(title);
    build();
}

GMessageBox::~GMessageBox()
{
}

void GMessageBox::build()
{
    auto* widget = new GWidget;
    set_main_widget(widget);

    int text_width = widget->font().width(m_text);

    set_rect(x(), y(), text_width + 80, 80);

    widget->set_layout(make<GBoxLayout>(Orientation::Vertical));
    widget->set_fill_with_background_color(true);

    widget->layout()->set_margins({ 0, 15, 0, 15 });
    widget->layout()->set_spacing(15);

    auto* label = new GLabel(m_text, widget);
    label->set_size_policy(SizePolicy::Fixed, SizePolicy::Fixed);
    label->set_preferred_size({ text_width, 16 });

    auto* button = new GButton(widget);
    button->set_size_policy(SizePolicy::Fixed, SizePolicy::Fixed);
    button->set_preferred_size({ 100, 20 });
    button->set_caption("OK");
    button->on_click = [this] (auto&) {
        dbgprintf("GMessageBox: OK button clicked\n");
        done(0);
    };
}
