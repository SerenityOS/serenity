#pragma once

#include <LibGUI/GWindow.h>
#include <LibGUI/GWidget.h>
#include "IRCClient.h"
#include "IRCSubWindow.h"

class IRCAppWindow : public GWindow {
public:
    IRCAppWindow();
    virtual ~IRCAppWindow() override;

private:
    void setup_client();
    void setup_widgets();

    void create_subwindow(IRCSubWindow::Type, const String& name);

    IRCClient m_client;

    GWidget* m_subwindow_container { nullptr };
    Vector<IRCSubWindow*> m_subwindows;
};
