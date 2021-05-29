/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/RefCounted.h>

namespace AK {

RefCountedBase::~RefCountedBase()
{
    VERIFY(m_ref_count.load(AK::MemoryOrder::memory_order_relaxed) == 0);
}

void RefCountedBase::ref() const
{
    auto old_ref_count = m_ref_count.fetch_add(1, AK::MemoryOrder::memory_order_relaxed);
    VERIFY(old_ref_count > 0);
    VERIFY(!Checked<RefCountType>::addition_would_overflow(old_ref_count, 1));
}

[[nodiscard]] bool RefCountedBase::try_ref() const
{
    RefCountType expected = m_ref_count.load(AK::MemoryOrder::memory_order_relaxed);
    for (;;) {
        if (expected == 0)
            return false;
        VERIFY(!Checked<RefCountType>::addition_would_overflow(expected, 1));
        if (m_ref_count.compare_exchange_strong(expected, expected + 1, AK::MemoryOrder::memory_order_acquire))
            return true;
    }
}

RefCountedBase::RefCountType RefCountedBase::ref_count() const
{
    return m_ref_count.load(AK::MemoryOrder::memory_order_relaxed);
}

RefCountedBase::RefCountType RefCountedBase::deref_base() const
{
    auto old_ref_count = m_ref_count.fetch_sub(1, AK::MemoryOrder::memory_order_acq_rel);
    VERIFY(old_ref_count > 0);
    return old_ref_count - 1;
}

}
