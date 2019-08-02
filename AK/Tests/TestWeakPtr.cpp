#include <AK/TestSuite.h>
#include <AK/AKString.h>
#include <AK/Weakable.h>
#include <AK/WeakPtr.h>

class SimpleWeakable : public Weakable<SimpleWeakable> {
public:
    SimpleWeakable() {}

private:
    int m_member { 123 };
};

TEST_CASE(basic_weak)
{
    WeakPtr<SimpleWeakable> weak1;
    WeakPtr<SimpleWeakable> weak2;

    {
        SimpleWeakable simple;
        weak1 = simple.make_weak_ptr();
        weak2 = simple.make_weak_ptr();
        EXPECT_EQ(weak1.is_null(), false);
        EXPECT_EQ(weak2.is_null(), false);
        EXPECT_EQ(weak1.ptr(), &simple);
        EXPECT_EQ(weak1.ptr(), weak2.ptr());
    }

    EXPECT_EQ(weak1.is_null(), true);
    EXPECT_EQ(weak1.ptr(), nullptr);
    EXPECT_EQ(weak1.ptr(), weak2.ptr());
}

TEST_CASE(weakptr_move)
{
    WeakPtr<SimpleWeakable> weak1;
    WeakPtr<SimpleWeakable> weak2;

    {
        SimpleWeakable simple;
        weak1 = simple.make_weak_ptr();
        weak2 = move(weak1);
        EXPECT_EQ(weak1.is_null(), true);
        EXPECT_EQ(weak2.is_null(), false);
        EXPECT_EQ(weak2.ptr(), &simple);
    }

    EXPECT_EQ(weak2.is_null(), true);

    fprintf(stderr, "ok\n");
}

TEST_MAIN(WeakPtr)
