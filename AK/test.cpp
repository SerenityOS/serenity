#include "String.h"
//#include "StringBuilder.h"
#include "Vector.h"
#include <stdio.h>
#include "HashTable.h"
#include "SinglyLinkedList.h"
#include "HashMap.h"
#include "TemporaryFile.h"
#include "Buffer.h"

int main(int, char**)
{
    String empty = "";

    char* buffer;
    auto test = StringImpl::createUninitialized(3, buffer); 
    auto hello = String("hello");
    auto Hello = String("Hello");

    printf("hello: '%s'\n", hello.characters());
    printf("Hello: '%s'\n", Hello.characters());
    printf("'Hello'.lower(): '%s'\n", Hello.toLowercase().characters());
    printf("'hello'.upper(): '%s'\n", Hello.toUppercase().characters());

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
    ASSERT(!list.isEmpty());
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

    auto charbuf = Buffer<char>::createUninitialized(1024);
    printf("charbuf.size() = %u\n", charbuf->size());

    {
    Vector<String> problem;
    for (int i = 0; i < 256; ++i)
        problem.append("test");
    }

    return 0;
}
