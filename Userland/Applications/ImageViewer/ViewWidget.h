/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Mohsan Ali <mohsan0073@gmail.com>
 * Copyright (c) 2022, Mustafa Quraish <mustafa@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2023, Caoimhe Byrne <caoimhebyrne06@gmail.com>
 * Copyright (c) 2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/Timer.h>
#include <LibGUI/AbstractZoomPanWidget.h>
#include <LibGUI/Painter.h>
#include <LibGfx/VectorGraphic.h>

namespace ImageViewer {

class Image : public RefCounted<Image> {
public:
    virtual Gfx::IntSize size() const = 0;
    virtual Gfx::IntRect rect() const { return { {}, size() }; }

    virtual void flip(Gfx::Orientation) = 0;
    virtual void rotate(Gfx::RotationDirection) = 0;

    virtual void draw_into(Gfx::Painter&, Gfx::IntRect const& dest, Gfx::ScalingMode) const = 0;

    virtual ErrorOr<NonnullRefPtr<Gfx::Bitmap>> bitmap(Optional<Gfx::IntSize> ideal_size) const = 0;

    virtual ~Image() = default;
};

class VectorImage final : public Image {
public:
    static NonnullRefPtr<VectorImage> create(Gfx::VectorGraphic& vector) { return adopt_ref(*new VectorImage(vector)); }

    virtual Gfx::IntSize size() const override { return m_size; }

    virtual void flip(Gfx::Orientation) override;
    virtual void rotate(Gfx::RotationDirection) override;

    virtual void draw_into(Gfx::Painter&, Gfx::IntRect const& dest, Gfx::ScalingMode) const override;

    virtual ErrorOr<NonnullRefPtr<Gfx::Bitmap>> bitmap(Optional<Gfx::IntSize> ideal_size) const override;

private:
    VectorImage(Gfx::VectorGraphic& vector)
        : m_vector(vector)
        , m_size(vector.size())
    {
    }

    void apply_transform(Gfx::AffineTransform transform)
    {
        m_transform = transform.multiply(m_transform);
    }

    NonnullRefPtr<Gfx::VectorGraphic> m_vector;
    Gfx::IntSize m_size;
    Gfx::AffineTransform m_transform;
};

class BitmapImage final : public Image {
public:
    static NonnullRefPtr<BitmapImage> create(Gfx::Bitmap& bitmap, Gfx::FloatPoint scale) { return adopt_ref(*new BitmapImage(bitmap, scale)); }

    virtual Gfx::IntSize size() const override { return { round(m_bitmap->size().width() * m_scale.x()), round(m_bitmap->size().height() * m_scale.y()) }; }

    virtual void flip(Gfx::Orientation) override;
    virtual void rotate(Gfx::RotationDirection) override;

    virtual void draw_into(Gfx::Painter&, Gfx::IntRect const& dest, Gfx::ScalingMode) const override;

    virtual ErrorOr<NonnullRefPtr<Gfx::Bitmap>> bitmap(Optional<Gfx::IntSize>) const override
    {
        return m_bitmap;
    }

private:
    BitmapImage(Gfx::Bitmap& bitmap, Gfx::FloatPoint scale)
        : m_bitmap(bitmap)
        , m_scale(scale)
    {
    }

    NonnullRefPtr<Gfx::Bitmap> m_bitmap;
    Gfx::FloatPoint m_scale;
};

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

    Image const* image() const { return m_image.ptr(); }
    String const& path() const { return m_path; }
    void set_toolbar_height(int height) { m_toolbar_height = height; }
    int toolbar_height() { return m_toolbar_height; }
    bool scaled_for_first_image() { return m_scaled_for_first_image; }
    void set_scaled_for_first_image(bool val) { m_scaled_for_first_image = val; }
    void set_path(String const& path);
    void resize_window();
    void scale_image_for_window();
    void set_scaling_mode(Gfx::ScalingMode);

    bool is_next_available() const;
    bool is_previous_available() const;

    void clear();
    void flip(Gfx::Orientation);
    void rotate(Gfx::RotationDirection);
    void navigate(Directions);
    void open_file(String const&, Core::File&);

    Function<void()> on_doubleclick;
    Function<void(const GUI::DropEvent&)> on_drop;

    Function<void(Image*)> on_image_change;

private:
    ViewWidget();
    virtual void doubleclick_event(GUI::MouseEvent&) override;
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void mouseup_event(GUI::MouseEvent&) override;
    virtual void drag_enter_event(GUI::DragEvent&) override;
    virtual void drop_event(GUI::DropEvent&) override;
    virtual void resize_event(GUI::ResizeEvent&) override;

    void set_image(Image const* image);
    void animate();
    Vector<ByteString> load_files_from_directory(ByteString const& path) const;
    ErrorOr<void> try_open_file(String const&, Core::File&);

    String m_path;
    RefPtr<Image> m_image;

    struct Animation {
        struct Frame {
            RefPtr<Image> image;
            int duration { 0 };
        };

        size_t loop_count { 0 };
        Vector<Frame> frames;
    };

    Optional<Animation> m_animation;

    size_t m_current_frame_index { 0 };
    size_t m_loops_completed { 0 };
    NonnullRefPtr<Core::Timer> m_timer;

    int m_toolbar_height { 28 };
    bool m_scaled_for_first_image { false };
    Vector<ByteString> m_files_in_same_dir;
    Optional<size_t> m_current_index;
    Gfx::ScalingMode m_scaling_mode { Gfx::ScalingMode::BoxSampling };
};

}
