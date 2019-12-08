#pragma once

#include <AK/Function.h>
#include <AK/HashMap.h>
#include <LibGUI/GAbstractView.h>
#include <LibGUI/GModel.h>

class GScrollBar;
class Painter;

class GItemView : public GAbstractView {
    C_OBJECT(GItemView)
public:
    virtual ~GItemView() override;

    int content_width() const;
    int horizontal_padding() const { return m_horizontal_padding; }

    void scroll_into_view(const GModelIndex&, Orientation);
    Size effective_item_size() const { return m_effective_item_size; }

    int model_column() const { return m_model_column; }
    void set_model_column(int column) { m_model_column = column; }

private:
    explicit GItemView(GWidget* parent);

    virtual void did_update_model() override;
    virtual void paint_event(GPaintEvent&) override;
    virtual void resize_event(GResizeEvent&) override;
    virtual void mousedown_event(GMouseEvent&) override;
    virtual void mousemove_event(GMouseEvent&) override;
    virtual void keydown_event(GKeyEvent&) override;
    virtual void doubleclick_event(GMouseEvent&) override;
    virtual void context_menu_event(GContextMenuEvent&) override;

    int item_count() const;
    Rect item_rect(int item_index) const;
    int item_at_event_position(const Point&) const;
    void update_content_size();

    int m_horizontal_padding { 5 };
    int m_model_column { 0 };
    int m_visual_column_count { 0 };
    int m_visual_row_count { 0 };

    Point m_left_mousedown_position;

    Size m_effective_item_size { 80, 80 };
};
