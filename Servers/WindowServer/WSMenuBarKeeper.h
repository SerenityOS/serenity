#pragma once

#include <LibCore/CObject.h>
#include <WindowServer/WSCPUMonitor.h>
#include <WindowServer/WSWindow.h>

class WSMenuManager final : public CObject {
public:
    WSMenuManager();
    virtual ~WSMenuManager() override;

    void setup();
    void refresh();

    virtual void event(CEvent&) override;
    virtual const char* class_name() const override { return "WSMenuManager"; }

private:
    WSWindow& window() { return *m_window; }
    const WSWindow& window() const { return *m_window; }

    void handle_menu_mouse_event(WSMenu&, const WSMouseEvent&);

    void draw();
    void tick_clock();

    OwnPtr<WSWindow> m_window;
    WSCPUMonitor m_cpu_monitor;
    String m_username;
};
