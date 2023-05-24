/*
 * Copyright (c) 2023, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ScopeGuard.h>
#include <AK/StringBuilder.h>
#include <LibCore/System.h>
#include <LibSQL/Heap.h>
#include <LibTest/TestCase.h>

static constexpr auto db_path = "/tmp/test.db"sv;

static NonnullRefPtr<SQL::Heap> create_heap()
{
    auto heap = MUST(SQL::Heap::try_create(db_path));
    MUST(heap->open());
    return heap;
}

TEST_CASE(heap_write_large_storage_without_flush)
{
    ScopeGuard guard([]() { MUST(Core::System::unlink(db_path)); });
    auto heap = create_heap();
    auto storage_block_id = heap->request_new_block_index();

    // Write large storage spanning multiple blocks
    StringBuilder builder;
    MUST(builder.try_append_repeated('x', SQL::Block::DATA_SIZE * 4));
    auto long_string = builder.string_view();
    TRY_OR_FAIL(heap->write_storage(storage_block_id, long_string.bytes()));

    // Read back
    auto stored_long_string = TRY_OR_FAIL(heap->read_storage(storage_block_id));
    EXPECT_EQ(long_string.bytes(), stored_long_string.bytes());
}

TEST_CASE(heap_write_large_storage_with_flush)
{
    ScopeGuard guard([]() { MUST(Core::System::unlink(db_path)); });
    auto heap = create_heap();
    auto storage_block_id = heap->request_new_block_index();

    // Write large storage spanning multiple blocks
    StringBuilder builder;
    MUST(builder.try_append_repeated('x', SQL::Block::DATA_SIZE * 4));
    auto long_string = builder.string_view();
    TRY_OR_FAIL(heap->write_storage(storage_block_id, long_string.bytes()));
    MUST(heap->flush());

    // Read back
    auto stored_long_string = TRY_OR_FAIL(heap->read_storage(storage_block_id));
    EXPECT_EQ(long_string.bytes(), stored_long_string.bytes());
}
