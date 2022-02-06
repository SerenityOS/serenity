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
    void set_preview_palette(const Gfx::Palette& palette)
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

PreviewWidget::PreviewWidget(const Gfx::Palette& preview_palette)
    : m_preview_palette(preview_palette)
{
    m_active_window_icon = Gfx::Bitmap::try_load_from_file("/res/icons/16x16/window.png").release_value_but_fixme_should_propagate_errors();
    m_inactive_window_icon = Gfx::Bitmap::try_load_from_file("/res/icons/16x16/window.png").release_value_but_fixme_should_propagate_errors();

    m_default_close_bitmap = Gfx::Bitmap::try_load_from_file("/res/icons/16x16/window-close.png").release_value_but_fixme_should_propagate_errors();
    m_default_maximize_bitmap = Gfx::Bitmap::try_load_from_file("/res/icons/16x16/upward-triangle.png").release_value_but_fixme_should_propagate_errors();
    m_default_minimize_bitmap = Gfx::Bitmap::try_load_from_file("/res/icons/16x16/downward-triangle.png").release_value_but_fixme_should_propagate_errors();

    VERIFY(m_active_window_icon);
    VERIFY(m_inactive_window_icon);
    VERIFY(m_default_close_bitmap);
    VERIFY(m_default_maximize_bitmap);
    VERIFY(m_default_minimize_bitmap);

    load_theme_bitmaps();

    m_gallery = add<MiniWidgetGallery>();
    set_greedy_for_hits(true);
}

void PreviewWidget::load_theme_bitmaps()
{
    auto load_bitmap = [](String const& path, String& last_path, RefPtr<Gfx::Bitmap>& bitmap) {
        bitmap = nullptr;
        if (path.is_empty()) {
            last_path = String::empty();
        } else if (last_path != path) {
            auto bitmap_or_error = Gfx::Bitmap::try_load_from_file(path);
            if (bitmap_or_error.is_error()) {
                last_path = String::empty();
            } else {
                last_path = path;
                bitmap = bitmap_or_error.release_value();
            }
        }
    };

    auto buttons_path = m_preview_palette.title_button_icons_path();

    load_bitmap(LexicalPath::absolute_path(buttons_path, "window-close.png"), m_last_close_path, m_close_bitmap);
    load_bitmap(LexicalPath::absolute_path(buttons_path, "window-maximize.png"), m_last_maximize_path, m_maximize_bitmap);
    load_bitmap(LexicalPath::absolute_path(buttons_path, "window-minimize.png"), m_last_minimize_path, m_minimize_bitmap);

    load_bitmap(m_preview_palette.active_window_shadow_path(), m_last_active_window_shadow_path, m_active_window_shadow);
    load_bitmap(m_preview_palette.inactive_window_shadow_path(), m_last_inactive_window_shadow_path, m_inactive_window_shadow);
    load_bitmap(m_preview_palette.menu_shadow_path(), m_last_menu_shadow_path, m_menu_shadow);
    load_bitmap(m_preview_palette.taskbar_shadow_path(), m_last_taskbar_shadow_path, m_taskbar_shadow);
    load_bitmap(m_preview_palette.tooltip_shadow_path(), m_last_tooltip_shadow_path, m_tooltip_shadow);
}

void PreviewWidget::set_preview_palette(const Gfx::Palette& palette)
{
    m_preview_palette = palette;
    m_gallery->set_preview_palette(palette);
    load_theme_bitmaps();
    update();
}

void PreviewWidget::set_theme_from_file(Core::File& file)
{
    auto config_file = Core::ConfigFile::open(file.filename(), file.leak_fd()).release_value_but_fixme_should_propagate_errors();
    auto theme = Gfx::load_system_theme(config_file);
    VERIFY(theme.is_valid());

    m_preview_palette = Gfx::Palette(Gfx::PaletteImpl::create_with_anonymous_buffer(theme));
    set_preview_palette(m_preview_palette);
    if (on_theme_load_from_file)
        on_theme_load_from_file(file.filename());
}

void PreviewWidget::set_color_filter(OwnPtr<Gfx::ColorBlindnessFilter> color_filter)
{
    m_color_filter = move(color_filter);
    repaint();
}

void PreviewWidget::paint_event(GUI::PaintEvent& event)
{
    GUI::Frame::paint_event(event);
    GUI::Painter painter(*this);

    painter.add_clip_rect(event.rect());
    painter.add_clip_rect(frame_inner_rect());

    painter.fill_rect(frame_inner_rect(), m_preview_palette.desktop_background());

    struct Button {
        Gfx::IntRect rect;
        RefPtr<Gfx::Bitmap> bitmap;
    };

    auto paint_window = [&](auto& title, const Gfx::IntRect& rect, auto state, const Gfx::Bitmap& icon) {
        int window_button_width = m_preview_palette.window_title_button_width();
        int window_button_height = m_preview_palette.window_title_button_height();
        auto titlebar_text_rect = Gfx::WindowTheme::current().titlebar_text_rect(Gfx::WindowTheme::WindowType::Normal, rect, m_preview_palette);
        int pos = titlebar_text_rect.right() + 1;

        Vector<Button> buttons;
        buttons.append(Button { {}, m_close_bitmap.is_null() ? m_default_close_bitmap : m_close_bitmap });
        buttons.append(Button { {}, m_maximize_bitmap.is_null() ? m_default_maximize_bitmap : m_maximize_bitmap });
        buttons.append(Button { {}, m_minimize_bitmap.is_null() ? m_default_minimize_bitmap : m_minimize_bitmap });

        for (auto& button : buttons) {
            pos -= window_button_width;
            Gfx::IntRect button_rect { pos, 0, window_button_width, window_button_height };
            button_rect.center_vertically_within(titlebar_text_rect);
            button.rect = button_rect;
        }

        painter.fill_rect(rect, m_preview_palette.window());

        auto frame_rect = Gfx::WindowTheme::current().frame_rect_for_window(Gfx::WindowTheme::WindowType::Normal, rect, m_preview_palette, 0);

        auto paint_shadow = [](Gfx::Painter& painter, Gfx::IntRect& frame_rect, Gfx::Bitmap const& shadow_bitmap) {
            auto total_shadow_size = shadow_bitmap.height();
            auto shadow_rect = frame_rect.inflated(total_shadow_size, total_shadow_size);
            Gfx::StylePainter::paint_simple_rect_shadow(painter, shadow_rect, shadow_bitmap);
        };

        if (state == Gfx::WindowTheme::WindowState::Active && m_active_window_shadow) {
            paint_shadow(painter, frame_rect, *m_active_window_shadow);
        } else if (state == Gfx::WindowTheme::WindowState::Inactive && m_inactive_window_shadow) {
            paint_shadow(painter, frame_rect, *m_inactive_window_shadow);
        }

        Gfx::PainterStateSaver saver(painter);
        painter.translate(frame_rect.location());
        Gfx::WindowTheme::current().paint_normal_frame(painter, state, rect, title, icon, m_preview_palette, buttons.last().rect, 0, false);

        for (auto& button : buttons) {
            Gfx::StylePainter::paint_button(painter, button.rect, m_preview_palette, Gfx::ButtonStyle::Normal, false);
            auto bitmap_rect = button.bitmap->rect().centered_within(button.rect);
            painter.blit(bitmap_rect.location(), *button.bitmap, button.bitmap->rect());
        }
    };

    auto active_rect = Gfx::IntRect(0, 0, 320, 240).centered_within(frame_inner_rect()).translated(0, 20);
    auto inactive_rect = active_rect.translated(-20, -20);

    paint_window("Inactive window", inactive_rect, Gfx::WindowTheme::WindowState::Inactive, *m_active_window_icon);
    paint_window("Active window", active_rect, Gfx::WindowTheme::WindowState::Active, *m_inactive_window_icon);
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
