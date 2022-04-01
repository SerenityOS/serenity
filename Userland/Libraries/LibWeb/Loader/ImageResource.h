/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Loader/Resource.h>

namespace Web {

class ImageResource final : public Resource {
    friend class Resource;

public:
    static NonnullRefPtr<ImageResource> convert_from_resource(Resource&);

    virtual ~ImageResource() override;

    struct Frame {
        RefPtr<Gfx::Bitmap> bitmap;
        size_t duration { 0 };
    };

    Gfx::Bitmap const* bitmap(size_t frame_index = 0) const;
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
    explicit ImageResource(LoadRequest const&);
    explicit ImageResource(Resource&);

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
    ImageResource const* resource() const { return static_cast<ImageResource const*>(ResourceClient::resource()); }

private:
    virtual Resource::Type client_type() const override { return Resource::Type::Image; }
};

}
