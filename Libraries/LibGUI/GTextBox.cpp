#include <LibGUI/GTextBox.h>

GTextBox::GTextBox(GWidget* parent)
    : GTextEditor(GTextEditor::SingleLine, parent)
{
}

GTextBox::~GTextBox()
{
}
