/*
 * Copyright (c) 2020, the SerenityOS developers.
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

#include <AK/FlyString.h>
#include <LibWeb/Namespace.h>

namespace Web {

class QualifiedName {
public:
    QualifiedName(const FlyString& local_name, const FlyString& prefix, const FlyString& namespace_)
        : m_local_name(local_name)
        , m_prefix(prefix)
        , m_namespace(namespace_)
    {
    }

    const FlyString& local_name() const { return m_local_name; }
    const FlyString& prefix() const { return m_prefix; }
    const FlyString& namespace_() const { return m_namespace; }

    // Implementation of https://dom.spec.whatwg.org/#validate-and-extract
    static QualifiedName validate_and_extract(const FlyString& namespace_, const FlyString& qualified_name)
    {
        auto result_namespace = namespace_;
        if (result_namespace.is_empty())
            result_namespace = FlyString();
        // FIXME: Validate qualified_name https://dom.spec.whatwg.org/#validate
        FlyString prefix;
        auto local_name = qualified_name;
        if (qualified_name.view().contains(':')) {
            auto split = qualified_name.view().split_view(':');
            prefix = split[0];
            local_name = split[1];
        }
        if (!prefix.is_null() && namespace_.is_null()) {
            // FIXME: throw a "NamespaceError" DOMException.
            TODO();
        }
        if (prefix == "xml" && namespace_ != Web::Namespace::XML) {
            // FIXME: throw a "NamespaceError" DOMException.
            TODO();
        }
        auto names_have_xmlns = qualified_name == "xmlns" || prefix == "xmlns";
        if (names_have_xmlns && namespace_ != Web::Namespace::XMLNS) {
            // FIXME: throw a "NamespaceError" DOMException.
            TODO();
        }
        if (namespace_ == Namespace::XMLNS && !names_have_xmlns) {
            // FIXME: throw a "NamespaceError" DOMException.
            TODO();
        }
        return QualifiedName(local_name, prefix, result_namespace);
    }

private:
    FlyString m_local_name;
    FlyString m_prefix;
    FlyString m_namespace;
};

}
