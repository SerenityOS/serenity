#include "WindowList.h"
#include <LibGUI/GWidget.h>
#include <LibGUI/GWindow.h>

class TaskbarWindow final : public GWindow {
    C_OBJECT(TaskbarWindow)
public:
    TaskbarWindow();
    virtual ~TaskbarWindow() override;

    int taskbar_height() const { return 28; }

private:
    void create_quick_launch_bar();
    void on_screen_rect_change(const Rect&);
    NonnullRefPtr<GButton> create_button(const WindowIdentifier&);

    virtual void wm_event(GWMEvent&) override;
};
