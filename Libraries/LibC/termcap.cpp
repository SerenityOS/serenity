#include <AK/AKString.h>
#include <AK/HashMap.h>
#include <assert.h>
#include <stdio.h>
#include <string.h>
#include <termcap.h>

//#define TERMCAP_DEBUG

extern "C" {

char PC;
char* UP;
char* BC;

int tgetent(char* bp, const char* name)
{
    (void)bp;
    (void)name;
#ifdef TERMCAP_DEBUG
    fprintf(stderr, "tgetent: bp=%p, name='%s'\n", bp, name);
#endif
    PC = '\0';
    BC = const_cast<char*>("\033[D");
    UP = const_cast<char*>("\033[A");
    return 1;
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
    caps->set("@7", "");
    caps->set("kH", "");
    caps->set("kI", "\033[L");
    caps->set("kh", "\033[H");
    caps->set("vs", "");
    caps->set("ve", "");
    caps->set("E3", "");
    caps->set("kD", "");
    caps->set("nd", "\033[C");

    caps->set("co", "80");
    caps->set("li", "25");
}

char* tgetstr(const char* id, char** area)
{
    ensure_caps();
#ifdef TERMCAP_DEBUG
    fprintf(stderr, "tgetstr: id='%s'\n", id);
#endif
    auto it = caps->find(id);
    if (it != caps->end()) {
        char* ret = *area;
        const char* val = (*it).value;
        strcpy(*area, val);
        *area += strlen(val) + 1;
        return ret;
    }
    fprintf(stderr, "tgetstr: missing cap id='%s'\n", id);
    return nullptr;
}

int tgetflag(const char* id)
{
    (void)id;
#ifdef TERMCAP_DEBUG
    fprintf(stderr, "tgetflag: '%s'\n", id);
#endif
    auto it = caps->find(id);
    if (it != caps->end())
        return 1;
    return 0;
}

int tgetnum(const char* id)
{
#ifdef TERMCAP_DEBUG
    fprintf(stderr, "tgetnum: '%s'\n", id);
#endif
    auto it = caps->find(id);
    if (it != caps->end())
        return atoi((*it).value);
    ASSERT_NOT_REACHED();
}

char* tgoto(const char* cap, int col, int row)
{
    (void)cap;
    (void)col;
    (void)row;
    ASSERT_NOT_REACHED();
}

int tputs(const char* str, int affcnt, int (*putc)(int))
{
    (void)affcnt;
    size_t len = strlen(str);
    for (size_t i = 0; i < len; ++i)
        putc(str[i]);
    return 0;
}
}
