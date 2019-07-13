#pragma once

#include "IRCClient.h"
#include "IRCWindow.h"
#include <LibGUI/GWidget.h>
#include <LibGUI/GWindow.h>

class GAction;
class GStackWidget;

class IRCAppWindow : public GWindow {
public:
    IRCAppWindow();
    virtual ~IRCAppWindow() override;

    static IRCAppWindow& the();

    void set_active_window(IRCWindow&);

private:
    void setup_client();
    void setup_actions();
    void setup_menus();
    void setup_widgets();
    void update_title();
    void update_part_action();

    IRCWindow& create_window(void* owner, IRCWindow::Type, const String& name);
    IRCClient m_client;
    GStackWidget* m_container { nullptr };
    GTableView* m_window_list { nullptr };
    RefPtr<GAction> m_join_action;
    RefPtr<GAction> m_part_action;
    RefPtr<GAction> m_whois_action;
    RefPtr<GAction> m_open_query_action;
    RefPtr<GAction> m_close_query_action;
    RefPtr<GAction> m_change_nick_action;
};
