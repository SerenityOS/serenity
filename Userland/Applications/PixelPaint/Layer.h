/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2022, Timothy Slater <tslater2006@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/Noncopyable.h>
#include <AK/RefCounted.h>
#include <AK/Weakable.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Painter.h>

namespace PixelPaint {

class Image;
class Selection;

class Layer
    : public RefCounted<Layer>
    , public Weakable<Layer> {

    AK_MAKE_NONCOPYABLE(Layer);
    AK_MAKE_NONMOVABLE(Layer);

public:
    static ErrorOr<NonnullRefPtr<Layer>> create_with_size(Image&, Gfx::IntSize, DeprecatedString name);
    static ErrorOr<NonnullRefPtr<Layer>> create_with_bitmap(Image&, NonnullRefPtr<Gfx::Bitmap>, DeprecatedString name);
    static ErrorOr<NonnullRefPtr<Layer>> create_snapshot(Image&, Layer const&);

    ~Layer() = default;

    Gfx::IntPoint location() const { return m_location; }
    void set_location(Gfx::IntPoint location) { m_location = location; }

    Gfx::Bitmap const& display_bitmap() const { return m_cached_display_bitmap; }
    Gfx::Bitmap const& content_bitmap() const { return m_content_bitmap; }
    Gfx::Bitmap& content_bitmap() { return m_content_bitmap; }

    Gfx::Bitmap const* mask_bitmap() const { return m_mask_bitmap; }
    Gfx::Bitmap* mask_bitmap() { return m_mask_bitmap; }

    ErrorOr<void> create_mask();
    Gfx::Bitmap& get_scratch_edited_bitmap();

    Gfx::IntSize size() const { return content_bitmap().size(); }

    Gfx::IntRect relative_rect() const { return { location(), size() }; }
    Gfx::IntRect rect() const { return { {}, size() }; }

    DeprecatedString const& name() const { return m_name; }
    void set_name(DeprecatedString);

    enum class NotifyClients {
        Yes,
        No
    };

    ErrorOr<void> flip(Gfx::Orientation orientation, NotifyClients notify_clients = NotifyClients::Yes);
    ErrorOr<void> rotate(Gfx::RotationDirection direction, NotifyClients notify_clients = NotifyClients::Yes);
    ErrorOr<void> crop(Gfx::IntRect const& rect, NotifyClients notify_clients = NotifyClients::Yes);
    ErrorOr<void> resize(Gfx::IntSize new_size, Gfx::Painter::ScalingMode scaling_mode, NotifyClients notify_clients = NotifyClients::Yes);
    ErrorOr<void> resize(Gfx::IntRect const& new_rect, Gfx::Painter::ScalingMode scaling_mode, NotifyClients notify_clients = NotifyClients::Yes);
    ErrorOr<void> resize(Gfx::IntSize new_size, Gfx::IntPoint new_location, Gfx::Painter::ScalingMode scaling_mode, NotifyClients notify_clients = NotifyClients::Yes);

    Optional<Gfx::IntRect> nonempty_content_bounding_rect() const;

    ErrorOr<void> set_bitmaps(NonnullRefPtr<Gfx::Bitmap> content, RefPtr<Gfx::Bitmap> mask);

    void did_modify_bitmap(Gfx::IntRect const& = {}, NotifyClients notify_clients = NotifyClients::Yes);

    void set_selected(bool selected) { m_selected = selected; }
    bool is_selected() const { return m_selected; }

    bool is_visible() const { return m_visible; }
    void set_visible(bool visible);

    int opacity_percent() const { return m_opacity_percent; }
    void set_opacity_percent(int);

    RefPtr<Gfx::Bitmap> copy_bitmap(Selection const&) const;

    Image const& image() const { return m_image; }

    void erase_selection(Selection const&);

    bool is_masked() const { return !m_mask_bitmap.is_null(); }

    enum class EditMode {
        Content,
        Mask,
    };

    EditMode edit_mode() { return m_edit_mode; }
    void set_edit_mode(EditMode mode);

    Gfx::Bitmap& currently_edited_bitmap();

private:
    Layer(Image&, NonnullRefPtr<Gfx::Bitmap>, DeprecatedString name);

    Image& m_image;

    DeprecatedString m_name;
    Gfx::IntPoint m_location;
    NonnullRefPtr<Gfx::Bitmap> m_content_bitmap;
    RefPtr<Gfx::Bitmap> m_scratch_edited_bitmap { nullptr };
    RefPtr<Gfx::Bitmap> m_mask_bitmap { nullptr };
    NonnullRefPtr<Gfx::Bitmap> m_cached_display_bitmap;

    bool m_selected { false };
    bool m_visible { true };

    int m_opacity_percent { 100 };

    EditMode m_edit_mode { EditMode::Content };

    void update_cached_bitmap();
};

}
