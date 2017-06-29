#ifndef MYQTOBJECT_H_
#define MYQTOBJECT_H_

#include <QObject>

class MyQtObject : public QObject
{
  Q_OBJECT
public:
  virtual ~MyQtObject();
  MyQtObject();

public slots:
  void doTheMonkey(void);

};

#endif /* MYQTOBJECT_H_ */
