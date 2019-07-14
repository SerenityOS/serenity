#pragma once

#include <AK/Function.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GTextEditor.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GWindow.h>

class GTextEditor;

class TextEditorWidget final : public GWidget {
public:
    TextEditorWidget();
    virtual ~TextEditorWidget() override;
    void open_sesame(const String& path);

private:
    GTextEditor* m_editor { nullptr };
    String m_path;
};