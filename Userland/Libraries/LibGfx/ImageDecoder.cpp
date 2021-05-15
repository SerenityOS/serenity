/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/BMPLoader.h>
#include <LibGfx/DDSLoader.h>
#include <LibGfx/GIFLoader.h>
#include <LibGfx/ICOLoader.h>
#include <LibGfx/ImageDecoder.h>
#include <LibGfx/JPGLoader.h>
#include <LibGfx/PBMLoader.h>
#include <LibGfx/PGMLoader.h>
#include <LibGfx/PNGLoader.h>
#include <LibGfx/PPMLoader.h>

namespace Gfx {

ImageDecoder::ImageDecoder(const u8* data, size_t size)
{
    m_plugin = make<PNGImageDecoderPlugin>(data, size);
    if (m_plugin->sniff())
        return;

    m_plugin = make<GIFImageDecoderPlugin>(data, size);
    if (m_plugin->sniff())
        return;

    m_plugin = make<BMPImageDecoderPlugin>(data, size);
    if (m_plugin->sniff())
        return;

    m_plugin = make<PBMImageDecoderPlugin>(data, size);
    if (m_plugin->sniff())
        return;

    m_plugin = make<PGMImageDecoderPlugin>(data, size);
    if (m_plugin->sniff())
        return;

    m_plugin = make<PPMImageDecoderPlugin>(data, size);
    if (m_plugin->sniff())
        return;

    m_plugin = make<ICOImageDecoderPlugin>(data, size);
    if (m_plugin->sniff())
        return;

    m_plugin = make<JPGImageDecoderPlugin>(data, size);
    if (m_plugin->sniff())
        return;

    m_plugin = make<DDSImageDecoderPlugin>(data, size);
    if (m_plugin->sniff())
        return;

    m_plugin = nullptr;
}

ImageDecoder::~ImageDecoder()
{
}

RefPtr<Gfx::Bitmap> ImageDecoder::bitmap() const
{
    if (!m_plugin)
        return nullptr;
    return m_plugin->bitmap();
}

}
