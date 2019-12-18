OBJS = \
    MDDocument.o \
    MDParagraph.o \
    MDHeading.o \
    MDCodeBlock.o \
    MDList.o \
    MDText.o

LIBRARY = libmarkdown.a

install:
	mkdir -p $(SERENITY_BASE_DIR)/Root/usr/include/LibMarkdown/
	cp *.h $(SERENITY_BASE_DIR)/Root/usr/include/LibMarkdown/
	cp $(LIBRARY) $(SERENITY_BASE_DIR)/Root/usr/lib/

include ../../Makefile.common
