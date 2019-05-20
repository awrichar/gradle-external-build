include(qmake.pro)

QT += testlib
TARGET = sample_test_qmake

SOURCES -= \
    src/main/cpp/main.cpp

SOURCES += \
    src/test/cpp/dummy_test.cpp
