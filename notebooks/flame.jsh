// # Profiling
// Let's visualize the impact of correctly choosing the initial size of an ArrayList.

import tech.catheu.jnotebook.Nb;
import java.util.ArrayList;
import java.util.List;

// ## Profile
int elements = 1000;
int reps = 100000;
Runnable poorInit = () -> {
  final long start = System.nanoTime();
  for (int rep = 0; rep < reps; rep++) {
  List<Integer> a = new ArrayList<>();
  for (int i = 0; i < elements; i++) {
    a.add(i);
  }}
  final long end = System.nanoTime();
  System.out.println("Time for poor init: " + (end-start)/1_000_000 + "ms");
};
var poorInitProfilePath = Nb.profile(poorInit);
Runnable goodInit = () -> {
    final long start = System.nanoTime();
    for (int rep = 0; rep < reps; rep++) {
      ArrayList<Integer> b = new ArrayList<>(elements);
      for (int i = 0; i < elements; i++) {
        b.add(i);
      }}
    final long end = System.nanoTime();
    System.out.println("Time for good init: " + (end-start)/1_000_000 + "ms");
};
var goodInitProfilePath = Nb.profile(goodInit);

// ## Side by side comparison
Nb.col(Nb.flame(poorInitProfilePath), Nb.flame(goodInitProfilePath));

System.out.println("haha");

30 >> 1;
