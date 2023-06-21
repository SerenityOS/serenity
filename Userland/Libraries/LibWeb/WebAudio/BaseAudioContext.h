/*
 * Copyright (c) 2023, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/EventTarget.h>

namespace Web::WebAudio {

// https://webaudio.github.io/web-audio-api/#BaseAudioContext
class BaseAudioContext : public DOM::EventTarget {
    WEB_PLATFORM_OBJECT(BaseAudioContext, DOM::EventTarget);

public:
    virtual ~BaseAudioContext() override;

protected:
    explicit BaseAudioContext(JS::Realm&);

    virtual JS::ThrowCompletionOr<void> initialize(JS::Realm&) override;
};

}
