set(SOURCES main.cpp TypingTutorWidget.cpp TypingTutorCanvasWidget.cpp)

serenity_app(TypingTutor ICON app-typingtutor)
target_link_libraries(TypingTutor LibGUI LibGfx LibCore)
