#pragma once

#include <LibGUI/GWindow.h>
#include <LibGUI/GWidget.h>
#include "IRCClient.h"
#include "IRCClientWindow.h"

class IRCAppWindow : public GWindow {
public:
    IRCAppWindow();
    virtual ~IRCAppWindow() override;

private:
    void setup_client();
    void setup_widgets();

    void create_subwindow(IRCClientWindow::Type, const String& name);

    IRCClient m_client;

    GWidget* m_subwindow_container { nullptr };
    Vector<IRCClientWindow*> m_subwindows;
};
