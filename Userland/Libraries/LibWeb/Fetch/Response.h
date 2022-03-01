/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/HashMap.h>
#include <AK/HashTable.h>
#include <AK/MemoryStream.h>
#include <AK/Noncopyable.h>
#include <AK/RefCounted.h>
#include <AK/URL.h>
#include <AK/WeakPtr.h>
#include <AK/Weakable.h>
#include <LibGfx/Forward.h>
#include <LibWeb/Fetch/FetchParams.h>
#include <LibWeb/Fetch/LoadRequest.h>
#include <LibWeb/Forward.h>

namespace Web::Fetch {

class ResourceClient;

class Response : public RefCounted<Response> {
    AK_MAKE_NONCOPYABLE(Response);
    AK_MAKE_NONMOVABLE(Response);

public:
    enum class Type {
        Generic,
        Image,
    };

    // FIXME: This is here to ease refactoring
    enum class NewType {
        Basic,
        Cors,
        Default,
        Error,
        Opaque,
        OpaqueRedirect,
    };

    enum class CacheState {
        None,
        Local,
        Validated,
    };

    static NonnullRefPtr<Response> create(Badge<ResourceLoader>, Type);
    static NonnullRefPtr<Response> create_network_error(Badge<ResourceLoader>);
    virtual ~Response();

    Type type() const { return m_type; }
    NewType new_type() const { return m_new_type; } // FIXME: Rename to type

    bool is_network_error() const { return m_new_type == NewType::Error; }
    bool is_aborted_network_error() const { return m_new_type == NewType::Error && m_aborted; }

    bool has_encoded_data() const { return m_body.size() > 0; }

    /*Optional<URL>*/ void url() const
    {
        // if (m_url_list.is_empty())
        //     return {}; // FIXME: Returns local temp object!!
        dbgln("{}", m_url_list);
        return; // m_url_list.last();
    }

    const ByteBuffer& body() const { return m_body; }
    ErrorOr<void> set_body(ReadonlyBytes body) { m_body = TRY(ByteBuffer::copy(body)); }

    //    const HashMap<String, String, CaseInsensitiveStringTraits>& response_headers() const { return m_response_headers; }

    // [[nodiscard]] Optional<u32> status_code() const { return m_status_code; }

    void register_client(Badge<ResourceClient>, ResourceClient&);
    void unregister_client(Badge<ResourceClient>, ResourceClient&);

    //    const String& encoding() const { return m_encoding; }
    Optional<Core::MimeType> mime_type() const { return m_header_list.extract_mime_type(); }

    void for_each_client(Function<void(ResourceClient&)>);

    void did_load(Badge<ResourceLoader>, ReadonlyBytes data, const HashMap<String, String, CaseInsensitiveStringTraits>& headers, Optional<u32> status_code);
    void did_fail(Badge<ResourceLoader>, const String& error, Optional<u32> status_code);

    const HTTP::HeaderList& header_list() const { return m_header_list; }
    void set_header_list(Badge<ResourceLoader>, const HTTP::HeaderList& header_list) { m_header_list = header_list; }

    // https://fetch.spec.whatwg.org/#redirect-status
    bool has_redirect_status() const
    {
        return m_status == 301 || m_status == 302 || m_status == 303 || m_status == 307 || m_status == 308;
    }

    // https://fetch.spec.whatwg.org/#null-body-status
    bool has_null_body_status() const
    {
        return m_status == 101 || m_status == 204 || m_status == 205 || m_status == 304;
    }

    void set_timing_info(Badge<ResourceLoader>, const FetchTimingInfo& timing_info) { m_timing_info = timing_info; }

    NonnullRefPtr<Response> to_filtered_response(LoadRequest::ResponseTainting) const;
    bool is_filtered_response() const { return !!m_internal_response; }

    [[nodiscard]] bool should_be_blocked_due_to_mime_type(const LoadRequest&) const;
    [[nodiscard]] bool should_be_blocked_due_to_nosniff(const LoadRequest&) const;

    // https://html.spec.whatwg.org/multipage/urls-and-fetching.html#cors-same-origin
    bool is_cors_same_origin() const
    {
        return m_new_type == NewType::Basic || m_new_type == NewType::Cors || m_new_type == NewType::Default;
    }

    // https://html.spec.whatwg.org/multipage/urls-and-fetching.html#cors-cross-origin
    bool is_cors_cross_origin() const
    {
        return m_new_type == NewType::Opaque || m_new_type == NewType::OpaqueRedirect;
    }

    // https://html.spec.whatwg.org/multipage/urls-and-fetching.html#unsafe-response
    const Response& unsafe_response() const
    {
        if (m_internal_response)
            return *m_internal_response;
        return *this;
    }

    void append_header(Badge<ResourceLoader>, const String& name, const String& value)
    {
        m_header_list.append(name, value);
    }

    void set_status(Badge<ResourceLoader>, u32 status) { m_status = status; }

    bool range_requested() const { return m_range_requested; }
    void set_range_requested(Badge<ResourceLoader>, bool range_requested) { m_range_requested = range_requested; }

    // NOTE: This is an intentional copy, as the spec always makes a copy.
    // void set_url_list(Badge<ResourceLoader>, const Vector<URL>& url_list) { m_url_list = url_list; }
    // void set_url_list(const Vector<URL>& url_list) { m_url_list = url_list; } // FIXME: REMOVE

    u32 status() const { return m_status; }

    Optional<AK::URL> location_url(String const& request_fragment) const;

    NonnullRefPtr<Response> clone() const;

    void set_timing_allow_passed(Badge<ResourceLoader>, bool timing_allow_passed) { m_timing_allow_passed = timing_allow_passed; }

    RefPtr<Response> internal_response() const { return m_internal_response; }

    const String& status_message() const { return m_status_message; }

protected:
    Response();

private:
    Type m_type { Type::Generic };           // FIXME: Remove
    NewType m_new_type { NewType::Default }; // FIXME: Rename to m_type
    bool m_aborted { false };
    Vector<AK::URL> m_url_list;
    u32 m_status { 200 };
    String m_status_message; // FIXME: Should be a byte sequence
    HTTP::HeaderList m_header_list;
    ByteBuffer m_body; // FIXME: This should be a body object.
    CacheState m_cache_state { CacheState::None };
    Vector<String> m_cors_exposed_header_name_list; // FIXME: These should specifically store header names, and therefore be byte sequences.
    bool m_range_requested { false };
    bool m_timing_allow_passed { false };
    FetchTimingInfo m_timing_info; // FIXME: This should be nullable, and null by default.
    HashTable<ResourceClient*> m_clients;
    RefPtr<Response> m_internal_response; // Used for filtered responses to contain the original response.
};

class ResourceClient : public Weakable<ResourceClient> {
public:
    virtual ~ResourceClient();

    virtual void resource_did_load() { }
    virtual void resource_did_fail() { }

protected:
    virtual Response::Type client_type() const { return Response::Type::Generic; }

    Response* resource() { return m_resource; }
    const Response* resource() const { return m_resource; }
    void set_resource(Response*);

private:
    RefPtr<Response> m_resource;
};

}
