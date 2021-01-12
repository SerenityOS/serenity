/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <LibGfx/BMPLoader.h>
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
