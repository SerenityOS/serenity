#include <AK/TestSuite.h>

#include <AK/NonnullRefPtr.h>
#include <AK/AKString.h>

struct Object : public RefCounted<Object> {
    int x;
};

TEST_CASE(basics)
{
    auto object = adopt(*new Object);
    EXPECT(object.ptr() != nullptr);
    EXPECT_EQ(object->ref_count(), 1);
    object->ref();
    EXPECT_EQ(object->ref_count(), 2);
    object->deref();
    EXPECT_EQ(object->ref_count(), 1);

    {
        NonnullRefPtr another = object;
        EXPECT_EQ(object->ref_count(), 2);
    }

    EXPECT_EQ(object->ref_count(), 1);
}

TEST_CASE(assign_reference)
{
    auto object = adopt(*new Object);
    EXPECT_EQ(object->ref_count(), 1);
    object = *object;
    EXPECT_EQ(object->ref_count(), 1);
}

TEST_MAIN(String)
