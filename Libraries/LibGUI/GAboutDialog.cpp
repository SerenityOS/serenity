#include <LibGUI/GAboutDialog.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GWidget.h>

GAboutDialog::GAboutDialog(const StringView& name, const GraphicsBitmap* icon, CObject* parent)
    : GDialog(parent)
    , m_name(name)
    , m_icon(icon)
{
    resize(230, 120);
    set_title(String::format("About %s", m_name.characters()));
    set_resizable(false);

    auto widget = GWidget::construct();
    set_main_widget(widget);
    widget->set_fill_with_background_color(true);
    widget->set_layout(make<GBoxLayout>(Orientation::Horizontal));

    auto left_container = GWidget::construct(widget);
    left_container->set_size_policy(SizePolicy::Fixed, SizePolicy::Fill);
    left_container->set_preferred_size(48, 0);
    left_container->set_layout(make<GBoxLayout>(Orientation::Vertical));
    auto icon_label = GLabel::construct(left_container);
    icon_label->set_icon(m_icon);
    icon_label->set_size_policy(SizePolicy::Fixed, SizePolicy::Fixed);
    icon_label->set_preferred_size(40, 40);
    left_container->layout()->add_spacer();

    auto right_container = GWidget::construct(widget);
    right_container->set_layout(make<GBoxLayout>(Orientation::Vertical));
    right_container->layout()->set_margins({ 0, 4, 4, 4 });

    auto make_label = [&](const StringView& text, bool bold = false) {
        auto label = GLabel::construct(text, right_container);
        label->set_text_alignment(TextAlignment::CenterLeft);
        label->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
        label->set_preferred_size(0, 14);
        if (bold)
            label->set_font(Font::default_bold_font());
    };
    make_label(m_name, true);
    make_label("Serenity Operating System");
    make_label("(C) The Serenity Developers");

    right_container->layout()->add_spacer();

    auto button_container = GWidget::construct(right_container);
    button_container->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    button_container->set_preferred_size(0, 20);
    button_container->set_layout(make<GBoxLayout>(Orientation::Horizontal));
    button_container->layout()->add_spacer();
    auto ok_button = GButton::construct("OK", button_container);
    ok_button->set_size_policy(SizePolicy::Fixed, SizePolicy::Fixed);
    ok_button->set_preferred_size(80, 20);
    ok_button->on_click = [this](auto&) {
        done(GDialog::ExecOK);
    };
}

GAboutDialog::~GAboutDialog()
{
}
