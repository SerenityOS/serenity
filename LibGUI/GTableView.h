#pragma once

#include <LibGUI/GTableModel.h>
#include <LibGUI/GWidget.h>
#include <AK/Function.h>
#include <AK/HashMap.h>

class GScrollBar;

class GTableView : public GWidget {
public:
    explicit GTableView(GWidget* parent);
    virtual ~GTableView() override;

    virtual int header_height() const { return 16; }
    virtual int item_height() const { return 16; }

    void set_model(OwnPtr<GTableModel>&&);
    GTableModel* model() { return m_model.ptr(); }
    const GTableModel* model() const { return m_model.ptr(); }

    void did_update_model();

    int content_width() const;
    int horizontal_padding() const { return m_horizontal_padding; }

    virtual bool accepts_focus() const override { return true; }

    Rect visible_content_rect() const;
    void scroll_into_view(const GModelIndex&, Orientation);

private:
    virtual void model_notification(const GModelNotification&);

    virtual void paint_event(GPaintEvent&) override;
    virtual void resize_event(GResizeEvent&) override;
    virtual void mousedown_event(GMouseEvent&) override;
    virtual void keydown_event(GKeyEvent&) override;

    void update_scrollbar_ranges();
    int item_count() const;
    Rect row_rect(int item_index) const;

    GScrollBar* m_vertical_scrollbar { nullptr };
    GScrollBar* m_horizontal_scrollbar { nullptr };
    OwnPtr<GTableModel> m_model;
    int m_horizontal_padding { 5 };
};
