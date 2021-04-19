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

#include <LibWeb/DOM/Attr.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/NamedNodeMap.h>

namespace Web::DOM {

NamedNodeMap::NamedNodeMap(Element& element)
    : m_element(element)
{
}

unsigned int NamedNodeMap::length()
{
    return m_element.attribute_list().size();
}

RefPtr<Attr> NamedNodeMap::item(u32 index) const
{
    auto attributes = m_element.attribute_list();
    if (index >= attributes.size())
        return nullptr;
    return attributes[index];
}

RefPtr<Attr> NamedNodeMap::get_named_item(const FlyString& qualified_name) const
{
    return m_element.get_attribute_node(qualified_name);
}

RefPtr<Attr> NamedNodeMap::get_named_item_ns(const FlyString& namespace_, const FlyString& local_name) const
{
    return m_element.get_attribute_node_ns(namespace_, local_name);
}

RefPtr<Attr> NamedNodeMap::set_named_item(Attr& attr)
{
    return m_element.set_attribute(attr);
}

RefPtr<Attr> NamedNodeMap::set_named_item_ns(Attr& attr)
{
    return m_element.set_attribute(attr);
}

NonnullRefPtr<Attr> NamedNodeMap::remove_named_item(const FlyString& qualified_name)
{
    auto attr = m_element.remove_attribute_by_name(qualified_name);
    if (!attr) {
        // FIXME: throw a "NotFoundError" DOMException.
        TODO();
    }
    return attr.release_nonnull();
}

NonnullRefPtr<Attr> NamedNodeMap::remove_named_item_ns(const FlyString& namespace_, const FlyString& local_name)
{
    auto attr = m_element.remove_attribute_by_namespace_and_local(namespace_, local_name);
    if (!attr) {
        // FIXME: throw a "NotFoundError" DOMException.
        TODO();
    }
    return attr.release_nonnull();
}

}
