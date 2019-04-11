#pragma once

#include <LibGUI/GWidget.h>
#include <AK/Vector.h>
#include "VBWidget.h"

class VBForm : public GWidget {
public:
    explicit VBForm(const String& name, GWidget* parent = nullptr);
    virtual ~VBForm() override;

    static VBForm* current();

    String name() const { return m_name; }
    void set_name(const String& name) { m_name = name; }

    bool is_selected(const VBWidget&) const;
    VBWidget* widget_at(const Point&);

    void set_should_snap_to_grip(bool snap) { m_should_snap_to_grid = snap; }
    bool should_snap_to_grid() const { return m_should_snap_to_grid; }

    void insert_widget(WidgetType);

protected:
    virtual void paint_event(GPaintEvent&) override;
    virtual void second_paint_event(GPaintEvent&) override;
    virtual void mousedown_event(GMouseEvent&) override;
    virtual void mousemove_event(GMouseEvent&) override;
    virtual void mouseup_event(GMouseEvent&) override;

private:
    void grabber_mousedown_event(GMouseEvent&, VBWidget&, Direction grabber);

    String m_name;
    int m_grid_size { 8 };
    bool m_should_snap_to_grid { true };
    Vector<Retained<VBWidget>> m_widgets;
    WeakPtr<VBWidget> m_selected_widget;
    Point m_transform_event_origin;
    Rect m_transform_widget_origin_rect;
    Point m_next_insertion_position;
    Direction m_resize_direction { Direction::None };
};
