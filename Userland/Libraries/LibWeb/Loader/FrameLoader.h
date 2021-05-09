/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <LibWeb/Forward.h>
#include <LibWeb/Loader/Resource.h>

namespace Web {

class FrameLoader final
    : public ResourceClient {
public:
    enum class Type {
        Navigation,
        Reload,
        IFrame,
    };

    explicit FrameLoader(Frame&);
    ~FrameLoader();

    bool load(const URL&, Type);
    bool load(const LoadRequest&, Type);

    void load_html(const StringView&, const URL&);

    Frame& frame() { return m_frame; }
    const Frame& frame() const { return m_frame; }

private:
    // ^ResourceClient
    virtual void resource_did_load() override;
    virtual void resource_did_fail() override;

    void load_error_page(const URL& failed_url, const String& error_message);
    bool parse_document(DOM::Document&, const ByteBuffer& data);

    Frame& m_frame;
};

}
