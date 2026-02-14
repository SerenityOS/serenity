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

// NOTE: A OptimizedPath is simply a path that we know that is absolute
// and containers no .. or . nor excessive slashes.

class OptimizedPath : public AtomicRefCounted<OptimizedPath> {
public:
    // FIXME: We might be able to create proper copy and move constructors to eliminate the
    // need for creating this with NonnullOwnPtr
    static ErrorOr<NonnullRefPtr<OptimizedPath>> create(Custody const& base, StringView raw_path);

    static OptimizedPath& fake_root_path();

    ErrorOr<NonnullRefPtr<OptimizedPath>> clone() const;

    StringView full_canonicalized_path() const;
    StringView basename() const;
    StringView dirname() const;

    bool is_root() const { return m_path.ptr() == nullptr; }

    enum class SpecialBasename {
        None,
        DoubleDot,
        Dot,
    };
    SpecialBasename special_basename() const { return m_special_basename; }

private:
    OptimizedPath(NonnullOwnPtr<KString> path, SpecialBasename);
    OptimizedPath();

    OwnPtr<KString> const m_path;
    SpecialBasename m_special_basename { SpecialBasename::None };
};

}
