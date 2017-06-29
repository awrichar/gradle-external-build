#include <QCoreApplication>
#include <QTimer>

#include "MyQtObject.h"

int main(int argc, char *argv[])
{
  QCoreApplication app(argc, argv);

  MyQtObject o;

  QTimer::singleShot(200, &o, SLOT(doTheMonkey()));
  QTimer::singleShot(600, &app, SLOT(quit()));

  return app.exec();
}
