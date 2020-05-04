OBJS = \
    DebugSession.o \
	DebugInfo.o \
	Dwarf/LineProgram.o \
	Dwarf/DwarfInfo.o \
	Dwarf/CompilationUnit.o \
	Dwarf/AbbreviationsMap.o \
	Dwarf/Expression.o \
	Dwarf/DIE.o \


LIBRARY = libdebug.a

install:
	mkdir -p $(SERENITY_BASE_DIR)/Root/usr/include/LibDebug/
	cp *.h $(SERENITY_BASE_DIR)/Root/usr/include/LibDebug/
	cp $(LIBRARY) $(SERENITY_BASE_DIR)/Root/usr/lib/

include ../../Makefile.common
