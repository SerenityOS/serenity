/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Mustafa Quraish <mustafa@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashTable.h>
#include <AK/JsonObjectSerializer.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/Result.h>
#include <LibCore/File.h>
#include <LibGUI/Command.h>
#include <LibGUI/Forward.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Rect.h>
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
    static ErrorOr<NonnullRefPtr<Image>> try_create_with_size(Gfx::IntSize const&);
    static ErrorOr<NonnullRefPtr<Image>> try_create_from_pixel_paint_json(JsonObject const&);
    static ErrorOr<NonnullRefPtr<Image>> try_create_from_bitmap(NonnullRefPtr<Gfx::Bitmap>);

    static ErrorOr<NonnullRefPtr<Gfx::Bitmap>> try_decode_bitmap(ReadonlyBytes);

    // This generates a new Bitmap with the final image (all layers composed according to their attributes.)
    ErrorOr<NonnullRefPtr<Gfx::Bitmap>> try_compose_bitmap(Gfx::BitmapFormat format) const;
    RefPtr<Gfx::Bitmap> try_copy_bitmap(Selection const&) const;

    size_t layer_count() const { return m_layers.size(); }
    Layer const& layer(size_t index) const { return m_layers.at(index); }
    Layer& layer(size_t index) { return m_layers.at(index); }

    Gfx::IntSize const& size() const { return m_size; }
    Gfx::IntRect rect() const { return { {}, m_size }; }

    void add_layer(NonnullRefPtr<Layer>);
    ErrorOr<NonnullRefPtr<Image>> take_snapshot() const;
    ErrorOr<void> restore_snapshot(Image const&);

    void paint_into(GUI::Painter&, Gfx::IntRect const& dest_rect) const;

    void serialize_as_json(JsonObjectSerializer<StringBuilder>& json) const;
    ErrorOr<void> write_to_file(String const& file_path) const;
    ErrorOr<void> export_bmp_to_file(Core::File&, bool preserve_alpha_channel);
    ErrorOr<void> export_png_to_file(Core::File&, bool preserve_alpha_channel);

    void move_layer_to_front(Layer&);
    void move_layer_to_back(Layer&);
    void move_layer_up(Layer&);
    void move_layer_down(Layer&);
    void change_layer_index(size_t old_index, size_t new_index);
    void remove_layer(Layer&);
    void select_layer(Layer*);
    void flatten_all_layers();
    void merge_visible_layers();
    void merge_active_layer_down(Layer& layer);

    void add_client(ImageClient&);
    void remove_client(ImageClient&);

    void layer_did_modify_bitmap(Badge<Layer>, Layer const&, Gfx::IntRect const& modified_layer_rect);
    void layer_did_modify_properties(Badge<Layer>, Layer const&);

    size_t index_of(Layer const&) const;

    void flip(Gfx::Orientation orientation);
    void rotate(Gfx::RotationDirection direction);
    void crop(Gfx::IntRect const& rect);

    Color color_at(Gfx::IntPoint const& point) const;

private:
    explicit Image(Gfx::IntSize const&);

    void did_change(Gfx::IntRect const& modified_rect = {});
    void did_change_rect(Gfx::IntRect const& modified_rect = {});
    void did_modify_layer_stack();

    Gfx::IntSize m_size;
    NonnullRefPtrVector<Layer> m_layers;

    HashTable<ImageClient*> m_clients;
};

class ImageUndoCommand : public GUI::Command {
public:
    ImageUndoCommand(Image& image);

    virtual void undo() override;
    virtual void redo() override;

private:
    RefPtr<Image> m_snapshot;
    Image& m_image;
};

}
