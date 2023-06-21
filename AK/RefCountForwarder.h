/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace AK {

template<typename T>
class RefCountForwarder {
public:
    void ref() { m_ref_count_target.ref(); }
    void unref() { m_ref_count_target.unref(); }

    T& ref_count_target() { return m_ref_count_target; }
    T const& ref_count_target() const { return m_ref_count_target; }

protected:
    RefCountForwarder(T& target)
        : m_ref_count_target(target)
    {
    }

private:
    T& m_ref_count_target;
};

}

#if USING_AK_GLOBALLY
using AK::RefCountForwarder;
#endif
