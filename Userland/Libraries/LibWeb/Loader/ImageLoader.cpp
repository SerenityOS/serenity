/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include <LibCore/Timer.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/ImageDecoder.h>
#include <LibWeb/Loader/ImageLoader.h>
#include <LibWeb/Loader/ResourceLoader.h>

namespace Web {

ImageLoader::ImageLoader()
    : m_timer(Core::Timer::construct())
{
}

void ImageLoader::load(const URL& url)
{
    m_loading_state = LoadingState::Loading;
    LoadRequest request;
    request.set_url(url);
    set_resource(ResourceLoader::the().load_resource(Resource::Type::Image, request));
}

void ImageLoader::set_visible_in_viewport(bool visible_in_viewport) const
{
    if (m_visible_in_viewport == visible_in_viewport)
        return;
    m_visible_in_viewport = visible_in_viewport;

    // FIXME: Don't update volatility every time. If we're here, we're probably scanning through
    //        the whole document, updating "is visible in viewport" flags, and this could lead
    //        to the same bitmap being marked volatile back and forth unnecessarily.
    if (resource())
        const_cast<ImageResource*>(resource())->update_volatility();
}

void ImageLoader::resource_did_load()
{
    ASSERT(resource());

    if (!resource()->mime_type().starts_with("image/")) {
        m_loading_state = LoadingState::Failed;
        if (on_fail)
            on_fail();
        return;
    }

    m_loading_state = LoadingState::Loaded;

#ifdef IMAGE_LOADER_DEBUG
    if (!resource()->has_encoded_data()) {
        dbg() << "ImageLoader: Resource did load, no encoded data. URL: " << resource()->url();
    } else {
        dbg() << "ImageLoader: Resource did load, has encoded data. URL: " << resource()->url();
    }
#endif

    if (resource()->should_decode_in_process()) {
        auto& decoder = resource()->ensure_decoder();

        if (decoder.is_animated() && decoder.frame_count() > 1) {
            const auto& first_frame = decoder.frame(0);
            m_timer->set_interval(first_frame.duration);
            m_timer->on_timeout = [this] { animate(); };
            m_timer->start();
        }
    }

    if (on_load)
        on_load();
}

void ImageLoader::animate()
{
    if (!m_visible_in_viewport)
        return;

    auto& decoder = resource()->ensure_decoder();

    m_current_frame_index = (m_current_frame_index + 1) % decoder.frame_count();
    const auto& current_frame = decoder.frame(m_current_frame_index);

    if (current_frame.duration != m_timer->interval()) {
        m_timer->restart(current_frame.duration);
    }

    if (m_current_frame_index == decoder.frame_count() - 1) {
        ++m_loops_completed;
        if (m_loops_completed > 0 && m_loops_completed == decoder.loop_count()) {
            m_timer->stop();
        }
    }

    if (on_animate)
        on_animate();
}

void ImageLoader::resource_did_fail()
{
    dbg() << "ImageLoader: Resource did fail. URL: " << resource()->url();
    m_loading_state = LoadingState::Failed;
    if (on_fail)
        on_fail();
}

bool ImageLoader::has_image() const
{
    if (!resource())
        return false;
    if (resource()->should_decode_in_process())
        return const_cast<ImageResource*>(resource())->ensure_decoder().bitmap();
    return true;
}

unsigned ImageLoader::width() const
{
    if (!resource())
        return 0;
    if (resource()->should_decode_in_process())
        return const_cast<ImageResource*>(resource())->ensure_decoder().width();
    return bitmap() ? bitmap()->width() : 0;
}

unsigned ImageLoader::height() const
{
    if (!resource())
        return 0;
    if (resource()->should_decode_in_process())
        return const_cast<ImageResource*>(resource())->ensure_decoder().height();
    return bitmap() ? bitmap()->height() : 0;
}

const Gfx::Bitmap* ImageLoader::bitmap() const
{
    if (!resource())
        return nullptr;
    return resource()->bitmap(m_current_frame_index);
}

}
