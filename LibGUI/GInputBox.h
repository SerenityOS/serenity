#pragma once

#include <LibGUI/GDialog.h>

class GInputBox : public GDialog {
public:
    enum ExecResult { ExecOK = 0, ExecCancel = 1 };

    explicit GInputBox(const String& prompt, const String& title, GObject* parent = nullptr);
    virtual ~GInputBox() override;

    String text_value() const { return m_text_value; }

private:
    void build();
    String m_prompt;
    String m_text_value;
};
