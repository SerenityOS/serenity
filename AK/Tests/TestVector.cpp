#include <AK/TestSuite.h>

#include <AK/AKString.h>
#include <AK/OwnPtr.h>
#include <AK/Vector.h>

TEST_CASE(construct)
{
    EXPECT(Vector<int>().is_empty());
    EXPECT(Vector<int>().size() == 0);
}

TEST_CASE(ints)
{
    Vector<int> ints;
    ints.append(1);
    ints.append(2);
    ints.append(3);
    EXPECT_EQ(ints.size(), 3);
    EXPECT_EQ(ints.take_last(), 3);
    EXPECT_EQ(ints.size(), 2);
    EXPECT_EQ(ints.take_last(), 2);
    EXPECT_EQ(ints.size(), 1);
    EXPECT_EQ(ints.take_last(), 1);
    EXPECT_EQ(ints.size(), 0);

    ints.clear();
    EXPECT_EQ(ints.size(), 0);
}

TEST_CASE(strings)
{
    Vector<String> strings;
    strings.append("ABC");
    strings.append("DEF");

    int loop_counter = 0;
    for (const String& string : strings) {
        EXPECT(!string.is_null());
        EXPECT(!string.is_empty());
        ++loop_counter;
    }

    loop_counter = 0;
    for (auto& string : (const_cast<const Vector<String>&>(strings))) {
        EXPECT(!string.is_null());
        EXPECT(!string.is_empty());
        ++loop_counter;
    }
    EXPECT_EQ(loop_counter, 2);
}

TEST_CASE(strings_insert_ordered)
{
    Vector<String> strings;
    strings.append("abc");
    strings.append("def");
    strings.append("ghi");

    strings.insert_before_matching("f-g", [](auto& entry) {
        return "f-g" < entry;
    });

    EXPECT_EQ(strings[0], "abc");
    EXPECT_EQ(strings[1], "def");
    EXPECT_EQ(strings[2], "f-g");
    EXPECT_EQ(strings[3], "ghi");
}

TEST_CASE(prepend_vector)
{
    Vector<int> ints;
    ints.append(1);
    ints.append(2);
    ints.append(3);

    Vector<int> more_ints;
    more_ints.append(4);
    more_ints.append(5);
    more_ints.append(6);

    ints.prepend(move(more_ints));

    EXPECT_EQ(ints.size(), 6);
    EXPECT_EQ(more_ints.size(), 0);

    EXPECT_EQ(ints[0], 4);
    EXPECT_EQ(ints[1], 5);
    EXPECT_EQ(ints[2], 6);
    EXPECT_EQ(ints[3], 1);
    EXPECT_EQ(ints[4], 2);
    EXPECT_EQ(ints[5], 3);

    ints.prepend(move(more_ints));
    EXPECT_EQ(ints.size(), 6);
    EXPECT_EQ(more_ints.size(), 0);

    more_ints.prepend(move(ints));
    EXPECT_EQ(more_ints.size(), 6);
    EXPECT_EQ(ints.size(), 0);
}

TEST_CASE(prepend_vector_object)
{
    struct SubObject {
        SubObject(int v)
            : value(v)
        {
        }
        int value { 0 };
    };
    struct Object {
        Object(NonnullOwnPtr<SubObject>&& a_subobject)
            : subobject(move(a_subobject))
        {
        }
        OwnPtr<SubObject> subobject;
    };

    Vector<Object> objects;
    objects.empend(make<SubObject>(1));
    objects.empend(make<SubObject>(2));
    objects.empend(make<SubObject>(3));

    EXPECT_EQ(objects.size(), 3);

    Vector<Object> more_objects;
    more_objects.empend(make<SubObject>(4));
    more_objects.empend(make<SubObject>(5));
    more_objects.empend(make<SubObject>(6));
    EXPECT_EQ(more_objects.size(), 3);

    objects.prepend(move(more_objects));
    EXPECT_EQ(more_objects.size(), 0);
    EXPECT_EQ(objects.size(), 6);

    EXPECT_EQ(objects[0].subobject->value, 4);
    EXPECT_EQ(objects[1].subobject->value, 5);
    EXPECT_EQ(objects[2].subobject->value, 6);
    EXPECT_EQ(objects[3].subobject->value, 1);
    EXPECT_EQ(objects[4].subobject->value, 2);
    EXPECT_EQ(objects[5].subobject->value, 3);
}

TEST_MAIN(Vector)
