#include <AK/TestSuite.h>

#include <AK/AKString.h>
#include <AK/Queue.h>

TEST_CASE(construct)
{
    EXPECT(Queue<int>().is_empty());
    EXPECT(Queue<int>().size() == 0);
}

TEST_CASE(populate_int)
{
    Queue<int> ints;
    ints.enqueue(1);
    ints.enqueue(2);
    ints.enqueue(3);
    EXPECT_EQ(ints.size(), 3);
    EXPECT_EQ(ints.dequeue(), 1);
    EXPECT_EQ(ints.size(), 2);
    EXPECT_EQ(ints.dequeue(), 2);
    EXPECT_EQ(ints.size(), 1);
    EXPECT_EQ(ints.dequeue(), 3);
    EXPECT_EQ(ints.size(), 0);
}

TEST_CASE(populate_string)
{
    Queue<String> strings;
    strings.enqueue("ABC");
    strings.enqueue("DEF");
    EXPECT_EQ(strings.size(), 2);
    EXPECT_EQ(strings.dequeue(), "ABC");
    EXPECT_EQ(strings.dequeue(), "DEF");
    EXPECT(strings.is_empty());
}

TEST_CASE(order)
{
    Queue<String> strings;
    EXPECT(strings.is_empty());

    for (int i = 0; i < 10000; ++i) {
        strings.enqueue(String::number(i));
        EXPECT_EQ(strings.size(), i + 1);
    }

    for (int i = 0; i < 10000; ++i) {
        bool ok;
        EXPECT_EQ(strings.dequeue().to_int(ok), i);
    }

    EXPECT(strings.is_empty());
}

TEST_MAIN(Queue)
