/*
 * Copyright (c) 2023, Torsten Engelmann <engelTorsten@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Frame.h>
#include <LibGUI/Label.h>
#include <LibGUI/LabelWithEventDispatcher.h>
#include <LibGUI/Window.h>

namespace GUI {

class DynamicWidgetContainer : public Frame {
    C_OBJECT(DynamicWidgetContainer);

public:
    enum class ViewState {
        Expanded,
        Collapsed,
        Detached
    };

    enum class MoveTargetOperation {
        SetTarget,
        ClearAllTargets
    };

    ViewState view_state() const { return m_view_state; }
    void set_view_state(ViewState);
    StringView section_label() const& { return m_section_label; }
    void set_section_label(String);
    StringView config_domain() const& { return m_config_domain; }
    void set_config_domain(String);
    bool persist_state() const { return !m_config_domain.is_empty(); }
    void set_detached_size(Gfx::IntSize const);
    Gfx::IntSize detached_size() const { return m_detached_size.value(); }
    bool has_detached_size() { return m_detached_size.has_value(); }
    void set_container_with_individual_order(bool);
    bool is_container_with_individual_order() const { return m_is_container_with_individual_order; }
    void set_show_controls(bool);
    bool show_controls() const { return m_show_controls; }
    void set_parent_container(RefPtr<GUI::DynamicWidgetContainer>);
    bool check_has_move_target(Gfx::IntPoint, MoveTargetOperation);
    // FIXME: this should not be a public api and static - but currently the destructor is not being called when the widget was created via a gml file.
    static void close_all_detached_windows();
    virtual ~DynamicWidgetContainer() override;

protected:
    explicit DynamicWidgetContainer(Gfx::Orientation = Gfx::Orientation::Vertical);
    virtual void paint_event(PaintEvent&) override {};
    virtual void second_paint_event(PaintEvent&) override;
    virtual void resize_event(ResizeEvent&) override;
    virtual void child_event(Core::ChildEvent&) override;

    template<typename Callback>
    void for_each_child_container(Callback callback);
    Vector<GUI::DynamicWidgetContainer&> child_containers() const;

private:
    struct RelevantSizes {
        UISize preferred_size;
        UISize min_size;
    };
    ViewState m_view_state = ViewState::Expanded;
    String m_section_label;
    String m_config_domain;
    bool m_is_container_with_individual_order { false };
    bool m_persist_state { false };
    bool m_is_dragging { false };
    bool m_render_as_move_target { false };
    bool m_show_controls { true };
    Gfx::IntPoint m_drag_start_location;
    Gfx::IntPoint m_current_mouse_position;
    RefPtr<GUI::Widget> m_controls_widget;
    RefPtr<GUI::LabelWithEventDispatcher> m_label_widget;
    Gfx::IntRect m_move_widget_knurl = { 0, 0, 16, 16 };
    Optional<NonnullRefPtr<GUI::Window>> m_detached_widgets_window;
    Optional<Gfx::FrameStyle> m_previous_frame_style;
    Optional<RelevantSizes> m_dimensions_before_collapse;
    Optional<Gfx::IntSize> m_detached_size;
    RefPtr<GUI::DynamicWidgetContainer> m_parent_container;
    static Vector<NonnullRefPtr<GUI::Window>> s_open_windows;

    ErrorOr<void> detach_widgets();
    void restore_view_state();
    void register_open_window(NonnullRefPtr<GUI::Window>);
    void unregister_open_window(NonnullRefPtr<GUI::Window>);
    void set_render_as_move_target(bool);
    void swap_widget_positions(NonnullRefPtr<Core::EventReceiver> source, Gfx::IntPoint destination_positon);

    void handle_mousemove_event(MouseEvent&);
    void handle_mouseup_event(MouseEvent&);
    void handle_doubleclick_event(MouseEvent&);
    void update_control_button_visibility();
};
}
