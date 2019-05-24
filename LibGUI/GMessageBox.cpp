#include <LibGUI/GMessageBox.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GButton.h>
#include <stdio.h>

void GMessageBox::show(const String& text, const String& title, Type type, CObject* parent)
{
    GMessageBox box(text, title, type, parent);
    box.exec();
}

GMessageBox::GMessageBox(const String& text, const String& title, Type type, CObject* parent)
    : GDialog(parent)
    , m_text(text)
    , m_type(type)
{
    set_title(title);
    build();
}

GMessageBox::~GMessageBox()
{
}

RetainPtr<GraphicsBitmap> GMessageBox::icon() const
{
    switch (m_type) {
    case Type::Information:
        return GraphicsBitmap::load_from_file("/res/icons/32x32/msgbox-information.png");
    case Type::Warning:
        return GraphicsBitmap::load_from_file("/res/icons/32x32/msgbox-warning.png");
    case Type::Error:
        return GraphicsBitmap::load_from_file("/res/icons/32x32/msgbox-error.png");
    default:
        return nullptr;
    }
}

void GMessageBox::build()
{
    auto* widget = new GWidget;
    set_main_widget(widget);

    int text_width = widget->font().width(m_text);
    int icon_width = 0;

    widget->set_layout(make<GBoxLayout>(Orientation::Vertical));
    widget->set_fill_with_background_color(true);

    widget->layout()->set_margins({ 0, 15, 0, 15 });
    widget->layout()->set_spacing(15);

    GWidget* message_container = widget;
    if (m_type != Type::None) {
        message_container = new GWidget(widget);
        message_container->set_layout(make<GBoxLayout>(Orientation::Horizontal));
        message_container->layout()->set_margins({ 8, 0, 8, 0 });
        message_container->layout()->set_spacing(8);

        auto* icon_label = new GLabel(message_container);
        icon_label->set_size_policy(SizePolicy::Fixed, SizePolicy::Fixed);
        icon_label->set_preferred_size({ 32, 32 });
        icon_label->set_icon(icon());
        icon_width = icon_label->icon()->width();
    }

    auto* label = new GLabel(m_text, message_container);
    label->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    label->set_preferred_size({ text_width, 16 });

    auto* button = new GButton(widget);
    button->set_size_policy(SizePolicy::Fixed, SizePolicy::Fixed);
    button->set_preferred_size({ 100, 20 });
    button->set_text("OK");
    button->on_click = [this] (auto&) {
        dbgprintf("GMessageBox: OK button clicked\n");
        done(0);
    };

    set_rect(x(), y(), text_width + icon_width + 80, 100);
    set_resizable(false);
}
