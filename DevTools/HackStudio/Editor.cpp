#include "Editor.h"
#include "EditorWrapper.h"
#include <LibGUI/GPainter.h>
#include <LibGUI/GScrollBar.h>

EditorWrapper& Editor::wrapper()
{
    return static_cast<EditorWrapper&>(*parent());
}
const EditorWrapper& Editor::wrapper() const
{
    return static_cast<const EditorWrapper&>(*parent());
}

void Editor::focusin_event(CEvent& event)
{
    wrapper().set_editor_has_focus({}, true);
    if (on_focus)
        on_focus();
    GTextEditor::focusin_event(event);
}

void Editor::focusout_event(CEvent & event)
{
    wrapper().set_editor_has_focus({}, false);
    GTextEditor::focusout_event(event);
}

void Editor::paint_event(GPaintEvent& event)
{
    GTextEditor::paint_event(event);

    if (is_focused()) {
        GPainter painter(*this);
        painter.add_clip_rect(event.rect());

        auto rect = frame_inner_rect();
        if (vertical_scrollbar().is_visible())
            rect.set_width(rect.width() - vertical_scrollbar().width());
        if (horizontal_scrollbar().is_visible())
            rect.set_height(rect.height() - horizontal_scrollbar().height());
        painter.draw_rect(rect, Color::from_rgb(0x955233));
    }
}
