#include <stdio.h>
#include <iostream>
#include <iomanip>
using namespace std;
void wcout(char *wd, int lines, int words, int bytes, string name)
{
        while (*wd) switch(*wd++) {
                case 'l':
                        printf("%7i", lines);
                        break;
                case 'w':
                        printf("%7i", words);
                        break;
                case 'c':
                        printf("%8i", bytes);
                        break;
        }
        cout << setw(18) << name << endl;
}

int main(int argc, char **argv)
{
        int character;
        int linec=0, wordc=0, bytec=0, i = 1;
        int tlinec=0, twordc=0, tbytec=0;
        unsigned int spaceflag=0, lineflag=0, tabflag=0;
        char wd[] = "lwc";
        FILE *filep = stdin;

        if (argc > 1 && *argv[1]=='-') {
                strcpy(wd,++argv[1]);
                argc--;
                argv++;
        }

        do {
                lineflag=1;
                if (argc > 1 && (filep = fopen(argv[i], "r"))==NULL) {
                        fprintf(stderr, "wc: unable to open %s\n", argv[i]);
                        continue;
                }
                while ((character = fgetc(filep)) != EOF) {
                        bytec++;
                        if (character>='A' && character<='z' && (spaceflag==1||lineflag==1||tabflag==1)) {
                                wordc++;
                                spaceflag=0;
                                lineflag=0;
                                tabflag=0;
                        }
                        switch(character) {
                        case '\n': linec++;
                                lineflag=1;
                                break;
                        case ' ': spaceflag=1;
                                break;
                        case '\t': tabflag=1;
                                break;                                                                                                                                   
                        }
                } fclose(filep);

                if (argc > 2) {
                        tlinec += linec;
                        twordc += wordc;
                        tbytec += bytec;
                }
                if (argc > 1)
                        wcout(wd, linec, wordc, bytec, argv[i]);
                else
                        wcout(wd, linec, wordc, bytec, "");

                linec = 0;
                wordc = 0;
                bytec = 0;
        } while (++i<argc);
        if (argc > 2)
                wcout(wd, tlinec, twordc, tbytec, "total");                                                                                                              
}
