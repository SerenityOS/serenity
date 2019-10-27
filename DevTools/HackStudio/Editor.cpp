#include "Editor.h"
#include "EditorWrapper.h"

void Editor::focusin_event(CEvent& event)
{
    if (on_focus)
        on_focus();
    GTextEditor::focusin_event(event);
}
