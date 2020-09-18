/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "PreviewWidget.h"
#include <AK/StringView.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/Painter.h>
#include <LibGUI/RadioButton.h>
#include <LibGUI/StatusBar.h>
#include <LibGUI/TextEditor.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/WindowTheme.h>

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
        set_fill_with_background_color(true);
        m_button = add<GUI::Button>();
        m_button->set_text("Button");
        m_checkbox = add<GUI::CheckBox>();
        m_checkbox->set_text("Check box");
        m_radio = add<GUI::RadioButton>();
        m_radio->set_text("Radio button");
        m_statusbar = add<GUI::StatusBar>();
        m_statusbar->set_text("Status bar");
        m_editor = add<GUI::TextEditor>();
        m_editor->set_text("Text editor\nwith multiple\nlines.");
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
    RefPtr<GUI::StatusBar> m_statusbar;
};

PreviewWidget::PreviewWidget(const Gfx::Palette& preview_palette)
    : m_preview_palette(preview_palette)
{
    m_active_window_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/window.png");
    m_inactive_window_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/window.png");

    m_close_bitmap = Gfx::Bitmap::load_from_file("/res/icons/16x16/window-close.png");
    m_maximize_bitmap = Gfx::Bitmap::load_from_file("/res/icons/16x16/window-maximize.png");
    m_minimize_bitmap = Gfx::Bitmap::load_from_file("/res/icons/16x16/window-minimize.png");

    m_gallery = add<MiniWidgetGallery>();
    set_greedy_for_hits(true);
}

PreviewWidget::~PreviewWidget()
{
}

void PreviewWidget::set_preview_palette(const Gfx::Palette& palette)
{
    m_preview_palette = palette;
    m_gallery->set_preview_palette(palette);
    update();
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
        auto title_bar_text_rect = Gfx::WindowTheme::current().title_bar_text_rect(Gfx::WindowTheme::WindowType::Normal, rect, m_preview_palette);
        int pos = title_bar_text_rect.right() + 1;

        Vector<Button> buttons;
        buttons.append(Button { {}, m_close_bitmap });
        buttons.append(Button { {}, m_maximize_bitmap });
        buttons.append(Button { {}, m_minimize_bitmap });

        for (auto& button : buttons) {
            pos -= window_button_width;
            Gfx::IntRect rect { pos, 0, window_button_width, window_button_height };
            rect.center_vertically_within(title_bar_text_rect);
            button.rect = rect;
        }

        auto frame_rect = Gfx::WindowTheme::current().frame_rect_for_window(Gfx::WindowTheme::WindowType::Normal, rect, m_preview_palette);
        Gfx::PainterStateSaver saver(painter);
        painter.translate(frame_rect.location());
        Gfx::WindowTheme::current().paint_normal_frame(painter, state, rect, title, icon, m_preview_palette, buttons.last().rect);

        for (auto& button : buttons) {
            Gfx::StylePainter::paint_button(painter, button.rect, m_preview_palette, Gfx::ButtonStyle::Normal, false);
            auto bitmap_rect = button.bitmap->rect();
            bitmap_rect.center_within(button.rect);
            painter.blit(bitmap_rect.location(), *button.bitmap, button.bitmap->rect());
        }
    };

    Gfx::IntRect active_rect { 0, 0, 320, 240 };
    active_rect.center_within(frame_inner_rect());
    Gfx::IntRect inactive_rect = active_rect.translated(-20, -20);

    paint_window("Inactive window", inactive_rect, Gfx::WindowTheme::WindowState::Inactive, *m_active_window_icon);
    paint_window("Active window", active_rect, Gfx::WindowTheme::WindowState::Active, *m_inactive_window_icon);
}

void PreviewWidget::resize_event(GUI::ResizeEvent&)
{
    Gfx::IntRect gallery_rect { 0, 0, 320, 240 };
    gallery_rect.center_within(rect());
    m_gallery->set_relative_rect(gallery_rect);
}

}
