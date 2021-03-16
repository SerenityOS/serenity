/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
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

#include <AK/ScopeGuard.h>
#include <LibWeb/Bindings/CSSStyleDeclarationWrapper.h>
#include <LibWeb/CSS/Parser/DeprecatedCSSParser.h>
#include <LibWeb/DOM/Element.h>

namespace Web::Bindings {

JS::Value CSSStyleDeclarationWrapper::get(const JS::PropertyName& name, JS::Value receiver) const
{
    // FIXME: These should actually use camelCase versions of the property names!
    auto property_id = CSS::property_id_from_string(name.to_string());
    if (property_id == CSS::PropertyID::Invalid)
        return Base::get(name, receiver);
    for (auto& property : impl().properties()) {
        if (property.property_id == property_id)
            return js_string(vm(), property.value->to_string());
    }
    return js_string(vm(), String::empty());
}

bool CSSStyleDeclarationWrapper::put(const JS::PropertyName& name, JS::Value value, JS::Value receiver)
{
    // FIXME: These should actually use camelCase versions of the property names!
    auto property_id = CSS::property_id_from_string(name.to_string());
    if (property_id == CSS::PropertyID::Invalid)
        return Base::put(name, value, receiver);

    auto css_text = value.to_string(global_object());
    if (vm().exception())
        return false;

    auto new_value = parse_css_value(CSS::ParsingContext {}, css_text, property_id);
    // FIXME: What are we supposed to do if we can't parse it?
    if (!new_value)
        return false;

    ScopeGuard style_invalidation_guard = [&] {
        auto& declaration = downcast<CSS::ElementInlineCSSStyleDeclaration>(impl());
        if (auto* element = declaration.element())
            element->invalidate_style();
    };

    // FIXME: I don't think '!important' is being handled correctly here..

    for (auto& property : impl().m_properties) {
        if (property.property_id == property_id) {
            property.value = new_value.release_nonnull();
            return true;
        }
    }

    impl().m_properties.append(CSS::StyleProperty {
        .property_id = property_id,
        .value = new_value.release_nonnull(),
        .important = false,
    });
    return true;
}

}
