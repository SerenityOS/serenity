/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 *
 */

#pragma once

#include <AK/Noncopyable.h>
#include <AK/RefCounted.h>
#include <AK/String.h>
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
    virtual ~Layer() = default;

    Gfx::IntPoint const& location() const { return m_location; }
    void set_location(Gfx::IntPoint const& location) { m_location = location; }

    virtual Gfx::Bitmap const& display_bitmap() const { return m_cached_display_bitmap; }
    virtual Gfx::Bitmap const& content_bitmap() const = 0;
    virtual Gfx::Bitmap& content_bitmap() = 0;

    Gfx::Bitmap const* mask_bitmap() const { return m_mask_bitmap; }
    Gfx::Bitmap* mask_bitmap() { return m_mask_bitmap; }

    virtual void create_mask();

    Gfx::IntSize size() const { return content_bitmap().size(); }

    Gfx::IntRect relative_rect() const { return { location(), size() }; }
    Gfx::IntRect rect() const { return { {}, size() }; }

    String const& name() const { return m_name; }
    void set_name(String);

    virtual void flip(Gfx::Orientation orientation) = 0;
    virtual void rotate(Gfx::RotationDirection direction) = 0;
    virtual void crop(Gfx::IntRect const& rect) = 0;
    virtual void resize(Gfx::IntSize const& new_size, Gfx::Painter::ScalingMode scaling_mode);
    virtual void resize(Gfx::IntRect const& new_rect, Gfx::Painter::ScalingMode scaling_mode);
    virtual void resize(Gfx::IntSize const& new_size, Gfx::IntPoint const& new_location, Gfx::Painter::ScalingMode scaling_mode) = 0;

    void set_selected(bool selected) { m_selected = selected; }
    bool is_selected() const { return m_selected; }

    bool is_visible() const { return m_visible; }
    void set_visible(bool visible);

    int opacity_percent() const { return m_opacity_percent; }
    void set_opacity_percent(int);

    virtual RefPtr<Gfx::Bitmap> try_copy_bitmap(Selection const&) const;

    Image const& image() const { return m_image; }

    virtual void erase_selection(Selection const&);

    bool is_masked() const { return !m_mask_bitmap.is_null(); }

    enum class EditMode {
        Content,
        Mask,
    };

    EditMode edit_mode() { return m_edit_mode; }
    void set_edit_mode(EditMode mode);

    virtual bool is_current_bitmap_editable() { return true; }
    Gfx::Bitmap& currently_edited_bitmap();

    virtual void did_modify(Gfx::IntRect const& = {});

    enum class LayerType {
        Undefined,
        BitmapLayer,
    };

    LayerType layer_type() const { return m_layer_type; }

protected:
    Layer(LayerType type, Image&, String name, NonnullRefPtr<Gfx::Bitmap> cached_display_bitmap);

    Image& m_image;
    NonnullRefPtr<Gfx::Bitmap> m_cached_display_bitmap;
    RefPtr<Gfx::Bitmap> m_mask_bitmap { nullptr };
    bool m_visible { true };
    int m_opacity_percent { 100 };

    void update_cached_bitmap();

private:
    String m_name;
    Gfx::IntPoint m_location;

    bool m_selected { false };

    EditMode m_edit_mode { EditMode::Content };
    LayerType m_layer_type { LayerType::Undefined };
};

}
