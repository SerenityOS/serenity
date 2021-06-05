set(SOURCES
    main.cpp
)

serenity_bin(VideoPlayer)
target_link_libraries(VideoPlayer LibVideo LibAudio LibGUI)
