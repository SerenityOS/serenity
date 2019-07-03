#include "TestHelpers.h"
#include <AK/AKString.h>
#include <AK/Queue.h>

int main()
{
    EXPECT(Queue<int>().is_empty());
    EXPECT(Queue<int>().size() == 0);

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

    Queue<String> strings;
    strings.enqueue("ABC");
    strings.enqueue("DEF");
    EXPECT_EQ(strings.size(), 2);
    EXPECT_EQ(strings.dequeue(), "ABC");
    EXPECT_EQ(strings.dequeue(), "DEF");
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

    return 0;
}
