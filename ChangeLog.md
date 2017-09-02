## Changes Between 1.1.0 and 1.2.0

### Clojure 1.8.0

This project now depends on Clojure 1.8.0. Older
versions are still supported but tested on a
best-effort basis.



## Changes Between 1.0.0 and 1.1.0

### Unsigned Types

GitHub issue: [#30](https://github.com/clojurewerkz/buffy/pull/30).

Contributed by WickedShell.


### UUID Type

UUID type is a new type for serializing `java.util.UUID` type:

```
(let [s    (spec :first-field  (uuid-type))
      b    (compose-buffer s)
      uuid (java.util.UUID/randomUUID)]
    (set-field b :first-field uuid)
    (get-field b :first-field))
```

GitHub issue: [#25](https://github.com/clojurewerkz/buffy/pull/25).

Contributed by Aaron France.


### ByteBuffer Order Preserved

Underlying byte buffer order is now preserved by `buffy.core/wrapped-buffer`.

GitHub issue: [#32](https://github.com/clojurewerkz/buffy/pull/32).

Contributed by Willi Ballenthin.


### Improved Hexdump Formatting

GitHub issue: [#28](https://github.com/clojurewerkz/buffy/pull/28).

Contributed by Ning Sun.


### Fewer Reflection Warnings

GitHub issue: [#31](https://github.com/clojurewerkz/buffy/pull/31).

Contributed by WickedShell.
