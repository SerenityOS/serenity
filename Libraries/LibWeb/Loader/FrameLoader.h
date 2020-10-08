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
    RefPtr<DOM::Document> create_document_from_mime_type(const ByteBuffer&, const URL&, const String& mime_type, const String& encoding);

    Frame& m_frame;
};

}
