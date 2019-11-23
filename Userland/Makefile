include ../Makefile.common

SRCS = $(wildcard *.cpp)
OBJS = ${SRCS:.cpp=.o}
APPS = ${SRCS:.cpp=}

DEFINES += -DUSERLAND

.PHONY: all list clean

all: $(OBJS) $(APPS)

list:
	@echo $(APPS)

clean:
	@echo "CLEAN"
	@rm -f $(APPS) $(OBJS) *.d

$(APPS) : % : %.o $(OBJS)
	@echo "LD $@"
	@$(LD) -o $@ $(LDFLAGS) $< -lc -lhtml -lgui -ldraw -laudio -lipc -lthread -lcore -lpcidb -lmarkdown -lpthread -lprotocol -lipc

%.o: %.cpp
	@echo "CXX $<"
	@$(CXX) $(CXXFLAGS) -o $@ -c $<

-include $(OBJS:%.o=%.d)
