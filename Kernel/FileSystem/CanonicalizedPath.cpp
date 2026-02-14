/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/FileSystem/CanonicalizedPath.h>
#include <Kernel/Library/KLexicalPath.h>

namespace Kernel {

static RawPtr<CanonicalizedPath> s_root_path;

ErrorOr<NonnullRefPtr<CanonicalizedPath>> CanonicalizedPath::clone() const
{
    auto cloned_full_path = TRY(KString::try_create(full_path()));
    return TRY(adopt_nonnull_ref_or_enomem(new (nothrow) CanonicalizedPath(move(cloned_full_path))));
}

CanonicalizedPath& CanonicalizedPath::fake_root_path()
{
    // NOTE: The fake root path is used only during tear down of VFSRootContexts
    // or in Kernel processes.
    if (!s_root_path)
        s_root_path = &MUST(adopt_nonnull_ref_or_enomem(new (nothrow) CanonicalizedPath())).leak_ref();
    return *s_root_path;
}

ErrorOr<NonnullRefPtr<CanonicalizedPath>> CanonicalizedPath::create(Custody const& base, StringView raw_path)
{
    auto custody_absolute_base_path = TRY(base.try_serialize_absolute_path());
    OwnPtr<KString> full_path;
    if (KLexicalPath::is_absolute(raw_path))
        full_path = TRY(KString::try_create(raw_path));
    else
        full_path = TRY(KLexicalPath::try_join_non_canonical_second(custody_absolute_base_path->view(), raw_path));

    KLexicalPath::canonicalize_absolute_path(*full_path);
    return TRY(adopt_nonnull_ref_or_enomem(new (nothrow) CanonicalizedPath(full_path.release_nonnull())));
}

CanonicalizedPath::CanonicalizedPath()
{
}

StringView CanonicalizedPath::dirname() const
{
    if (!m_path)
        return "/"sv;
    return KLexicalPath::dirname(m_path->view());
}

StringView CanonicalizedPath::basename() const
{
    if (!m_path)
        return ""sv;
    return KLexicalPath::basename(m_path->view());
}

CanonicalizedPath::CanonicalizedPath(NonnullOwnPtr<KString> path)
    : m_path(move(path))
{
}

}
