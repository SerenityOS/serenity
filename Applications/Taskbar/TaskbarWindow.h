#include <LibGUI/GWindow.h>
#include <LibGUI/GWidget.h>
#include "WindowList.h"

class TaskbarWindow final : public GWindow {
public:
    TaskbarWindow();
    virtual ~TaskbarWindow() override;

    int taskbar_height() const { return 28; }

    virtual const char* class_name() const override { return "TaskbarWindow"; }

private:
    void on_screen_rect_change(const Rect&);

    virtual void wm_event(GWMEvent&) override;

    WindowList m_window_list;
};
