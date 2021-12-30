set(SOURCES
    ClassFileParser.cpp
)

serenity_lib(LibJVM jvm)
target_link_libraries(LibJVM LibCore)
