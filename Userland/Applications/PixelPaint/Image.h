/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashTable.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/Result.h>
#include <AK/Vector.h>
#include <LibGUI/Command.h>
#include <LibGUI/Forward.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Rect.h>
#include <LibGfx/Size.h>

namespace PixelPaint {

class Layer;

class ImageClient {
public:
    virtual void image_did_add_layer(size_t) { }
    virtual void image_did_remove_layer(size_t) { }
    virtual void image_did_modify_layer(size_t) { }
    virtual void image_did_modify_layer_stack() { }
    virtual void image_did_change() { }
    virtual void image_select_layer(Layer*) { }
    virtual void image_did_change_title(String const&) { }

protected:
    virtual ~ImageClient() = default;
};

class Image : public RefCounted<Image> {
public:
    static RefPtr<Image> try_create_with_size(Gfx::IntSize const&);
    static Result<NonnullRefPtr<Image>, String> try_create_from_file(String const& file_path);
    static RefPtr<Image> try_create_from_bitmap(NonnullRefPtr<Gfx::Bitmap>);

    // This generates a new Bitmap with the final image (all layers composed according to their attributes.)
    RefPtr<Gfx::Bitmap> try_compose_bitmap() const;

    size_t layer_count() const { return m_layers.size(); }
    Layer const& layer(size_t index) const { return m_layers.at(index); }
    Layer& layer(size_t index) { return m_layers.at(index); }

    Gfx::IntSize const& size() const { return m_size; }
    Gfx::IntRect rect() const { return { {}, m_size }; }

    void add_layer(NonnullRefPtr<Layer>);
    RefPtr<Image> take_snapshot() const;
    void restore_snapshot(Image const&);

    void paint_into(GUI::Painter&, Gfx::IntRect const& dest_rect) const;
    Result<void, String> write_to_file(String const& file_path) const;
    Result<void, String> export_bmp_to_file(String const& file_path);
    Result<void, String> export_png_to_file(String const& file_path);

    void move_layer_to_front(Layer&);
    void move_layer_to_back(Layer&);
    void move_layer_up(Layer&);
    void move_layer_down(Layer&);
    void change_layer_index(size_t old_index, size_t new_index);
    void remove_layer(Layer&);
    void select_layer(Layer*);

    void add_client(ImageClient&);
    void remove_client(ImageClient&);

    void layer_did_modify_bitmap(Badge<Layer>, Layer const&);
    void layer_did_modify_properties(Badge<Layer>, Layer const&);

    size_t index_of(Layer const&) const;

    String const& path() const { return m_path; }
    void set_path(String);

    String const& title() const { return m_title; }
    void set_title(String);

private:
    explicit Image(Gfx::IntSize const&);

    static Result<NonnullRefPtr<Image>, String> try_create_from_pixel_paint_file(String const& file_path);

    void did_change();
    void did_modify_layer_stack();

    String m_path;
    String m_title;

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
