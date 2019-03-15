#pragma once

#include <LibGUI/GWindow.h>
#include <LibGUI/GWidget.h>
#include "IRCClient.h"

class IRCAppWindow : public GWindow {
public:
    IRCAppWindow();
    virtual ~IRCAppWindow() override;

private:
    void setup_client();
    void setup_widgets();

    IRCClient m_client;
};
