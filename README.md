# counters-clj

A Clojure library designed to ... well, that part is up to you.

## Usage

- moar!

## TODO

- remove flushed-at (check delta != null instead)

- build with travis ci

- test metrics namespace!!

- change message-chunk-size, flush n limit, flush msecs limit, input channel size, output channel size with Midi device
  - close current input
  - create new agg :) with new params
  - mmmm I have to wait for the out channel to close ... otherwise aggregates will be skewed!darn!
    - it may be better if I could just stop the processing immediately !
  - execute Midi command only if it has been about 2 seconds since the last time a command was received

- use normal distribution for simulation

- (X) retrieve events in batches

- add core.typed

- (X) integrated with graphite !

- (X) midi control exploration

- integrate with kafka !
  - use one node for now

- use two kafka nodes (will need to figure out leader and stuff like that)

- (X) exit when put is closed

- need to write unit tests

- figure out creation of channels ... agg should handle it ...

- stop when outout channel is closed

- handle exceptions

- TODO flush changed entries only ! need a set to store keys, flush when count >= n.

- graphite!

- write changed counters only

- need to publish aggregation batches of a certain size  (the batch size is how many keys there are in state) that controls how much data will be written to the db in one transaction

- allow events to be fetched in batches

- only write counters that changed

- do-agg ---> flush on exit

- use spout and bolt nomenclature from apache spark ? maybe ...

## License

Copyright © 2014 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
