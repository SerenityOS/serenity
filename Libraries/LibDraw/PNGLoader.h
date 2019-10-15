#pragma once

#include <LibDraw/GraphicsBitmap.h>
#include <LibDraw/ImageLoader.h>

RefPtr<GraphicsBitmap> load_png(const StringView& path);
RefPtr<GraphicsBitmap> load_png_from_memory(const u8*, size_t);

struct PNGLoadingContext;

class PNGImageLoaderPlugin final : public ImageLoaderPlugin {
public:
    virtual ~PNGImageLoaderPlugin() override;
    PNGImageLoaderPlugin(const u8*, size_t);

    virtual Size size() override;
    virtual RefPtr<GraphicsBitmap> bitmap() override;

private:
    OwnPtr<PNGLoadingContext> m_context;
};
