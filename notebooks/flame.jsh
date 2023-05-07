// Profiling
// Let's visualize the impact of correctly choosing the initial size of an ArrayList.

import tech.catheu.jnotebook.Nb;
import java.util.ArrayList;
import java.util.List;

// ## Profile
int elements = 1;
int reps = 1;
Runnable poorInit = () -> {
  for (int rep = 0; rep < reps; rep++) {
  List<Integer> a = new ArrayList<>();
  for (int i = 0; i < elements; i++) {
    a.add(i);
  }}
};

var poorInitProfilePath = Nb.profile(poorInit);

Runnable goodInit = () -> {
for (int rep = 0; rep < reps; rep++) {
  ArrayList<Integer> b = new ArrayList<>(elements);
  for (int i = 0; i < elements; i++) {
    b.add(i);
  }
}
};

var goodInitProfilePath = Nb.profile(goodInit);

// ## Side by side comparison
Nb.col(Nb.flame(poorInitProfilePath), Nb.flame(goodInitProfilePath));


30 >> 1;
