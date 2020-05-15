#include <AK/Function.h>
#include <LibGUI/Label.h>

class IconWidget : public GUI::Label {
    C_OBJECT(IconWidget)
public:
    IconWidget();
    virtual ~IconWidget() override;

    Function<void()> on_click;

private:
    virtual void mousedown_event(GUI::MouseEvent&) override;
};
