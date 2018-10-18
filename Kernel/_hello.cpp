extern "C" int puts(const char*);

extern "C" int elf_entry()
{
    puts("Home, where you are supposed to be...");
    return 0;
}
