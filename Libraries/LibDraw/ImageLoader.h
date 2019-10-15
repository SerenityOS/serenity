#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/OwnPtr.h>
#include <AK/RefCounted.h>
#include <LibDraw/Size.h>

class GraphicsBitmap;

class ImageLoaderPlugin {
public:
    virtual ~ImageLoaderPlugin() {}

    virtual Size size() = 0;
    virtual RefPtr<GraphicsBitmap> bitmap() = 0;

protected:
    ImageLoaderPlugin() {}
};

class ImageLoader : public RefCounted<ImageLoader> {
public:
    static NonnullRefPtr<ImageLoader> create(const u8* data, size_t size) { return adopt(*new ImageLoader(data, size)); }
    ~ImageLoader();

    Size size() const { return m_plugin->size(); }
    int width() const { return size().width(); }
    int height() const { return size().height(); }
    RefPtr<GraphicsBitmap> bitmap() const { return m_plugin->bitmap(); }

private:
    ImageLoader(const u8*, size_t);

    mutable OwnPtr<ImageLoaderPlugin> m_plugin;
};
