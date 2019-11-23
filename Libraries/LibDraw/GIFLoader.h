#pragma once

#include <LibDraw/GraphicsBitmap.h>
#include <LibDraw/ImageDecoder.h>

RefPtr<GraphicsBitmap> load_gif(const StringView& path);
RefPtr<GraphicsBitmap> load_gif_from_memory(const u8*, size_t);

struct GIFLoadingContext;

class GIFImageDecoderPlugin final : public ImageDecoderPlugin {
public:
    virtual ~GIFImageDecoderPlugin() override;
    GIFImageDecoderPlugin(const u8*, size_t);

    virtual Size size() override;
    virtual RefPtr<GraphicsBitmap> bitmap() override;

private:
    OwnPtr<GIFLoadingContext> m_context;
};
