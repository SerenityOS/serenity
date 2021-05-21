/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Fetch/Response.h>

namespace Web::Fetch {

class ImageResource final : public Response {
    friend class Response;

public:
    virtual ~ImageResource() override;

    struct Frame {
        RefPtr<Gfx::Bitmap> bitmap;
        size_t duration { 0 };
    };

    const Gfx::Bitmap* bitmap(size_t frame_index = 0) const;
    int frame_duration(size_t frame_index) const;
    size_t frame_count() const
    {
        decode_if_needed();
        return m_decoded_frames.size();
    }
    bool is_animated() const
    {
        decode_if_needed();
        return m_animated;
    }
    size_t loop_count() const
    {
        decode_if_needed();
        return m_loop_count;
    }

    void update_volatility();

private:
    ImageResource();

    void decode_if_needed() const;

    mutable bool m_animated { false };
    mutable int m_loop_count { 0 };
    mutable Vector<Frame> m_decoded_frames;
    mutable bool m_has_attempted_decode { false };
};

class ImageResourceClient : public ResourceClient {
public:
    virtual ~ImageResourceClient();

    virtual bool is_visible_in_viewport() const { return false; }

protected:
    ImageResource* resource() { return static_cast<ImageResource*>(ResourceClient::resource()); }
    const ImageResource* resource() const { return static_cast<const ImageResource*>(ResourceClient::resource()); }

private:
    virtual Response::Type client_type() const override { return Response::Type::Image; }
};

}
