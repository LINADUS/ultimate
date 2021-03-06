/*
-------------------------------------------------------------------------
sphere.c
-------------------------------------------------------------------------
This program will generate a given number of points uniformly distributed
on the surface of a sphere. The number of points is given on the command
line as the first parameter.  Thus `sphere 100' will generate 100 points
on the surface of a sphere, and output them to stdout.
        A number of different command-line flags are provided to set the
radius of the sphere, control the output format, or generate points on
an ellipsoid.  The definition of the flags is printed if the program is
run without arguments: `sphere'.
        The idea behind the algorithm is that for a sphere of radius r, the
area of a zone of width h is always 2*pi*r*h, regardless of where the sphere
is sliced.  The implication is that the z-coordinates of random points on a
sphere are uniformly distributed, so that x and y can always be generated by
a given z and a given angle.
        The default output is integers, rounded from the floating point
computation.  The rounding implies that some points will fall outside
the sphere, and some inside.  If all are required to be inside, then
the calls to irint() should be removed.
        The flags -a, -b, -c are used to set ellipsoid axis lengths.
Note that the points are not uniformly distributed on the ellipsoid: they
are uniformly distributed on the sphere and that is scaled to an ellipsoid.
        random() is used to generate random numbers, seeded with time().
How to compile:
        gcc -o sphere sphere.c -lm

Reference: J. O'Rourke, Computational Geometry Column 31,
Internat. J. Comput. Geom. Appl. 7 379--382 (1997);
Also in SIGACT News, 28(2):20--23 (1997), Issue 103.

Written by Joseph O'Rourke and Min Xu, June 1997.
Used in the textbook, "Computational Geometry in C."
Questions to orourke@cs.smith.edu.
--------------------------------------------------------------------
This code is Copyright 1997 by Joseph O'Rourke.  It may be freely
redistributed in its entirety provided that this copyright notice is
not removed.
--------------------------------------------------------------------
*/
#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <string.h>
#include <time.h>

/* MAX_INT is the range of random(): 2^{31}-1 */
#define MAX_INT   2147483647
#define TRUE      1
#define FALSE     0

#define irint(x) ((int)rint(x))

void print_instruct( void )
{
  printf ( "Please enter your input according to the following format:\n" );
  printf ( "\tsphere [number of points] [-flag letter][parameter value]\n" );
  printf ( "\t\t (no space between flag letter and parameter value!)\n" );
  printf ( "Available flags are:\n" );
  printf ( "\t-r[parameter] \t set radius of the sphere (default: 100)\n" );
  printf ( "\t-f            \t set output to floating point format (default: integer)\n");
  printf ( "\t-a[parameter] \t ellipsoid x-axis length (default: sphere radius)\n");
  printf ( "\t-b[parameter] \t ellipsoid y-axis length (default: sphere radius)\n");
  printf ( "\t-c[parameter] \t ellipsoid z-axis length (default: sphere radius)\n");
}

void TestFlags (int argc, char *argv[], int *r1, int *r2, int *r3, int *r, int *float_pt)
{

  int i = 2;

  /* Test for flags */
  while ( i < argc ) {

    /* Test for radius flag */
    if ( strncmp ( argv[i], "-r", 2 ) == 0 ) {
      if ( sscanf( &argv[i][2], "%d", r ) != 1 )
        printf ( "No space between flag name and parameter, please!\n" ),
        exit (1);
      else if (*r == 0 )
        printf ( "Invalid radius flag\n" ),
        exit (1);
      else
        *r1 = *r2 = *r3 = *r;
    }

    /* Test whether user wants floating point output */
    if ( strncmp ( argv[i], "-f", 2 ) == 0 )
      *float_pt = TRUE;

    /* Test for ellipsoid radius if any */
    if ( strncmp ( argv[i], "-a", 2 ) == 0 )
      if ( sscanf ( &argv[i][2], "%d", r1 ) != 1 )
        printf ( "No space between flag name and parameter, please!\n" ),
        exit (1);
    if ( strncmp ( argv[i], "-b", 2 ) == 0 )
      if ( sscanf ( &argv[i][2], "%d", r2 ) != 1 )
        printf ( "No space between flag name and parameter, please!\n" ),
        exit (1);
    if ( strncmp ( argv[i], "-c", 2 ) == 0 )
      if ( sscanf ( &argv[i][2], "%d", r3 ) != 1 )
        printf ( "No space between flag name and parameter, please!\n" ),
        exit (1);

    i++;
  }

  if ( *r1 == 0 || *r2 == 0 || *r3 == 0 )
    printf ( "Invalid ellipsoid radius\n" ),
    exit (1);
}

int main( argc, argv )
int argc;
char *argv[];
{
  int n;                /* number of points */
  double x, y, z, w, t;
  double R = 100.0;     /* default radius */
  int r;                /* true radius */
  int r1, r2, r3;       /* ellipsoid axis lengths */
  int float_pt = FALSE;

  srandom( (int) time((long *) 0 ) );
  if ( argc < 2 )
    print_instruct(),
    exit (1);

  r = R;
  r1 = r2 = r3 = r;
  TestFlags ( argc, argv, &r1, &r2, &r3, &r, &float_pt );

  n = atoi( argv[1] );

  while (n > 0) {
    /* Generate a random point on a sphere of radius 1. */
    /* the sphere is sliced at z, and a random point at angle t
       generated on the circle of intersection. */
    z = 2.0 * (double) random() / MAX_INT - 1.0;
    t = 2.0 * M_PI * (double) random() / MAX_INT;
    w = sqrt( 1 - z*z );
    x = w * cos( t );
    y = w * sin( t );

    if ( float_pt == FALSE )
      printf ( "%6d  %6d  %6d\n",
                irint( r1 * x ),
                irint( r2 * y ),
                irint( r3 * z ) );
    else
      printf ( "%6f  %6f  %6f\n", r1 * x, r2 * y, r3 * z );

    n--;
  }
  return 0;
}
