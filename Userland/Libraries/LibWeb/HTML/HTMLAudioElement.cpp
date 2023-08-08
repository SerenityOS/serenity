/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/AudioTrack.h>
#include <LibWeb/HTML/AudioTrackList.h>
#include <LibWeb/HTML/HTMLAudioElement.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/Layout/AudioBox.h>

namespace Web::HTML {

HTMLAudioElement::HTMLAudioElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLMediaElement(document, move(qualified_name))
{
}

HTMLAudioElement::~HTMLAudioElement() = default;

void HTMLAudioElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::HTMLAudioElementPrototype>(realm, "HTMLAudioElement"));
}

JS::GCPtr<Layout::Node> HTMLAudioElement::create_layout_node(NonnullRefPtr<CSS::StyleProperties> style)
{
    return heap().allocate_without_realm<Layout::AudioBox>(document(), *this, move(style));
}

Layout::AudioBox* HTMLAudioElement::layout_node()
{
    return static_cast<Layout::AudioBox*>(Node::layout_node());
}

Layout::AudioBox const* HTMLAudioElement::layout_node() const
{
    return static_cast<Layout::AudioBox const*>(Node::layout_node());
}

void HTMLAudioElement::on_playing()
{
    audio_tracks()->for_each_enabled_track([](auto& audio_track) {
        audio_track.play({});
    });
}

void HTMLAudioElement::on_paused()
{
    audio_tracks()->for_each_enabled_track([](auto& audio_track) {
        audio_track.pause({});
    });
}

void HTMLAudioElement::on_seek(double position, MediaSeekMode seek_mode)
{
    audio_tracks()->for_each_enabled_track([&](auto& audio_track) {
        audio_track.seek(position, seek_mode);
    });
}

void HTMLAudioElement::on_volume_change()
{
    audio_tracks()->for_each_enabled_track([&](auto& audio_track) {
        audio_track.update_volume();
    });
}

}
