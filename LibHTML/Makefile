include ../Makefile.common

LIBHTML_OBJS = \
    DOM/Node.o \
    DOM/ParentNode.o \
    DOM/Element.o \
    DOM/Document.o \
    DOM/Text.o \
    Parser/Parser.o \
    Layout/LayoutNode.o \
    Layout/LayoutText.o \
    Layout/LayoutBlock.o \
    Layout/LayoutInline.o \
    Layout/LayoutDocument.o \
    Layout/LayoutStyle.o \
    Frame.o \
    Dump.o

TEST_OBJS = test.o
TEST_PROGRAM = tho

OBJS = $(LIBHTML_OBJS) $(TEST_OBJS)

LIBRARY = libhtml.a
DEFINES += -DUSERLAND

all: $(LIBRARY) $(TEST_PROGRAM)

$(TEST_PROGRAM): $(TEST_OBJS) $(LIBRARY)
	$(LD) -o $@ $(LDFLAGS) -L. $(TEST_OBJS) -lhtml -lgui -lcore -lc

$(LIBRARY): $(LIBHTML_OBJS)
	@echo "LIB $@"; $(AR) rcs $@ $(LIBHTML_OBJS)

.cpp.o:
	@echo "CXX $<"; $(CXX) $(CXXFLAGS) -o $@ -c $<

-include $(OBJS:%.o=%.d)

clean:
	@echo "CLEAN"; rm -f $(TEST_PROGRAM) $(LIBRARY) $(OBJS) *.d

install: $(LIBRARY)
	mkdir -p ../Root/usr/include/LibHTML
	# Copy headers
	rsync -r -a --include '*/' --include '*.h' --exclude '*' . ../Root/usr/include/LibHTML
	# Install the library
	cp $(LIBRARY) ../Root/usr/lib

