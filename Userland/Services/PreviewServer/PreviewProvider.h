/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/Noncopyable.h>
#include <LibImageDecoderClient/Client.h>
#include <Services/PreviewServer/Cache.h>

#define REGISTER_PREVIEW_PROVIDER(class_)                                        \
    PreviewServer::PreviewProviderRegistration registration_##class_ {           \
        []() -> AK::ErrorOr<AK::NonnullRefPtr<PreviewServer::PreviewProvider>> { \
            return try_make_ref_counted<PreviewServer::Providers::class_>();     \
        }                                                                        \
    };

namespace PreviewServer {

class PreviewProvider;

class PreviewProviderRegistration {
public:
    PreviewProviderRegistration(Function<ErrorOr<NonnullRefPtr<PreviewProvider>>()> constructor);
    ~PreviewProviderRegistration() = default;
};

// Children of this class provide implementations of how to generate previews for a variety of file types.
class PreviewProvider : public RefCounted<PreviewProvider> {
    AK_MAKE_NONCOPYABLE(PreviewProvider);
    AK_MAKE_NONMOVABLE(PreviewProvider);

    friend class PreviewProviderRegistration;

public:
    PreviewProvider() = default;
    virtual ~PreviewProvider() = default;

    static ErrorOr<NonnullRefPtr<ImageDecoderClient::Client>> image_decoder_client();

    // While generate_preview will also error out on incompatible files, can_generate_preview_for should provide a faster check, e.g. file extension.
    // Furthermore, if this returns true and generate_preview returns an error, other preview providers will not be used.
    virtual bool can_generate_preview_for(String const& file) = 0;
    virtual CacheEntry generate_preview(String const& file) = 0;

    // Searches the list of registered providers until one of them can generate a preview, or errors out.
    static CacheEntry generate_preview_with_any_provider(String const& file);
};

}
