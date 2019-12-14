#include <LibGUI/GFrame.h>

class Profile;

class ProfileTimelineWidget final : public GFrame {
    C_OBJECT(ProfileTimelineWidget)
public:
    virtual ~ProfileTimelineWidget() override;

private:
    virtual void paint_event(GPaintEvent&) override;
    virtual void mousedown_event(GMouseEvent&) override;
    virtual void mousemove_event(GMouseEvent&) override;
    virtual void mouseup_event(GMouseEvent&) override;

    ProfileTimelineWidget(Profile&, GWidget* parent);

    Profile& m_profile;
};
