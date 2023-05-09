the greedyInterpreter does not make a difference between

Flow:
List<Integer> l = List.of(1,2,3,4);
replace by: 
List.of(1,2,3,4);

replace by:
List<Integer> l = List.of(1,2,3,4);

call
l.get(0);
--> throws error



error make the whole rendering fail:

Nb.vega(new JSONObject("""{
"data": {
"values": [
{"a": "C", "b": 2}, {"a": "C", "b": 7}, {"a": "C", "b": 4},
{"a": "D", "b": 1}, {"a": "D", "b": 2}, {"a": "D", "b": 6},
{"a": "E", "b": 8}, {"a": "E", "b": 4}, {"a": "E", "b": 7}
]
},
"mark": "point",
"encoding": {}
}""");


Such incompleteness makes the rendering fail: 
Path profile1 = Nb.profile(() -> addElements(10_000, new ArrayList());






BUG 3
this snippets seems to evaluate even if there is an additional comma in new ArrayList<>(3 * elements));  
int elements = 10_001;
Runnable poorInit = () -> {
List<Integer> a = new ArrayList<>();
for (int i = 0; i < elements; i++) {
a.add(i);
}
};

var poorInitProfilePath = Nb.profile(poorInit, 1000L);

Runnable goodInit = () -> {
ArrayList<Integer> a = new ArrayList<>(3 * elements));
for (int i = 0; i < elements; i++) {
a.add(i);
}
};

It should fail with a syntax error. 
Maybe the example can be simplified.
Goal: 
- reduce this example to a minimal example
- fix issue 


BUG 4:
the snippet below seems to make the parser throw an exception an additional } is present

Runnable goodInit = () -> {
for (int rep = 0; rep<1000; rep++) {
ArrayList<Integer> a = new ArrayList<>(4 * elements);
for (int i = 0; i < elements; i++) {
a.add(i);
}
}
}
}
;



This seems to break the cache/fingerprinting: with or without trailing in (int i=0; i<4; i++;):
```
for (int i=0; i<4; i++) {
int x = i +1;
System.out.println(x + r);
}
```
