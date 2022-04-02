/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2021, Antonio Di Stefano <tonio9681@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PreviewWidget.h"
#include <AK/LexicalPath.h>
#include <AK/StringView.h>
#include <LibCore/MimeData.h>
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Painter.h>
#include <LibGUI/RadioButton.h>
#include <LibGUI/Statusbar.h>
#include <LibGUI/TextEditor.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/WindowTheme.h>
#include <WindowServer/Compositor.h>

namespace ThemeEditor {

class MiniWidgetGallery final : public GUI::Widget {
    C_OBJECT(MiniWidgetGallery);

public:
    void set_preview_palette(Gfx::Palette const& palette)
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
        m_button = add<GUI::Button>();
        m_button->set_text("Button");
        m_checkbox = add<GUI::CheckBox>();
        m_checkbox->set_text("Check box");
        m_radio = add<GUI::RadioButton>();
        m_radio->set_text("Radio button");
        m_statusbar = add<GUI::Statusbar>();
        m_statusbar->set_text("Status bar");
        m_editor = add<GUI::TextEditor>();
        m_editor->set_text("Text editor\nwith multiple\nlines.");

        for_each_child_widget([](auto& child) {
            child.set_focus_policy(GUI::FocusPolicy::NoFocus);
            return IterationDecision::Continue;
        });
    }

    virtual void resize_event(GUI::ResizeEvent&) override
    {
        m_editor->set_relative_rect(10, 70, 200, 140);
        m_button->set_relative_rect(10, 10, 200, 20);
        m_checkbox->set_relative_rect(10, 30, 200, 20);
        m_radio->set_relative_rect(10, 50, 200, 20);
        m_statusbar->set_relative_rect(0, height() - 16, width(), 16);
    }

    RefPtr<GUI::TextEditor> m_editor;
    RefPtr<GUI::Button> m_button;
    RefPtr<GUI::CheckBox> m_checkbox;
    RefPtr<GUI::RadioButton> m_radio;
    RefPtr<GUI::Statusbar> m_statusbar;
};

PreviewWidget::PreviewWidget(Gfx::Palette const& initial_preview_palette)
    : GUI::AbstractThemePreview(initial_preview_palette)
{
    on_palette_change = [&] {
        m_gallery->set_preview_palette(preview_palette());
    };
    m_gallery = add<MiniWidgetGallery>();
    set_greedy_for_hits(true);
}

void PreviewWidget::set_color_filter(OwnPtr<Gfx::ColorBlindnessFilter> color_filter)
{
    m_color_filter = move(color_filter);
    repaint();
}

void PreviewWidget::paint_preview(GUI::PaintEvent&)
{
    auto active_rect = Gfx::IntRect(0, 0, 320, 240).centered_within(frame_inner_rect()).translated(0, 20);
    auto inactive_rect = active_rect.translated(-20, -20);

    paint_window("Inactive window", inactive_rect, Gfx::WindowTheme::WindowState::Inactive, active_window_icon());
    paint_window("Active window", active_rect, Gfx::WindowTheme::WindowState::Active, inactive_window_icon());
}

void PreviewWidget::second_paint_event(GUI::PaintEvent&)
{
    if (!m_color_filter)
        return;

    GUI::Painter painter(*this);

    auto target = painter.target();
    auto bitmap_clone_or_error = target->clone();
    if (bitmap_clone_or_error.is_error())
        return;

    auto clone = bitmap_clone_or_error.release_value();
    auto rect = target->rect();

    m_color_filter->apply(*target, rect, *clone, rect);
}

void PreviewWidget::resize_event(GUI::ResizeEvent&)
{
    m_gallery->set_relative_rect(Gfx::IntRect(0, 0, 320, 240).centered_within(rect()).translated(0, 20));
}

void PreviewWidget::drop_event(GUI::DropEvent& event)
{
    event.accept();
    window()->move_to_front();

    if (event.mime_data().has_urls()) {
        auto urls = event.mime_data().urls();
        if (urls.is_empty())
            return;
        if (urls.size() > 1) {
            GUI::MessageBox::show(window(), "ThemeEditor can only open one file at a time!", "One at a time please!", GUI::MessageBox::Type::Error);
            return;
        }

        auto response = FileSystemAccessClient::Client::the().try_request_file(window(), urls.first().path(), Core::OpenMode::ReadOnly);
        if (response.is_error())
            return;
        set_theme_from_file(*response.value());
    }
}

}
