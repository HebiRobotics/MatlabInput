# HebiJoystick

HebiJoystick is joystick input library for MATLAB that serves as a mostly drop-in replacement for [vrjoystick](https://www.mathworks.com/help/sl3d/vrjoystick.html). It is intended for people who don't have access to the [3D Animation Toolbox](https://www.mathworks.com/products/3d-animation.html). 

## Notes

* The order of axes and buttons may be different from vrjoystick
* The order of axes and buttons may differ between operating systems
* There is a maximum number of events that can occur between reads. If reads don't happen frequently enough, the returned state may not match the real physical state. A polling rate of about once per second should be sufficient for the default settings.
* On some operating systems going into sleep mode while executing a script that reads from the joystick may make MATLAB seem unresponsive. Ctrl-C works eventually, but it may take on the order of minutes to recover.

## Installation

* Download the [latest release](https://github.com/HebiRobotics/HebiJoystick/releases)
* Extract the .zip file into a folder of your choice
* Add the unzipped files to the [MATLAB path](http://www.mathworks.com/help/matlab/ref/path.html)

* The underlying libraries are typically loaded automatically at the first call. However, if you are using other Java libraries, e.g., 
[HebiCam](https://github.com/HebiRobotics/HebiCam) or the [Hebi API](http://hebirobotics.com/matlab), it is better to load all libraries before instantiating any objects.

```matlab
HebiJoystick.loadLibs();
```

## Usage

Create joystick and react to button presses.

```matlab
joy = HebiJoystick(1);
while true
  [axes, buttons, povs] = read(joy);
  if any(buttons)
    disp('one or more buttons are pressed down');
  end
  pause(0.1);
end
```

See [vrjoystick](https://www.mathworks.com/help/sl3d/vrjoystick.html) documentation for more information.

![comparison](https://github.com/HebiRobotics/HebiJoystick/raw/resources/comparison.png)

## Building from source

Install [Apache Maven](http://maven.apache.org/install.html)

Build the default snapshot

```bash
git clone https://github.com/HebiRobotics/HebiJoystick.git
mvn package
```

Create a release

```bash
mvn package -DreleaseName="hebi-joystick-1.0"
```
