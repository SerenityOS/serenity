/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/RefPtr.h>
#include <AK/StringView.h>
#include <Kernel/FileSystem/Custody.h>

namespace Kernel {

// NOTE: UnresolvedPath is essentially a nice wrapper for dirfd and path string, to pass
// around functions be able to generate a custody when needed.
// Its main strength is to allow deferring of evaluation of a Custody only later on, when
// it could costly to do so.
class UnresolvedPath {
public:
    UnresolvedPath(int dirfd, StringView path)
        : m_path(path)
        , m_dirfd(dirfd)
    {
    }

    // NOTE: These constructors are for special cases - like symlinks
    // which have parents, and if a symlink points to a relative path
    // then we need to ensure we using the parent when resolving, or
    // otherwise use the absolute provided path.
    UnresolvedPath(NonnullRefPtr<Custody> parent, StringView path)
        : m_path(path)
        , m_parent(parent)
    {
    }

    UnresolvedPath(Custody const& parent, StringView path)
        : m_path(path)
        , m_parent(parent)
    {
    }

    UnresolvedPath(Custody& parent, StringView path)
        : m_path(path)
        , m_parent(parent)
    {
    }

    ErrorOr<NonnullRefPtr<Custody>> resolve() const;

    StringView raw() const { return m_path; }

    RefPtr<Custody> parent() const { return m_parent; }

private:
    StringView m_path;
    RefPtr<Custody> const m_parent;
    int m_dirfd { -1 };
};

}
