#include "EditorWrapper.h"
#include "Editor.h"
#include <LibGUI/GAction.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GInputBox.h>
#include <LibGUI/GLabel.h>

extern RefPtr<EditorWrapper> g_current_editor_wrapper;

EditorWrapper::EditorWrapper(GWidget* parent)
    : GWidget(parent)
{
    set_layout(make<GBoxLayout>(Orientation::Vertical));

    auto label_wrapper = GWidget::construct(this);
    label_wrapper->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    label_wrapper->set_preferred_size(0, 14);
    label_wrapper->set_fill_with_background_color(true);
    label_wrapper->set_layout(make<GBoxLayout>(Orientation::Horizontal));
    label_wrapper->layout()->set_margins({ 2, 0, 2, 0 });

    m_filename_label = GLabel::construct("(Untitled)", label_wrapper);
    m_filename_label->set_text_alignment(TextAlignment::CenterLeft);
    m_filename_label->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    m_filename_label->set_preferred_size(0, 14);

    m_cursor_label = GLabel::construct("(Cursor)", label_wrapper);
    m_cursor_label->set_text_alignment(TextAlignment::CenterRight);
    m_cursor_label->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    m_cursor_label->set_preferred_size(0, 14);

    m_editor = Editor::construct(this);
    m_editor->set_ruler_visible(true);
    m_editor->set_line_wrapping_enabled(true);
    m_editor->set_automatic_indentation_enabled(true);

    m_editor->on_cursor_change = [this] {
        m_cursor_label->set_text(String::format("Line: %d, Column: %d", m_editor->cursor().line() + 1, m_editor->cursor().column()));
    };

    m_editor->on_focus = [this] {
        g_current_editor_wrapper = this;
    };

    m_editor->add_custom_context_menu_action(GAction::create(
        "Go to line...", { Mod_Ctrl, Key_L }, GraphicsBitmap::load_from_file("/res/icons/16x16/go-forward.png"), [this](auto&) {
            auto input_box = GInputBox::construct("Line:", "Go to line", window());
            auto result = input_box->exec();
            if (result == GInputBox::ExecOK) {
                bool ok;
                auto line_number = input_box->text_value().to_uint(ok);
                if (ok) {
                    m_editor->set_cursor(line_number - 1, 0);
                }
            }
        },
        m_editor));
}

EditorWrapper::~EditorWrapper()
{
}

void EditorWrapper::set_editor_has_focus(Badge<Editor>, bool focus)
{
    m_filename_label->set_font(focus ? Font::default_bold_font() : Font::default_font());
}
