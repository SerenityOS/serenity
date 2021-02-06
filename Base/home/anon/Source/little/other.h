int func();

#define USE_VAR2

struct StructInHeader {
    int var1;
#ifdef USE_VAR2
    int var2;
#else
    int var3;
#endif
};
