OBJS = \
    JsonSchemaNode.o \
    Parser.o \
    Validator.o

LIBRARY = libjsonvalidator.a

install:
	mkdir -p $(SERENITY_BASE_DIR)/Root/usr/include/LibJsonValidator/
	cp *.h $(SERENITY_BASE_DIR)/Root/usr/include/LibJsonValidator/
	cp $(LIBRARY) $(SERENITY_BASE_DIR)/Root/usr/lib/

include ../../Makefile.common
