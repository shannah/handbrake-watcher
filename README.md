# Handbrake Watcher
Created by [Steve Hannah](http://www.sjhannah.com)

## Synopsis

Handbrake Watcher is a command line tool that monitors a 
folder and all of its subfolders for media files with a 
designated extension (default .mkv) and transcodes them using
the HandbrakeCLI command-line tool into a different codec 
(default mp4 with the 'High Profile').

## Installation

**Windows**

You will first need to install a package manager called npm if you do not have it already. [Download](https://www.npmjs.com/get-npm)
Once npm is installed on your computer, open a command prompt and run the following command:
~~~~
> npm install -g handbrake-watcher
~~~~

Once npm has installed Handbrake Watcher, you need to download the command line interface for Handbrake. [Download](https://handbrake.fr/downloads2.php)
Save the executable file in the directory where you will be running the Handbrake Watcher.

**Mac/Linux**
You will first need to install a package manager called npm if you do not have it already. [Download](https://www.npmjs.com/get-npm)
~~~~
$ sudo npm install -g handbrake-watcher
~~~~
Once npm has installed Handbrake Watcher, you need to download the command line interface for Handbrake. [Download](https://handbrake.fr/downloads2.php)
On Mac, run the .dmg file and move the executable file, named HandbrakeCLI, to where you will be running the Handbrake Watcher.

## Usage:

Open a terminal window and navigate to the directory you wish
to watch. First create a 'handbrake.properties' file.
Paste the contents as shown below.

~~~~
source.extension=mp4 mkv
destination.extension=avi
handbrake.options.preset=Fast 1080p30
~~~~

This will look for any .mp4 or .mkv file in the directory you are in and Handbrake the files into AVI format.
Save and quit the file, then run:

~~~~
$ handbrake-watcher
~~~~

This will start the daemon, which will scan the entire directory
and subdirectories every 5 minutes.  When it is finished 
converting a file, it will delete the original. If it sees that a file was recently
modified, it will not start executing Handbrake on the file until the file has not
been modified in a long time.

### Custom Configuration Options:

You can customize the operation of the watcher by placing a 
config file named 'handbrake.properties' in the directory that
is being watched. Properties may also be overridden on the command line using `-Dpropname=propvalue`

If you are passing in properties through the command line and they are giving error messages or not applying, then put them into the 'handbrake.properties' file as shown in the Usage section.

The following configuration options are supported:

* `source.extension` - The 'source' extension of files to look for.  Default is mkv.  Multiple extensions can be separated by spaces.
* `destination.extension` - The extension used for converted files. Default is mp4.  E.g. This would convert a file named *myvideo.mkv* into a file named *myvideo.mp4* in the same directory.
* `handbrakecli` - The path to the HandbrakeCLI binary.  If you have this binary in your path already, then the handbrake-watcher will use that one by default.
* `handbrake.flags` - The flags to use for the handbrake conversion.  Only provide flags that don't require a value.  E.g. --all-audio.  Separate flags by spaces. For a full list of HandbrakeCLI flags, see the [HandBrakeCLI documentation](https://handbrake.fr/docs/en/latest/cli/cli-guide.html)
* `handbrake.options.<optionname>` - Specify a particular handbrake command line option with value.  E.g. handbrake.options.preset=High Profile is akin to providing the command-line flag --preset='High Profile' to HandbrakeCLI.
* `delete_original` - Whether to delete the original file upon successful conversion.  Values: `true`|`false` . Default: `true`

An example of a 'handbrake.properties' file is:

~~~~
source.extension=mp4 mkv
destination.extension=avi
handbrake.options.preset=Fast 1080p30
~~~~
