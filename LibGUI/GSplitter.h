#pragma once

#include <LibGUI/GFrame.h>

class GSplitter : public GFrame {
public:
    GSplitter(Orientation, GWidget* parent);
    virtual ~GSplitter() override;

protected:
    virtual void mousedown_event(GMouseEvent&) override;
    virtual void mousemove_event(GMouseEvent&) override;
    virtual void mouseup_event(GMouseEvent&) override;
    virtual void enter_event(CEvent&) override;
    virtual void leave_event(CEvent&) override;

private:
    Orientation m_orientation;
    bool m_resizing { false };
    Point m_resize_origin;
    WeakPtr<GWidget> m_first_resizee;
    WeakPtr<GWidget> m_second_resizee;
    Size m_first_resizee_start_size;
    Size m_second_resizee_start_size;
};
