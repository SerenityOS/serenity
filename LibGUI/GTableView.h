#pragma once

#include <LibGUI/GWidget.h>
#include <AK/Function.h>
#include <AK/HashMap.h>

class GScrollBar;
class GTableModel;

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

private:
    virtual void paint_event(GPaintEvent&) override;
    virtual void resize_event(GResizeEvent&) override;
    virtual void mousedown_event(GMouseEvent&) override;

    int item_count() const;
    Rect row_rect(int item_index) const;

    GScrollBar* m_scrollbar { nullptr };
    OwnPtr<GTableModel> m_model;
};
