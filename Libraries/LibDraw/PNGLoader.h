#pragma once

#include <LibDraw/GraphicsBitmap.h>

RefPtr<GraphicsBitmap> load_png(const StringView& path);
RefPtr<GraphicsBitmap> load_png_from_memory(const u8*, size_t);
