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

#pragma once

#include <AK/Function.h>
#include <LibCore/Timer.h>
#include <LibWeb/Loader/ImageResource.h>

namespace Web {

class ImageLoader : public ImageResourceClient {
public:
    ImageLoader();

    void load(const URL&);

    const Gfx::Bitmap* bitmap() const;

    bool has_image() const;

    bool has_loaded_or_failed() const { return m_loading_state != LoadingState::Loading; }

    void set_visible_in_viewport(bool) const;

    unsigned width() const;
    unsigned height() const;

    Function<void()> on_load;
    Function<void()> on_fail;
    Function<void()> on_animate;

private:
    // ^ImageResourceClient
    virtual void resource_did_load() override;
    virtual void resource_did_fail() override;
    virtual bool is_visible_in_viewport() const override { return m_visible_in_viewport; }

    void animate();

    enum class LoadingState {
        None,
        Loading,
        Loaded,
        Failed,
    };

    mutable bool m_visible_in_viewport { false };

    size_t m_current_frame_index { 0 };
    size_t m_loops_completed { 0 };
    LoadingState m_loading_state { LoadingState::Loading };
    NonnullRefPtr<Core::Timer> m_timer;
};

}
