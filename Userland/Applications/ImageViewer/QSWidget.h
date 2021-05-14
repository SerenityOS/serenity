/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/Timer.h>
#include <LibGUI/Frame.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/ImageDecoder.h>
#include <LibGfx/Point.h>

class QSWidget final : public GUI::Frame {
    C_OBJECT(QSWidget)
public:
    enum Directions {
        First,
        Back,
        Forward,
        Last
    };

    virtual ~QSWidget() override;

    const Gfx::Bitmap* bitmap() const { return m_bitmap.ptr(); }
    const String& path() const { return m_path; }
    void set_scale(int);
    int scale() { return m_scale; }
    void set_toolbar_height(int height) { m_toolbar_height = height; }
    int toolbar_height() { return m_toolbar_height; }

    void clear();
    void flip(Gfx::Orientation);
    void rotate(Gfx::RotationDirection);
    void navigate(Directions);
    void load_from_file(const String&);

    Function<void(int, Gfx::IntRect)> on_scale_change;
    Function<void()> on_doubleclick;
    Function<void(const GUI::DropEvent&)> on_drop;

private:
    QSWidget();
    virtual void doubleclick_event(GUI::MouseEvent&) override;
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void resize_event(GUI::ResizeEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void mouseup_event(GUI::MouseEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void mousewheel_event(GUI::MouseEvent&) override;
    virtual void drop_event(GUI::DropEvent&) override;

    void set_bitmap(const Gfx::Bitmap* bitmap);

    void relayout();
    void resize_window();
    void reset_view();
    void animate();

    String m_path;
    RefPtr<Gfx::Bitmap> m_bitmap;
    Gfx::IntRect m_bitmap_rect;

    RefPtr<Gfx::ImageDecoder> m_image_decoder;
    size_t m_current_frame_index { 0 };
    size_t m_loops_completed { 0 };
    NonnullRefPtr<Core::Timer> m_timer;

    int m_scale { -1 };
    int m_toolbar_height { 28 };
    Gfx::FloatPoint m_pan_origin;
    Gfx::IntPoint m_click_position;
    Gfx::FloatPoint m_saved_pan_origin;
    Vector<String> m_files_in_same_dir;
};
