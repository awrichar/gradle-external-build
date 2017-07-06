#include <QCoreApplication>
#include <QtTest/QtTest>

#define RET_OK              0

class TestDummy: public QObject
{
    Q_OBJECT
private slots:
    void dummyTest() { QVERIFY(1 == 1); };
};

QTEST_MAIN(TestDummy)
#include "dummy_test.moc"

