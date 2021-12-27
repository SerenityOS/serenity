set(SOURCES
    DMT.cpp
    EDID.cpp
    VIC.cpp
)

serenity_lib(LibEDID edid)
target_link_libraries(LibEDID LibC)
