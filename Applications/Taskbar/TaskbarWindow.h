#include <LibGUI/GWindow.h>
#include <LibGUI/GWidget.h>

class TaskbarWindow final : public GWindow {
public:
    TaskbarWindow();
    virtual ~TaskbarWindow() override;

    int taskbar_height() const { return 20; }

    virtual const char* class_name() const override { return "TaskbarWindow"; }

private:
    void on_screen_rect_change(const Rect&);

    virtual void wm_event(GWMEvent&) override;
};
