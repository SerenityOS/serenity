#include <stdio.h>
#include <sys/stat.h>
#include "other.h"

enum TestEnum {
    ValueOne,
    ValueTwo
};

struct MyStruct {
    int x { -1 };
    bool status { false };
    TestEnum test_value { ValueOne };
};

int main(int, char**)
{
    MyStruct my_struct;
    my_struct.status = !my_struct.status;
    printf("my_struct.x is %d\n", my_struct.x);
    for (int i = 0; i < 3; ++i) {
        // This is a comment :^)
        func();
        printf("Hello friends!\n");
        mkdir("/tmp/xyz", 0755);
    }
    return 0;
}
