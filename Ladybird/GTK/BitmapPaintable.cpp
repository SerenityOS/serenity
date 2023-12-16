/*
 * Copyright (c) 2023, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BitmapPaintable.h"
#include <LibGfx/Bitmap.h>
#include <gtk/gtk.h>

struct _LadybirdBitmapPaintable {
    GObject parent_instance;
    GdkTexture* texture;
    float scale;
};

enum {
    PROP_0,
    PROP_TEXTURE,
    NUM_PROPS
};

static GParamSpec* props[NUM_PROPS];

G_BEGIN_DECLS

static void ladybird_bitmap_paintable_paintable_init(GdkPaintableInterface* iface);

G_DEFINE_FINAL_TYPE_WITH_CODE(LadybirdBitmapPaintable, ladybird_bitmap_paintable, G_TYPE_OBJECT,
    G_IMPLEMENT_INTERFACE(GDK_TYPE_PAINTABLE, ladybird_bitmap_paintable_paintable_init))

GdkTexture* ladybird_bitmap_paintable_get_texture(LadybirdBitmapPaintable* self)
{
    g_return_val_if_fail(LADYBIRD_IS_BITMAP_PAINTABLE(self), nullptr);

    return self->texture;
}

static void ladybird_bitmap_paintable_snapshot(GdkPaintable* paintable, GdkSnapshot* snapshot, double width, double height)
{
    LadybirdBitmapPaintable* self = LADYBIRD_BITMAP_PAINTABLE(paintable);

    if (self->texture == nullptr)
        return;

    graphene_rect_t paintable_bounds = GRAPHENE_RECT_INIT(0.0, 0.0, (float)width, (float)height);
    gtk_snapshot_push_clip(GTK_SNAPSHOT(snapshot), &paintable_bounds);

    graphene_rect_t texture_bounds = GRAPHENE_RECT_INIT(0.0, 0.0,
        gdk_texture_get_width(self->texture) / self->scale,
        gdk_texture_get_height(self->texture) / self->scale);
    gtk_snapshot_append_texture(GTK_SNAPSHOT(snapshot), self->texture, &texture_bounds);

    gtk_snapshot_pop(GTK_SNAPSHOT(snapshot));
}

void ladybird_bitmap_paintable_push_bitmap(LadybirdBitmapPaintable* self, Gfx::Bitmap const* bitmap, int width, int height, float scale, bool is_static)
{
    g_return_if_fail(LADYBIRD_IS_BITMAP_PAINTABLE(self));
    g_return_if_fail(scale > 0.0);
    g_return_if_fail(width > 0);
    g_return_if_fail(height > 0);

    g_clear_object(&self->texture);
    self->scale = scale;

    if (bitmap == nullptr)
        goto out;

    GdkMemoryFormat format;
    switch (bitmap->format()) {
    case Gfx::BitmapFormat::BGRA8888:
        format = GDK_MEMORY_B8G8R8A8;
        break;
    case Gfx::BitmapFormat::RGBA8888:
        format = GDK_MEMORY_R8G8B8A8;
        break;
#if defined(GDK_VERSION_4_14) && (GDK_VERSION_MAX_ALLOWED >= GDK_VERSION_4_14)
    // GDK_MEMORY_B8G8R8X8 added in GTK 4.14
    case Gfx::BitmapFormat::BGRx8888:
        format = GDK_MEMORY_B8G8R8X8;
        break;
#endif
    default:
        goto out;
    }

    // We're blatantly lying about the bytes being static here. This should cause GBytes to avoid
    // making a copy. Hopefully -- hopefully! -- the bitmap won't get invalidated until another
    // one is pushed, at which point we'll stop using this one. But it's a real risky thing we're
    // doing.
    GBytes* bytes;
    if (is_static)
        bytes = g_bytes_new_static(bitmap->scanline_u8(0), bitmap->size_in_bytes());
    else
        bytes = g_bytes_new(bitmap->scanline_u8(0), bitmap->size_in_bytes());
    self->texture = gdk_memory_texture_new(width, height, format, bytes, bitmap->pitch());
    g_bytes_unref(bytes);

out:
    gdk_paintable_invalidate_contents(GDK_PAINTABLE(self));
    g_object_notify_by_pspec(G_OBJECT(self), props[PROP_TEXTURE]);
}

static GdkPaintable* ladybird_bitmap_paintable_get_current_image(GdkPaintable* paintable)
{
    LadybirdBitmapPaintable* self = LADYBIRD_BITMAP_PAINTABLE(paintable);

    if (self->texture == nullptr)
        return gdk_paintable_new_empty(0, 0);

    double width = gdk_texture_get_width(self->texture) / self->scale;
    double height = gdk_texture_get_height(self->texture) / self->scale;
    graphene_size_t size = GRAPHENE_SIZE_INIT((float)width, (float)height);

    GtkSnapshot* snapshot = gtk_snapshot_new();
    gdk_paintable_snapshot(paintable, snapshot, width, height);
    return gtk_snapshot_free_to_paintable(snapshot, &size);
}

static int ladybird_bitmap_paintable_get_intrinsic_width(GdkPaintable* paintable)
{
    LadybirdBitmapPaintable* self = LADYBIRD_BITMAP_PAINTABLE(paintable);
    if (self->texture == nullptr)
        return 0;
    return (int)round(gdk_texture_get_width(self->texture) / self->scale);
}

static int ladybird_bitmap_paintable_get_intrinsic_height(GdkPaintable* paintable)
{
    LadybirdBitmapPaintable* self = LADYBIRD_BITMAP_PAINTABLE(paintable);
    if (self->texture == nullptr)
        return 0;
    return (int)round(gdk_texture_get_height(self->texture) / self->scale);
}

static void ladybird_bitmap_paintable_get_property(GObject* object, guint prop_id, GValue* value, GParamSpec* pspec)
{
    LadybirdBitmapPaintable* self = LADYBIRD_BITMAP_PAINTABLE(object);

    switch (prop_id) {
    case PROP_TEXTURE:
        g_value_set_object(value, self->texture);
        break;

    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void ladybird_bitmap_paintable_set_property(GObject* object, guint prop_id, [[maybe_unused]] GValue const* value, GParamSpec* pspec)
{
    G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
}

static void ladybird_bitmap_paintable_dispose(GObject* object)
{
    LadybirdBitmapPaintable* self = LADYBIRD_BITMAP_PAINTABLE(object);

    g_clear_object(&self->texture);

    G_OBJECT_CLASS(ladybird_bitmap_paintable_parent_class)->dispose(object);
}

static void ladybird_bitmap_paintable_init([[maybe_unused]] LadybirdBitmapPaintable* self)
{
}

static void ladybird_bitmap_paintable_class_init(LadybirdBitmapPaintableClass* klass)
{
    GObjectClass* object_class = G_OBJECT_CLASS(klass);

    object_class->get_property = ladybird_bitmap_paintable_get_property;
    object_class->set_property = ladybird_bitmap_paintable_set_property;
    object_class->dispose = ladybird_bitmap_paintable_dispose;

    props[PROP_TEXTURE] = g_param_spec_object("texture", nullptr, nullptr, GDK_TYPE_TEXTURE, GParamFlags(G_PARAM_READABLE | G_PARAM_STATIC_STRINGS | G_PARAM_EXPLICIT_NOTIFY));
    g_object_class_install_properties(object_class, NUM_PROPS, props);
}

static void ladybird_bitmap_paintable_paintable_init(GdkPaintableInterface* iface)
{
    iface->snapshot = ladybird_bitmap_paintable_snapshot;
    iface->get_current_image = ladybird_bitmap_paintable_get_current_image;
    iface->get_intrinsic_width = ladybird_bitmap_paintable_get_intrinsic_width;
    iface->get_intrinsic_height = ladybird_bitmap_paintable_get_intrinsic_height;
}

LadybirdBitmapPaintable* ladybird_bitmap_paintable_new(void)
{
    return LADYBIRD_BITMAP_PAINTABLE(g_object_new(LADYBIRD_TYPE_BITMAP_PAINTABLE, nullptr));
}

G_END_DECLS
