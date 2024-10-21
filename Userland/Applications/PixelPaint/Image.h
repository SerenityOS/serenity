/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Mustafa Quraish <mustafa@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Selection.h"
#include <AK/HashTable.h>
#include <AK/JsonObjectSerializer.h>
#include <AK/Optional.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/Result.h>
#include <LibGUI/Command.h>
#include <LibGUI/Forward.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Rect.h>
#include <LibGfx/ScalingMode.h>
#include <LibGfx/Size.h>

namespace PixelPaint {

class Layer;
class Selection;

class ImageClient {
public:
    virtual void image_did_add_layer(size_t) { }
    virtual void image_did_remove_layer(size_t) { }
    virtual void image_did_modify_layer_properties(size_t) { }
    virtual void image_did_modify_layer_bitmap(size_t) { }
    virtual void image_did_modify_layer_stack() { }
    virtual void image_did_change(Gfx::IntRect const&) { }
    virtual void image_did_change_rect(Gfx::IntRect const&) { }
    virtual void image_select_layer(Layer*) { }

protected:
    virtual ~ImageClient() = default;
};

class Image : public RefCounted<Image> {
public:
    static ErrorOr<NonnullRefPtr<Image>> create_with_size(Gfx::IntSize);
    static ErrorOr<NonnullRefPtr<Image>> create_from_pixel_paint_json(JsonObject const&);
    static ErrorOr<NonnullRefPtr<Image>> create_from_bitmap(NonnullRefPtr<Gfx::Bitmap> const&);

    static ErrorOr<NonnullRefPtr<Gfx::Bitmap>> decode_bitmap(ReadonlyBytes, Optional<StringView> guessed_mime_type);

    // This generates a new Bitmap with the final image (all layers composed according to their attributes.)
    ErrorOr<NonnullRefPtr<Gfx::Bitmap>> compose_bitmap(Gfx::BitmapFormat format) const;
    RefPtr<Gfx::Bitmap> copy_bitmap(Selection const&) const;

    Selection& selection() { return m_selection; }
    Selection const& selection() const { return m_selection; }

    size_t layer_count() const { return m_layers.size(); }
    Layer const& layer(size_t index) const { return m_layers.at(index); }
    Layer& layer(size_t index) { return m_layers.at(index); }

    Gfx::IntSize size() const { return m_size; }
    Gfx::IntRect rect() const { return { {}, m_size }; }

    void add_layer(NonnullRefPtr<Layer>);
    void insert_layer(NonnullRefPtr<Layer>, size_t index);
    ErrorOr<NonnullRefPtr<Image>> take_snapshot() const;
    ErrorOr<void> restore_snapshot(Image const&);

    void paint_into(GUI::Painter&, Gfx::IntRect const& dest_rect, float scale) const;

    ErrorOr<void> serialize_as_json(JsonObjectSerializer<StringBuilder>& json) const;
    ErrorOr<void> export_bmp_to_file(NonnullOwnPtr<Stream>, bool preserve_alpha_channel) const;
    ErrorOr<void> export_png_to_file(NonnullOwnPtr<Stream>, bool preserve_alpha_channel) const;
    ErrorOr<void> export_qoi_to_file(NonnullOwnPtr<Stream>) const;

    void move_layer_to_front(Layer&);
    void move_layer_to_back(Layer&);
    void move_layer_up(Layer&);
    void move_layer_down(Layer&);
    void change_layer_index(size_t old_index, size_t new_index);
    void remove_layer(Layer&);
    void select_layer(Layer*);
    ErrorOr<void> flatten_all_layers();
    ErrorOr<void> merge_visible_layers();
    ErrorOr<void> merge_active_layer_up(Layer& layer);
    ErrorOr<void> merge_active_layer_down(Layer& layer);

    void add_client(ImageClient&);
    void remove_client(ImageClient&);

    void layer_did_modify_bitmap(Badge<Layer>, Layer const&, Gfx::IntRect const& modified_layer_rect);
    void layer_did_modify_properties(Badge<Layer>, Layer const&);

    size_t index_of(Layer const&) const;

    ErrorOr<void> flip(Gfx::Orientation orientation);
    ErrorOr<void> rotate(Gfx::RotationDirection direction);
    ErrorOr<void> crop(Gfx::IntRect const& rect);
    ErrorOr<void> resize(Gfx::IntSize new_size, Gfx::ScalingMode scaling_mode);

    Optional<Gfx::IntRect> nonempty_content_bounding_rect() const;

    Color color_at(Gfx::IntPoint point) const;

private:
    enum class LayerMergeMode {
        All,
        VisibleOnly
    };

    enum class LayerMergeDirection {
        Up,
        Down
    };

    explicit Image(Gfx::IntSize);

    void did_change(Gfx::IntRect const& modified_rect = {});
    void did_change_rect(Gfx::IntRect const& modified_rect = {});
    void did_modify_layer_stack();

    ErrorOr<void> merge_layers(LayerMergeMode);
    ErrorOr<void> merge_active_layer(NonnullRefPtr<Layer> const&, LayerMergeDirection);

    Gfx::IntSize m_size;
    Vector<NonnullRefPtr<Layer>> m_layers;

    HashTable<ImageClient*> m_clients;

    Selection m_selection;
};

class ImageUndoCommand : public GUI::Command {
public:
    ImageUndoCommand(Image&, ByteString action_text);

    virtual void undo() override;
    virtual void redo() override;
    virtual ByteString action_text() const override { return m_action_text; }

private:
    RefPtr<Image> m_snapshot;
    Image& m_image;
    ByteString m_action_text;
};

}
