/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/HTMLImageElementPrototype.h>
#include <LibWeb/Bindings/HTMLImageElementWrapper.h>
#include <LibWeb/Bindings/ImageConstructor.h>
#include <LibWeb/Bindings/NodeWrapperFactory.h>
#include <LibWeb/DOM/ElementFactory.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/Namespace.h>

namespace Web::Bindings {

ImageConstructor::ImageConstructor(JS::Realm& realm)
    : NativeFunction(*realm.global_object().function_prototype())
{
}

void ImageConstructor::initialize(JS::Realm& realm)
{
    auto& vm = this->vm();
    auto& window = static_cast<WindowObject&>(realm.global_object());
    NativeFunction::initialize(realm);

    define_direct_property(vm.names.prototype, &window.ensure_web_prototype<HTMLImageElementPrototype>("HTMLImageElement"), 0);
    define_direct_property(vm.names.length, JS::Value(0), JS::Attribute::Configurable);
}

JS::ThrowCompletionOr<JS::Value> ImageConstructor::call()
{
    return vm().throw_completion<JS::TypeError>(JS::ErrorType::ConstructorWithoutNew, "Image");
}

// https://html.spec.whatwg.org/multipage/embedded-content.html#dom-image
JS::ThrowCompletionOr<JS::Object*> ImageConstructor::construct(FunctionObject&)
{
    // 1. Let document be the current global object's associated Document.
    auto& window = static_cast<WindowObject&>(HTML::current_global_object());
    auto& document = window.impl().associated_document();

    // 2. Let img be the result of creating an element given document, img, and the HTML namespace.
    auto image_element = DOM::create_element(document, HTML::TagNames::img, Namespace::HTML);

    // 3. If width is given, then set an attribute value for img using "width" and width.
    if (vm().argument_count() > 0) {
        u32 width = TRY(vm().argument(0).to_u32(global_object()));
        image_element->set_attribute(HTML::AttributeNames::width, String::formatted("{}", width));
    }

    // 4. If height is given, then set an attribute value for img using "height" and height.
    if (vm().argument_count() > 1) {
        u32 height = TRY(vm().argument(1).to_u32(global_object()));
        image_element->set_attribute(HTML::AttributeNames::height, String::formatted("{}", height));
    }

    // 5. Return img.
    return wrap(global_object(), image_element);
}

}
