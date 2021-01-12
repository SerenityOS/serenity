set(SOURCES
    main.cpp
)

serenity_bin(DumpLayoutTree)
target_link_libraries(DumpLayoutTree LibWeb)
