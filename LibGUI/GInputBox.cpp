#include <LibGUI/GInputBox.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GTextEditor.h>
#include <stdio.h>

GInputBox::GInputBox(const String& prompt, const String& title, GObject* parent)
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

    set_rect(x(), y(), text_width + 80, 120);

    widget->set_layout(make<GBoxLayout>(Orientation::Vertical));
    widget->set_fill_with_background_color(true);

    widget->layout()->set_margins({ 8, 8, 8, 8 });
    widget->layout()->set_spacing(8);

    auto* label = new GLabel(m_prompt, widget);
    label->set_size_policy(SizePolicy::Fixed, SizePolicy::Fixed);
    label->set_preferred_size({ text_width, 16 });

    auto* text_editor = new GTextEditor(GTextEditor::SingleLine, widget);
    text_editor->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    text_editor->set_preferred_size({ 0, 16 });

    auto* button_container_outer = new GWidget(widget);
    button_container_outer->set_layout(make<GBoxLayout>(Orientation::Vertical));

    auto* button_container_inner = new GWidget(button_container_outer);
    button_container_inner->set_layout(make<GBoxLayout>(Orientation::Horizontal));
    button_container_inner->layout()->set_spacing(8);

    auto* cancel_button = new GButton(button_container_inner);
    cancel_button->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    cancel_button->set_preferred_size({ 0, 16 });
    cancel_button->set_caption("Cancel");
    cancel_button->on_click = [&] (auto&) {
        fprintf(stderr, "GInputBox: Cancel button clicked\n");
        done(1);
    };

    auto* ok_button = new GButton(button_container_inner);
    ok_button->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    ok_button->set_preferred_size({ 0, 16 });
    ok_button->set_caption("OK");
    ok_button->on_click = [&] (auto&) {
        fprintf(stderr, "GInputBox: OK button clicked\n");
        m_text_value = text_editor->text();
        done(0);
    };

    text_editor->on_return_pressed = [&] (auto&) {
        ok_button->click();
    };
    text_editor->on_escape_pressed = [&] (auto&) {
        cancel_button->click();
    };
    text_editor->set_focus(true);
}
