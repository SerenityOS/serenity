#include <LibGUI/GFrame.h>

class WindowList;

class TaskbarWidget final : public GFrame {
public:
    TaskbarWidget(WindowList&, GWidget* parent = nullptr);
    virtual ~TaskbarWidget() override;

    virtual const char* class_name() const override { return "TaskbarWidget"; }

private:
    WindowList& m_window_list;
};
