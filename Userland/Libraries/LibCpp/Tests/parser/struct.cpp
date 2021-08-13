
struct MyStruct
{
    int x;
    struct s* next;
};

int foo()
{
    MyStruct s;
    printf("%d\n", s.x);
    return s.x;
}
