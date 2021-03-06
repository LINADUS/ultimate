type ref;
type realVar;
type classConst;
// type Field x;
// var $HeapVar : <x>[ref, Field x]x;

const unique $null : ref ;
const unique $intArrNull : [int]int ;
const unique $realArrNull : [int]realVar ;
const unique $refArrNull : [int]ref ;

const unique $arrSizeIdx : int;
var $intArrSize : [int]int;
var $realArrSize : [realVar]int;
var $refArrSize : [ref]int;

var $stringSize : [ref]int;

//built-in axioms 
axiom ($arrSizeIdx == -1);

//note: new version doesn't put helpers in the perlude anymore//Prelude finished 



var java.lang.Object$List$head254 : Field ref;
var List$List$tail255 : Field ref;


// procedure is generated by joogie.
function {:inline true} $neref(x : ref, y : ref) returns (__ret : int) {
if (x != y) then 1 else 0
}


// procedure is generated by joogie.
function {:inline true} $realarrtoref($param00 : [int]realVar) returns (__ret : ref);



// procedure is generated by joogie.
function {:inline true} $modreal($param00 : realVar, $param11 : realVar) returns (__ret : realVar);



// procedure is generated by joogie.
function {:inline true} $leref($param00 : ref, $param11 : ref) returns (__ret : int);



	 //  @line: 42
// <Test5: void test(List,List,List)>
procedure void$Test5$test$2231($param_0 : ref, $param_1 : ref, $param_2 : ref) {
var $r142 : ref;
var r447 : ref;
var r346 : ref;
var $r040 : ref;
var r245 : ref;
Block47:
	r245 := $param_0;
	r346 := $param_1;
	r447 := $param_2;
	 goto Block48;
	 //  @line: 43
Block48:
	 goto Block49, Block51;
	 //  @line: 43
Block49:
	 assume ($eqref((r245), ($null))==1);
	 goto Block50;
	 //  @line: 43
Block51:
	 //  @line: 43
	 assume ($negInt(($eqref((r245), ($null))))==1);
	 //  @line: 44
	$r040 := $newvariable((52));
	 assume ($neref(($newvariable((52))), ($null))==1);
	 assert ($neref(($r040), ($null))==1);
	 //  @line: 44
	 call void$List$$la$init$ra$$2232(($r040), (r245), (r346));
	 //  @line: 44
	r346 := $r040;
	 //  @line: 45
	$r142 := $newvariable((53));
	 assume ($neref(($newvariable((53))), ($null))==1);
	 assert ($neref(($r142), ($null))==1);
	 //  @line: 45
	 call void$List$$la$init$ra$$2232(($r142), (r346), (r447));
	 //  @line: 45
	r447 := $r142;
	 assert ($neref((r245), ($null))==1);
	 //  @line: 46
	 call r245 := List$List$getTail$2233((r245));
	 goto Block48;
	 //  @line: 48
Block50:
	 return;
}


// procedure is generated by joogie.
function {:inline true} $modint($param00 : int, $param11 : int) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $gtref($param00 : ref, $param11 : ref) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $eqrealarray($param00 : [int]realVar, $param11 : [int]realVar) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $addint(x : int, y : int) returns (__ret : int) {
(x + y)
}


// procedure is generated by joogie.
function {:inline true} $subref($param00 : ref, $param11 : ref) returns (__ret : ref);



// procedure is generated by joogie.
function {:inline true} $inttoreal($param00 : int) returns (__ret : realVar);



// procedure is generated by joogie.
function {:inline true} $shrint($param00 : int, $param11 : int) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $negReal($param00 : realVar) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $ushrint($param00 : int, $param11 : int) returns (__ret : int);



	 //  @line: 14
// <List: List mk(int)>
procedure List$List$mk$2234($param_0 : int) returns (__ret : ref) {
var i158 : int;
var $r157 : ref;
var $i055 : int;
var r259 : ref;
var $r056 : ref;
Block56:
	i158 := $param_0;
	 //  @line: 15
	r259 := $null;
	 goto Block57;
	 //  @line: 17
Block57:
	 //  @line: 17
	$i055 := i158;
	 //  @line: 17
	i158 := $addint((i158), (-1));
	 goto Block58;
	 //  @line: 17
Block58:
	 goto Block59, Block61;
	 //  @line: 17
Block59:
	 assume ($leint(($i055), (0))==1);
	 goto Block60;
	 //  @line: 17
Block61:
	 //  @line: 17
	 assume ($negInt(($leint(($i055), (0))))==1);
	 //  @line: 18
	$r056 := $newvariable((62));
	 assume ($neref(($newvariable((62))), ($null))==1);
	 //  @line: 18
	$r157 := $newvariable((63));
	 assume ($neref(($newvariable((63))), ($null))==1);
	 assert ($neref(($r157), ($null))==1);
	 //  @line: 18
	 call void$java.lang.Object$$la$init$ra$$28(($r157));
	 assert ($neref(($r056), ($null))==1);
	 //  @line: 18
	 call void$List$$la$init$ra$$2232(($r056), ($r157), (r259));
	 //  @line: 18
	r259 := $r056;
	 goto Block57;
	 //  @line: 20
Block60:
	 //  @line: 20
	__ret := r259;
	 return;
}


// procedure is generated by joogie.
function {:inline true} $refarrtoref($param00 : [int]ref) returns (__ret : ref);



// <java.lang.Object: void <init>()>
procedure void$java.lang.Object$$la$init$ra$$28(__this : ref);



// procedure is generated by joogie.
function {:inline true} $divref($param00 : ref, $param11 : ref) returns (__ret : ref);



// procedure is generated by joogie.
function {:inline true} $mulref($param00 : ref, $param11 : ref) returns (__ret : ref);



// procedure is generated by joogie.
function {:inline true} $neint(x : int, y : int) returns (__ret : int) {
if (x != y) then 1 else 0
}


// <Test5: void <init>()>
procedure void$Test5$$la$init$ra$$2228(__this : ref)  requires ($neref((__this), ($null))==1);
 {
var r01 : ref;
Block16:
	r01 := __this;
	 assert ($neref((r01), ($null))==1);
	 //  @line: 1
	 call void$java.lang.Object$$la$init$ra$$28((r01));
	 return;
}


// procedure is generated by joogie.
function {:inline true} $ltreal($param00 : realVar, $param11 : realVar) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $reftorefarr($param00 : ref) returns (__ret : [int]ref);



// procedure is generated by joogie.
function {:inline true} $gtint(x : int, y : int) returns (__ret : int) {
if (x > y) then 1 else 0
}


	 //  @line: 10
// <List: List getTail()>
procedure List$List$getTail$2233(__this : ref) returns (__ret : ref)  requires ($neref((__this), ($null))==1);
 {
var $r152 : ref;
var r051 : ref;
Block55:
	r051 := __this;
	 assert ($neref((r051), ($null))==1);
	 //  @line: 11
	$r152 := $HeapVar[r051, List$List$tail255];
	 //  @line: 11
	__ret := $r152;
	 return;
}


// procedure is generated by joogie.
function {:inline true} $reftoint($param00 : ref) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $addref($param00 : ref, $param11 : ref) returns (__ret : ref);



	 //  @line: 31
// <Test5: int length(List)>
procedure int$Test5$length$2230($param_0 : ref) returns (__ret : int) {
var r037 : ref;
var i038 : int;
Block42:
	r037 := $param_0;
	 //  @line: 32
	i038 := 0;
	 goto Block43;
	 //  @line: 34
Block43:
	 goto Block44, Block46;
	 //  @line: 34
Block44:
	 assume ($eqref((r037), ($null))==1);
	 goto Block45;
	 //  @line: 34
Block46:
	 //  @line: 34
	 assume ($negInt(($eqref((r037), ($null))))==1);
	 assert ($neref((r037), ($null))==1);
	 //  @line: 35
	 call r037 := List$List$getTail$2233((r037));
	 //  @line: 36
	i038 := $addint((i038), (1));
	 goto Block43;
	 //  @line: 39
Block45:
	 //  @line: 39
	__ret := i038;
	 return;
}


// procedure is generated by joogie.
function {:inline true} $xorreal($param00 : realVar, $param11 : realVar) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $andref($param00 : ref, $param11 : ref) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $cmpreal(x : realVar, y : realVar) returns (__ret : int) {
if ($ltreal((x), (y)) == 1) then 1 else if ($eqreal((x), (y)) == 1) then 0 else -1
}


// procedure is generated by joogie.
function {:inline true} $addreal($param00 : realVar, $param11 : realVar) returns (__ret : realVar);



// procedure is generated by joogie.
function {:inline true} $gtreal($param00 : realVar, $param11 : realVar) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $eqreal(x : realVar, y : realVar) returns (__ret : int) {
if (x == y) then 1 else 0
}


// procedure is generated by joogie.
function {:inline true} $ltint(x : int, y : int) returns (__ret : int) {
if (x < y) then 1 else 0
}


// procedure is generated by joogie.
function {:inline true} $newvariable($param00 : int) returns (__ret : ref);



// procedure is generated by joogie.
function {:inline true} $divint($param00 : int, $param11 : int) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $geint(x : int, y : int) returns (__ret : int) {
if (x >= y) then 1 else 0
}


// procedure is generated by joogie.
function {:inline true} $mulint($param00 : int, $param11 : int) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $leint(x : int, y : int) returns (__ret : int) {
if (x <= y) then 1 else 0
}


// procedure is generated by joogie.
function {:inline true} $shlref($param00 : ref, $param11 : ref) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $eqrefarray($param00 : [int]ref, $param11 : [int]ref) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $reftointarr($param00 : ref) returns (__ret : [int]int);



// procedure is generated by joogie.
function {:inline true} $ltref($param00 : ref, $param11 : ref) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $mulreal($param00 : realVar, $param11 : realVar) returns (__ret : realVar);



	 //  @line: 4
// <List: void <init>(java.lang.Object,List)>
procedure void$List$$la$init$ra$$2232(__this : ref, $param_0 : ref, $param_1 : ref)
  modifies $HeapVar;
  requires ($neref((__this), ($null))==1);
 {
var r048 : ref;
var r149 : ref;
var r250 : ref;
Block54:
	r048 := __this;
	r149 := $param_0;
	r250 := $param_1;
	 assert ($neref((r048), ($null))==1);
	 //  @line: 5
	 call void$java.lang.Object$$la$init$ra$$28((r048));
	 assert ($neref((r048), ($null))==1);
	 //  @line: 6
	$HeapVar[r048, java.lang.Object$List$head254] := r149;
	 assert ($neref((r048), ($null))==1);
	 //  @line: 7
	$HeapVar[r048, List$List$tail255] := r250;
	 return;
}


// procedure is generated by joogie.
function {:inline true} $shrref($param00 : ref, $param11 : ref) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $ushrreal($param00 : realVar, $param11 : realVar) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $shrreal($param00 : realVar, $param11 : realVar) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $divreal($param00 : realVar, $param11 : realVar) returns (__ret : realVar);



// procedure is generated by joogie.
function {:inline true} $orint($param00 : int, $param11 : int) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $reftorealarr($param00 : ref) returns (__ret : [int]realVar);



// procedure is generated by joogie.
function {:inline true} $cmpref(x : ref, y : ref) returns (__ret : int) {
if ($ltref((x), (y)) == 1) then 1 else if ($eqref((x), (y)) == 1) then 0 else -1
}


// procedure is generated by joogie.
function {:inline true} $realtoint($param00 : realVar) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $geref($param00 : ref, $param11 : ref) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $orreal($param00 : realVar, $param11 : realVar) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $eqint(x : int, y : int) returns (__ret : int) {
if (x == y) then 1 else 0
}


// procedure is generated by joogie.
function {:inline true} $ushrref($param00 : ref, $param11 : ref) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $modref($param00 : ref, $param11 : ref) returns (__ret : ref);



// procedure is generated by joogie.
function {:inline true} $eqintarray($param00 : [int]int, $param11 : [int]int) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $negRef($param00 : ref) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $lereal($param00 : realVar, $param11 : realVar) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $nereal(x : realVar, y : realVar) returns (__ret : int) {
if (x != y) then 1 else 0
}


	 //  @line: 2
// <Test5: void main(java.lang.String[])>
procedure void$Test5$main$2229($param_0 : [int]ref) {
var $i310 : int;
var r432 : ref;
var $i616 : int;
var $i717 : int;
var $i16 : int;
var $i27 : int;
var $i514 : int;
var $i919 : int;
var r331 : ref;
var r230 : ref;
var $i1121 : int;
var r129 : ref;
var $i1222 : int;
var $i411 : int;
var $i818 : int;
var $i1020 : int;
var r02 : [int]ref;
var $i03 : int;
var $i1323 : int;

 //temp local variables 
var $freshlocal0 : ref;

Block17:
	r02 := $param_0;
	 //  @line: 3
	$i03 := $refArrSize[r02[$arrSizeIdx]];
	 //  @line: 3
	 call r129 := List$List$mk$2234(($i03));
	 //  @line: 4
	$i16 := $refArrSize[r02[$arrSizeIdx]];
	 //  @line: 4
	$i27 := $addint(($i16), (3));
	 //  @line: 4
	 call r230 := List$List$mk$2234(($i27));
	 //  @line: 5
	$i310 := $refArrSize[r02[$arrSizeIdx]];
	 //  @line: 5
	$i411 := $addint(($i310), (5));
	 //  @line: 5
	 call r331 := List$List$mk$2234(($i411));
	 goto Block18;
	 //  @line: 8
Block18:
	 //  @line: 8
	 call $i514 := int$Test5$length$2230((r129));
	 goto Block19;
	 //  @line: 8
Block19:
	 goto Block20, Block22;
	 //  @line: 8
Block20:
	 assume ($leint(($i514), (0))==1);
	 goto Block21;
	 //  @line: 8
Block22:
	 //  @line: 8
	 assume ($negInt(($leint(($i514), (0))))==1);
	 //  @line: 9
	r432 := r129;
	 //  @line: 10
	r129 := r230;
	 //  @line: 11
	r230 := r331;
	 //  @line: 12
	r331 := r432;
	 //  @line: 14
	 call $i616 := int$Test5$length$2230((r230));
	 //  @line: 14
	$i717 := $modint(($i616), (3));
	 goto Block23;
	 //  @line: 29
Block21:
	 return;
	 //  @line: 14
Block23:
	 goto Block26, Block24;
	 //  @line: 14
Block26:
	 //  @line: 14
	 assume ($negInt(($neint(($i717), (0))))==1);
	 assert ($neref((r432), ($null))==1);
	 //  @line: 15
	 call $freshlocal0 := List$List$getTail$2233((r432));
	 goto Block25;
	 //  @line: 14
Block24:
	 assume ($neint(($i717), (0))==1);
	 goto Block25;
	 //  @line: 17
Block25:
	 //  @line: 17
	 call $i818 := int$Test5$length$2230((r331));
	 //  @line: 17
	$i919 := $modint(($i818), (5));
	 goto Block27;
	 //  @line: 17
Block27:
	 goto Block30, Block28;
	 //  @line: 17
Block30:
	 //  @line: 17
	 assume ($negInt(($neint(($i919), (0))))==1);
	 assert ($neref((r331), ($null))==1);
	 //  @line: 18
	 call r331 := List$List$getTail$2233((r331));
	 goto Block29;
	 //  @line: 17
Block28:
	 assume ($neint(($i919), (0))==1);
	 goto Block29;
	 //  @line: 20
Block29:
	 //  @line: 20
	 call $i1020 := int$Test5$length$2230((r129));
	 //  @line: 20
	 call $i1121 := int$Test5$length$2230((r230));
	 goto Block31;
	 //  @line: 20
Block31:
	 goto Block32, Block34;
	 //  @line: 20
Block32:
	 assume ($leint(($i1020), ($i1121))==1);
	 goto Block33;
	 //  @line: 20
Block34:
	 //  @line: 20
	 assume ($negInt(($leint(($i1020), ($i1121))))==1);
	 assert ($neref((r129), ($null))==1);
	 //  @line: 21
	 call r129 := List$List$getTail$2233((r129));
	 goto Block35;
	 //  @line: 22
Block33:
	 //  @line: 22
	 call $i1222 := int$Test5$length$2230((r129));
	 goto Block36;
	 //  @line: 27
Block35:
	 //  @line: 27
	 call void$Test5$test$2231((r129), (r230), (r331));
	 goto Block41;
	 //  @line: 22
Block36:
	 //  @line: 22
	 call $i1323 := int$Test5$length$2230((r230));
	 goto Block37;
	 //  @line: 27
Block41:
	 goto Block18;
	 //  @line: 22
Block37:
	 goto Block38, Block40;
	 //  @line: 22
Block38:
	 assume ($neint(($i1222), ($i1323))==1);
	 goto Block39;
	 //  @line: 22
Block40:
	 //  @line: 22
	 assume ($negInt(($neint(($i1222), ($i1323))))==1);
	 assert ($neref((r230), ($null))==1);
	 //  @line: 23
	 call r230 := List$List$getTail$2233((r230));
	 goto Block35;
	 //  @line: 25
Block39:
	 assert ($neref((r331), ($null))==1);
	 //  @line: 25
	 call r331 := List$List$getTail$2233((r331));
	 goto Block35;
}


// procedure is generated by joogie.
function {:inline true} $instanceof($param00 : ref, $param11 : classConst) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $xorref($param00 : ref, $param11 : ref) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $orref($param00 : ref, $param11 : ref) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $intarrtoref($param00 : [int]int) returns (__ret : ref);



// procedure is generated by joogie.
function {:inline true} $subreal($param00 : realVar, $param11 : realVar) returns (__ret : realVar);



// procedure is generated by joogie.
function {:inline true} $shlreal($param00 : realVar, $param11 : realVar) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $negInt(x : int) returns (__ret : int) {
if (x == 0) then 1 else 0
}


// procedure is generated by joogie.
function {:inline true} $gereal($param00 : realVar, $param11 : realVar) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $eqref(x : ref, y : ref) returns (__ret : int) {
if (x == y) then 1 else 0
}


// procedure is generated by joogie.
function {:inline true} $cmpint(x : int, y : int) returns (__ret : int) {
if (x < y) then 1 else if (x == y) then 0 else -1
}


// procedure is generated by joogie.
function {:inline true} $andint($param00 : int, $param11 : int) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $andreal($param00 : realVar, $param11 : realVar) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $shlint($param00 : int, $param11 : int) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $xorint($param00 : int, $param11 : int) returns (__ret : int);



// procedure is generated by joogie.
function {:inline true} $subint(x : int, y : int) returns (__ret : int) {
(x - y)
}


