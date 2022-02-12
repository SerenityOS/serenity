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

constexpr size_t maximum_redirects_allowed = 20;

class FrameLoader final
    : public ResourceClient {
public:
    enum class Type {
        Navigation,
        Reload,
        IFrame,
    };

    explicit FrameLoader(HTML::BrowsingContext&);
    ~FrameLoader();

    bool load(const AK::URL&, Type);
    bool load(LoadRequest&, Type);

    void load_html(StringView, const AK::URL&);

    HTML::BrowsingContext& browsing_context() { return m_browsing_context; }
    HTML::BrowsingContext const& browsing_context() const { return m_browsing_context; }

private:
    // ^ResourceClient
    virtual void resource_did_load() override;
    virtual void resource_did_fail() override;

    void load_error_page(const AK::URL& failed_url, const String& error_message);
    void load_favicon(RefPtr<Gfx::Bitmap> bitmap = nullptr);
    bool parse_document(DOM::Document&, const ByteBuffer& data);

    void store_response_cookies(AK::URL const& url, String const& cookies);

    HTML::BrowsingContext& m_browsing_context;
    size_t m_redirects_count { 0 };
};

}
