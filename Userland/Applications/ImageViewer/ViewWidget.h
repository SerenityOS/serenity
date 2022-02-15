/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Mohsan Ali <mohsan0073@gmail.com>
 * Copyright (c) 2022, Mustafa Quraish <mustafa@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/Timer.h>
#include <LibGUI/AbstractZoomPanWidget.h>
#include <LibGUI/Painter.h>
#include <LibImageDecoderClient/Client.h>

namespace ImageViewer {

class ViewWidget final : public GUI::AbstractZoomPanWidget {
    C_OBJECT(ViewWidget)
public:
    enum Directions {
        First,
        Back,
        Forward,
        Last
    };

    virtual ~ViewWidget() override = default;

    const Gfx::Bitmap* bitmap() const { return m_bitmap.ptr(); }
    const String& path() const { return m_path; }
    void set_toolbar_height(int height) { m_toolbar_height = height; }
    int toolbar_height() { return m_toolbar_height; }
    bool scaled_for_first_image() { return m_scaled_for_first_image; }
    void set_scaled_for_first_image(bool val) { m_scaled_for_first_image = val; }
    void set_path(const String& path);
    void resize_window();
    void set_scaling_mode(Gfx::Painter::ScalingMode);

    bool is_next_available() const;
    bool is_previous_available() const;

    void clear();
    void flip(Gfx::Orientation);
    void rotate(Gfx::RotationDirection);
    void navigate(Directions);
    void load_from_file(const String&);

    Function<void()> on_doubleclick;
    Function<void(const GUI::DropEvent&)> on_drop;
    Function<void(const Gfx::Bitmap*)> on_image_change;

private:
    ViewWidget();
    virtual void doubleclick_event(GUI::MouseEvent&) override;
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void mouseup_event(GUI::MouseEvent&) override;
    virtual void drop_event(GUI::DropEvent&) override;

    void set_bitmap(const Gfx::Bitmap* bitmap);
    void animate();
    Vector<String> load_files_from_directory(const String& path) const;

    String m_path;
    RefPtr<Gfx::Bitmap> m_bitmap;
    Optional<ImageDecoderClient::DecodedImage> m_decoded_image;

    size_t m_current_frame_index { 0 };
    size_t m_loops_completed { 0 };
    NonnullRefPtr<Core::Timer> m_timer;

    int m_toolbar_height { 28 };
    bool m_scaled_for_first_image { false };
    Vector<String> m_files_in_same_dir;
    Optional<size_t> m_current_index;
    Gfx::Painter::ScalingMode m_scaling_mode { Gfx::Painter::ScalingMode::NearestNeighbor };
};

}
