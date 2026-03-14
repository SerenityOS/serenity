/*
 * Copyright (c) 2026, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <Kernel/FileSystem/CanonicalizedPath.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/Mount.h>
#include <Kernel/Library/KString.h>

namespace Kernel {

class Path {
public:
    Path(Custody const& custody, Mount const& mount, CanonicalizedPath const& canonicalized_path)
        : m_custody(custody)
        , m_mount(mount)
        , m_canonicalized_path(canonicalized_path)
    {
    }

    Path(Custody& custody, Mount& mount, CanonicalizedPath& canonicalized_path)
        : m_custody(custody)
        , m_mount(mount)
        , m_canonicalized_path(canonicalized_path)
    {
    }

    Path(NonnullRefPtr<Custody> custody, NonnullRefPtr<Mount> mount, NonnullRefPtr<CanonicalizedPath> canonicalized_path)
        : m_custody(move(custody))
        , m_mount(move(mount))
        , m_canonicalized_path(move(canonicalized_path))
    {
    }

    Path& operator=(Path& other)
    {
        m_custody = other.custody();
        m_mount = other.mount();
        m_canonicalized_path = other.canonicalized_path();
        return *this;
    }

    Path& operator=(Path const& other)
    {
        m_custody = other.custody();
        m_mount = other.mount();
        m_canonicalized_path = other.canonicalized_path();
        return *this;
    }

    // Define copy constructors so we can simply copy the properties of
    // the Path to another one.
    Path(Path const& other_path)
        : m_custody(other_path.custody())
        , m_mount(other_path.mount())
        , m_canonicalized_path(other_path.canonicalized_path())
    {
    }

    Path(Path& other_path)
        : m_custody(other_path.custody())
        , m_mount(other_path.mount())
        , m_canonicalized_path(other_path.canonicalized_path())
    {
    }

    Custody& custody() { return *m_custody; }
    Custody const& custody() const { return *m_custody; }

    Mount& mount() { return *m_mount; }
    Mount const& mount() const { return *m_mount; }

    CanonicalizedPath& canonicalized_path() { return *m_canonicalized_path; }
    CanonicalizedPath const& canonicalized_path() const { return *m_canonicalized_path; }

private:
    NonnullRefPtr<Custody> m_custody;
    NonnullRefPtr<Mount> m_mount;
    NonnullRefPtr<CanonicalizedPath> m_canonicalized_path;
};

}
