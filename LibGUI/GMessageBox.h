#pragma once

#include <LibGUI/GDialog.h>

class GMessageBox : public GDialog {
public:
    explicit GMessageBox(const String& text, const String& title, GObject* parent = nullptr);
    virtual ~GMessageBox() override;

    String text() const { return m_text; }

    void build();

private:
    String m_text;
};
