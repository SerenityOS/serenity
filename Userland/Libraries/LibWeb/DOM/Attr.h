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

#include <LibWeb/DOM/Node.h>

namespace Web::DOM {

class Attr final : public Node {
public:
    using WrapperType = Bindings::AttrWrapper;

    Attr(Document&, const FlyString& name, const String& value);
    virtual ~Attr() override;

    virtual FlyString node_name() const override { return "attr"; }

    Element* owner_element() const { return m_element; }
    void set_owner_element(Element* element) { m_element = element; }

    const FlyString& namespace_uri() const { return m_namespace; }
    void set_namespace_uri(const FlyString& namespace_) { m_namespace = namespace_; }
    const FlyString& prefix() const { return m_prefix; }
    void set_prefix(const FlyString&);
    const FlyString& local_name() const { return m_local_name; }
    const FlyString& name() const { return m_qualified_name; }
    const String& value() const { return m_value; }
    void set_value(const String& value) { m_value = value; }

    // According to the spec, this is useless and always returns true :^)
    bool specified() const { return true; }

private:
    FlyString m_local_name;
    FlyString m_qualified_name;
    String m_value;
    FlyString m_namespace;
    FlyString m_prefix;
    Element* m_element { nullptr };
};

}
