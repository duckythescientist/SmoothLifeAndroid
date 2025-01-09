
# "Generalization of Conway's "Game of Life" to a continuous domain - SmoothLife"

This is a Live Wallpaper that makes some really pretty patterns. There's cool math behind it.

~Please visit [SmoothLife on Google Play](https://play.google.com/store/apps/details?id=ninja.duck.smoothlife).~ Google sucks. Check the [releases](https://github.com/duckythescientist/SmoothLifeAndroid/releases) for compiled APKs. I'll hopefully get this on Fdroid eventually.

I'm writing a series of blog posts about this app in case people want to know about the internals. Find that here: [duckythescientist.github.io](https://duckythescientist.github.io/)

I also have a [Python version with explanations](https://github.com/duckythescientist/SmoothLife) that's a lot easier to read.


## Explanation for the nerds:


Conway's Game of Life (GoL) is a cellular automaton -- a set of rules that determine how a grid of cells evolves from one generation to the next. GoL creates amazingly complex patterns from simple set of rules.

A while back, a research paper was published that presented the mathematics behind turning GoL from a discrete grid and discrete time-step system into a continuous system (https://arxiv.org/abs/1111.1567). This is a (partial) implementation of the underlying math.

## Information:

This app requires that your device support Android Live Wallpapers. It will fail to open otherwise. Hopefully I can eventually fix that. It's also a moderately CPU-intensive app. Certain settings can be changed to help with that, but it may not run well on old or low-end devices. I've spent a lot of time on optimizations, but the underlying math that runs the app is computationally complex.

## Permissions:

This app requires the ability to set the wallpaper. It does no other monitoring or reporting.

## Cost:

Free and Open Source. The source code and compiled APKs can be found here: https://github.com/duckythescientist/SmoothLifeAndroid
I plan to eventually add extra features that can be unlocked by in-app purchases for those wishing to support the app, but I will still keep the entire codebase open.

## Usage:

When you open the app, it will redirect you to the settings dialog to set SmoothLife as the wallpaper. To change settings, click the gear icon at the top right.

Because of some weird things with wallpapers and how this app works, the app will sometimes not start up properly. This is most common just after an install. Open the app switcher tray, and swipe closed the app. You can then reopen the app, and it should start working.

## Settings:

* Frame Delay (ms): Extra time to wait between screen updates. 0 is really fast. 60 is what I like.
* Color Map: Which colors to use. I like Viridis best.
* Color Scaling: Turn down to have a smoother transition between colors. Turn up to have a sharp transition. There's a slight performance boost if you select exactly 50.
* Scale: How much to zoom in compared to your native resolution. Small numbers can impact performance.
* Cell Inner Radius: This is the "ri" value from the research paper. I can be interesting to play with but usually should be left alone.
* Smooth Timestepping: Use smooth instead of discrete timestepping.
* Timestep: For smooth timestepping, the timestep factor. Should be between 0 and 1. 0.2 is good.
