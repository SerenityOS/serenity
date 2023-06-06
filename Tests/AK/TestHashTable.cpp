/*
 * Copyright (c) 2021, thislooksfun <tlf@thislooks.fun>
 * Copyright (c) 2023, Jelle Raaijmakers <jelle@gmta.nl>
 * Copyright (c) 2023, Ben Wiederhake <BenWiederhake.GitHub@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/DeprecatedString.h>
#include <AK/HashTable.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/StdLibExtras.h>
#include <AK/Vector.h>

TEST_CASE(construct)
{
    using IntTable = HashTable<int>;
    EXPECT(IntTable().is_empty());
    EXPECT_EQ(IntTable().size(), 0u);
}

TEST_CASE(basic_move)
{
    HashTable<int> foo;
    foo.set(1);
    EXPECT_EQ(foo.size(), 1u);
    auto bar = move(foo);
    EXPECT_EQ(bar.size(), 1u);
    EXPECT_EQ(foo.size(), 0u);
    foo = move(bar);
    EXPECT_EQ(bar.size(), 0u);
    EXPECT_EQ(foo.size(), 1u);
}

TEST_CASE(move_is_not_swap)
{
    HashTable<int> foo;
    foo.set(1);
    HashTable<int> bar;
    bar.set(2);
    foo = move(bar);
    EXPECT(foo.contains(2));
    EXPECT(!bar.contains(1));
    EXPECT_EQ(bar.size(), 0u);
}

TEST_CASE(populate)
{
    HashTable<DeprecatedString> strings;
    strings.set("One");
    strings.set("Two");
    strings.set("Three");

    EXPECT_EQ(strings.is_empty(), false);
    EXPECT_EQ(strings.size(), 3u);
}

TEST_CASE(range_loop)
{
    HashTable<DeprecatedString> strings;
    EXPECT_EQ(strings.set("One"), AK::HashSetResult::InsertedNewEntry);
    EXPECT_EQ(strings.set("Two"), AK::HashSetResult::InsertedNewEntry);
    EXPECT_EQ(strings.set("Three"), AK::HashSetResult::InsertedNewEntry);

    int loop_counter = 0;
    for (auto& it : strings) {
        EXPECT_EQ(it.is_null(), false);
        ++loop_counter;
    }
    EXPECT_EQ(loop_counter, 3);
}

TEST_CASE(table_remove)
{
    HashTable<DeprecatedString> strings;
    EXPECT_EQ(strings.set("One"), AK::HashSetResult::InsertedNewEntry);
    EXPECT_EQ(strings.set("Two"), AK::HashSetResult::InsertedNewEntry);
    EXPECT_EQ(strings.set("Three"), AK::HashSetResult::InsertedNewEntry);

    EXPECT_EQ(strings.remove("One"), true);
    EXPECT_EQ(strings.size(), 2u);
    EXPECT(strings.find("One") == strings.end());

    EXPECT_EQ(strings.remove("Three"), true);
    EXPECT_EQ(strings.size(), 1u);
    EXPECT(strings.find("Three") == strings.end());
    EXPECT(strings.find("Two") != strings.end());
}

TEST_CASE(remove_all_matching)
{
    HashTable<int> ints;

    ints.set(1);
    ints.set(2);
    ints.set(3);
    ints.set(4);

    EXPECT_EQ(ints.size(), 4u);

    EXPECT_EQ(ints.remove_all_matching([&](int value) { return value > 2; }), true);
    EXPECT_EQ(ints.remove_all_matching([&](int) { return false; }), false);

    EXPECT_EQ(ints.size(), 2u);

    EXPECT(ints.contains(1));
    EXPECT(ints.contains(2));

    EXPECT_EQ(ints.remove_all_matching([&](int) { return true; }), true);

    EXPECT(ints.is_empty());

    EXPECT_EQ(ints.remove_all_matching([&](int) { return true; }), false);
}

TEST_CASE(case_insensitive)
{
    HashTable<DeprecatedString, CaseInsensitiveStringTraits> casetable;
    EXPECT_EQ(DeprecatedString("nickserv").to_lowercase(), DeprecatedString("NickServ").to_lowercase());
    EXPECT_EQ(casetable.set("nickserv"), AK::HashSetResult::InsertedNewEntry);
    EXPECT_EQ(casetable.set("NickServ"), AK::HashSetResult::ReplacedExistingEntry);
    EXPECT_EQ(casetable.size(), 1u);
}

TEST_CASE(many_strings)
{
    HashTable<DeprecatedString> strings;
    for (int i = 0; i < 999; ++i) {
        EXPECT_EQ(strings.set(DeprecatedString::number(i)), AK::HashSetResult::InsertedNewEntry);
    }
    EXPECT_EQ(strings.size(), 999u);
    for (int i = 0; i < 999; ++i) {
        EXPECT_EQ(strings.remove(DeprecatedString::number(i)), true);
    }
    EXPECT_EQ(strings.is_empty(), true);
}

TEST_CASE(many_collisions)
{
    struct StringCollisionTraits : public GenericTraits<DeprecatedString> {
        static unsigned hash(DeprecatedString const&) { return 0; }
    };

    HashTable<DeprecatedString, StringCollisionTraits> strings;
    for (int i = 0; i < 999; ++i) {
        EXPECT_EQ(strings.set(DeprecatedString::number(i)), AK::HashSetResult::InsertedNewEntry);
    }

    EXPECT_EQ(strings.set("foo"), AK::HashSetResult::InsertedNewEntry);
    EXPECT_EQ(strings.size(), 1000u);

    for (int i = 0; i < 999; ++i) {
        EXPECT_EQ(strings.remove(DeprecatedString::number(i)), true);
    }

    EXPECT(strings.find("foo") != strings.end());
}

TEST_CASE(space_reuse)
{
    struct StringCollisionTraits : public GenericTraits<DeprecatedString> {
        static unsigned hash(DeprecatedString const&) { return 0; }
    };

    HashTable<DeprecatedString, StringCollisionTraits> strings;

    // Add a few items to allow it to do initial resizing.
    EXPECT_EQ(strings.set("0"), AK::HashSetResult::InsertedNewEntry);
    for (int i = 1; i < 5; ++i) {
        EXPECT_EQ(strings.set(DeprecatedString::number(i)), AK::HashSetResult::InsertedNewEntry);
        EXPECT_EQ(strings.remove(DeprecatedString::number(i - 1)), true);
    }

    auto capacity = strings.capacity();

    for (int i = 5; i < 999; ++i) {
        EXPECT_EQ(strings.set(DeprecatedString::number(i)), AK::HashSetResult::InsertedNewEntry);
        EXPECT_EQ(strings.remove(DeprecatedString::number(i - 1)), true);
    }

    EXPECT_EQ(strings.capacity(), capacity);
}

TEST_CASE(basic_remove)
{
    HashTable<int> table;
    table.set(1);
    table.set(2);
    table.set(3);

    EXPECT_EQ(table.remove(3), true);
    EXPECT_EQ(table.remove(3), false);
    EXPECT_EQ(table.size(), 2u);

    EXPECT_EQ(table.remove(1), true);
    EXPECT_EQ(table.remove(1), false);
    EXPECT_EQ(table.size(), 1u);

    EXPECT_EQ(table.remove(2), true);
    EXPECT_EQ(table.remove(2), false);
    EXPECT_EQ(table.size(), 0u);
}

TEST_CASE(basic_contains)
{
    HashTable<int> table;
    table.set(1);
    table.set(2);
    table.set(3);

    EXPECT_EQ(table.contains(1), true);
    EXPECT_EQ(table.contains(2), true);
    EXPECT_EQ(table.contains(3), true);
    EXPECT_EQ(table.contains(4), false);

    EXPECT_EQ(table.remove(3), true);
    EXPECT_EQ(table.contains(3), false);
    EXPECT_EQ(table.contains(1), true);
    EXPECT_EQ(table.contains(2), true);

    EXPECT_EQ(table.remove(2), true);
    EXPECT_EQ(table.contains(2), false);
    EXPECT_EQ(table.contains(3), false);
    EXPECT_EQ(table.contains(1), true);

    EXPECT_EQ(table.remove(1), true);
    EXPECT_EQ(table.contains(1), false);
}

TEST_CASE(capacity_leak)
{
    HashTable<int> table;
    for (size_t i = 0; i < 10000; ++i) {
        table.set(i);
        table.remove(i);
    }
    EXPECT(table.capacity() < 100u);
}

TEST_CASE(non_trivial_type_table)
{
    HashTable<NonnullOwnPtr<int>> table;

    table.set(make<int>(3));
    table.set(make<int>(11));

    for (int i = 0; i < 1'000; ++i) {
        table.set(make<int>(-i));
    }
    for (int i = 0; i < 10'000; ++i) {
        table.set(make<int>(i));
        table.remove(make<int>(i));
    }

    EXPECT_EQ(table.remove_all_matching([&](auto&) { return true; }), true);
    EXPECT(table.is_empty());
    EXPECT_EQ(table.remove_all_matching([&](auto&) { return true; }), false);
}

TEST_CASE(floats)
{
    HashTable<float> table;
    table.set(0);
    table.set(1.0f);
    table.set(2.0f);
    EXPECT_EQ(table.size(), 3u);
    EXPECT(table.contains(0));
    EXPECT(table.contains(1.0f));
    EXPECT(table.contains(2.0f));
}

TEST_CASE(doubles)
{
    HashTable<double> table;
    table.set(0);
    table.set(1.0);
    table.set(2.0);
    EXPECT_EQ(table.size(), 3u);
    EXPECT(table.contains(0));
    EXPECT(table.contains(1.0));
    EXPECT(table.contains(2.0));
}

TEST_CASE(reinsertion)
{
    OrderedHashTable<DeprecatedString> map;
    map.set("ytidb::LAST_RESULT_ENTRY_KEY");
    map.set("__sak");
    map.remove("__sak");
    map.set("__sak");
}

TEST_CASE(clear_with_capacity_when_empty)
{
    HashTable<int> map;
    map.clear_with_capacity();
    map.set(0);
    map.set(1);
    VERIFY(map.size() == 2);
}

TEST_CASE(iterator_removal)
{
    HashTable<int> map;
    map.set(0);
    map.set(1);

    auto it = map.begin();
    map.remove(it);
    EXPECT_EQ(it, map.end());
    EXPECT_EQ(map.size(), 1u);
}

TEST_CASE(ordered_insertion_and_deletion)
{
    OrderedHashTable<int> table;
    EXPECT_EQ(table.set(0), HashSetResult::InsertedNewEntry);
    EXPECT_EQ(table.set(1), HashSetResult::InsertedNewEntry);
    EXPECT_EQ(table.set(2), HashSetResult::InsertedNewEntry);
    EXPECT_EQ(table.set(3), HashSetResult::InsertedNewEntry);
    EXPECT_EQ(table.size(), 4u);

    auto expect_table = [](OrderedHashTable<int>& table, Span<int> values) {
        auto index = 0u;
        for (auto it = table.begin(); it != table.end(); ++it, ++index) {
            EXPECT_EQ(*it, values[index]);
            EXPECT(table.contains(values[index]));
        }
    };

    expect_table(table, Array<int, 4> { 0, 1, 2, 3 });

    EXPECT(table.remove(0));
    EXPECT(table.remove(2));
    EXPECT(!table.remove(4));
    EXPECT_EQ(table.size(), 2u);

    expect_table(table, Array<int, 2> { 1, 3 });
}

TEST_CASE(ordered_deletion_and_reinsertion)
{
    OrderedHashTable<int> table;
    table.set(1);
    table.set(3);
    table.remove(1);
    EXPECT_EQ(table.size(), 1u);

    // By adding 1 again but this time in a different position, we
    // test whether the bucket's neighbors are reset properly.
    table.set(1);
    EXPECT_EQ(table.size(), 2u);

    auto it = table.begin();
    EXPECT_EQ(*it, 3);
    ++it;
    EXPECT_EQ(*it, 1);
    ++it;
    EXPECT_EQ(it, table.end());
}

TEST_CASE(ordered_take_last)
{
    OrderedHashTable<int> table;
    table.set(1);
    table.set(2);
    table.set(3);

    EXPECT_EQ(table.take_last(), 3);
    EXPECT_EQ(table.take_last(), 2);
    EXPECT_EQ(table.take_last(), 1);
    EXPECT(table.is_empty());
}

TEST_CASE(ordered_iterator_removal)
{
    OrderedHashTable<int> map;
    map.set(0);
    map.set(1);

    auto it = map.begin();
    map.remove(it);
    EXPECT_EQ(it, map.end());
    EXPECT_EQ(map.size(), 1u);
}

TEST_CASE(ordered_remove_from_head)
{
    OrderedHashTable<int> map;
    map.set(1);
    map.set(2);
    map.set(3);
    map.set(4);
    map.set(5);
    map.set(6);

    EXPECT_EQ(map.size(), 6u);

    auto it = map.begin();
    map.remove(it);
    it = map.begin();
    map.remove(it);
    it = map.begin();
    map.remove(it);
    it = map.begin();
    map.remove(it);
    it = map.begin();
    map.remove(it);
    it = map.begin();
    map.remove(it);

    EXPECT_EQ(map.size(), 0u);
}

TEST_CASE(ordered_infinite_loop_clang_regression)
{
    OrderedHashTable<DeprecatedString> map;
    map.set("");
    map.set("1");
    map.set("_cb");
    map.set("2");
    map.set("3");
    map.set("_cb_svref");
    map.set("_cb_svref_expires");
    map.remove("_cb_svref");
    map.remove("_cb_svref_expires");
    map.set("_cb_svref");

    size_t iterations = 0;
    auto size = map.size();
    for (auto it = map.begin(); it != map.end(); ++it) {
        if (++iterations > size) {
            VERIFY(false);
            break;
        }
    }
}

TEST_CASE(values)
{
    OrderedHashTable<int> table;
    table.set(10);
    table.set(30);
    table.set(20);

    Vector<int> values = table.values();

    EXPECT_EQ(values.size(), table.size());
    EXPECT_EQ(values[0], 10);
    EXPECT_EQ(values[1], 30);
    EXPECT_EQ(values[2], 20);
}

TEST_CASE(clone_pod)
{
    HashTable<int> table1;
    TRY_OR_FAIL(table1.try_set(42));
    TRY_OR_FAIL(table1.try_set(1337));
    TRY_OR_FAIL(table1.try_set(123456789));
    EXPECT_EQ(table1.size(), static_cast<size_t>(3));
    EXPECT_EQ(table1.contains(42), true);
    EXPECT_EQ(table1.contains(43), false);

    HashTable<int> table2 = TRY_OR_FAIL(table1.clone());
    EXPECT_EQ(table1.size(), static_cast<size_t>(3));
    EXPECT_EQ(table2.size(), static_cast<size_t>(3));
    EXPECT_EQ(table1.contains(42), true);
    EXPECT_EQ(table1.contains(43), false);
    EXPECT_EQ(table2.contains(42), true);
    EXPECT_EQ(table2.contains(43), false);
}

struct WeirdIntTraits {
    using PeekType = int&;
    using ConstPeekType = int const&;
    static constexpr bool is_trivial() { return false; }
    static constexpr bool is_trivially_serializable() { return false; }
    static constexpr bool equals(int const& a, int const& b) { return a == b; }
    template<AK::Concepts::HashCompatible<int> U>
    static bool equals(U const& a, int const& b) { return a == b; }
    static constexpr unsigned hash(int value)
    {
        return value + 42;
    }
};

TEST_CASE(clone_pod_nonstandard_template_args)
{
    HashTable<int, Traits<int>, true> table1;
    TRY_OR_FAIL(table1.try_set(42));
    TRY_OR_FAIL(table1.try_set(1337));
    TRY_OR_FAIL(table1.try_set(123456789));
    EXPECT_EQ(table1.size(), static_cast<size_t>(3));
    EXPECT_EQ(table1.contains(42), true);
    EXPECT_EQ(table1.contains(43), false);

    auto table2 = TRY_OR_FAIL(
        (table1.clone<int, WeirdIntTraits, false>()));
    static_assert(IsSame<decltype(table2), HashTable<int, WeirdIntTraits, false>>);
    EXPECT_EQ(table1.size(), static_cast<size_t>(3));
    EXPECT_EQ(table2.size(), static_cast<size_t>(3));
    EXPECT_EQ(table1.contains(42), true);
    EXPECT_EQ(table1.contains(43), false);
    EXPECT_EQ(table2.contains(42), true);
    EXPECT_EQ(table2.contains(43), false);
}

template<bool CanClone, bool CanCopyConstruct = true>
class ObservableConstructor {
public:
    ObservableConstructor(int value)
        : m_value(value)
    {
    }
    ObservableConstructor(ObservableConstructor const& other)
    requires(CanCopyConstruct)
        : m_value(other.m_value)
    {
        s_observed_copy_constructs += 1;
    }
    ObservableConstructor(ObservableConstructor&& other)
        : m_value(other.m_value)
    {
        s_observed_move_constructs += 1;
    }

    ObservableConstructor& operator=(ObservableConstructor const& other)
    requires(CanCopyConstruct)
    {
        m_value = other.m_value;
        s_observed_copy_assigns += 1;
        return *this;
    }
    ObservableConstructor& operator=(ObservableConstructor&& other)
    {
        m_value = other.m_value;
        s_observed_move_assigns += 1;
        return *this;
    }

    int value() const { return m_value; }

    ErrorOr<ObservableConstructor> clone() const
    requires(CanClone)
    {
        s_observed_clones += 1;
        return { ObservableConstructor { m_value } };
    }

    bool operator==(ObservableConstructor const& other) const
    {
        return m_value == other.m_value;
    }

    static int observed_copy_constructs() { return s_observed_copy_constructs; }
    static int observed_move_constructs() { return s_observed_move_constructs; }
    static int observed_copy_assigns() { return s_observed_copy_assigns; }
    static int observed_move_assigns() { return s_observed_move_assigns; }
    static int observed_clones() { return s_observed_clones; }

private:
    int m_value;

    static int s_observed_copy_constructs;
    static int s_observed_move_constructs;
    static int s_observed_copy_assigns;
    static int s_observed_move_assigns;
    static int s_observed_clones;
};

template<bool CanClone, bool CanCopyConstruct>
int ObservableConstructor<CanClone, CanCopyConstruct>::s_observed_copy_constructs = 0;
template<bool CanClone, bool CanCopyConstruct>
int ObservableConstructor<CanClone, CanCopyConstruct>::s_observed_move_constructs = 0;
template<bool CanClone, bool CanCopyConstruct>
int ObservableConstructor<CanClone, CanCopyConstruct>::s_observed_copy_assigns = 0;
template<bool CanClone, bool CanCopyConstruct>
int ObservableConstructor<CanClone, CanCopyConstruct>::s_observed_move_assigns = 0;
template<bool CanClone, bool CanCopyConstruct>
int ObservableConstructor<CanClone, CanCopyConstruct>::s_observed_clones = 0;

static_assert(HasFallibleClone<ObservableConstructor<true, true>>);
static_assert(!HasFallibleClone<ObservableConstructor<false, true>>);
static_assert(HasFallibleClone<ObservableConstructor<true, false>>);
static_assert(!HasFallibleClone<ObservableConstructor<false, false>>);

template<bool CanClone, bool CanCopyConstruct>
struct Traits<ObservableConstructor<CanClone, CanCopyConstruct>> : public GenericTraits<ObservableConstructor<CanClone, CanCopyConstruct>> {
    static constexpr unsigned hash(ObservableConstructor<CanClone, CanCopyConstruct> const& value)
    {
        return value.value() + 1;
    }
};

TEST_CASE(clone_copy_construct)
{
    using TypeUnderTest = ObservableConstructor<false, true>;
    auto old_copies = TypeUnderTest::observed_copy_constructs();
    auto old_moves = TypeUnderTest::observed_move_constructs();
    auto old_copies_a = TypeUnderTest::observed_copy_assigns();
    auto old_moves_a = TypeUnderTest::observed_move_assigns();
    auto old_clones = TypeUnderTest::observed_clones();

    HashTable<TypeUnderTest> table1;
    TRY_OR_FAIL(table1.try_set({ 42 }));
    TRY_OR_FAIL(table1.try_set({ 1337 }));
    TRY_OR_FAIL(table1.try_set({ 123456789 }));
    EXPECT_EQ(table1.size(), static_cast<size_t>(3));
    EXPECT_EQ(TypeUnderTest::observed_copy_constructs() - old_copies, 0);
    EXPECT_EQ(TypeUnderTest::observed_move_constructs() - old_moves, 3);
    EXPECT_EQ(TypeUnderTest::observed_copy_assigns() - old_copies_a, 0);
    EXPECT_EQ(TypeUnderTest::observed_move_assigns() - old_moves_a, 0);
    EXPECT_EQ(TypeUnderTest::observed_clones() - old_clones, 0);

    HashTable<TypeUnderTest> table2 = TRY_OR_FAIL(table1.clone());
    EXPECT_EQ(table1.size(), static_cast<size_t>(3));
    EXPECT_EQ(table2.size(), static_cast<size_t>(3));
    EXPECT_EQ(TypeUnderTest::observed_copy_constructs() - old_copies, 3);
    EXPECT_EQ(TypeUnderTest::observed_move_constructs() - old_moves, 6);
    EXPECT_EQ(TypeUnderTest::observed_copy_assigns() - old_copies_a, 0);
    EXPECT_EQ(TypeUnderTest::observed_move_assigns() - old_moves_a, 0);
    EXPECT_EQ(TypeUnderTest::observed_clones() - old_clones, 0);
    EXPECT_EQ(table1.contains({ 42 }), true);
    EXPECT_EQ(table1.contains({ 43 }), false);
    EXPECT_EQ(table2.contains({ 42 }), true);
    EXPECT_EQ(table2.contains({ 43 }), false);
}

TEST_CASE(clone_call)
{
    using TypeUnderTest = ObservableConstructor<true, false>;
    auto old_copies = TypeUnderTest::observed_copy_constructs();
    auto old_moves = TypeUnderTest::observed_move_constructs();
    auto old_copies_a = TypeUnderTest::observed_copy_assigns();
    auto old_moves_a = TypeUnderTest::observed_move_assigns();
    auto old_clones = TypeUnderTest::observed_clones();

    HashTable<TypeUnderTest> table1;
    TRY_OR_FAIL(table1.try_set({ 42 }));
    TRY_OR_FAIL(table1.try_set({ 1337 }));
    TRY_OR_FAIL(table1.try_set({ 123456789 }));
    EXPECT_EQ(table1.size(), static_cast<size_t>(3));
    EXPECT_EQ(TypeUnderTest::observed_copy_constructs() - old_copies, 0);
    EXPECT_EQ(TypeUnderTest::observed_move_constructs() - old_moves, 3);
    EXPECT_EQ(TypeUnderTest::observed_copy_assigns() - old_copies_a, 0);
    EXPECT_EQ(TypeUnderTest::observed_move_assigns() - old_moves_a, 0);
    EXPECT_EQ(TypeUnderTest::observed_clones() - old_clones, 0);

    HashTable<TypeUnderTest> table2 = TRY_OR_FAIL(table1.clone());
    EXPECT_EQ(table1.size(), static_cast<size_t>(3));
    EXPECT_EQ(table2.size(), static_cast<size_t>(3));
    EXPECT_EQ(TypeUnderTest::observed_copy_constructs() - old_copies, 0);
    // We should cut down the number of moves, but for now it suffices that we don't copy-construct or copy-assign.
    EXPECT_EQ(TypeUnderTest::observed_move_constructs() - old_moves, 12);
    EXPECT_EQ(TypeUnderTest::observed_copy_assigns() - old_copies_a, 0);
    EXPECT_EQ(TypeUnderTest::observed_move_assigns() - old_moves_a, 0);
    EXPECT_EQ(TypeUnderTest::observed_clones() - old_clones, 3);
    EXPECT_EQ(table1.contains({ 42 }), true);
    EXPECT_EQ(table1.contains({ 43 }), false);
    EXPECT_EQ(table2.contains({ 42 }), true);
    EXPECT_EQ(table2.contains({ 43 }), false);
}

TEST_CASE(clone_lambda)
{
    using TypeUnderTest = ObservableConstructor<true, false>;
    auto old_copies = TypeUnderTest::observed_copy_constructs();
    auto old_moves = TypeUnderTest::observed_move_constructs();
    auto old_copies_a = TypeUnderTest::observed_copy_assigns();
    auto old_moves_a = TypeUnderTest::observed_move_assigns();
    auto old_clones = TypeUnderTest::observed_clones();

    HashTable<TypeUnderTest> table1;
    TRY_OR_FAIL(table1.try_set({ 42 }));
    TRY_OR_FAIL(table1.try_set({ 1337 }));
    TRY_OR_FAIL(table1.try_set({ 123456789 }));
    EXPECT_EQ(table1.size(), static_cast<size_t>(3));
    EXPECT_EQ(TypeUnderTest::observed_copy_constructs() - old_copies, 0);
    EXPECT_EQ(TypeUnderTest::observed_move_constructs() - old_moves, 3);
    EXPECT_EQ(TypeUnderTest::observed_copy_assigns() - old_copies_a, 0);
    EXPECT_EQ(TypeUnderTest::observed_move_assigns() - old_moves_a, 0);
    EXPECT_EQ(TypeUnderTest::observed_clones() - old_clones, 0);

    HashTable<TypeUnderTest> table2 = TRY_OR_FAIL(table1.clone(
        [](TypeUnderTest const& source) -> ErrorOr<TypeUnderTest> {
            return TypeUnderTest(source.value() + 1);
        }));
    EXPECT_EQ(table1.size(), static_cast<size_t>(3));
    EXPECT_EQ(table2.size(), static_cast<size_t>(3));
    EXPECT_EQ(TypeUnderTest::observed_copy_constructs() - old_copies, 0);
    // We should cut down the number of moves, but for now it suffices that we don't copy-construct or copy-assign.
    EXPECT_EQ(TypeUnderTest::observed_move_constructs() - old_moves, 12);
    EXPECT_EQ(TypeUnderTest::observed_copy_assigns() - old_copies_a, 0);
    EXPECT_EQ(TypeUnderTest::observed_move_assigns() - old_moves_a, 0);
    EXPECT_EQ(TypeUnderTest::observed_clones() - old_clones, 0);
    EXPECT_EQ(table1.contains({ 42 }), true);
    EXPECT_EQ(table1.contains({ 43 }), false);
    EXPECT_EQ(table2.contains({ 42 }), false);
    EXPECT_EQ(table2.contains({ 43 }), true);
}

// clone() of recursive HashTables can't be easily tested, because the containers in AK don't implement hash().
// This functionality will be tested through HashMap instead.
