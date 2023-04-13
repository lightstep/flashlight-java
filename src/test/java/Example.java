public class Example {
  public synchronized int synchronizedMethod() {
    int i = 1;
    i++;
    return i;
  }
  public Object methodWithSynchronized() {
    Object lock = new Object();
    synchronized (lock) {
      return lock;
    }
  }

  public static Object staticMethodWithSynchronized() {
    Object lock = new Object();
    synchronized (lock) {
      return lock;
    }
  }

  public int methodWithBranch(int inc) {
    if(inc == 1) {
      return 1;
    }
    if(inc == 2) {
      return 2;
    }
    if(inc == 3) {
      return 3;
    }
    if(inc == 4) {
      return 4;
    }
    if(inc == 5) {
      return 5;
    } else {
      return -1;
    }
  }

  public int methodWithSwitch(int i) {
    switch (i) {
      case 1:
        return 1;
      case 2:
        return 2;
      case 3:
        return 3;
      case 4:
        return 4;
      case 5:
        return 5;
      default:
        return -1;
    }
  }

  void methodWithMethodCalls() {
    new Object();
    synchronizedMethod();
    methodWithSynchronized();
    staticMethodWithSynchronized();
    methodWithBranch(1);
  }
}
