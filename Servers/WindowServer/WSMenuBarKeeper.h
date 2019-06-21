#pragma once

#include <LibCore/CObject.h>
#include <WindowServer/WSCPUMonitor.h>
#include <WindowServer/WSWindow.h>

class WSMenuBarKeeper final : public CObject {
public:
    WSMenuBarKeeper();
    virtual ~WSMenuBarKeeper() override;

    WSWindow& window() { return *m_window; }
    const WSWindow& window() const { return *m_window; }

    void draw();
    void refresh();
    void setup();

    virtual void event(CEvent&) override;
    virtual const char* class_name() const override { return "WSMenuBarKeeper"; }

private:
    void tick_clock();

    OwnPtr<WSWindow> m_window;
    WSCPUMonitor m_cpu_monitor;
    String m_username;
};
