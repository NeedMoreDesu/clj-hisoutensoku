# clj-hisoutensoku

When my autohotkey script struck 1500 lines, my patience was
broken, and I tried to find a way to make things lispy way.
That's like this package was created. It uses Winapi throung
JNA, and creates re-mapping system lispy way.

This script is created to extend controls in
[Touhou 12.3 Hisoutensoku](http://hisouten.koumakan.jp/wiki/Main_Page) game.

## Usage

If you don't want to compile the code yourself, just copy target/clj-hisoutensoku.jar and targer/clj-hisoutensoku-config.clj (must be in the same folder) and run. Else, use
[Leiningen](http://github.com/technomancy/leiningen/) to
build the project (use "lein run" or "lein uberjar").

Didn't made any interface, so you'll end up closing it from
your process manager. :)


It only works with english touhou window title. To change that, place a different pattern in clj-hisoutensoku-config.clj

It have some pre-defined hotkey rules inside, but top-level
rebinding is configurable with
clj-hisoutensoku-config.clj. You can use that as an example
to make your own script. Hard places are commented
out. Lunatic places are still uncommented, though. :)


Set up player keys to match old keys section in clj-hisoutensoku-config.clj

Resulting config:

You can use all 8 numpad directions, numpad5 stands for Flight(D), End = A, Plus(with Numpad+) = B, Enter(with numpad_enter) = C, Multiply(with numpad*) = Switch card, Subtract(with numpad-) = Use card.
You shouldnt use same key for new and old key, this is STRONGLY➈ not recommended.

Holding 1 or 3 or 7 or 9 and Enter or Plus will make some
special moves. For example, 1+enter is 236b
(Hisoutensoku-style key input, stands for press down, press
right, up down, up right, press x, up x).
6+9+end is tkj6a(there are more versions of tk moves, including bullets, excluding specials),
Home+numbers - specials,
1+6 or 4+9 = 66, 3+4 or 6+7 = 44,
1+6+end = 66 j5a loop, 1+6+enter = 66 j5c loop.
3+9 = 29 d3 (wait for release) 29 29 (works with 1/2/3+7/8/9)

## License

Copyright ⑨ 2013 Cirno Isnotbaka

This program is free software. It comes without any warranty, to
the extent permitted by applicable law. You can redistribute it
and/or modify it under the terms of the Do What The Fuck You Want
To Public License, Version 2, as published by Sam Hocevar. See
COPYING for more details.
