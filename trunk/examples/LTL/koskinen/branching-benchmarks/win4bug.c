// *************************************************************
//
//     Branching-time reasoning for infinite-state systems
//
//              Byron Cook * Eric Koskinen
//                     July 2010
//
// *************************************************************

// Benchmark: win4bug.c
// Property: AF AG WItemsNum >= 1

int WItemsNum;

void init() { WItemsNum = nondet(); }

void callback1() {}
void callback2() {}
#define MoreWItems() nondet()

void body() {
    WItemsNum = nondet();
    while(1) {
        while(WItemsNum<=5 && MoreWItems()) {
               if (WItemsNum<=5) {
                   callback1();
                   WItemsNum++;
    
               } else {
                   WItemsNum++;
               }
        }
    
        while(WItemsNum>2) {
             callback2();
             WItemsNum--;
        }
    }
    while(1) {}
}
    
int main () {}
