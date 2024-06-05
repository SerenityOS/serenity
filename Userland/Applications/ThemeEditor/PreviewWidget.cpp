/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2021, Antonio Di Stefano <tonio9681@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PreviewWidget.h"
#include <AK/LexicalPath.h>
#include <AK/StringView.h>
#include <Applications/ThemeEditor/WindowPreviewGML.h>
#include <LibCore/MimeData.h>
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/WindowTheme.h>

REGISTER_WIDGET(ThemeEditor, PreviewWidget);

namespace ThemeEditor {

class MiniWidgetGallery final : public GUI::Widget {
    C_OBJECT_ABSTRACT(MiniWidgetGallery);

public:
    static ErrorOr<NonnullRefPtr<MiniWidgetGallery>> try_create()
    {
        auto gallery = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) MiniWidgetGallery()));
        TRY(gallery->load_from_gml(window_preview_gml));

        gallery->for_each_child_widget([](auto& child) {
            child.set_focus_policy(GUI::FocusPolicy::NoFocus);
            return IterationDecision::Continue;
        });

        return gallery;
    }

    void set_preview_palette(Gfx::Palette& palette)
    {
        set_palette(palette);
        Function<void(GUI::Widget&)> recurse = [&](GUI::Widget& parent_widget) {
            parent_widget.for_each_child_widget([&](auto& widget) {
                widget.set_palette(palette);
                recurse(widget);
                return IterationDecision::Continue;
            });
        };
        recurse(*this);
    }

private:
    MiniWidgetGallery()
    {
    }
};

ErrorOr<NonnullRefPtr<PreviewWidget>> PreviewWidget::try_create()
{
    auto preview_widget = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) PreviewWidget()));
    preview_widget->m_gallery = TRY(preview_widget->try_add<MiniWidgetGallery>());
    return preview_widget;
}

PreviewWidget::PreviewWidget()
    : GUI::AbstractThemePreview(GUI::Application::the()->palette())
{
    set_greedy_for_hits(true);
}

void PreviewWidget::palette_changed()
{
    m_gallery->set_preview_palette(preview_palette());
    update_preview_window_locations();
}

void PreviewWidget::set_color_filter(OwnPtr<Gfx::ColorBlindnessFilter> color_filter)
{
    m_color_filter = move(color_filter);
    repaint();
}

void PreviewWidget::update_preview_window_locations()
{
    auto& palette = preview_palette();
    int window_title_height = palette.metric(Gfx::MetricRole::TitleHeight)
        + palette.metric(Gfx::MetricRole::BorderThickness);

    constexpr int inactive_offset_x = -20;
    int inactive_offset_y = -(window_title_height + 4);
    constexpr int hightlight_offset_x = 140;
    int hightlight_offset_y = window_title_height + 40;

    m_active_window_rect = Gfx::IntRect(0, 0, 320, 220);
    m_inactive_window_rect = m_active_window_rect.translated(inactive_offset_x, inactive_offset_y);
    m_highlight_window_rect = Gfx::IntRect(m_active_window_rect.location(), { 160, 70 }).translated(hightlight_offset_x, hightlight_offset_y);

    auto window_group = Array {
        Window { m_active_window_rect },
        Window { m_inactive_window_rect },
        Window { m_highlight_window_rect },
    };

    center_window_group_within(window_group, frame_inner_rect());

    m_gallery->set_relative_rect(m_active_window_rect);
}

void PreviewWidget::paint_preview(GUI::PaintEvent&)
{
    paint_window("Inactive window"sv, m_inactive_window_rect, Gfx::WindowTheme::WindowState::Inactive, active_window_icon());
    paint_window("Active window"sv, m_active_window_rect, Gfx::WindowTheme::WindowState::Active, inactive_window_icon());
}

void PreviewWidget::paint_hightlight_window()
{
    GUI::Painter painter(*this);
    paint_window("Highlight window"sv, m_highlight_window_rect, Gfx::WindowTheme::WindowState::Highlighted, inactive_window_icon(), 1);
    auto button_rect = Gfx::IntRect(0, 0, 80, 22).centered_within(m_highlight_window_rect);
    Gfx::StylePainter::paint_button(painter, button_rect, preview_palette(), Gfx::ButtonStyle::Normal, false, false, false, true, false, false);
    painter.draw_text(button_rect, ":^)"sv, Gfx::TextAlignment::Center, preview_palette().color(foreground_role()), Gfx::TextElision::Right, Gfx::TextWrapping::DontWrap);
}

void PreviewWidget::second_paint_event(GUI::PaintEvent&)
{
    GUI::Painter painter(*this);

    paint_hightlight_window();

    if (!m_color_filter)
        return;

    auto& target = painter.target();
    auto bitmap_clone_or_error = target.clone();
    if (bitmap_clone_or_error.is_error())
        return;

    auto clone = bitmap_clone_or_error.release_value();
    auto rect = target.rect();

    m_color_filter->apply(target, rect, *clone, rect);
}

void PreviewWidget::resize_event(GUI::ResizeEvent&)
{
    update_preview_window_locations();
}

}
