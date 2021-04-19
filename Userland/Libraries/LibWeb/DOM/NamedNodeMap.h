/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
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

#pragma once

#include <AK/String.h>
#include <LibWeb/DOM/Attr.h>
#include <LibWeb/DOM/Node.h>

namespace Web::DOM {

class NamedNodeMap final
    : public Bindings::Wrappable
    , public RefCounted<NamedNodeMap> {
public:
    using WrapperType = Bindings::NamedNodeMapWrapper;

    explicit NamedNodeMap(Element& element);

    unsigned length();
    RefPtr<Attr> item(u32 index) const;
    RefPtr<Attr> get_named_item(const FlyString& qualified_name) const;
    RefPtr<Attr> get_named_item_ns(const FlyString& namespace_, const FlyString& local_name) const;
    RefPtr<Attr> set_named_item(Attr& attr);
    RefPtr<Attr> set_named_item_ns(Attr& attr);
    NonnullRefPtr<Attr> remove_named_item(const FlyString& qualified_name);
    NonnullRefPtr<Attr> remove_named_item_ns(const FlyString& namespace_, const FlyString& local_name);

private:
    Element& m_element;
};

}
