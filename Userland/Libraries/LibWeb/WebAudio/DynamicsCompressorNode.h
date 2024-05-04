/*
 * Copyright (c) 2024, Shannon Booth <shannon@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/WebAudio/AudioNode.h>

namespace Web::WebAudio {

// https://webaudio.github.io/web-audio-api/#DynamicsCompressorOptions
struct DynamicsCompressorOptions : AudioNodeOptions {
    float attack { 0.003 };
    float knee { 30 };
    float ratio { 12 };
    float release { 0.25 };
    float threshold { -24 };
};

// https://webaudio.github.io/web-audio-api/#DynamicsCompressorNode
class DynamicsCompressorNode : public AudioNode {
    WEB_PLATFORM_OBJECT(DynamicsCompressorNode, AudioNode);
    JS_DECLARE_ALLOCATOR(DynamicsCompressorNode);

public:
    virtual ~DynamicsCompressorNode() override;

    static WebIDL::ExceptionOr<JS::NonnullGCPtr<DynamicsCompressorNode>> create(JS::Realm&, JS::NonnullGCPtr<BaseAudioContext>, DynamicsCompressorOptions const& = {});
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<DynamicsCompressorNode>> construct_impl(JS::Realm&, JS::NonnullGCPtr<BaseAudioContext>, DynamicsCompressorOptions const& = {});

protected:
    DynamicsCompressorNode(JS::Realm&, JS::NonnullGCPtr<BaseAudioContext>, DynamicsCompressorOptions const& = {});

    virtual void initialize(JS::Realm&) override;
    virtual void visit_edges(Cell::Visitor&) override;
};

}
