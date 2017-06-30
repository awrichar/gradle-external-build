#include "MyQtObject.h"

#include <QDebug>

MyQtObject::MyQtObject()
{
}

MyQtObject::~MyQtObject()
{
}

void MyQtObject::doTheMonkey(void)
{
  qDebug() << "Do the monkey with me!";
}
