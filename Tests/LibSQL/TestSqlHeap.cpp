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

TEST_CASE(heap_overwrite_large_storage)
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
    auto heap_size = MUST(heap->file_size_in_bytes());

    // Let's write it again and check whether the Heap reused the same extended blocks
    TRY_OR_FAIL(heap->write_storage(storage_block_id, long_string.bytes()));
    MUST(heap->flush());
    auto new_heap_size = MUST(heap->file_size_in_bytes());
    EXPECT_EQ(heap_size, new_heap_size);

    // Write a smaller string and read back - heap size should be at most the previous size
    builder.clear();
    MUST(builder.try_append_repeated('y', SQL::Block::DATA_SIZE * 2));
    auto shorter_string = builder.string_view();
    TRY_OR_FAIL(heap->write_storage(storage_block_id, shorter_string.bytes()));
    MUST(heap->flush());
    new_heap_size = MUST(heap->file_size_in_bytes());
    EXPECT(new_heap_size <= heap_size);
    auto stored_shorter_string = TRY_OR_FAIL(heap->read_storage(storage_block_id));
    EXPECT_EQ(shorter_string.bytes(), stored_shorter_string.bytes());

    // Write a longer string and read back - heap size is expected to grow
    builder.clear();
    MUST(builder.try_append_repeated('z', SQL::Block::DATA_SIZE * 6));
    auto longest_string = builder.string_view();
    TRY_OR_FAIL(heap->write_storage(storage_block_id, longest_string.bytes()));
    MUST(heap->flush());
    new_heap_size = MUST(heap->file_size_in_bytes());
    EXPECT(new_heap_size > heap_size);
    auto stored_longest_string = TRY_OR_FAIL(heap->read_storage(storage_block_id));
    EXPECT_EQ(longest_string.bytes(), stored_longest_string.bytes());
}
