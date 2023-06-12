/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Platform/AudioCodecPlugin.h>

namespace Web::Platform {

static Function<ErrorOr<NonnullOwnPtr<AudioCodecPlugin>>()> s_creation_hook;

AudioCodecPlugin::AudioCodecPlugin() = default;
AudioCodecPlugin::~AudioCodecPlugin() = default;

void AudioCodecPlugin::install_creation_hook(Function<ErrorOr<NonnullOwnPtr<AudioCodecPlugin>>()> creation_hook)
{
    VERIFY(!s_creation_hook);
    s_creation_hook = move(creation_hook);
}

ErrorOr<NonnullOwnPtr<AudioCodecPlugin>> AudioCodecPlugin::create()
{
    VERIFY(s_creation_hook);
    return s_creation_hook();
}

}
