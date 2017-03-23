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

~~~~
> npm install -g handbrake-watcher
~~~~

**Mac/Linux**

~~~~
$ sudo npm install -g handbrake-watcher
~~~~


## Usage:

Open a terminal window and navigate to the directory you wish
to watch.  Then run:

~~~~
$ handbrake-watcher
~~~~

This will start the daemon, which will scan the entire directory
and subdirectories every 5 minutes.  When it is finished 
converting a file, it will delete the original.

### Custom Configuration Options:

You can customize the operation of the watcher by placing a 
config file named 'handbrake.properties' in the directory that
is being watched.  The followign configuration options are 
supported:

* `source.extension` - The 'source' extension of files to look for.  Default is mkv
* `destination.extension` - The extension used for converted files. Default is mp4.  E.g. This would convert a file named *myvideo.mkv* into a file named *myvideo.mp4* in the same directory.
* `handbrakecli` - The path to the HandbrakeCLI binary.  If you have this binary in your path already, then the handbrake-watcher will use that one by default.
* `handbrake.flags` - The flags to use for the handbrake conversion.  Only provide flags that don't require a value.  E.g. --all-audio.  Separate flags by spaces. For a full list of HandbrakeCLI flags, see the [HandBrakeCLI documentation](https://handbrake.fr/docs/en/latest/cli/cli-guide.html) at >
* `handbrake.options.<optionname>` - Specify a particular handbrake command line option with value.  E.g. handbrake.options.preset=High Profile is akin to providing the command-line flag --preset='High Profile' to HandbrakeCLI.
* `delete_original` - Whether to delete the original file upon successful conversion.  Values: `true`|`false` . Default: `true`