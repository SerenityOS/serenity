#pragma once

#include <AK/Vector.h>
#include <LibGUI/GAbstractView.h>

class GColumnsView : public GAbstractView {
    C_OBJECT(GColumnsView)
public:
    int model_column() const { return m_model_column; }
    void set_model_column(int column) { m_model_column = column; }

private:
    GColumnsView(GWidget* parent = nullptr);
    virtual ~GColumnsView();

    GModelIndex index_at_event_position(const Point&) const;
    void push_column(GModelIndex& parent_index);
    void update_column_sizes();

    int item_height() const { return 16; }
    int icon_size() const { return 16; }
    int icon_spacing() const { return 2; }
    int text_padding() const { return 2; }

    virtual void did_update_model() override;
    virtual void paint_event(GPaintEvent&) override;
    virtual void mousedown_event(GMouseEvent& event) override;
    virtual void doubleclick_event(GMouseEvent& event) override;
    virtual void context_menu_event(GContextMenuEvent& event) override;
    virtual void keydown_event(GKeyEvent& event) override;

    struct Column {
        GModelIndex parent_index;
        int width;
        // TODO: per-column vertical scroll?
    };

    Vector<Column> m_columns;
    int m_model_column;
};
