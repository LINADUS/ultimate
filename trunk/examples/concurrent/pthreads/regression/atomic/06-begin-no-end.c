//#Safe
/*
 * Test behaviour for atomic block begin without end (implicitly ends at function boundary).
 *
 * Author: Dominik Klumpp (klumpp@informatik.uni-freiburg.de)
 * Date: 2020-01-24
 *
 */

#include <pthread.h>
extern void __VERIFIER_atomic_begin();
extern void __VERIFIER_atomic_end();

typedef unsigned long int pthread_t;
int x = 0;

void *foo(void *arg) {
  x = 2;
  //@ assert x == 2;
  return (void*)NULL;
}

int main() {
  pthread_t threadId;
  pthread_create(&threadId, NULL, &foo, NULL);

  __VERIFIER_atomic_begin();
  x = 1;
  x = x + 1;
}
