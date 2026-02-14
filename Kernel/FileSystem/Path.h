/*
 * Copyright (c) 2026, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/Mount.h>
#include <Kernel/FileSystem/OptimizedPath.h>
#include <Kernel/Library/KString.h>

namespace Kernel {

class Path {
public:
    Path(Custody const& custody, Mount const& mount, OptimizedPath const& optimized_path)
        : m_custody(custody)
        , m_mount(mount)
        , m_optimized_path(optimized_path)
    {
    }

    Path(Custody& custody, Mount& mount, OptimizedPath& optimized_path)
        : m_custody(custody)
        , m_mount(mount)
        , m_optimized_path(optimized_path)
    {
    }

    Path(NonnullRefPtr<Custody> custody, NonnullRefPtr<Mount> mount, NonnullRefPtr<OptimizedPath> optimized_path)
        : m_custody(move(custody))
        , m_mount(move(mount))
        , m_optimized_path(move(optimized_path))
    {
    }

    Path& operator=(Path& other)
    {
        m_custody = other.custody();
        m_mount = other.mount();
        m_optimized_path = other.optimized_path();
        return *this;
    }

    Path& operator=(Path const& other)
    {
        m_custody = other.custody();
        m_mount = other.mount();
        m_optimized_path = other.optimized_path();
        return *this;
    }

    // Define copy constructors so we can simply copy the properties of
    // the Path to another one.
    Path(Path const& other_path)
        : m_custody(other_path.custody())
        , m_mount(other_path.mount())
        , m_optimized_path(other_path.optimized_path())
    {
    }

    Path(Path& other_path)
        : m_custody(other_path.custody())
        , m_mount(other_path.mount())
        , m_optimized_path(other_path.optimized_path())
    {
    }

    Custody& custody() { return *m_custody; }
    Custody const& custody() const { return *m_custody; }

    Mount& mount() { return *m_mount; }
    Mount const& mount() const { return *m_mount; }

    OptimizedPath& optimized_path() { return *m_optimized_path; }
    OptimizedPath const& optimized_path() const { return *m_optimized_path; }

private:
    NonnullRefPtr<Custody> m_custody;
    NonnullRefPtr<Mount> m_mount;
    NonnullRefPtr<OptimizedPath> m_optimized_path;
};

}
