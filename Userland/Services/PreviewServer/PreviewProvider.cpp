/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PreviewProvider.h"

namespace PreviewServer {

// Populated via REGISTER_PREVIEW_PROVIDER.
static Vector<NonnullRefPtr<PreviewProvider>> s_providers;

PreviewProviderRegistration::PreviewProviderRegistration(Function<ErrorOr<NonnullRefPtr<PreviewProvider>>()> constructor)
{
    if (auto instantiated_provider = constructor(); !instantiated_provider.is_error())
        s_providers.append(instantiated_provider.release_value());
    else
        dbgln("Warning: Could not instantiate preview provider due to OOM at startup");
}

CacheEntry PreviewProvider::generate_preview_with_any_provider(String const& file)
{
    for (auto const& provider : s_providers) {
        if (provider->can_generate_preview_for(file))
            return provider->generate_preview(file);
    }
    return Error::from_string_view("No suitable provider found"sv);
}

ErrorOr<NonnullRefPtr<ImageDecoderClient::Client>> PreviewProvider::image_decoder_client()
{
    static Optional<NonnullRefPtr<ImageDecoderClient::Client>> s_image_decoder_client;

    if (s_image_decoder_client.has_value())
        return s_image_decoder_client.value();

    s_image_decoder_client = TRY(ImageDecoderClient::Client::try_create());
    return s_image_decoder_client.value();
}

}
