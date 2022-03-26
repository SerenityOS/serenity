set(SOURCES
    Parser/Parser.cpp
    DOM/Node.cpp
)

serenity_lib(LibXML xml)
target_link_libraries(LibXML LibC)
