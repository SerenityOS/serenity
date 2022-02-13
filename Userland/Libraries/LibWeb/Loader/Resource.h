/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/HashMap.h>
#include <AK/HashTable.h>
#include <AK/Noncopyable.h>
#include <AK/RefCounted.h>
#include <AK/URL.h>
#include <AK/WeakPtr.h>
#include <AK/Weakable.h>
#include <LibGfx/Forward.h>
#include <LibWeb/Forward.h>
#include <LibWeb/Loader/LoadRequest.h>

namespace Web {

class ResourceClient;

class Resource : public RefCounted<Resource> {
    AK_MAKE_NONCOPYABLE(Resource);
    AK_MAKE_NONMOVABLE(Resource);

public:
    enum class Type {
        Generic,
        Image,
    };

    static NonnullRefPtr<Resource> create(Badge<ResourceLoader>, Type, const LoadRequest&);
    virtual ~Resource();

    Type type() const { return m_type; }

    bool is_loaded() const { return m_loaded; }

    bool is_failed() const { return m_failed; }
    const String& error() const { return m_error; }

    bool has_encoded_data() const { return !m_encoded_data.is_empty(); }

    const AK::URL& url() const { return m_request.url(); }
    const ByteBuffer& encoded_data() const { return m_encoded_data; }

    const HashMap<String, String, CaseInsensitiveStringTraits>& response_headers() const { return m_response_headers; }

    [[nodiscard]] Optional<u32> status_code() const { return m_status_code; }

    void register_client(Badge<ResourceClient>, ResourceClient&);
    void unregister_client(Badge<ResourceClient>, ResourceClient&);

    bool has_encoding() const { return m_encoding.has_value(); }
    const Optional<String>& encoding() const { return m_encoding; }
    const String& mime_type() const { return m_mime_type; }

    void for_each_client(Function<void(ResourceClient&)>);

    void did_load(Badge<ResourceLoader>, ReadonlyBytes data, const HashMap<String, String, CaseInsensitiveStringTraits>& headers, Optional<u32> status_code);
    void did_fail(Badge<ResourceLoader>, const String& error, Optional<u32> status_code);

protected:
    explicit Resource(Type, const LoadRequest&);

private:
    LoadRequest m_request;
    ByteBuffer m_encoded_data;
    Type m_type { Type::Generic };
    bool m_loaded { false };
    bool m_failed { false };
    String m_error;
    Optional<String> m_encoding;

    String m_mime_type;
    HashMap<String, String, CaseInsensitiveStringTraits> m_response_headers;
    Optional<u32> m_status_code;
    HashTable<ResourceClient*> m_clients;
};

class ResourceClient : public Weakable<ResourceClient> {
public:
    virtual ~ResourceClient();

    virtual void resource_did_load() { }
    virtual void resource_did_fail() { }

protected:
    virtual Resource::Type client_type() const { return Resource::Type::Generic; }

    Resource* resource() { return m_resource; }
    const Resource* resource() const { return m_resource; }
    void set_resource(Resource*);

private:
    RefPtr<Resource> m_resource;
};

}
