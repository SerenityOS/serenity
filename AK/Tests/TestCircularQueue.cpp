#include <AK/TestSuite.h>
#include <AK/String.h>
#include <AK/CircularQueue.h>

TEST_CASE(basic)
{
    CircularQueue<int, 3> ints;
    EXPECT(ints.is_empty());
    ints.enqueue(1);
    ints.enqueue(2);
    ints.enqueue(3);
    EXPECT_EQ(ints.size(), 3);

    ints.enqueue(4);
    EXPECT_EQ(ints.size(), 3);
    EXPECT_EQ(ints.dequeue(), 2);
    EXPECT_EQ(ints.dequeue(), 3);
    EXPECT_EQ(ints.dequeue(), 4);
    EXPECT_EQ(ints.size(), 0);
}

TEST_CASE(complex_type)
{
    CircularQueue<String, 2> strings;

    strings.enqueue("ABC");
    strings.enqueue("DEF");

    EXPECT_EQ(strings.size(), 2);

    strings.enqueue("abc");
    strings.enqueue("def");

    EXPECT_EQ(strings.dequeue(), "abc");
    EXPECT_EQ(strings.dequeue(), "def");
}

TEST_CASE(complex_type_clear)
{
    CircularQueue<String, 5> strings;
    strings.enqueue("xxx");
    strings.enqueue("xxx");
    strings.enqueue("xxx");
    strings.enqueue("xxx");
    strings.enqueue("xxx");
    EXPECT_EQ(strings.size(), 5);
    strings.clear();
    EXPECT_EQ(strings.size(), 0);
}

TEST_MAIN(CircularQueue)
