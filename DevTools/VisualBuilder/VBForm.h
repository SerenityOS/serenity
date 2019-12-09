#pragma once

#include "VBWidget.h"
#include <AK/NonnullRefPtrVector.h>
#include <LibGUI/GWidget.h>

class VBForm : public GWidget {
    C_OBJECT(VBForm)
    friend class VBWidget;
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

    void insert_widget(VBWidgetType);

    Function<void(VBWidget*)> on_widget_selected;

    void load_from_file(const String& path);
    void write_to_file(const String& path);
    void dump();

protected:
    virtual void paint_event(GPaintEvent&) override;
    virtual void second_paint_event(GPaintEvent&) override;
    virtual void mousedown_event(GMouseEvent&) override;
    virtual void mousemove_event(GMouseEvent&) override;
    virtual void mouseup_event(GMouseEvent&) override;
    virtual void context_menu_event(GContextMenuEvent&) override;
    virtual void keydown_event(GKeyEvent&) override;

private:
    void grabber_mousedown_event(GMouseEvent&, Direction grabber);
    void set_single_selected_widget(VBWidget*);
    void add_to_selection(VBWidget&);
    void remove_from_selection(VBWidget&);
    void delete_selected_widgets();
    template<typename Callback>
    void for_each_selected_widget(Callback);
    void set_cursor_type_from_grabber(Direction grabber);

    VBWidget* single_selected_widget();

    String m_name;
    int m_grid_size { 5 };
    bool m_should_snap_to_grid { true };
    NonnullRefPtrVector<VBWidget> m_widgets;
    HashMap<GWidget*, VBWidget*> m_gwidget_map;
    HashTable<VBWidget*> m_selected_widgets;
    Point m_transform_event_origin;
    Point m_next_insertion_position;
    Direction m_resize_direction { Direction::None };
    Direction m_mouse_direction_type { Direction::None };
    RefPtr<GMenu> m_context_menu;
};
