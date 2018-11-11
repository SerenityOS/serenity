#include <termcap.h>
#include <assert.h>
#include <string.h>
#include <stdio.h>
#include <AK/HashMap.h>
#include <AK/String.h>

extern "C" {

char PC;
char* UP;
char* BC;

int tgetent(char* bp, const char* name)
{
    fprintf(stderr, "tgetent: bp=%p, name='%s'\n", bp, name);
    if (!strcmp(name, "ansi")) {
        PC = '\0';
        BC = const_cast<char*>("\033[D");
        UP = const_cast<char*>("\033[A");
        return 1;
    }
    assert(false);
}

static HashMap<String, const char*>* caps = nullptr;

void ensure_caps()
{
    if (caps)
        return;
    caps = new HashMap<String, const char*>;
    caps->set("DC", "\033[%p1%dP");
    caps->set("IC", "\033[%p1%d@");
    caps->set("ce", "\033[K");
    caps->set("cl", "\033[H\033[J");
    caps->set("cr", "\015");
    caps->set("dc", "\033[P");
    caps->set("ei", "");
    caps->set("ic", "");
    caps->set("im", "");
    caps->set("kd", "\033[B");
    caps->set("kl", "\033[D");
    caps->set("kr", "\033[C");
    caps->set("ku", "\033[A");
    caps->set("ks", "");
    caps->set("ke", "");
    caps->set("le", "\033[D");
    caps->set("mm", "");
    caps->set("mo", "");
    caps->set("pc", "");
    caps->set("up", "\033[A");
    caps->set("vb", "");
    caps->set("am", "");

    caps->set("co", "80");
    caps->set("li", "25");
}

char* tgetstr(char* id, char** area)
{
    ensure_caps();
    fprintf(stderr, "tgetstr: id='%s', area=%p", id, area);
    auto it = caps->find(id);
    if (it != caps->end()) {
        char* ret = *area;
        const char* val = (*it).value;
        strcpy(*area, val);
        *area += strlen(val) + 1;
        return ret;
    }
    assert(false);
}

int tgetflag(char* id)
{
    fprintf(stderr, "tgetflag: '%s'\n", id);
    auto it = caps->find(id);
    if (it != caps->end())
        return 1;
    return 0;
}

int tgetnum(char* id)
{
    fprintf(stderr, "tgetnum: '%s'\n", id);
    auto it = caps->find(id);
    if (it != caps->end()) {
        return atoi((*it).value);
    }
    assert(false);
}

char* tgoto(const char* cap, int col, int row)
{
    assert(false);
}

int tputs(const char* str, int affcnt, int (*putc)(int))
{
    printf("%s", str);
}

}

