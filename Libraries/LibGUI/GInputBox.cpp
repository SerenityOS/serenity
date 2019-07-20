#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GInputBox.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GTextEditor.h>
#include <stdio.h>

GInputBox::GInputBox(const StringView& prompt, const StringView& title, CObject* parent)
    : GDialog(parent)
    , m_prompt(prompt)
{
    set_title(title);
    build();
}

GInputBox::~GInputBox()
{
}

void GInputBox::build()
{
    auto* widget = new GWidget;
    set_main_widget(widget);

    int text_width = widget->font().width(m_prompt);
    int title_width = widget->font().width(title()) + 24 /* icon, plus a little padding -- not perfect */;
    int max_width = AK::max(text_width, title_width);

    set_rect(x(), y(), max_width + 80, 80);

    widget->set_layout(make<GBoxLayout>(Orientation::Vertical));
    widget->set_fill_with_background_color(true);

    widget->layout()->set_margins({ 8, 8, 8, 8 });
    widget->layout()->set_spacing(8);

    auto* label = new GLabel(m_prompt, widget);
    label->set_size_policy(SizePolicy::Fixed, SizePolicy::Fixed);
    label->set_preferred_size(text_width, 16);

    m_text_editor = new GTextEditor(GTextEditor::SingleLine, widget);
    m_text_editor->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    m_text_editor->set_preferred_size(0, 19);

    auto* button_container_outer = new GWidget(widget);
    button_container_outer->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    button_container_outer->set_preferred_size(0, 20);
    button_container_outer->set_layout(make<GBoxLayout>(Orientation::Vertical));

    auto* button_container_inner = new GWidget(button_container_outer);
    button_container_inner->set_layout(make<GBoxLayout>(Orientation::Horizontal));
    button_container_inner->layout()->set_spacing(8);

    m_cancel_button = new GButton(button_container_inner);
    m_cancel_button->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    m_cancel_button->set_preferred_size(0, 20);
    m_cancel_button->set_text("Cancel");
    m_cancel_button->on_click = [this](auto&) {
        dbgprintf("GInputBox: Cancel button clicked\n");
        done(ExecCancel);
    };

    m_ok_button = new GButton(button_container_inner);
    m_ok_button->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    m_ok_button->set_preferred_size(0, 20);
    m_ok_button->set_text("OK");
    m_ok_button->on_click = [this](auto&) {
        dbgprintf("GInputBox: OK button clicked\n");
        m_text_value = m_text_editor->text();
        done(ExecOK);
    };

    m_text_editor->on_return_pressed = [this] {
        m_ok_button->click();
    };
    m_text_editor->on_escape_pressed = [this] {
        m_cancel_button->click();
    };
    m_text_editor->set_focus(true);
}
