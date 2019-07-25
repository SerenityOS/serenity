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
    void on_screen_rect_change(const Rect&);
    GButton* create_button(const WindowIdentifier&);

    virtual void wm_event(GWMEvent&) override;
};
