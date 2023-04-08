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

Nb.vegaLite(new JSONObject("""{
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
