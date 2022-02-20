/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/HashTable.h>
#include <LibWeb/DOM/QualifiedName.h>

namespace Web::DOM {

struct ImplTraits : public Traits<QualifiedName::Impl*> {
    static unsigned hash(QualifiedName::Impl* impl)
    {
        return pair_int_hash(impl->local_name.hash(), pair_int_hash(impl->prefix.hash(), impl->namespace_.hash()));
    }

    static bool equals(QualifiedName::Impl* a, QualifiedName::Impl* b)
    {
        return a->local_name == b->local_name
            && a->prefix == b->prefix
            && a->namespace_ == b->namespace_;
    }
};

static HashTable<QualifiedName::Impl*, ImplTraits> impls;

static NonnullRefPtr<QualifiedName::Impl> ensure_impl(FlyString const& local_name, FlyString const& prefix, FlyString const& namespace_)
{
    auto hash = pair_int_hash(local_name.hash(), pair_int_hash(prefix.hash(), namespace_.hash()));
    auto it = impls.find(hash, [&](QualifiedName::Impl* entry) {
        return entry->local_name == local_name
            && entry->prefix == prefix
            && entry->namespace_ == namespace_;
    });
    if (it != impls.end())
        return *(*it);
    return adopt_ref(*new QualifiedName::Impl(local_name, prefix, namespace_));
}

QualifiedName::QualifiedName(FlyString const& local_name, FlyString const& prefix, FlyString const& namespace_)
    : m_impl(ensure_impl(local_name, prefix, namespace_))
{
}

QualifiedName::Impl::Impl(FlyString const& a_local_name, FlyString const& a_prefix, FlyString const& a_namespace)
    : local_name(a_local_name)
    , prefix(a_prefix)
    , namespace_(a_namespace)
{
    impls.set(this);
    make_internal_string();
}

QualifiedName::Impl::~Impl()
{
    impls.remove(this);
}

// https://dom.spec.whatwg.org/#concept-attribute-qualified-name
// https://dom.spec.whatwg.org/#concept-element-qualified-name
void QualifiedName::Impl::make_internal_string()
{
    // This is possible to do according to the spec: "User agents could have this as an internal slot as an optimization."
    if (prefix.is_null()) {
        as_string = local_name;
        return;
    }

    as_string = String::formatted("{}:{}", prefix, local_name);
}

}
