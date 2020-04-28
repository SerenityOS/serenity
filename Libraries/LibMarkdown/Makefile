OBJS = \
    Document.o \
    Paragraph.o \
    Heading.o \
    CodeBlock.o \
    List.o \
    Text.o

LIBRARY = libmarkdown.a

install:
	mkdir -p $(SERENITY_BASE_DIR)/Root/usr/include/LibMarkdown/
	cp *.h $(SERENITY_BASE_DIR)/Root/usr/include/LibMarkdown/
	cp $(LIBRARY) $(SERENITY_BASE_DIR)/Root/usr/lib/

include ../../Makefile.common
