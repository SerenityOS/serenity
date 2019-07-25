#pragma once

#include <AK/Function.h>
#include <AK/HashMap.h>
#include <LibGUI/GAbstractView.h>
#include <LibGUI/GModel.h>

class GScrollBar;
class Painter;

class GListView : public GAbstractView {
    C_OBJECT(GListView)
public:
    explicit GListView(GWidget* parent);
    virtual ~GListView() override;

    int item_height() const { return 16; }

    bool alternating_row_colors() const { return m_alternating_row_colors; }
    void set_alternating_row_colors(bool b) { m_alternating_row_colors = b; }

    int horizontal_padding() const { return m_horizontal_padding; }

    void scroll_into_view(const GModelIndex&, Orientation);

    Point adjusted_position(const Point&);

    virtual Rect content_rect(const GModelIndex&) const override;

private:
    virtual void did_update_model() override;
    virtual void paint_event(GPaintEvent&) override;
    virtual void mousedown_event(GMouseEvent&) override;
    virtual void doubleclick_event(GMouseEvent&) override;
    virtual void keydown_event(GKeyEvent&) override;
    virtual void resize_event(GResizeEvent&) override;

    Rect content_rect(int row) const;
    int item_count() const;
    void update_content_size();

    int m_horizontal_padding { 2 };
    int m_model_column { 0 };
    bool m_alternating_row_colors { true };
};
