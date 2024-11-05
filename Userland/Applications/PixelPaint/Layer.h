/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2022, Timothy Slater <tslater2006@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/Noncopyable.h>
#include <AK/RefCounted.h>
#include <AK/Weakable.h>
#include <LibGUI/Event.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/ScalingMode.h>

namespace PixelPaint {

class Image;
class ImageEditor;
class Selection;

class Layer
    : public RefCounted<Layer>
    , public Weakable<Layer> {

    AK_MAKE_NONCOPYABLE(Layer);
    AK_MAKE_NONMOVABLE(Layer);

public:
    static ErrorOr<NonnullRefPtr<Layer>> create_with_size(Image&, Gfx::IntSize, ByteString name);
    static ErrorOr<NonnullRefPtr<Layer>> create_with_bitmap(Image&, NonnullRefPtr<Gfx::Bitmap>, ByteString name);
    static ErrorOr<NonnullRefPtr<Layer>> create_snapshot(Image&, Layer const&);

    ~Layer() = default;

    Gfx::IntPoint location() const { return m_location; }
    void set_location(Gfx::IntPoint location) { m_location = location; }

    Gfx::Bitmap const& display_bitmap() const { return m_cached_display_bitmap; }
    Gfx::Bitmap const& content_bitmap() const { return m_content_bitmap; }
    Gfx::Bitmap& content_bitmap() { return m_content_bitmap; }

    Gfx::Bitmap const* mask_bitmap() const { return m_mask_bitmap; }
    Gfx::Bitmap* mask_bitmap() { return m_mask_bitmap; }

    enum class MaskType {
        None,
        BasicMask,
        EditingMask,
    };

    ErrorOr<void> create_mask(MaskType);
    void delete_mask();
    void apply_mask();
    void invert_mask();
    void clear_mask();
    void set_mask_visibility(bool visible) { m_visible_mask = visible; }
    bool mask_visibility() { return m_visible_mask; }

    Gfx::Bitmap& get_scratch_edited_bitmap();

    Gfx::IntSize size() const { return content_bitmap().size(); }

    Gfx::IntRect relative_rect() const { return { location(), size() }; }
    Gfx::IntRect rect() const { return { {}, size() }; }

    ByteString const& name() const { return m_name; }
    void set_name(ByteString);

    enum class NotifyClients {
        Yes,
        No
    };

    ErrorOr<void> flip(Gfx::Orientation orientation, NotifyClients notify_clients = NotifyClients::Yes);
    ErrorOr<void> rotate(Gfx::RotationDirection direction, NotifyClients notify_clients = NotifyClients::Yes);
    ErrorOr<void> crop(Gfx::IntRect const& rect, NotifyClients notify_clients = NotifyClients::Yes);
    ErrorOr<void> scale(Gfx::IntRect const& new_rect, Gfx::ScalingMode scaling_mode, NotifyClients notify_clients = NotifyClients::Yes);

    Optional<Gfx::IntRect> nonempty_content_bounding_rect() const;
    Optional<Gfx::IntRect> editing_mask_bounding_rect() const;

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
    MaskType mask_type() const;

    enum class EditMode {
        Content,
        Mask,
    };

    EditMode edit_mode() { return m_edit_mode; }
    void set_edit_mode(EditMode mode);

    Gfx::Bitmap& currently_edited_bitmap();

    ErrorOr<NonnullRefPtr<Layer>> duplicate(ByteString name);

    ALWAYS_INLINE Color modify_pixel_with_editing_mask(int x, int y, Color const& target_color, Color const& current_color)
    {
        if (mask_type() != MaskType::EditingMask)
            return target_color;

        auto mask = mask_bitmap()->get_pixel(x, y).alpha();
        if (!mask)
            return current_color;

        float mask_intensity = mask / 255.0f;
        return current_color.mixed_with(target_color, mask_intensity);
    }

    void on_second_paint(ImageEditor&);

private:
    Layer(Image&, NonnullRefPtr<Gfx::Bitmap>, ByteString name);

    Image& m_image;

    ByteString m_name;
    Gfx::IntPoint m_location;
    NonnullRefPtr<Gfx::Bitmap> m_content_bitmap;
    RefPtr<Gfx::Bitmap> m_scratch_edited_bitmap { nullptr };
    RefPtr<Gfx::Bitmap> m_mask_bitmap { nullptr };
    NonnullRefPtr<Gfx::Bitmap> m_cached_display_bitmap;

    bool m_selected { false };
    bool m_visible { true };
    bool m_visible_mask { false };

    int m_opacity_percent { 100 };

    EditMode m_edit_mode { EditMode::Content };
    MaskType m_mask_type { MaskType::None };

    void update_cached_bitmap();
};

}
