/*
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GuideTool.h"
#include "../EditGuideDialog.h"
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Label.h>
#include <LibGUI/Menu.h>
#include <LibGUI/ValueSlider.h>
#include <LibGUI/Widget.h>

namespace PixelPaint {

RefPtr<Guide> GuideTool::closest_guide(Gfx::IntPoint point)
{
    auto guides = editor()->guides();
    Guide* closest_guide = nullptr;
    int closest_guide_distance = NumericLimits<int>::max();

    for (auto& guide : guides) {
        int relevant_position = 0;
        if (guide->orientation() == Guide::Orientation::Horizontal)
            relevant_position = point.y();
        else if (guide->orientation() == Guide::Orientation::Vertical)
            relevant_position = point.x();

        auto distance = abs(relevant_position - (int)guide->offset());

        if (distance < closest_guide_distance) {
            closest_guide = guide;
            closest_guide_distance = distance;
        }
    }

    if (closest_guide_distance < 20)
        return closest_guide;
    return nullptr;
}

void GuideTool::on_mousedown(Layer*, MouseEvent& event)
{
    if (!m_editor)
        return;

    auto& image_event = event.image_event();

    if (image_event.button() != GUI::MouseButton::Primary)
        return;

    m_editor->set_guide_visibility(true);

    RefPtr<Guide> new_guide;
    if (image_event.position().x() < 0 || image_event.position().x() > editor()->image().size().width()) {
        new_guide = make_ref_counted<Guide>(Guide::Orientation::Vertical, image_event.position().x());
    } else if (image_event.position().y() < 0 || image_event.position().y() > editor()->image().size().height()) {
        new_guide = make_ref_counted<Guide>(Guide::Orientation::Horizontal, image_event.position().y());
    }

    if (new_guide) {
        m_selected_guide = new_guide;
        m_guide_origin = 0;
        editor()->add_guide(new_guide.release_nonnull());
        return;
    }

    m_event_origin = image_event.position();

    m_selected_guide = closest_guide(image_event.position());

    if (m_selected_guide) {
        m_guide_origin = m_selected_guide->offset();
        GUI::Application::the()->show_tooltip_immediately(String::number(m_guide_origin), GUI::Application::the()->tooltip_source_widget());
    }
}

void GuideTool::on_mouseup(Layer*, MouseEvent&)
{
    m_guide_origin = 0;
    m_event_origin = { 0, 0 };
    GUI::Application::the()->hide_tooltip();

    if (!m_selected_guide)
        return;

    if (m_selected_guide->offset() < 0
        || (m_selected_guide->orientation() == Guide::Orientation::Horizontal && m_selected_guide->offset() > editor()->image().size().height())
        || (m_selected_guide->orientation() == Guide::Orientation::Vertical && m_selected_guide->offset() > editor()->image().size().width())) {
        editor()->remove_guide(*m_selected_guide);
        editor()->update();
    }

    m_selected_guide = nullptr;
}

void GuideTool::on_mousemove(Layer*, MouseEvent& event)
{
    if (!m_selected_guide)
        return;

    auto& image_event = event.image_event();
    auto delta = image_event.position() - m_event_origin;

    auto relevant_offset = 0;
    if (m_selected_guide->orientation() == Guide::Orientation::Horizontal)
        relevant_offset = delta.y();
    else if (m_selected_guide->orientation() == Guide::Orientation::Vertical)
        relevant_offset = delta.x();

    auto new_offset = (float)relevant_offset + m_guide_origin;

    if (image_event.shift() && m_snap_size > 0) {
        float snap_size_half = m_snap_size / 2.0;
        new_offset -= fmodf(new_offset + snap_size_half, m_snap_size) - snap_size_half;
    }

    m_selected_guide->set_offset(new_offset);

    GUI::Application::the()->show_tooltip_immediately(String::number(new_offset), GUI::Application::the()->tooltip_source_widget());

    editor()->update();
}

void GuideTool::on_context_menu(Layer*, GUI::ContextMenuEvent& event)
{
    if (!m_editor)
        return;

    m_editor->set_guide_visibility(true);

    if (!m_context_menu) {
        m_context_menu = GUI::Menu::construct();
        m_context_menu->add_action(GUI::Action::create(
            "Set &Offset", Gfx::Bitmap::load_from_file("/res/icons/16x16/gear.png"sv).release_value_but_fixme_should_propagate_errors(), [this](auto&) {
                if (!m_context_menu_guide)
                    return;
                auto dialog = EditGuideDialog::construct(
                    editor()->window(),
                    ByteString::formatted("{}", m_context_menu_guide->offset()),
                    m_context_menu_guide->orientation());
                if (dialog->exec() != GUI::Dialog::ExecResult::OK)
                    return;
                auto offset = dialog->offset_as_pixel(*editor());
                if (!offset.has_value())
                    return;
                m_context_menu_guide->set_offset(offset.release_value());
                m_context_menu_guide->set_orientation(dialog->orientation());
                editor()->layers_did_change();
            },
            editor()));
        m_context_menu->add_action(GUI::Action::create(
            "&Delete Guide", Gfx::Bitmap::load_from_file("/res/icons/16x16/delete.png"sv).release_value_but_fixme_should_propagate_errors(), [this](auto&) {
                if (!m_context_menu_guide)
                    return;
                editor()->remove_guide(*m_context_menu_guide);
                m_selected_guide = nullptr;
                m_guide_origin = 0;
                editor()->layers_did_change();
            },
            editor()));
    }

    auto image_position = editor()->frame_to_content_position(event.position());
    m_context_menu_guide = closest_guide({ (int)image_position.x(), (int)image_position.y() });
    if (m_context_menu_guide)
        m_context_menu->popup(event.screen_position());
}

void GuideTool::on_tool_activation()
{
    if (m_editor)
        m_editor->set_guide_visibility(true);
}

NonnullRefPtr<GUI::Widget> GuideTool::get_properties_widget()
{
    if (!m_properties_widget) {
        auto properties_widget = GUI::Widget::construct();
        properties_widget->set_layout<GUI::VerticalBoxLayout>();

        auto& snapping_container = properties_widget->add<GUI::Widget>();
        snapping_container.set_fixed_height(20);
        snapping_container.set_layout<GUI::HorizontalBoxLayout>();

        auto& snapping_label = snapping_container.add<GUI::Label>("Snap offset:"_string);
        snapping_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        snapping_label.set_fixed_size(80, 20);
        snapping_label.set_tooltip("Press Shift to snap"_string);

        auto& snapping_slider = snapping_container.add<GUI::ValueSlider>(Orientation::Horizontal, "px"_string);
        snapping_slider.set_range(0, 50);
        snapping_slider.set_value(m_snap_size);

        snapping_slider.on_change = [this](int value) {
            m_snap_size = value;
        };
        set_primary_slider(&snapping_slider);
        m_properties_widget = properties_widget;
    }

    return *m_properties_widget;
}

}
