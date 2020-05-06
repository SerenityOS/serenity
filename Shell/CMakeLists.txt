set(SOURCES
    main.cpp
    Parser.cpp
)

serenity_bin(Shell)
target_link_libraries(Shell LibCore LibLine)
