//#Safe
// Author: Evren

procedure main() {
	var x,y,z,n: int;

	assume(n == x && y == 0 && z == 0);
	
	while(x != 0) {
		if (*) {
			x := x + 1;
			y := y - 1;
		}
		if (*) {
			y := y + 1;
			z := z - 1;
		}
		if (*) {
			x := x - 1;
			z := z + 1;
		}
	}
	
//	assert(n == y + z);
	
	while(n != 0) {
		if (*) {
			y := y - 1;
		} else {
			z := z - 1;
		}
		n := n - 1;
	}
	assert(y + z == 0);
}