set(SOURCES
    readers/BitStreamReader.cpp
    readers/AVI/AVI.cpp
    readers/AVI/Header.cpp
    readers/AVI/Info.cpp
    readers/AVI/Index.cpp
    Manip.cpp
)

serenity_lib(LibMedia media)
target_link_libraries(LibMedia LibCore LibC)
