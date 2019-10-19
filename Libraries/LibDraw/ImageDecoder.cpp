#include <LibDraw/ImageDecoder.h>
#include <LibDraw/PNGLoader.h>

ImageDecoder::ImageDecoder(const u8* data, size_t size)
{
    m_plugin = make<PNGImageDecoderPlugin>(data, size);
}

ImageDecoder::~ImageDecoder()
{
}
