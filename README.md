# Chip8-Emulator

![alt text](https://github.com/Calistex/Chip8-Emulator/blob/master/screenshot.png)

Chip8 Emulator written in Java following [this](https://www.youtube.com/playlist?list=PL5PyurErl12czoLyYD8za68d61T_OZsP2) tutorial.  
A bit thank to [Johnnei](https://github.com/Johnnei) for his detailed explanation.

The video output is handled by Swing. Sound is working.

## Controls
Every game is controlled by different keys.

`1 | 2 | 3 | 4`

`Q | W | E | R`

`A | S | D | F`

`Z | X | C | V`

## Added features
- CHIP-8 Extended Opcodes (COSMAC ELF)
- File chooser when the emulator starts
- Pause button to pause and resume the game (Alt + P)
- Sound control to enable/disable it (Alt + M)
- Reset button

## Roadmap
- Adding SUPER CHIP-8 Opcodes
- Changing game from the emulator menu
- Enhancing emulation (some games are slow or incorrectly emulated)

## References
- The base code of this emulator is taken from [Johnnei](https://github.com/Johnnei)'s [tutorial](https://www.youtube.com/playlist?list=PL5PyurErl12czoLyYD8za68d61T_OZsP2).   
- The Extended Opcodes were implemented following [Massung](https://github.com/massung)'s [documentation](https://massung.github.io/CHIP-8/) and code implementation (expecially instruction FX94).
