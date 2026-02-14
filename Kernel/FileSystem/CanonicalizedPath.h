/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <AK/StringView.h>
#include <Kernel/FileSystem/Custody.h>

namespace Kernel {

// NOTE: A CanonicalizedPath is simply a path that we know that is absolute
// and containers no .. or . nor excessive slashes.

class CanonicalizedPath : public AtomicRefCounted<CanonicalizedPath> {
public:
    // FIXME: We might be able to create proper copy and move constructors to eliminate the
    // need for creating this with NonnullOwnPtr
    static ErrorOr<NonnullRefPtr<CanonicalizedPath>> create(Custody const& base, StringView raw_path);

    static CanonicalizedPath& fake_root_path();

    ErrorOr<NonnullRefPtr<CanonicalizedPath>> clone() const;

    StringView full_path() const
    {
        // NOTE: An empty path (or simply when m_path is nullptr) signals for being root path
        // It should be noted that a symlink can give an empty string, so m_path is still not a null pointer.
        if (!m_path)
            return "/"sv;
        return m_path->view();
    }

    StringView basename() const;
    StringView dirname() const;

    bool is_root() const { return m_path.ptr() == nullptr; }

private:
    explicit CanonicalizedPath(NonnullOwnPtr<KString> path);
    CanonicalizedPath();

    OwnPtr<KString> const m_path;
};

}
