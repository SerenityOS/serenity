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

    u64 timestamp_at_x(int x) const;

    Profile& m_profile;

    bool m_selecting { false };
    u64 m_select_start_time { 0 };
    u64 m_select_end_time { 0 };
};
