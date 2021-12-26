#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GMessageBox.h>
#include <stdio.h>

int GMessageBox::show(const StringView& text, const StringView& title, Type type, InputType input_type, CObject* parent)
{
    auto box = GMessageBox::construct(text, title, type, input_type, parent);
    return box->exec();
}

GMessageBox::GMessageBox(const StringView& text, const StringView& title, Type type, InputType input_type, CObject* parent)
    : GDialog(parent)
    , m_text(text)
    , m_type(type)
    , m_input_type(input_type)
{
    set_title(title);
    build();
}

GMessageBox::~GMessageBox()
{
}

RefPtr<GraphicsBitmap> GMessageBox::icon() const
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

bool GMessageBox::should_include_ok_button() const
{
    return m_input_type == InputType::OK || m_input_type == InputType::OKCancel;
}

bool GMessageBox::should_include_cancel_button() const
{
    return m_input_type == InputType::OKCancel;
}

void GMessageBox::build()
{
    auto widget = GWidget::construct();
    set_main_widget(widget);

    int text_width = widget->font().width(m_text);
    int icon_width = 0;

    widget->set_layout(make<GBoxLayout>(Orientation::Vertical));
    widget->set_fill_with_background_color(true);

    widget->layout()->set_margins({ 0, 15, 0, 15 });
    widget->layout()->set_spacing(15);

    ObjectPtr<GWidget> message_container = widget;
    if (m_type != Type::None) {
        message_container = GWidget::construct(widget);
        message_container->set_layout(make<GBoxLayout>(Orientation::Horizontal));
        message_container->layout()->set_margins({ 8, 0, 8, 0 });
        message_container->layout()->set_spacing(8);

        auto icon_label = GLabel::construct(message_container);
        icon_label->set_size_policy(SizePolicy::Fixed, SizePolicy::Fixed);
        icon_label->set_preferred_size(32, 32);
        icon_label->set_icon(icon());
        icon_width = icon_label->icon()->width();
    }

    auto label = GLabel::construct(m_text, message_container);
    label->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    label->set_preferred_size(text_width, 16);

    auto button_container = GWidget::construct(widget);
    button_container->set_layout(make<GBoxLayout>(Orientation::Horizontal));
    button_container->layout()->set_spacing(5);
    button_container->layout()->set_margins({ 15, 0, 15, 0 });

    if (should_include_ok_button()) {
        auto ok_button = GButton::construct(button_container);
        ok_button->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
        ok_button->set_preferred_size(0, 20);
        ok_button->set_text("OK");
        ok_button->on_click = [this](auto&) {
            dbgprintf("GMessageBox: OK button clicked\n");
            done(GDialog::ExecOK);
        };
    }

    if (should_include_cancel_button()) {
        auto cancel_button = GButton::construct(button_container);
        cancel_button->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
        cancel_button->set_preferred_size(0, 20);
        cancel_button->set_text("Cancel");
        cancel_button->on_click = [this](auto&) {
            dbgprintf("GMessageBox: Cancel button clicked\n");
            done(GDialog::ExecCancel);
        };
    }

    set_rect(x(), y(), text_width + icon_width + 80, 100);
    set_resizable(false);
}
