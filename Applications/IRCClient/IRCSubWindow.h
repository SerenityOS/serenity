#pragma once

#include <LibGUI/GWidget.h>

class IRCSubWindow : public GWidget {
public:
    explicit IRCSubWindow(const String& name, GWidget* parent);
    virtual ~IRCSubWindow() override;

    String name() const { return m_name; }
    void set_name(const String& name) { m_name = name; }

private:
    String m_name;
};
