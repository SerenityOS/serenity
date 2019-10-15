#include <LibDraw/ImageLoader.h>
#include <LibDraw/PNGLoader.h>

ImageLoader::ImageLoader(const u8* data, size_t size)
{
    m_plugin = make<PNGImageLoaderPlugin>(data, size);
}

ImageLoader::~ImageLoader()
{
}
