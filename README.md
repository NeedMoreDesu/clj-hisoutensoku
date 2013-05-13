# clj-hisoutensoku

When my autohotkey script struck 1500 lines, my patience was
broken, and I tried to find a way to make things lispy way.
That's like this package was created. It uses Winapi throung
JNA, and creates re-mapping system lispy way.

This script is created to extend controls in
[Touhou 12.3 Hisoutensoku](http://hisouten.koumakan.jp/wiki/Main_Page) game.

## Usage

If you don't want to compile the code yourself, just copy target/clj-hisoutensoku.jar and run. Else, use
[Leiningen](http://github.com/technomancy/leiningen/) to
build the project (use "lein run" or "lein uberjar").

Didn't made any interface, so you'll end up closing it from
your process manager. :)


It only works with english touhou window title. You can
change that and recompile the project.

It have pre-defined hotkey rules inside. You can use that as
an example to make your own script. Hard places are
commented out. Lunatic places are still uncommented, though. :)


Set up player keys like that:

9. up - w;
9. down - s;
9. left - a;
9. right - d;
9. Melee - z;
9. Weak Shot - x;
9. Strong Shot - c;
9. Flight - v;
9. Switch card - b;
9. Use card - n;

Resulting config:

You can use all 8 numpad directions, numpad5 stands for Flight(D), End = A, Plus(with Numpad+) = B, Enter(with numpad_enter) = C, Multiply(with numpad*) = Switch card, Subtract(with numpad-) = Use card. You can still use the original keys, but this is STRONGLY➈ not recommended.

Holding 1 or 3 or 7 or 9 and Enter or Plus will make some
special moves. For example, 1+enter is 236b
(Hisoutensoku-style key input, stands for press down, press
right, up down, up right, press x, up x).
6+9+end is tkj6a, Home+numbers - specials, 1+6 = 66, 3+4 = 44,
1+6+end = 66 j6a loop, 1+6+enter = 66 j5c loop.

## License

Copyright ⑨ 2013 Cirno Isnotbaka

This program is free software. It comes without any warranty, to
the extent permitted by applicable law. You can redistribute it
and/or modify it under the terms of the Do What The Fuck You Want
To Public License, Version 2, as published by Sam Hocevar. See
COPYING for more details.
