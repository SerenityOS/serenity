include ../../Makefile.common

OBJS = \
    Database.o

LIBRARY = libpcidb.a
DEFINES += -DUSERLAND

all: $(LIBRARY)

$(LIBRARY): $(OBJS)
	@echo "LIB $@"; $(AR) rcs $@ $(OBJS) $(LIBS)

.cpp.o:
	@echo "CXX $<"; $(CXX) $(CXXFLAGS) -o $@ -c $<

-include $(OBJS:%.o=%.d)

clean:
	@echo "CLEAN"; rm -f $(LIBRARY) $(OBJS) *.d
