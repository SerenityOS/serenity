/*
 * Copyright (c) 2023, Torsten Engelmann <engelTorsten@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibConfig/Client.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/DynamicWidgetContainer.h>
#include <LibGUI/DynamicWidgetContainerControls.h>
#include <LibGUI/Label.h>
#include <LibGUI/LabelWithEventDispatcher.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Window.h>
#include <LibGfx/Palette.h>

REGISTER_WIDGET(GUI, DynamicWidgetContainer)

namespace GUI {
Vector<NonnullRefPtr<GUI::Window>> DynamicWidgetContainer::s_open_windows;

DynamicWidgetContainer::DynamicWidgetContainer(Gfx::Orientation orientation)
{
    VERIFY(orientation == Gfx::Orientation::Vertical);
    REGISTER_STRING_PROPERTY("section_label", section_label, set_section_label);
    REGISTER_STRING_PROPERTY("config_domain", config_domain, set_config_domain);
    REGISTER_SIZE_PROPERTY("detached_size", detached_size, set_detached_size);
    REGISTER_BOOL_PROPERTY("with_individual_order", is_container_with_individual_order, set_container_with_individual_order);
    REGISTER_BOOL_PROPERTY("show_controls", show_controls, set_show_controls);

    set_layout<GUI::VerticalBoxLayout>(0, 0);
    set_preferred_height(SpecialDimension::Shrink);

    auto controls_widget = MUST(GUI::DynamicWidgetContainerControls::try_create());
    m_controls_widget = controls_widget;
    add_child(*m_controls_widget);

    controls_widget->get_collapse_button()->on_click = [&](auto) {
        set_view_state(ViewState::Collapsed);
    };

    controls_widget->get_expand_button()->on_click = [&](auto) {
        set_view_state(ViewState::Expanded);
    };

    controls_widget->get_detach_button()->on_click = [&](auto) {
        set_view_state(ViewState::Detached);
    };

    update_control_button_visibility();

    m_label_widget = controls_widget->get_event_dispatcher();
    m_label_widget->on_double_click = [&](MouseEvent& event) {
        handle_doubleclick_event(event);
    };
    m_label_widget->on_mouseup_event = [&](MouseEvent& event) {
        handle_mouseup_event(event);
    };
    m_label_widget->on_mousemove_event = [&](MouseEvent& event) {
        handle_mousemove_event(event);
    };

    m_label_widget->set_grabbable_margins({ 0, 0, 0, m_label_widget->rect().width() });
}

DynamicWidgetContainer::~DynamicWidgetContainer()
{
    close_all_detached_windows();
}

template<typename Callback>
void DynamicWidgetContainer::for_each_child_container(Callback callback)
{
    for_each_child([&](auto& child) {
        if (is<DynamicWidgetContainer>(child))
            return callback(static_cast<DynamicWidgetContainer&>(child));
        return IterationDecision::Continue;
    });
}

Vector<GUI::DynamicWidgetContainer&> DynamicWidgetContainer::child_containers() const
{
    Vector<GUI::DynamicWidgetContainer&> widgets;
    widgets.ensure_capacity(children().size());
    for (auto& child : children()) {
        if (is<DynamicWidgetContainer>(*child))
            widgets.append(static_cast<DynamicWidgetContainer&>(*child));
    }
    return widgets;
}

void DynamicWidgetContainer::set_view_state(ViewState state)
{
    if (view_state() == state)
        return;

    m_view_state = state;
    set_visible(view_state() != ViewState::Detached);

    for_each_child_widget([&](auto& widget) {
        if (m_controls_widget != widget)
            widget.set_visible(view_state() == ViewState::Expanded);
        return IterationDecision::Continue;
    });

    if (m_dimensions_before_collapse.has_value()) {
        set_min_size(m_dimensions_before_collapse->min_size);
        set_preferred_size(m_dimensions_before_collapse->preferred_size);
        m_dimensions_before_collapse = {};
    }
    if (view_state() == ViewState::Collapsed) {
        // We still need to force a minimal height in case of a container is configured as "grow". Even then we would like to let it collapse.
        m_dimensions_before_collapse = { { .preferred_size = preferred_size(), .min_size = min_size() } };

        set_min_height(m_controls_widget->height() + content_margins().vertical_total());
        set_preferred_size(preferred_width(), SpecialDimension::Shrink);
    }

    update_control_button_visibility();

    if (view_state() == ViewState::Detached)
        (void)detach_widgets();

    if (persist_state())
        Config::write_i32(config_domain(), "DynamicWidgetContainers"sv, section_label(), to_underlying(state));
}

void DynamicWidgetContainer::restore_view_state()
{
    if (!persist_state())
        return;

    deferred_invoke([&]() {
        if (is_container_with_individual_order()) {
            auto order_or_error = JsonValue::from_string(Config::read_string(config_domain(), "DynamicWidgetContainers"sv, section_label()));
            if (order_or_error.is_error() || !order_or_error.value().is_array()) {
                Config::remove_key(config_domain(), "DynamicWidgetContainers"sv, section_label());
                return;
            }

            Vector<NonnullRefPtr<Widget>> new_child_order;
            auto containers = child_containers();

            order_or_error.value().as_array().for_each([&](auto& section_label) {
                for (auto& container : containers) {
                    if (container.section_label() == section_label.as_string())
                        new_child_order.append(container);
                }
            });

            // Are there any children that are not known to our persisted order?
            for (auto& container : containers) {
                // FIXME: Optimize performance and get rid of contains_slow so that this does not become a issue when a lot of child containers are used.
                if (!new_child_order.contains_slow(container))
                    new_child_order.append(container);
            }

            // Rearrange child widgets to the defined order.
            auto childs = child_widgets();
            for (auto& child : childs) {
                if (new_child_order.contains_slow(child))
                    child.remove_from_parent();
            }

            for (auto const& child : new_child_order)
                add_child(*child);
        } else {
            int persisted_state = Config::read_i32(config_domain(), "DynamicWidgetContainers"sv, section_label(), to_underlying(ViewState::Expanded));
            set_view_state(static_cast<ViewState>(persisted_state));
        }
        update();
    });
}

void DynamicWidgetContainer::set_section_label(String label)
{
    m_section_label = move(label);
    m_label_widget->set_text(m_section_label);
}

void DynamicWidgetContainer::set_config_domain(String domain)
{
    m_config_domain = move(domain);
    // FIXME: A much better solution would be to call the restore_view_state within a dedicated "initialization finished" method that is called by the gml runtime after that widget is ready.
    //        We do not have such a method yet.
    restore_view_state();
}

void DynamicWidgetContainer::set_detached_size(Gfx::IntSize const size)
{
    m_detached_size = { size };
}

void DynamicWidgetContainer::set_show_controls(bool value)
{
    m_show_controls = value;
    m_controls_widget->set_visible(m_controls_widget->is_visible() && show_controls());
    update();
}

void DynamicWidgetContainer::set_container_with_individual_order(bool value)
{
    m_is_container_with_individual_order = value;
}

void DynamicWidgetContainer::second_paint_event(PaintEvent&)
{
    GUI::Painter painter(*this);
    painter.draw_line({ 0, height() - 1 }, { width(), height() - 1 }, palette().threed_shadow1());

    if (!m_is_dragging && !m_render_as_move_target)
        return;

    if (m_is_dragging) {
        // FIXME: Would be nice if we could paint outside our own boundaries.
        auto move_widget_indicator = rect().translated(m_current_mouse_position).translated(-m_drag_start_location);
        painter.fill_rect(move_widget_indicator, palette().rubber_band_fill());
        painter.draw_rect_with_thickness(move_widget_indicator, palette().rubber_band_border(), 1);
    } else if (m_render_as_move_target) {
        painter.fill_rect(rect(), palette().rubber_band_fill());
        painter.draw_rect_with_thickness({ rect().x(), rect().y(), rect().width() - 1, rect().height() - 1 }, palette().rubber_band_border(), 1);
    }
}

ErrorOr<void> DynamicWidgetContainer::detach_widgets()
{
    if (!m_detached_widgets_window.has_value()) {
        auto detached_window = TRY(GUI::Window::try_create());
        detached_window->set_title(section_label().to_byte_string());
        detached_window->set_window_type(WindowType::Normal);
        if (has_detached_size())
            detached_window->resize(detached_size());
        else
            detached_window->resize(size());

        detached_window->center_on_screen();

        auto root_container = detached_window->set_main_widget<GUI::Frame>();
        root_container->set_fill_with_background_color(true);
        root_container->set_layout<GUI::VerticalBoxLayout>();
        root_container->set_frame_style(Gfx::FrameStyle::Window);
        auto transfer_children = [this](auto reciever, auto children) {
            for (NonnullRefPtr<GUI::Widget> widget : children) {
                if (widget == m_controls_widget)
                    continue;
                widget->remove_from_parent();
                widget->set_visible(true);
                reciever->add_child(widget);
            }
        };

        transfer_children(root_container, child_widgets());

        detached_window->on_close = [this, root_container, transfer_children]() {
            transfer_children(this, root_container->child_widgets());
            set_view_state(ViewState::Expanded);
            unregister_open_window(m_detached_widgets_window.value());
            m_detached_widgets_window = {};
        };

        m_detached_widgets_window = detached_window;
    }

    register_open_window(m_detached_widgets_window.value());

    if (m_is_dragging)
        m_detached_widgets_window.value()->move_to(screen_relative_rect().location().translated(m_current_mouse_position).translated({ m_detached_widgets_window.value()->width() / -2, 0 }));
    m_detached_widgets_window.value()->show();
    return {};
}

void DynamicWidgetContainer::close_all_detached_windows()
{
    for (auto window : DynamicWidgetContainer::s_open_windows.in_reverse())
        window->close();
}

void DynamicWidgetContainer::register_open_window(NonnullRefPtr<GUI::Window> window)
{
    s_open_windows.append(window);
}

void DynamicWidgetContainer::unregister_open_window(NonnullRefPtr<GUI::Window> window)
{
    Optional<size_t> match = s_open_windows.find_first_index(window);
    if (match.has_value())
        s_open_windows.remove(match.value());
}

void DynamicWidgetContainer::handle_mouseup_event(MouseEvent& event)
{
    if (event.button() != MouseButton::Primary)
        return;

    if (m_is_dragging) {
        // If we dropped the widget outside of ourself, we would like to detach it.
        if (m_parent_container == nullptr && !rect().contains(event.position()))
            set_view_state(ViewState::Detached);

        if (m_parent_container != nullptr) {
            bool should_move_position = m_parent_container->check_has_move_target(relative_position().translated(m_current_mouse_position), MoveTargetOperation::ClearAllTargets);

            if (should_move_position)
                m_parent_container->swap_widget_positions(*this, relative_position().translated(m_current_mouse_position));
            else
                set_view_state(ViewState::Detached);
        }

        m_is_dragging = false;

        // Change the cursor back to normal after dragging is finished. Otherwise the cursor will only change if the mouse moves.
        m_label_widget->update_cursor(Gfx::StandardCursor::Arrow);

        update();
    }
}

void DynamicWidgetContainer::handle_mousemove_event(MouseEvent& event)
{
    auto changed_cursor = m_is_dragging ? Gfx::StandardCursor::Move : Gfx::StandardCursor::Arrow;
    if (m_move_widget_knurl.contains(event.position()) && !m_is_dragging)
        changed_cursor = Gfx::StandardCursor::Hand;

    if (event.buttons() == MouseButton::Primary && !m_is_dragging) {
        m_is_dragging = true;
        m_drag_start_location = event.position();
        changed_cursor = Gfx::StandardCursor::Move;
    }

    if (m_is_dragging) {
        m_current_mouse_position = event.position();
        if (m_parent_container != nullptr) {
            m_parent_container->check_has_move_target(relative_position().translated(m_current_mouse_position), MoveTargetOperation::SetTarget);
        }
        update();
    }

    m_label_widget->update_cursor(changed_cursor);
}

void DynamicWidgetContainer::handle_doubleclick_event(MouseEvent& event)
{
    if (event.button() != MouseButton::Primary)
        return;

    if (view_state() == ViewState::Expanded)
        set_view_state(ViewState::Collapsed);
    else if (view_state() == ViewState::Collapsed)
        set_view_state(ViewState::Expanded);
}

void DynamicWidgetContainer::resize_event(ResizeEvent&)
{
    // Check if there is any content to display, and hide ourself if there would be nothing to display.
    // This allows us to make the whole section not taking up space if child-widget visibility is maintained outside.
    if (m_previous_frame_style.has_value() && height() != 0) {
        m_controls_widget->set_visible(show_controls());
        set_frame_style(m_previous_frame_style.value());
        m_previous_frame_style = {};

        // FIXME: Get rid of this, without the deferred invoke the lower part of the containing widget might not be drawn correctly :-/
        deferred_invoke([&]() {
            invalidate_layout();
        });
    }

    if (view_state() == ViewState::Expanded && !m_previous_frame_style.has_value() && height() == (content_margins().top() + content_margins().bottom() + m_controls_widget->height())) {
        m_controls_widget->set_visible(false);
        m_previous_frame_style = frame_style();
        set_frame_style(Gfx::FrameStyle::NoFrame);
        deferred_invoke([&]() {
            invalidate_layout();
        });
    }
}

void DynamicWidgetContainer::child_event(Core::ChildEvent& event)
{
    if (event.type() == Event::ChildAdded && event.child() && is<GUI::DynamicWidgetContainer>(*event.child()))
        static_cast<DynamicWidgetContainer&>(*event.child()).set_parent_container(*this);

    GUI::Frame::child_event(event);
}

void DynamicWidgetContainer::set_parent_container(RefPtr<GUI::DynamicWidgetContainer> container)
{
    m_parent_container = container;
}

bool DynamicWidgetContainer::check_has_move_target(Gfx::IntPoint relative_mouse_position, MoveTargetOperation operation)
{
    bool matched = false;
    for_each_child_container([&](auto& child) {
        bool is_target = child.relative_rect().contains(relative_mouse_position);
        matched |= is_target;
        child.set_render_as_move_target(operation == MoveTargetOperation::SetTarget ? is_target : false);

        return IterationDecision::Continue;
    });
    return matched;
}

void DynamicWidgetContainer::set_render_as_move_target(bool is_target)
{
    if (m_render_as_move_target == is_target)
        return;
    m_render_as_move_target = is_target;
    update();
}

void DynamicWidgetContainer::swap_widget_positions(NonnullRefPtr<Core::EventReceiver> source_widget, Gfx::IntPoint destination_positon)
{
    Optional<NonnullRefPtr<Core::EventReceiver>> destination_widget;
    for_each_child_container([&](auto& child) {
        if (child.relative_rect().contains(destination_positon)) {
            destination_widget = child;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });

    VERIFY(destination_widget.has_value());
    if (source_widget == destination_widget.value())
        return;

    auto source_position = children().find_first_index(*source_widget);
    auto destination_position = children().find_first_index(*destination_widget.value());
    VERIFY(source_position.has_value());
    VERIFY(destination_position.has_value());

    swap(children()[source_position.value()], children()[destination_position.value()]);

    // FIXME: Find a better solution to instantly display the new widget order.
    //        invalidate_layout is not working :/
    auto childs = child_widgets();
    for (RefPtr<GUI::Widget> widget : childs) {
        widget->remove_from_parent();
        add_child(*widget);
    }

    if (!persist_state())
        return;

    JsonArray new_widget_order;
    for (auto& child : child_containers())
        new_widget_order.must_append(child.section_label());

    Config::write_string(config_domain(), "DynamicWidgetContainers"sv, section_label(), new_widget_order.serialized<StringBuilder>());
}

void DynamicWidgetContainer::update_control_button_visibility()
{
    auto expand_button = m_controls_widget->find_descendant_of_type_named<GUI::Button>("expand_button");
    expand_button->set_visible(view_state() == ViewState::Collapsed);

    auto collapse_button = m_controls_widget->find_descendant_of_type_named<GUI::Button>("collapse_button");
    collapse_button->set_visible(view_state() == ViewState::Expanded);
}
}
