#pragma once

#include <LibGUI/GWindow.h>
#include <LibGUI/GWidget.h>
#include "IRCClient.h"
#include "IRCWindow.h"

class GAction;
class GStackWidget;

class IRCAppWindow : public GWindow {
public:
    IRCAppWindow();
    virtual ~IRCAppWindow() override;

private:
    void setup_client();
    void setup_actions();
    void setup_menus();
    void setup_widgets();
    void update_title();

    IRCWindow& create_window(void* owner, IRCWindow::Type, const String& name);
    IRCClient m_client;
    GStackWidget* m_container { nullptr };
    GTableView* m_window_list { nullptr };
    RetainPtr<GAction> m_join_action;
    RetainPtr<GAction> m_part_action;
    RetainPtr<GAction> m_whois_action;
    RetainPtr<GAction> m_open_query_action;
    RetainPtr<GAction> m_close_query_action;
    RetainPtr<GAction> m_change_nick_action;
};
