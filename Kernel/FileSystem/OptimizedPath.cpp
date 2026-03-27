/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/FileSystem/OptimizedPath.h>
#include <Kernel/Library/KLexicalPath.h>
#include <Kernel/Library/PathCanonicalization.h>

namespace Kernel {

static RawPtr<OptimizedPath> s_root_path;

ErrorOr<NonnullRefPtr<OptimizedPath>> OptimizedPath::clone() const
{
    auto cloned_full_path = TRY(KString::try_create(full_canonicalized_path()));
    return TRY(adopt_nonnull_ref_or_enomem(new (nothrow) OptimizedPath(move(cloned_full_path), special_basename())));
}

OptimizedPath& OptimizedPath::fake_root_path()
{
    // NOTE: The fake root path is used only during tear down of VFSRootContexts
    // or in Kernel processes.
    if (!s_root_path)
        s_root_path = &MUST(adopt_nonnull_ref_or_enomem(new (nothrow) OptimizedPath())).leak_ref();
    return *s_root_path;
}

ErrorOr<NonnullRefPtr<OptimizedPath>> OptimizedPath::create(Custody const& base, StringView raw_path)
{
    SpecialBasename special_basename { SpecialBasename::None };
    auto basename = KLexicalPath::basename(raw_path);
    if (basename == ".."sv)
        special_basename = SpecialBasename::DoubleDot;
    if (basename == "."sv)
        special_basename = SpecialBasename::Dot;

    auto custody_absolute_base_path = TRY(base.try_serialize_absolute_path());
    OwnPtr<KString> full_path;
    if (is_absolute_path(raw_path))
        full_path = TRY(KString::try_create(raw_path));
    else
        full_path = TRY(KLexicalPath::try_join_non_canonical_second(custody_absolute_base_path->view(), raw_path));

    canonicalize_absolute_path(*full_path);
    return TRY(adopt_nonnull_ref_or_enomem(new (nothrow) OptimizedPath(full_path.release_nonnull(), special_basename)));
}

OptimizedPath::OptimizedPath()
{
}

StringView OptimizedPath::full_canonicalized_path() const
{
    // NOTE: An empty path (or simply when m_path is nullptr) signals for being root path
    // It should be noted that a symlink can give an empty string, so m_path is still not a null pointer.
    if (!m_path)
        return "/"sv;
    return m_path->view();
}

StringView OptimizedPath::dirname() const
{
    if (!m_path)
        return "/"sv;
    if (m_special_basename == SpecialBasename::Dot)
        return m_path->view();

    // If we have SpecialBasename::DoubleDot, then this call should still be valid
    return KLexicalPath::dirname(m_path->view());
}

StringView OptimizedPath::basename() const
{
    if (!m_path)
        return ""sv;

    if (m_special_basename == SpecialBasename::DoubleDot)
        return ".."sv;
    if (m_special_basename == SpecialBasename::Dot)
        return "."sv;

    return KLexicalPath::basename(m_path->view());
}

OptimizedPath::OptimizedPath(NonnullOwnPtr<KString> path, SpecialBasename special_basename)
    : m_path(move(path))
    , m_special_basename(special_basename)
{
}

}
