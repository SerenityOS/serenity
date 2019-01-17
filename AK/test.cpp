#include "AKString.h"
//#include "StringBuilder.h"
#include "Vector.h"
#include <stdio.h>
#include "HashTable.h"
#include "SinglyLinkedList.h"
#include "HashMap.h"
#include "TemporaryFile.h"
#include "Buffer.h"
#include "Weakable.h"
#include "WeakPtr.h"
#include "CircularQueue.h"
#include "FileSystemPath.h"
#include "Lock.h"

static void testWeakPtr();

void log_locked() { }
void log_unlocked() { }

int main(int c, char** v)
{
    StringImpl::initialize_globals();

    {
        Lock lock;
        Locker locker(lock);
    }

    {
        const char* testpath = "/proc/../proc/1/../../proc/1/vm";
        if (c == 2)
            testpath = v[1];
        FileSystemPath p(testpath);
        if (p.string().is_null())
            printf("canonicalized path is null\n");
        else
            printf("%s\n", p.string().characters());
        return 0;
    }

    {
        struct entry {
            String s;
        };

        HashMap<unsigned, entry> tab;
        tab.set(1, { "one" });
        tab.set(2, { "two" });
        tab.set(3, { "three" });
        tab.set(4, { "four" });
        tab.remove(1);
        tab.remove(2);
        tab.remove(3);
        for (auto& it : tab) {
            printf("%s\n", it.value.s.characters());
        }
    }

    {
        CircularQueue<int, 4> queue;
        queue.dump();
        queue.enqueue(1);
        queue.dump();
        queue.enqueue(2);
        queue.dump();
        queue.enqueue(3);
        queue.dump();
        queue.enqueue(4);
        ASSERT(!queue.is_empty());
        ASSERT(queue.size() == 4);
        ASSERT(queue.dequeue() == 1);
        queue.dump();
        ASSERT(queue.dequeue() == 2);
        queue.dump();
        ASSERT(queue.dequeue() == 3);
        queue.dump();
        ASSERT(queue.dequeue() == 4);
        queue.dump();
        ASSERT(queue.is_empty());
        queue.enqueue(1);
        queue.enqueue(2);
        queue.enqueue(3);
        queue.enqueue(4);
        queue.enqueue(5);
        queue.enqueue(6);
        queue.enqueue(7);
        ASSERT(queue.dequeue() == 4);
        ASSERT(queue.dequeue() == 5);
        ASSERT(queue.dequeue() == 6);
        ASSERT(queue.dequeue() == 7);
        ASSERT(queue.is_empty());
    }

    {
        String path = "/////abc/def////g/h/i//";
        auto parts = path.split('/');
        for (auto& part : parts)
            printf("<%s>\n", part.characters());
    }
    {
        String cmd = "cd";
        auto parts = cmd.split(' ');
        for (auto& part : parts)
            printf("<%s>\n", part.characters());
    }

    String empty = "";

    char* buffer;
    auto test = StringImpl::create_uninitialized(3, buffer);
    auto hello = String("hello");
    auto Hello = String("Hello");

    printf("hello: '%s'\n", hello.characters());
    printf("Hello: '%s'\n", Hello.characters());
    printf("'Hello'.lower(): '%s'\n", Hello.to_lowercase().characters());
    printf("'hello'.upper(): '%s'\n", Hello.to_uppercase().characters());

    Vector<String> strings;
    strings.append("a");
    strings.append("b");
    strings.append("c");
    strings.append("d");
    strings.append("e");
    strings.append("f");
    strings.append("g");

    auto g = strings.takeLast();

    for (unsigned i = 0; i < strings.size(); ++i) {
        printf("[%u]: '%s'\n", i, strings[i].characters());
    }

    printf("snodde sista: '%s'\n", g.characters());
    printf("kvar:\n");

    for (auto& s : strings) {
        printf("  > %s\n", s.characters());
    }

#if 0
    StringBuilder builder;
    builder.append("HEJ");
    builder.append(' ');
    builder.append("KAJ");

    printf("byggd: '%s'\n", builder.build().characters());
#endif

#if 1
    HashTable<int> ints;
    ints.set(10);
    ints.set(20);
    ints.set(30);
    ints.dump();

    ASSERT(ints.size() == 3);
    ASSERT(ints.contains(10));
    ASSERT(ints.contains(20));
    ASSERT(ints.contains(30));
    ASSERT(!ints.contains(0));
    ASSERT(!ints.contains(40));

    HashTable<String> sss;
    sss.set("foo");
    sss.set("bar");
    sss.set("baz");
    sss.set("bee");
    ASSERT(sss.size() == 4);
    sss.dump();
    ASSERT(sss.contains("foo"));
    ASSERT(sss.contains("bar"));
    ASSERT(sss.contains("baz"));
    ASSERT(sss.contains("bee"));
    ASSERT(!sss.contains("boo"));
    ASSERT(!sss.contains(""));
    ASSERT(!sss.contains(String()));

    printf(">>> iterate Hash:\n");
    for (auto& s : sss) {
        printf("+ %s\n", s.characters());
    }
    printf("<<<\n");
#endif

    SinglyLinkedList<int> list;
    list.append(3);
    list.append(6);
    list.append(9);
    ASSERT(!list.is_empty());
    ASSERT(list.first() == 3);
    ASSERT(list.last() == 9);

    for (int i : list) {
        printf("Iterated to %d\n", i);
    }

    HashMap<String, int> map;
    map.set("lol", 100);
    map.set("kek", 500);
    map.set("zoo", 300);
    ASSERT(map.size() == 3);
    map.dump();
    for (auto& it : map) {
        printf("[%s] := %d\n", it.key.characters(), it.value);
    }

    auto found = map.find("kek");
    if (found != map.end()) {
        printf("found 'kek', key: %s, value: %d\n", (*found).key.characters(), (*found).value);
    } else {
        printf("not found\n");
    }

    auto charbuf = Buffer<char>::create_uninitialized(1024);
    printf("charbuf.size() = %zu\n", charbuf->size());

    {
    Vector<String> problem;
    for (int i = 0; i < 256; ++i)
        problem.append("test");
    }

    {
        auto printInts = [] (const Vector<int>& v) {
            printf("Vector {\n    size: %zu\n    capacity: %zu\n    elements: ", v.size(), v.capacity());
            for (auto i : v)
                printf("%d ", i);
            printf("\n}\n");
        };

        Vector<int> v;
        v.append(0);
        v.append(1);
        v.append(2);
        v.append(3);
        printInts(v);

        v.remove(1);
        printInts(v);

        v.remove(0);
        printInts(v);

        v.remove(0);
        printInts(v);

        v.remove(0);
        printInts(v);
    }

    {
        auto printInts = [] (const HashTable<int>& h) {
            printf("HashTable {\n    size: %u\n    capacity: %u\n    elements: ", h.size(), h.capacity());
            for (auto i : h)
                printf("%d ", i);
            printf("\n}\n");
        };

        HashTable<int> h;

        h.set(10);
        h.set(20);
        h.set(30);
        h.set(40);
        h.set(50);

        h.dump();

        printInts(h);

        h.remove(30);
        printInts(h);

        h.set(30);
        h.remove(30);
        printInts(h);
    }

    testWeakPtr();

    return 0;
}

class TestWeakable : public Weakable<TestWeakable> {
public:
    TestWeakable() { }
    ~TestWeakable() { }
};

void testWeakPtr()
{
    auto* weakable = new TestWeakable;

    auto weakPtr = weakable->makeWeakPtr();
    ASSERT(weakPtr);
    ASSERT(weakPtr.ptr() == weakable);

    delete weakable;

    ASSERT(!weakPtr);
    ASSERT(weakPtr.ptr() == nullptr);
}
