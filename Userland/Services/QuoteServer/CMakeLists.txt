set(SOURCES
    main.cpp
)

serenity_bin(QuoteServer)
target_link_libraries(QuoteServer LibCore)
