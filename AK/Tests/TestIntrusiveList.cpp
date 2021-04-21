/*
 * Copyright (c) 2021, Brian Gianforcaro <b.gianfo@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/IntrusiveList.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/RefPtr.h>
#include <AK/TestSuite.h>

class IntrusiveTestItem {
public:
    IntrusiveTestItem() = default;
    IntrusiveListNode<IntrusiveTestItem> m_list_node;
};
using IntrusiveTestList = IntrusiveList<IntrusiveTestItem, RawPtr<IntrusiveTestItem>, &IntrusiveTestItem::m_list_node>;

TEST_CASE(construct)
{
    IntrusiveTestList empty;
    EXPECT(empty.is_empty());
}

TEST_CASE(insert)
{
    IntrusiveTestList list;
    list.append(*new IntrusiveTestItem());

    EXPECT(!list.is_empty());

    delete list.take_last();
}

TEST_CASE(enumeration)
{
    constexpr size_t expected_size = 10;
    IntrusiveTestList list;
    for (size_t i = 0; i < expected_size; i++) {
        list.append(*new IntrusiveTestItem());
    }

    size_t actual_size = 0;
    for (auto& elem : list) {
        (void)elem;
        actual_size++;
    }
    EXPECT_EQ(expected_size, actual_size);
    while (auto elem = list.take_first()) {
        delete elem;
    }
}

class IntrusiveRefPtrItem : public RefCounted<IntrusiveRefPtrItem> {
public:
    IntrusiveRefPtrItem() = default;
    IntrusiveListNode<IntrusiveRefPtrItem, RefPtr<IntrusiveRefPtrItem>> m_list_node;
};
using IntrusiveRefPtrList = IntrusiveList<IntrusiveRefPtrItem, RefPtr<IntrusiveRefPtrItem>, &IntrusiveRefPtrItem::m_list_node>;

TEST_CASE(intrusive_ref_ptr_no_ref_leaks)
{
    auto item = adopt(*new IntrusiveRefPtrItem());
    EXPECT_EQ(1u, item->ref_count());
    IntrusiveRefPtrList ref_list;

    ref_list.append(*item);
    EXPECT_EQ(2u, item->ref_count());

    ref_list.remove(*item);
    EXPECT_EQ(1u, item->ref_count());
}

TEST_CASE(intrusive_ref_ptr_clear)
{
    auto item = adopt(*new IntrusiveRefPtrItem());
    EXPECT_EQ(1u, item->ref_count());
    IntrusiveRefPtrList ref_list;

    ref_list.append(*item);
    EXPECT_EQ(2u, item->ref_count());

    ref_list.clear();
    EXPECT_EQ(1u, item->ref_count());
}

TEST_CASE(intrusive_ref_ptr_destructor)
{
    auto item = adopt(*new IntrusiveRefPtrItem());
    EXPECT_EQ(1u, item->ref_count());

    {
        IntrusiveRefPtrList ref_list;
        ref_list.append(*item);
        EXPECT_EQ(2u, item->ref_count());
    }

    EXPECT_EQ(1u, item->ref_count());
}

class IntrusiveNonnullRefPtrItem : public RefCounted<IntrusiveNonnullRefPtrItem> {
public:
    IntrusiveNonnullRefPtrItem() = default;
    IntrusiveListNode<IntrusiveNonnullRefPtrItem, NonnullRefPtr<IntrusiveNonnullRefPtrItem>> m_list_node;
};
using IntrusiveNonnullRefPtrList = IntrusiveList<IntrusiveNonnullRefPtrItem, NonnullRefPtr<IntrusiveNonnullRefPtrItem>, &IntrusiveNonnullRefPtrItem::m_list_node>;

TEST_CASE(intrusive_nonnull_ref_ptr_intrusive)
{
    auto item = adopt(*new IntrusiveNonnullRefPtrItem());
    EXPECT_EQ(1u, item->ref_count());
    IntrusiveNonnullRefPtrList nonnull_ref_list;

    nonnull_ref_list.append(*item);
    EXPECT_EQ(2u, item->ref_count());
    EXPECT(!nonnull_ref_list.is_empty());

    nonnull_ref_list.remove(*item);
    EXPECT_EQ(1u, item->ref_count());

    EXPECT(nonnull_ref_list.is_empty());
}

TEST_MAIN(IntrusiveList)
