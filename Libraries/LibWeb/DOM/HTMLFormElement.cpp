/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/StringBuilder.h>
#include <LibWeb/DOM/HTMLFormElement.h>
#include <LibWeb/DOM/HTMLInputElement.h>
#include <LibWeb/Frame.h>
#include <LibWeb/HtmlView.h>

namespace Web {

HTMLFormElement::HTMLFormElement(Document& document, const String& tag_name)
    : HTMLElement(document, tag_name)
{
}

HTMLFormElement::~HTMLFormElement()
{
}

void HTMLFormElement::submit()
{
    if (action().is_null()) {
        dbg() << "Unsupported form action ''";
        return;
    }

    if (method().to_lowercase() != "get") {
        dbg() << "Unsupported form method '" << method() << "'";
        return;
    }

    URL url(document().complete_url(action()));

    struct NameAndValue {
        String name;
        String value;
    };

    Vector<NameAndValue> parameters;

    for_each_in_subtree_of_type<HTMLInputElement>([&](auto& node) {
        auto& input = to<HTMLInputElement>(node);
        if (!input.name().is_null())
            parameters.append({ input.name(), input.value() });
        return IterationDecision::Continue;
    });

    StringBuilder builder;
    for (size_t i = 0; i < parameters.size(); ++i) {
        builder.append(parameters[i].name);
        builder.append('=');
        builder.append(parameters[i].value);
        if (i != parameters.size() - 1)
            builder.append('&');
    }
    url.set_query(builder.to_string());

    // FIXME: We shouldn't let the form just do this willy-nilly.
    document().frame()->html_view()->load(url);
}

}
