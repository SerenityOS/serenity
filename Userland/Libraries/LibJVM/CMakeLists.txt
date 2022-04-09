set(SOURCES
    Class.cpp
    Instructions.cpp
    JVM.cpp
    Thread.cpp
    Value.cpp)

serenity_lib(LibJVM jvm)
target_link_libraries(LibJVM LibCore LibJS)
