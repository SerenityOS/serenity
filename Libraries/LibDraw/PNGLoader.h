#pragma once

#include <LibDraw/GraphicsBitmap.h>
#include <LibDraw/ImageDecoder.h>

RefPtr<GraphicsBitmap> load_png(const StringView& path);
RefPtr<GraphicsBitmap> load_png_from_memory(const u8*, size_t);

struct PNGLoadingContext;

class PNGImageDecoderPlugin final : public ImageDecoderPlugin {
public:
    virtual ~PNGImageDecoderPlugin() override;
    PNGImageDecoderPlugin(const u8*, size_t);

    virtual Size size() override;
    virtual RefPtr<GraphicsBitmap> bitmap() override;
    virtual void set_volatile() override;
    [[nodiscard]] virtual bool set_nonvolatile() override;

private:
    OwnPtr<PNGLoadingContext> m_context;
};
