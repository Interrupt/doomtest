# doomtest
An editor for a Doom-like engine, using LibGdx.

The level format is a 2.5d Sector and Line collection like in the original Doom, and tesselation is done using GLUTesselator. No fancy BSP tree stuff happening here yet (or ever, possibly).

![Preview](http://i.imgur.com/lVKf23l.png)

## Things that work now
* Sector / Wall tesselation
* Sector / subsector creation
* Surface picking (mostly)
* Texture picking

## Todo
* Save / Load
* History / Undo
* Multiple texture atlases

## Usage
* WSAD - move camera
* Arrow keys - rotate camera
* Enter - switch to edit mode
  * In edit mode, enter creates sector out of the current path
  * Click to place path vertices
  * ESC to cancel current path
* P - switch to pick mode
  * Click on a sector / wall to pick it
  * Alt + click & drag - adjust floor height of picked sector
  * Shift + Alt + click & drag - adjust ceiling height of picked sector

Feel free to use this for whatever, but get in touch for commercial uses.
