#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/OwnPtr.h>
#include <AK/RefCounted.h>
#include <LibDraw/Size.h>

class GraphicsBitmap;

class ImageDecoderPlugin {
public:
    virtual ~ImageDecoderPlugin() {}

    virtual Size size() = 0;
    virtual RefPtr<GraphicsBitmap> bitmap() = 0;

    virtual void set_volatile() = 0;
    [[nodiscard]] virtual bool set_nonvolatile() = 0;

protected:
    ImageDecoderPlugin() {}
};

class ImageDecoder : public RefCounted<ImageDecoder> {
public:
    static NonnullRefPtr<ImageDecoder> create(const u8* data, size_t size) { return adopt(*new ImageDecoder(data, size)); }
    ~ImageDecoder();

    Size size() const { return m_plugin->size(); }
    int width() const { return size().width(); }
    int height() const { return size().height(); }
    RefPtr<GraphicsBitmap> bitmap() const { return m_plugin->bitmap(); }
    void set_volatile() { m_plugin->set_volatile(); }
    [[nodiscard]] bool set_nonvolatile() { return m_plugin->set_nonvolatile(); }

private:
    ImageDecoder(const u8*, size_t);

    mutable OwnPtr<ImageDecoderPlugin> m_plugin;
};
