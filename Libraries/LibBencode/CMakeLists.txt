set(SOURCES
    Dictionary.cpp
    List.cpp
    Parser.cpp
    Value.cpp
)

serenity_lib(LibBencode bencode)
target_link_libraries(LibBencode LibC)
