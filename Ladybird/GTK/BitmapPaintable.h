#pragma once

#include <gdk/gdk.h>

G_BEGIN_DECLS

G_DECLARE_FINAL_TYPE(LadybirdBitmapPaintable, ladybird_bitmap_paintable, LADYBIRD, BITMAP_PAINTABLE, GObject)
#define LADYBIRD_TYPE_BITMAP_PAINTABLE ladybird_bitmap_paintable_get_type()

namespace Gfx {
class Bitmap;
}

LadybirdBitmapPaintable* ladybird_bitmap_paintable_new(void);
void ladybird_bitmap_paintable_push_bitmap(LadybirdBitmapPaintable* self, Gfx::Bitmap const*, int width, int height, float scale, bool is_static);

GdkTexture* ladybird_bitmap_paintable_get_texture(LadybirdBitmapPaintable* self);

G_END_DECLS
