#include <AK/TestSuite.h>

#include <AK/NonnullRefPtr.h>
#include <AK/AKString.h>

struct Object : public RefCounted<Object> {
    int x;
};

TEST_CASE(basics)
{
    RefPtr<Object> object = adopt(*new Object);
    EXPECT(object.ptr() != nullptr);
    EXPECT_EQ(object->ref_count(), 1);
    object->ref();
    EXPECT_EQ(object->ref_count(), 2);
    object->deref();
    EXPECT_EQ(object->ref_count(), 1);

    {
        NonnullRefPtr another = *object;
        EXPECT_EQ(object->ref_count(), 2);
    }

    EXPECT_EQ(object->ref_count(), 1);
}

TEST_CASE(assign_reference)
{
    RefPtr<Object> object = adopt(*new Object);
    EXPECT_EQ(object->ref_count(), 1);
    object = *object;
    EXPECT_EQ(object->ref_count(), 1);
}

TEST_CASE(assign_ptr)
{
    RefPtr<Object> object = adopt(*new Object);
    EXPECT_EQ(object->ref_count(), 1);
    object = object.ptr();
    EXPECT_EQ(object->ref_count(), 1);
}

TEST_CASE(assign_moved_self)
{
    RefPtr<Object> object = adopt(*new Object);
    EXPECT_EQ(object->ref_count(), 1);
    object = move(object);
    EXPECT_EQ(object->ref_count(), 1);
}

TEST_CASE(assign_copy_self)
{
    RefPtr<Object> object = adopt(*new Object);
    EXPECT_EQ(object->ref_count(), 1);
    object = object;
    EXPECT_EQ(object->ref_count(), 1);
}

TEST_MAIN(String)
