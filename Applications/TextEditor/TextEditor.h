#pragma once

#include <LibGUI/GApplication.h>
#include <LibGUI/GTextEditor.h>
#include <LibGUI/GWindow.h>

class TextEditorWidget final : public GWidget {
public:
    TextEditorWidget(GApplication* app, GWindow* window, const int& argc, char** argv);
    virtual ~TextEditorWidget() override;
    void open_sesame(GWindow* window, GTextEditor* editor, const String& path);

private:
    String path = "/tmp/TextEditor.save.txt";
};
