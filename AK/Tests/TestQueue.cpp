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
    EXPECT(ints.size() == 3);
    EXPECT(ints.dequeue() == 1);
    EXPECT(ints.size() == 2);
    EXPECT(ints.dequeue() == 2);
    EXPECT(ints.size() == 1);
    EXPECT(ints.dequeue() == 3);
    EXPECT(ints.size() == 0);

    Queue<String> strings;
    strings.enqueue("ABC");
    strings.enqueue("DEF");
    EXPECT(strings.size() == 2);
    EXPECT(strings.dequeue() == "ABC");
    EXPECT(strings.dequeue() == "DEF");
    EXPECT(strings.is_empty());

    return 0;
}
