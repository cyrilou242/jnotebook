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

This incorrect input breaks the parsing with an NPE:
```
Nb.vega(Map.of(
          "data", Map.of("url", "data/seattle-weather.csv"),
          "mark", "bar",
          "encoding", Map.of(
            "x": Map.of("timeUnit", "month", "field", "date", "type", "ordinal"),
            "y": Map.of("aggregate", "mean", "field", "precipitation")
          )
        );
```
catch these cases
