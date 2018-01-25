# MatlabInput

MatlabInput allows MATLAB users to get input from keyboards and joysticks in a non-blocking manner. 

## Installation

* Download the [latest release](https://github.com/HebiRobotics/MatlabInput/releases)
* Extract the .zip file into a folder of your choice
* Add the unzipped files to the [MATLAB path](http://www.mathworks.com/help/matlab/ref/path.html)

## Warning: Problems when used with other Java libraries

Typically the underlying Java libraries are loaded automatically at the first call. However, there is a limitation in MATLAB that prevents Java libraries to be loaded once any Java object is instantiated. Thus, if you are using other Java libraries, e.g., [HebiCam](https://github.com/HebiRobotics/HebiCam) or the [Hebi API](http://hebirobotics.com/matlab), you will need to pre-load the libraries manually before using them.

```matlab
HebiKeyboard.loadLibs();
```

## HebiKeyboard

HebiKeyboard provides a way to get keyboard input in a non-blocking manner. The default driver is based on Java-AWT and requires focus on a MATLAB window, i.e., the console, an editor window, or a figure. Note that inputs from all keyboards are combined.

### Usage

The 'read' method returns a snapshot of the current state of various standard and meta keys. Numbers and letters can be queried by indexing into a key vector. Meta keys are exposed directly as fields in the returned struct.

Display a message whenever letter 'x' and the number '0' are pressed at the same time.

```matlab
kb = HebiKeyboard();
while true
    state = read(kb);
    if all(state.keys('x0'))
        disp('x and 0 are both pressed!')
    end
    pause(0.01);
end
```

Display all pressed letters whenever shift is up

```matlab
kb = HebiKeyboard();
while true
    state = read(kb);
    down = find(state.keys('a':'z')) + 'a';
    if ~state.SHIFT
        disp(char(down));
    end
    pause(0.01);
end
```

### Driver Selection

Alternatively, users can choose to use a 'native' driver (based on [JInput](https://github.com/jinput/jinput) binaries) that may support the selection of individual keyboards and may allow reading input when MATLAB is running in the background.

| OS      | Requires Focus   | Selectable Keyboard |
|---------|------------------|---------------------|
| Windows | No               | No                  |
| OSX     | No               | Yes                 |
| Linux   | N/A (needs sudo) | N/A (needs sudo)    |

Note that this is not the default behavior because of security concerns, e.g., displaying pressed keys while entering passwords into a browser window.

```matlab
deviceId = 1;
kb = HebiKeyboard('native', deviceId);
state = read(kb);
```

## HebiJoystick

HebiJoystick is intended for people who don't have access to the [3D Animation Toolbox](https://www.mathworks.com/products/3d-animation.html) and serves as a drop-in replacement for [vrjoystick](https://www.mathworks.com/help/sl3d/vrjoystick.html).

### Usage

Create a joystick and react to button presses.

```matlab
joy = HebiJoystick(1);
while true
  [axes, buttons, povs] = read(joy);
  if any(buttons)
    disp(['Pressed buttons: ' num2str(find(buttons))]);
  end
  pause(0.1);
end
```

See [vrjoystick](https://www.mathworks.com/help/sl3d/vrjoystick.html) documentation for more information.

![comparison](https://github.com/HebiRobotics/MatlabInput/raw/resources/comparison.png)

## Notes

* There is a maximum number of events that can occur between reads. If reads don't happen frequently enough, the returned state may not match the real physical state.
* On some operating systems going into sleep mode while executing a script that reads from the joystick may make MATLAB seem unresponsive. Ctrl-C works eventually, but it may take on the order of minutes to recover.
* The number and behavior of joystick axes / buttons / povs may differ between operating systems

## Building from source

Install [Apache Maven](http://maven.apache.org/install.html)

Build the default snapshot

```bash
git clone https://github.com/HebiRobotics/MatlabInput.git
mvn package
```

Create a release

```bash
mvn package -DreleaseName="matlab-input-x.y"
```
