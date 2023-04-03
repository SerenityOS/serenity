/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLMediaElement : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLMediaElement, HTMLElement);

public:
    virtual ~HTMLMediaElement() override;

    enum class NetworkState : u16 {
        Empty,
        Idle,
        Loading,
        NoSource,
    };
    NetworkState network_state() const { return m_network_state; }

    Bindings::CanPlayTypeResult can_play_type(DeprecatedString const& type) const;

    void load() const;
    void pause() const;

protected:
    HTMLMediaElement(DOM::Document&, DOM::QualifiedName);

    virtual JS::ThrowCompletionOr<void> initialize(JS::Realm&) override;

private:
    // https://html.spec.whatwg.org/multipage/media.html#dom-media-networkstate
    NetworkState m_network_state { NetworkState::Empty };
};

}
