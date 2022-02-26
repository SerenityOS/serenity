/*
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/AudioConstructor.h>
#include <LibWeb/Bindings/HTMLAudioElementPrototype.h>
#include <LibWeb/Bindings/HTMLAudioElementWrapper.h>
#include <LibWeb/Bindings/NodeWrapperFactory.h>
#include <LibWeb/DOM/ElementFactory.h>
#include <LibWeb/DOM/Window.h>
#include <LibWeb/Namespace.h>

namespace Web::Bindings {

AudioConstructor::AudioConstructor(JS::GlobalObject& global_object)
    : NativeFunction(*global_object.function_prototype())
{
}

void AudioConstructor::initialize(JS::GlobalObject& global_object)
{
    auto& vm = this->vm();
    auto& window = static_cast<WindowObject&>(global_object);
    NativeFunction::initialize(global_object);

    define_direct_property(vm.names.prototype, &window.ensure_web_prototype<HTMLAudioElementPrototype>("HTMLAudioElement"), 0);
    define_direct_property(vm.names.length, JS::Value(0), JS::Attribute::Configurable);
}

AudioConstructor::~AudioConstructor()
{
}

JS::ThrowCompletionOr<JS::Value> AudioConstructor::call()
{
    return vm().throw_completion<JS::TypeError>(global_object(), JS::ErrorType::ConstructorWithoutNew, "Audio");
}

// https://html.spec.whatwg.org/multipage/media.html#dom-audio
JS::ThrowCompletionOr<JS::Object*> AudioConstructor::construct(FunctionObject&)
{
    // 1. Let document be the current global object's associated Document.
    auto& window = static_cast<WindowObject&>(global_object());
    auto& document = window.impl().associated_document();

    // 2. Let audio be the result of creating an element given document, audio, and the HTML namespace.
    auto audio = DOM::create_element(document, HTML::TagNames::audio, Namespace::HTML);

    // 3. Set an attribute value for audio using "preload" and "auto".
    audio->set_attribute(HTML::AttributeNames::preload, "auto"sv);

    auto src_value = vm().argument(0);

    // 4. If src is given, then set an attribute value for audio using "src" and src.
    //    (This will cause the user agent to invoke the object's resource selection algorithm before returning.)
    if (!src_value.is_undefined()) {
        auto src = TRY(src_value.to_string(global_object()));
        audio->set_attribute(HTML::AttributeNames::src, move(src));
    }

    // 5. Return audio.
    return wrap(global_object(), audio);
}

}
