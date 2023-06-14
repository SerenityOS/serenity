/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Ben Wiederhake <BenWiederhake.GitHub@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/DeprecatedString.h>
#include <AK/HashMap.h>
#include <AK/OwnPtr.h>
#include <AK/String.h>

TEST_CASE(construct)
{
    using IntIntMap = HashMap<int, int>;
    EXPECT(IntIntMap().is_empty());
    EXPECT_EQ(IntIntMap().size(), 0u);
}

TEST_CASE(construct_from_initializer_list)
{
    HashMap<int, DeprecatedString> number_to_string {
        { 1, "One" },
        { 2, "Two" },
        { 3, "Three" },
    };
    EXPECT_EQ(number_to_string.is_empty(), false);
    EXPECT_EQ(number_to_string.size(), 3u);
}

TEST_CASE(populate)
{
    HashMap<int, DeprecatedString> number_to_string;
    number_to_string.set(1, "One");
    number_to_string.set(2, "Two");
    number_to_string.set(3, "Three");

    EXPECT_EQ(number_to_string.is_empty(), false);
    EXPECT_EQ(number_to_string.size(), 3u);
}

TEST_CASE(range_loop)
{
    HashMap<int, DeprecatedString> number_to_string;
    EXPECT_EQ(number_to_string.set(1, "One"), AK::HashSetResult::InsertedNewEntry);
    EXPECT_EQ(number_to_string.set(2, "Two"), AK::HashSetResult::InsertedNewEntry);
    EXPECT_EQ(number_to_string.set(3, "Three"), AK::HashSetResult::InsertedNewEntry);

    int loop_counter = 0;
    for (auto& it : number_to_string) {
        EXPECT_EQ(it.value.is_null(), false);
        ++loop_counter;
    }
    EXPECT_EQ(loop_counter, 3);
}

TEST_CASE(map_remove)
{
    HashMap<int, DeprecatedString> number_to_string;
    EXPECT_EQ(number_to_string.set(1, "One"), AK::HashSetResult::InsertedNewEntry);
    EXPECT_EQ(number_to_string.set(2, "Two"), AK::HashSetResult::InsertedNewEntry);
    EXPECT_EQ(number_to_string.set(3, "Three"), AK::HashSetResult::InsertedNewEntry);

    EXPECT_EQ(number_to_string.remove(1), true);
    EXPECT_EQ(number_to_string.size(), 2u);
    EXPECT(number_to_string.find(1) == number_to_string.end());

    EXPECT_EQ(number_to_string.remove(3), true);
    EXPECT_EQ(number_to_string.size(), 1u);
    EXPECT(number_to_string.find(3) == number_to_string.end());
    EXPECT(number_to_string.find(2) != number_to_string.end());
}

TEST_CASE(remove_all_matching)
{
    HashMap<int, DeprecatedString> map;

    map.set(1, "One");
    map.set(2, "Two");
    map.set(3, "Three");
    map.set(4, "Four");

    EXPECT_EQ(map.size(), 4u);

    EXPECT_EQ(map.remove_all_matching([&](int key, DeprecatedString const& value) { return key == 1 || value == "Two"; }), true);
    EXPECT_EQ(map.size(), 2u);

    EXPECT_EQ(map.remove_all_matching([&](int, DeprecatedString const&) { return false; }), false);
    EXPECT_EQ(map.size(), 2u);

    EXPECT(map.contains(3));
    EXPECT(map.contains(4));

    EXPECT_EQ(map.remove_all_matching([&](int, DeprecatedString const&) { return true; }), true);
    EXPECT_EQ(map.remove_all_matching([&](int, DeprecatedString const&) { return false; }), false);

    EXPECT(map.is_empty());

    EXPECT_EQ(map.remove_all_matching([&](int, DeprecatedString const&) { return true; }), false);
}

TEST_CASE(case_insensitive)
{
    HashMap<DeprecatedString, int, CaseInsensitiveStringTraits> casemap;
    EXPECT_EQ(DeprecatedString("nickserv").to_lowercase(), DeprecatedString("NickServ").to_lowercase());
    EXPECT_EQ(casemap.set("nickserv", 3), AK::HashSetResult::InsertedNewEntry);
    EXPECT_EQ(casemap.set("NickServ", 3), AK::HashSetResult::ReplacedExistingEntry);
    EXPECT_EQ(casemap.size(), 1u);
}

TEST_CASE(case_insensitive_stringview)
{
    HashMap<StringView, int, CaseInsensitiveASCIIStringViewTraits> casemap;
    EXPECT_EQ(casemap.set("nickserv"sv, 3), AK::HashSetResult::InsertedNewEntry);
    EXPECT_EQ(casemap.set("NickServ"sv, 3), AK::HashSetResult::ReplacedExistingEntry);
    EXPECT_EQ(casemap.size(), 1u);
}

TEST_CASE(hashmap_of_nonnullownptr_get)
{
    struct Object {
        Object(DeprecatedString const& s)
            : string(s)
        {
        }
        DeprecatedString string;
    };

    HashMap<int, NonnullOwnPtr<Object>> objects;
    objects.set(1, make<Object>("One"));
    objects.set(2, make<Object>("Two"));
    objects.set(3, make<Object>("Three"));

    {
        auto x = objects.get(2);
        EXPECT_EQ(x.has_value(), true);
        EXPECT_EQ(x.value()->string, "Two");
    }

    {
        // Do it again to make sure that peeking into the map above didn't
        // remove the value from the map.
        auto x = objects.get(2);
        EXPECT_EQ(x.has_value(), true);
        EXPECT_EQ(x.value()->string, "Two");
    }

    EXPECT_EQ(objects.size(), 3u);
}

TEST_CASE(many_strings)
{
    HashMap<DeprecatedString, int> strings;
    for (int i = 0; i < 999; ++i) {
        EXPECT_EQ(strings.set(DeprecatedString::number(i), i), AK::HashSetResult::InsertedNewEntry);
    }
    EXPECT_EQ(strings.size(), 999u);
    for (auto& it : strings) {
        EXPECT_EQ(it.key.to_int().value(), it.value);
    }
    for (int i = 0; i < 999; ++i) {
        EXPECT_EQ(strings.remove(DeprecatedString::number(i)), true);
    }
    EXPECT_EQ(strings.is_empty(), true);
}

TEST_CASE(basic_remove)
{
    HashMap<int, int> map;
    map.set(1, 10);
    map.set(2, 20);
    map.set(3, 30);

    EXPECT_EQ(map.remove(3), true);
    EXPECT_EQ(map.remove(3), false);
    EXPECT_EQ(map.size(), 2u);

    EXPECT_EQ(map.remove(1), true);
    EXPECT_EQ(map.remove(1), false);
    EXPECT_EQ(map.size(), 1u);

    EXPECT_EQ(map.remove(2), true);
    EXPECT_EQ(map.remove(2), false);
    EXPECT_EQ(map.size(), 0u);
}

TEST_CASE(basic_contains)
{
    HashMap<int, int> map;
    map.set(1, 10);
    map.set(2, 20);
    map.set(3, 30);

    EXPECT_EQ(map.contains(1), true);
    EXPECT_EQ(map.contains(2), true);
    EXPECT_EQ(map.contains(3), true);
    EXPECT_EQ(map.contains(4), false);

    EXPECT_EQ(map.remove(3), true);
    EXPECT_EQ(map.contains(3), false);
    EXPECT_EQ(map.contains(1), true);
    EXPECT_EQ(map.contains(2), true);

    EXPECT_EQ(map.remove(2), true);
    EXPECT_EQ(map.contains(2), false);
    EXPECT_EQ(map.contains(3), false);
    EXPECT_EQ(map.contains(1), true);

    EXPECT_EQ(map.remove(1), true);
    EXPECT_EQ(map.contains(1), false);
}

TEST_CASE(in_place_rehashing_ordered_loop_bug)
{
    OrderedHashMap<DeprecatedString, DeprecatedString> map;
    map.set("yt.innertube::nextId", "");
    map.set("yt.innertube::requests", "");
    map.remove("yt.innertube::nextId");
    map.set("yt.innertube::nextId", "");
    VERIFY(map.keys().size() == 2);
}

TEST_CASE(take)
{
    HashMap<String, int> map;

    EXPECT(!map.take("foo"sv).has_value());
    EXPECT(!map.take("bar"sv).has_value());
    EXPECT(!map.take("baz"_short_string).has_value());

    map.set("foo"_short_string, 1);
    map.set("bar"_short_string, 2);
    map.set("baz"_short_string, 3);

    auto foo = map.take("foo"sv);
    EXPECT_EQ(foo, 1);

    foo = map.take("foo"sv);
    EXPECT(!foo.has_value());

    auto bar = map.take("bar"sv);
    EXPECT_EQ(bar, 2);

    bar = map.take("bar"sv);
    EXPECT(!bar.has_value());

    auto baz = map.take("baz"_short_string);
    EXPECT_EQ(baz, 3);

    baz = map.take("baz"_short_string);
    EXPECT(!baz.has_value());
}

TEST_CASE(clone_same_template_args)
{
    HashMap<int, int> orig;
    orig.set(1, 10);
    orig.set(2, 20);
    orig.set(3, 30);
    EXPECT_EQ(orig.size(), static_cast<size_t>(3));
    EXPECT_EQ(orig.get(2), Optional<int>(20));

    auto second = TRY_OR_FAIL(orig.clone());

    EXPECT_EQ(orig.size(), static_cast<size_t>(3));
    EXPECT_EQ(orig.get(2), Optional<int>(20));
    EXPECT_EQ(second.size(), static_cast<size_t>(3));
    EXPECT_EQ(second.get(2), Optional<int>(20));
}

TEST_CASE(clone_different_traits)
{
    HashMap<StringView, StringView> orig;
    orig.set("Well"sv, "hello friends!"sv);
    orig.set("Thank"sv, "you, very cool!"sv);
    EXPECT_EQ(orig.size(), static_cast<size_t>(2));
    EXPECT_EQ(orig.get("Well"sv), Optional<StringView>("hello friends!"sv));
    EXPECT_EQ(orig.get("weLL"sv), Optional<StringView>());

    auto second = TRY_OR_FAIL(orig.clone<CaseInsensitiveASCIIStringViewTraits>());

    EXPECT_EQ(orig.size(), static_cast<size_t>(2));
    EXPECT_EQ(orig.get("Well"sv), Optional<StringView>("hello friends!"sv));
    EXPECT_EQ(orig.get("weLL"sv), Optional<StringView>());
    EXPECT_EQ(second.size(), static_cast<size_t>(2));
    EXPECT_EQ(second.get("Well"sv), Optional<StringView>("hello friends!"sv));
    EXPECT_EQ(second.get("weLL"sv), Optional<StringView>("hello friends!"sv));
}

TEST_CASE(move_construct)
{
    HashMap<int, int> orig;
    orig.set(1, 10);
    orig.set(2, 20);
    orig.set(3, 30);
    EXPECT_EQ(orig.size(), static_cast<size_t>(3));
    EXPECT_EQ(orig.get(2), Optional<int>(20));

    HashMap<int, int> second = move(orig);

    EXPECT_EQ(orig.size(), static_cast<size_t>(0));
    EXPECT_EQ(orig.get(2), Optional<int>());
    EXPECT_EQ(second.size(), static_cast<size_t>(3));
    EXPECT_EQ(second.get(2), Optional<int>(20));
}

TEST_CASE(move_assign)
{
    HashMap<int, int> orig;
    HashMap<int, int> second;
    orig.set(1, 10);
    orig.set(2, 20);
    orig.set(3, 30);

    EXPECT_EQ(orig.size(), static_cast<size_t>(3));
    EXPECT_EQ(orig.get(2), Optional<int>(20));
    EXPECT_EQ(second.size(), static_cast<size_t>(0));
    EXPECT_EQ(second.get(2), Optional<int>());

    // 'Hashtable::operator=(Hashtable&&)' allocates temporarily an empty table,
    // so we can't use NoAllocationGuard here. :(
    second = move(orig);

    EXPECT_EQ(orig.size(), static_cast<size_t>(0));
    EXPECT_EQ(orig.get(2), Optional<int>());
    EXPECT_EQ(second.size(), static_cast<size_t>(3));
    EXPECT_EQ(second.get(2), Optional<int>(20));
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

TEST_CASE(clone_copy_construct_key)
{
    using TypeUnderTest = ObservableConstructor<false, true>;
    auto old_copies = TypeUnderTest::observed_copy_constructs();
    auto old_moves = TypeUnderTest::observed_move_constructs();
    auto old_copies_a = TypeUnderTest::observed_copy_assigns();
    auto old_moves_a = TypeUnderTest::observed_move_assigns();
    auto old_clones = TypeUnderTest::observed_clones();

    HashMap<TypeUnderTest, int> map1;
    TRY_OR_FAIL(map1.try_set({ 42 }, 43));
    TRY_OR_FAIL(map1.try_set({ 1337 }, 1338));
    TRY_OR_FAIL(map1.try_set({ 123456789 }, 987654321));
    TRY_OR_FAIL(map1.try_set({ 55 }, 56));
    EXPECT_EQ(map1.size(), static_cast<size_t>(4));
    EXPECT_EQ(TypeUnderTest::observed_copy_constructs() - old_copies, 0);
    EXPECT_EQ(TypeUnderTest::observed_move_constructs() - old_moves, 8);
    EXPECT_EQ(TypeUnderTest::observed_copy_assigns() - old_copies_a, 0);
    EXPECT_EQ(TypeUnderTest::observed_move_assigns() - old_moves_a, 0);
    EXPECT_EQ(TypeUnderTest::observed_clones() - old_clones, 0);

    HashMap<TypeUnderTest, int> map2 = TRY_OR_FAIL(map1.clone());
    EXPECT_EQ(map1.size(), static_cast<size_t>(4));
    EXPECT_EQ(map2.size(), static_cast<size_t>(4));
    EXPECT_EQ(TypeUnderTest::observed_copy_constructs() - old_copies, 4);
    EXPECT_EQ(TypeUnderTest::observed_move_constructs() - old_moves, 8 + 12);
    EXPECT_EQ(TypeUnderTest::observed_copy_assigns() - old_copies_a, 0);
    EXPECT_EQ(TypeUnderTest::observed_move_assigns() - old_moves_a, 0);
    EXPECT_EQ(TypeUnderTest::observed_clones() - old_clones, 0);
    EXPECT_EQ(map1.get({ 42 }).value(), 43);
    EXPECT_EQ(map1.get({ 43 }).has_value(), false);
    EXPECT_EQ(map2.get({ 42 }).value(), 43);
    EXPECT_EQ(map2.get({ 43 }).has_value(), false);
}

TEST_CASE(clone_copy_construct_value)
{
    using TypeUnderTest = ObservableConstructor<false, true>;
    auto old_copies = TypeUnderTest::observed_copy_constructs();
    auto old_moves = TypeUnderTest::observed_move_constructs();
    auto old_copies_a = TypeUnderTest::observed_copy_assigns();
    auto old_moves_a = TypeUnderTest::observed_move_assigns();
    auto old_clones = TypeUnderTest::observed_clones();

    HashMap<int, TypeUnderTest> map1;
    TRY_OR_FAIL(map1.try_set(42, { 43 }));
    TRY_OR_FAIL(map1.try_set(1337, { 1338 }));
    TRY_OR_FAIL(map1.try_set(123456789, { 987654321 }));
    TRY_OR_FAIL(map1.try_set(55, { 56 }));
    EXPECT_EQ(map1.size(), static_cast<size_t>(4));
    EXPECT_EQ(TypeUnderTest::observed_copy_constructs() - old_copies, 0);
    EXPECT_EQ(TypeUnderTest::observed_move_constructs() - old_moves, 8);
    EXPECT_EQ(TypeUnderTest::observed_copy_assigns() - old_copies_a, 0);
    EXPECT_EQ(TypeUnderTest::observed_move_assigns() - old_moves_a, 0);
    EXPECT_EQ(TypeUnderTest::observed_clones() - old_clones, 0);

    HashMap<int, TypeUnderTest> map2 = TRY_OR_FAIL(map1.clone());
    EXPECT_EQ(map1.size(), static_cast<size_t>(4));
    EXPECT_EQ(map2.size(), static_cast<size_t>(4));
    EXPECT_EQ(TypeUnderTest::observed_copy_constructs() - old_copies, 4);
    EXPECT_EQ(TypeUnderTest::observed_move_constructs() - old_moves, 8 + 12);
    EXPECT_EQ(TypeUnderTest::observed_copy_assigns() - old_copies_a, 0);
    EXPECT_EQ(TypeUnderTest::observed_move_assigns() - old_moves_a, 0);
    EXPECT_EQ(TypeUnderTest::observed_clones() - old_clones, 0);
    EXPECT_EQ(map1.get(42).value().value(), 43);
    EXPECT_EQ(map1.get(43).has_value(), false);
    EXPECT_EQ(map2.get(42).value().value(), 43);
    EXPECT_EQ(map2.get(43).has_value(), false);
}

TEST_CASE(clone_call_key)
{
    using TypeUnderTest = ObservableConstructor<true, false>;
    auto old_copies = TypeUnderTest::observed_copy_constructs();
    auto old_moves = TypeUnderTest::observed_move_constructs();
    auto old_copies_a = TypeUnderTest::observed_copy_assigns();
    auto old_moves_a = TypeUnderTest::observed_move_assigns();
    auto old_clones = TypeUnderTest::observed_clones();

    HashMap<TypeUnderTest, int> map1;
    TRY_OR_FAIL(map1.try_set({ 42 }, 43));
    TRY_OR_FAIL(map1.try_set({ 1337 }, 1338));
    TRY_OR_FAIL(map1.try_set({ 123456789 }, 987654321));
    TRY_OR_FAIL(map1.try_set({ 55 }, 56));
    EXPECT_EQ(map1.size(), static_cast<size_t>(4));
    EXPECT_EQ(TypeUnderTest::observed_copy_constructs() - old_copies, 0);
    EXPECT_EQ(TypeUnderTest::observed_move_constructs() - old_moves, 8);
    EXPECT_EQ(TypeUnderTest::observed_copy_assigns() - old_copies_a, 0);
    EXPECT_EQ(TypeUnderTest::observed_move_assigns() - old_moves_a, 0);
    EXPECT_EQ(TypeUnderTest::observed_clones() - old_clones, 0);

    HashMap<TypeUnderTest, int> map2 = TRY_OR_FAIL(map1.clone());
    EXPECT_EQ(map1.size(), static_cast<size_t>(4));
    EXPECT_EQ(map2.size(), static_cast<size_t>(4));
    EXPECT_EQ(TypeUnderTest::observed_copy_constructs() - old_copies, 0);
    // We have way too many moves.
    EXPECT_EQ(TypeUnderTest::observed_move_constructs() - old_moves, 8 + 20);
    EXPECT_EQ(TypeUnderTest::observed_copy_assigns() - old_copies_a, 0);
    EXPECT_EQ(TypeUnderTest::observed_move_assigns() - old_moves_a, 0);
    EXPECT_EQ(TypeUnderTest::observed_clones() - old_clones, 4);
    EXPECT_EQ(map1.get({ 42 }).value(), 43);
    EXPECT_EQ(map1.get({ 43 }).has_value(), false);
    EXPECT_EQ(map2.get({ 42 }).value(), 43);
    EXPECT_EQ(map2.get({ 43 }).has_value(), false);
}

TEST_CASE(clone_call_value)
{
    using TypeUnderTest = ObservableConstructor<true, false>;
    auto old_copies = TypeUnderTest::observed_copy_constructs();
    auto old_moves = TypeUnderTest::observed_move_constructs();
    auto old_copies_a = TypeUnderTest::observed_copy_assigns();
    auto old_moves_a = TypeUnderTest::observed_move_assigns();
    auto old_clones = TypeUnderTest::observed_clones();

    HashMap<int, TypeUnderTest> map1;
    TRY_OR_FAIL(map1.try_set(42, { 43 }));
    TRY_OR_FAIL(map1.try_set(1337, { 1338 }));
    TRY_OR_FAIL(map1.try_set(123456789, { 987654321 }));
    TRY_OR_FAIL(map1.try_set(55, { 56 }));
    EXPECT_EQ(map1.size(), static_cast<size_t>(4));
    EXPECT_EQ(TypeUnderTest::observed_copy_constructs() - old_copies, 0);
    EXPECT_EQ(TypeUnderTest::observed_move_constructs() - old_moves, 8);
    EXPECT_EQ(TypeUnderTest::observed_copy_assigns() - old_copies_a, 0);
    EXPECT_EQ(TypeUnderTest::observed_move_assigns() - old_moves_a, 0);
    EXPECT_EQ(TypeUnderTest::observed_clones() - old_clones, 0);

    HashMap<int, TypeUnderTest> map2 = TRY_OR_FAIL(map1.clone());
    EXPECT_EQ(map1.size(), static_cast<size_t>(4));
    EXPECT_EQ(map2.size(), static_cast<size_t>(4));
    EXPECT_EQ(TypeUnderTest::observed_copy_constructs() - old_copies, 0);
    // We have way too many moves.
    EXPECT_EQ(TypeUnderTest::observed_move_constructs() - old_moves, 8 + 20);
    EXPECT_EQ(TypeUnderTest::observed_copy_assigns() - old_copies_a, 0);
    EXPECT_EQ(TypeUnderTest::observed_move_assigns() - old_moves_a, 0);
    EXPECT_EQ(TypeUnderTest::observed_clones() - old_clones, 4);
    EXPECT_EQ(map1.get(42).value().value(), 43);
    EXPECT_EQ(map1.get(43).has_value(), false);
    EXPECT_EQ(map2.get(42).value().value(), 43);
    EXPECT_EQ(map2.get(43).has_value(), false);
}

static_assert(HasFallibleClone<Vector<ObservableConstructor<true>>>);
static_assert(HasFallibleClone<Vector<ObservableConstructor<false>>>);

TEST_CASE(clone_fallible_recursive)
{
    using TypeUnderTest = ObservableConstructor<true, false>;

    auto old_copies = TypeUnderTest::observed_copy_constructs();
    auto old_moves = TypeUnderTest::observed_move_constructs();
    auto old_copies_a = TypeUnderTest::observed_copy_assigns();
    auto old_moves_a = TypeUnderTest::observed_move_assigns();
    auto old_clones = TypeUnderTest::observed_clones();

    HashMap<int, Vector<TypeUnderTest>> map1;
    TRY_OR_FAIL(map1.try_set(42, TRY_OR_FAIL((Vector<int> { 41, 42, 43 }.clone<TypeUnderTest>()))));
    TRY_OR_FAIL(map1.try_set(1337, TRY_OR_FAIL((Vector<int> { 1336, 1337, 1338 }.clone<TypeUnderTest>()))));
    TRY_OR_FAIL(map1.try_set(123456789, TRY_OR_FAIL((Vector<int> { 987654321, 999 }.clone<TypeUnderTest>()))));
    EXPECT_EQ(TypeUnderTest::observed_copy_constructs() - old_copies, 0);
    EXPECT_EQ(TypeUnderTest::observed_move_constructs() - old_moves, 0);
    EXPECT_EQ(TypeUnderTest::observed_copy_assigns() - old_copies_a, 0);
    EXPECT_EQ(TypeUnderTest::observed_move_assigns() - old_moves_a, 0);
    EXPECT_EQ(TypeUnderTest::observed_clones() - old_clones, 0);
    EXPECT_EQ(map1.size(), static_cast<size_t>(3));
    EXPECT_EQ(map1.get(1337).value()[2].value(), 1338);

    auto map2 = TRY_OR_FAIL(map1.clone());
    static_assert(IsSame<decltype(map2), HashMap<int, Vector<TypeUnderTest>>>);
    EXPECT_EQ(TypeUnderTest::observed_copy_constructs() - old_copies, 0);
    EXPECT_EQ(TypeUnderTest::observed_move_constructs() - old_moves, 16);
    EXPECT_EQ(TypeUnderTest::observed_copy_assigns() - old_copies_a, 0);
    EXPECT_EQ(TypeUnderTest::observed_move_assigns() - old_moves_a, 0);
    EXPECT_EQ(TypeUnderTest::observed_clones() - old_clones, 8);
    EXPECT_EQ(map1.size(), static_cast<size_t>(3));
    EXPECT_EQ(map1.get(1337).value()[2].value(), 1338);
    EXPECT_EQ(map2.size(), static_cast<size_t>(3));
    EXPECT_EQ(map2.get(1337).value()[2].value(), 1338);
}
