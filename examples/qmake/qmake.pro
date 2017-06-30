QT += core
QT -= gui

TARGET = sample_qmake
CONFIG += console
CONFIG -= app_bundle

TEMPLATE = app

SOURCES += \
    src/main/cpp/main.cpp \
    src/main/cpp/MyQtObject.cpp

DEFINES += QT_DEPRECATED_WARNINGS

HEADERS += \
    src/main/cpp/MyQtObject.h
    
