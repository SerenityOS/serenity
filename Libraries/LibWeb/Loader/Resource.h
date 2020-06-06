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

    bool has_encoded_data() const { return !m_encoded_data.is_null(); }

    const URL& url() const { return m_request.url(); }
    const ByteBuffer& encoded_data() const { return m_encoded_data; }

    const HashMap<String, String, CaseInsensitiveStringTraits>& response_headers() const { return m_response_headers; }

    void register_client(Badge<ResourceClient>, ResourceClient&);
    void unregister_client(Badge<ResourceClient>, ResourceClient&);

    const String& encoding() const { return m_encoding; }
    const String& mime_type() const { return m_mime_type; }

    void for_each_client(Function<void(ResourceClient&)>);

    void did_load(Badge<ResourceLoader>, const ByteBuffer& data, const HashMap<String, String, CaseInsensitiveStringTraits>& headers);
    void did_fail(Badge<ResourceLoader>, const String& error);

protected:
    explicit Resource(Type, const LoadRequest&);

private:
    LoadRequest m_request;
    ByteBuffer m_encoded_data;
    Type m_type { Type::Generic };
    bool m_loaded { false };
    bool m_failed { false };
    String m_error;
    String m_encoding;
    String m_mime_type;
    HashMap<String, String, CaseInsensitiveStringTraits> m_response_headers;
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
