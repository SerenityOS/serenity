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
#include <LibWeb/HTML/HTMLFormElement.h>
#include <LibWeb/HTML/HTMLInputElement.h>
#include <LibWeb/InProcessWebView.h>
#include <LibWeb/Page/Frame.h>
#include <LibWeb/URLEncoder.h>

namespace Web::HTML {

HTMLFormElement::HTMLFormElement(DOM::Document& document, const FlyString& tag_name)
    : HTMLElement(document, tag_name)
{
}

HTMLFormElement::~HTMLFormElement()
{
}

void HTMLFormElement::submit(RefPtr<HTMLInputElement> submitter)
{
    if (action().is_null()) {
        dbg() << "Unsupported form action ''";
        return;
    }

    auto effective_method = method().to_lowercase();
    if (effective_method != "get" && effective_method != "post") {
        if (effective_method == "dialog") {
            dbg() << "Unsupported form method '" << method() << "'";
            return;
        }
        effective_method = "get";
    }

    URL url(document().complete_url(action()));

    Vector<URLQueryParam> parameters;

    for_each_in_subtree_of_type<HTMLInputElement>([&](auto& node) {
        auto& input = downcast<HTMLInputElement>(node);
        if (!input.name().is_null() && (input.type() != "submit" || &input == submitter))
            parameters.append({ input.name(), input.value() });
        return IterationDecision::Continue;
    });

    if (effective_method == "get") {
        url.set_query(urlencode(parameters));
    }

    // FIXME: We shouldn't let the form just do this willy-nilly.

    LoadRequest request;
    request.set_url(url);

    if (effective_method == "post") {
        auto body = urlencode(parameters).to_byte_buffer();
        request.set_method("POST");
        request.set_header("Content-Type", "application/x-www-form-urlencoded");
        request.set_header("Content-Length", String::number(body.size()));
        request.set_body(body);
    }

    document().frame()->page().load(request);
}

}
