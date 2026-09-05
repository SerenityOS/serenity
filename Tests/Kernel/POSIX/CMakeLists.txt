add_subdirectory(Wait)

serenity_test(TestFIFO.cpp Kernel)
serenity_test(TestPthreadCond.cpp Kernel)
