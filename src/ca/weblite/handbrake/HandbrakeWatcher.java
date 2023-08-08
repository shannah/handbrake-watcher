/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.weblite.handbrake;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author shannah
 */
public class HandbrakeWatcher {
    private static final String VERSION="1.0.14";
    Properties props;
    File root;
    public HandbrakeWatcher(File root, Properties props) {
        this.root = root;
        this.props = props;
    }
    
    
    private void watch() {
        while (true) {
            try {
                crawl(root);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            try {
                // We'll wait 5 minutes between crawls
                Thread.sleep(5 * 60 * 1000l);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            
        }
    }
    
    private String getBaseName(File file, String sourceExt) {
        if (!file.getName().endsWith("."+sourceExt)) {
            throw new IllegalArgumentException("File "+file.getName()+" does not end with source extension "+sourceExt);
        }
        String baseName = file.getName().substring(0, file.getName().length() - sourceExt.length() -1);
        return baseName;
        
    }
    
    private int convert(File file, String sourceExt, String destExt) throws IOException {
        File destFile = new File(getDestinationDirectoryFor(file, true), getBaseName(file, sourceExt) + "." + destExt);
        if (destFile.exists()) {
            throw new IOException(destFile.getPath() + " already exists.");
        }
        
        // Check to make sure that the file isn't currently being copied.
        if (file.lastModified() > System.currentTimeMillis() - 30000) {
            // The file has been modified in the past 30 seconds... We're playing it safe
            // and NOT doing the conversion just in case it is still being copied or something.
            System.out.println("The file "+file+" was modified less than 30 seconds ago.  It may still be in state of being copied to this location.  We're skipping it for now.");
            return 1;
        }
        
        ProcessBuilder pb = new ProcessBuilder();
        String handbrake = getProperty("handbrakecli", "HandBrakeCLI");
        
        ArrayList<String> commands = new ArrayList<String>();
        commands.add(handbrake);
        Map<String, String> handbrakeOpts = new HashMap<String,String>();
        
        for (Object key : props.keySet()) {
            String skey = (String)key;
            if ( skey.startsWith("handbrake.options.")) {
                handbrakeOpts.put(skey.substring("handbrake.options.".length()), getProperty(skey, ""));
            }
        }
        
        List<String> flags = new ArrayList<String>(Arrays.asList(getProperty("handbrake.flags", "").split(" ")));
        if (!flags.contains("--all-audio")) {
            flags.add("--all-audio");
        }
        if (!handbrakeOpts.containsKey("preset")) {
            handbrakeOpts.put("preset", "HQ 1080p30 Surround");
        }
        
        flags.stream().forEach((flag) -> {
            commands.add(flag);
        });
        
        handbrakeOpts.keySet().stream().forEach((key) -> {
            commands.add("--"+key);
            commands.add(handbrakeOpts.get(key));
        });
        
        commands.add("-i");
        commands.add(file.getAbsolutePath());
        commands.add("-o");
        commands.add(destFile.getAbsolutePath());
        
        pb.command(commands);
        pb.inheritIO();
        
        Process p = pb.start();
        try {
            if (p.waitFor() == 0) {
                // The conversion was successful
                // Let's delete the original
                if (getProperty("delete_original", "true").equals("true")) {
                    file.delete();
                }
            } else {
                System.err.println("Failed to convert file "+file+" to "+destFile+".  Exit code "+p.exitValue());
                // We'll delete the destFile because, if it failed, we don't want it
                if (destFile.exists()) {
                    destFile.delete();
                }
                return p.exitValue();
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(HandbrakeWatcher.class.getName()).log(Level.SEVERE, null, ex);
            throw new IOException(ex);
        }
        return 0;
    }
    
    private String getProperty(String key, String defaultVal) {
        return System.getProperty(key, props.getProperty(key, defaultVal));
    }

    private File getDestinationDirectoryFor(File file, boolean mkdirs) {
        String destinationDirectory = getProperty("destination.dir", null);
        if (destinationDirectory == null || destinationDirectory.isEmpty()) {
            return file.getParentFile();
        }

        try {
            destinationDirectory = destinationDirectory.replace("${src.dir}", file.getParentFile().getCanonicalPath());

            File out = new File(destinationDirectory);
            if (mkdirs && !out.exists()) {
                boolean mkdirSuccess = out.mkdirs();
                if (!mkdirSuccess) {
                    warn("Failed to create destination directory " + out + ".  Using default destination directory.");
                    return file.getParentFile();
                }

                File ignoreFile = new File(out, ".handbrake-ignore");
                ignoreFile.createNewFile();
            }

            return out;

        } catch (Exception ex) {
            warn("destination.dir was specified but an error occurred trying to parse it: " + ex.getMessage()+".");
            return file.getParentFile();
        }

    }
    
    private void crawl(File root) {
        
        // Let's rename any autonamed titles from makemkv
        if (root.isFile() && root.getName().matches("^(D\\d\\:)?title\\d\\d\\.(mkv|mp4)$")) {
            String newName = root.getName().replace("title", "Extras (autogen) ").replace(".mkv", "-behindthescenes.mkv");
            File newFile = new File(getDestinationDirectoryFor(root, true), newName);
            if (root.renameTo(newFile)) {
                root = newFile;
            }
        }
        
        if (root.isFile() && root.getName().matches("^(D\\d\\:)?Extras \\(autogen\\) \\d\\d\\.(mkv|mp4)$")) {
            String newName = root.getName().replace(".mkv", "-behindthescenes.mkv")
                    .replace(".mp4", "-behindthescenes.mp4");
            File newFile = new File(getDestinationDirectoryFor(root, true), newName);
            if (root.renameTo(newFile)) {
                root = newFile;
            }
        }
        
        if (root.isFile() && root.getName().matches("^.*_t\\d\\d\\.mkv$")) {
            String newName = root.getName().substring(0, root.getName().lastIndexOf('.')) + "-behindthescenes.mkv";
            File newFile = new File(getDestinationDirectoryFor(root,true), newName);
            if (root.renameTo(newFile)) {
                root = newFile;
            }
        }
        
        if (root.isFile() && root.getName().matches("^.*_t\\d\\d\\.mp4$")) {
            String newName = root.getName().substring(0, root.getName().lastIndexOf('.')) + "-behindthescenes.mp4";
            File newFile = new File(getDestinationDirectoryFor(root, true), newName);
            if (root.renameTo(newFile)) {
                root = newFile;
            }
        }
        
        // To make it faster to catalog sometimes we just dump disc 2 or disc 3 inside the movie
        // In this case we'll move all of the children out to the movie folder directly
        // taking care to avoid naming collisions.
        if (root.isDirectory() && "D2".equals(root.getName()) || "D3".equals(root.getName())) {
            // This is a directory with disc 2 or disc 3
            File parentDir = root.getParentFile();
            for (File child : root.listFiles()) {
                File destChild = new File(parentDir, child.getName());
                while (destChild.exists()) {
                    String destChildName = root.getName()+":"+destChild.getName();
                    destChild = new File(parentDir, destChildName);
                }
                
                if (child.renameTo(destChild)) {
                    // Since it might not be processed by this crawl,
                    // we'll process it now.
                    crawl(destChild);
                }
            }
        }
        
        String[] sourceExtensions = getProperty("source.extension", "mkv").split(" ");
        String destExtension = getProperty("destination.extension", "mp4");
        
        if (root.isFile()) {
            for (String sourceExtension : sourceExtensions) {
                sourceExtension = sourceExtension.trim();
                if (sourceExtension.isEmpty()) {
                    continue;
                }
                if (root.getName().endsWith("." + sourceExtension)) {
                    String baseName = root.getName().substring(0, root.getName().length() - sourceExtension.length() - 1);
                    File destFile = new File(getDestinationDirectoryFor(root, true), baseName + "." + destExtension);
                    if (destFile.exists()) {
                        System.err.println(destFile + " already exists.  Not converting " + root);
                    } else {
                        try {
                            int result = convert(root, sourceExtension, destExtension);
                            if (result != 0) {
                                System.err.println("Failed to convert file " + root);
                            }
                        } catch (Exception ex) {
                            System.err.println("Failed to convert file " + root + ": " + ex.getMessage());
                            ex.printStackTrace();
                        }
                    }
                }
            }
        } else if (root.isDirectory()) {
            if (new File(root, ".handbrake-ignore").exists()) {
                System.out.println("Skipping " + root + " because a .handbrake-ignore file was found.");
            }
            for (File child : root.listFiles()) {
                try {
                    crawl(child);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("--help")) {
            System.out.println(help());
            System.exit(1);
        }
        File watchFolder = new File(".");
        File propertiesFile = new File(watchFolder, "handbrake.properties");
        Properties props = new Properties();
        if (propertiesFile.exists()) {
            try (FileInputStream fis = new FileInputStream(propertiesFile)) {
                props.load(fis);
            }
        }
        System.out.println("Handbrake Watcher v"+VERSION);
        HandbrakeWatcher watcher = new HandbrakeWatcher(watchFolder, props);
        System.out.println("Watching "+watchFolder);
        watcher.watch();
    }
    
    public static String help() {
        String out = "Handbrake Watcher version "+VERSION+"\n"
                + "Created by Steve Hannah <http://www.sjhannah.com>\n\n"
                + "Synopsis:\n"
                + "--------\n\n"
                + "Handbrake Watcher is a command line tool that monitors a \n"
                + "folder and all of its subfolders for media files with a \n"
                + "designated extension (default .mkv) and transcodes them using\n"
                + "the HandbrakeCLI command-line tool into a different codec \n"
                + "(default mp4 with the 'High Profile').\n\n"
                + "Usage:\n"
                + "-----\n\n"
                + "Open a terminal window and navigate to the directory you wish\n"
                + "to watch.  Then run:\n\n"
                + "$ handbrake-watcher\n"
                + "\n"
                + "This will start the daemon, which will scan the entire directory\n"
                + "and subdirectories every 5 minutes.  When it is finished \n"
                + "converting a file, it will delete the original.\n\n"
                + "Custom Configuration Options:\n"
                + "----------------------------\n\n"
                + "You can customize the operation of the watcher by placing a \n"
                + "config file named 'handbrake.properties' in the directory that\n"
                + "is being watched.  Properties can also be specified on the \n"
                + "command-line using -Dpropname=valuename.\n"
                + "The following configuration options are \n"
                + "supported:\n\n"
                + "  source.extension - The 'source' extension of files to look \n"
                + "      for.  Default is mkv. Multiple extensions separated by \n"
                + "      spaces.\n\n"
                + "  destination.extension - The extension used for converted files.\n"
                + "      Default is mp4.  E.g. This would convert a file named\n"
                + "      myvideo.mkv into a file named myvideo.mp4 in the same\n"
                + "      directory.\n\n"
                + "  destination.dir - Optional destination directory for converted files.\n"
                + "      Default is same as source file.  E.g. This would convert a file named\n"
                + "      myvideo.mkv into a file named myvideo.mp4 in the same\n"
                + "      directory.\n"
                + "      Optional placeholder of source parent directory ${src.dir}.  E.g. "
                + "      destination.dir=${src.dir}/handbrake-converted\n"
                + "      would place files in subdirectory named handbrake-converted of the source"
                + "      file's directory.\n\n"
                + "  handbrakecli - The path to the HandbrakeCLI binary.  If you\n"
                + "      have this binary in your path already, then the \n"
                + "      handbrake-watcher will use that one by default.\n\n"
                + "  handbrake.flags - The flags to use for the handbrake \n"
                + "      conversion.  Only provide flags that don't require a \n"
                + "      value.  E.g. --all-audio.  Separate flags by spaces.\n"
                + "      For a full list of HandbrakeCLI flags, see the \n"
                + "      HandBrakeCLI documentation at \n"
                + "      <https://handbrake.fr/docs/en/latest/cli/cli-guide.html>\n"
                + "  handbrake.options.<optionname> - Specify a particular \n"
                + "      handbrake command line option with value.  E.g.\n"
                + "      handbrake.options.preset=HQ 1080p30 Surround \n"
                + "      is akin to providing the command-line flag --preset='High Profile'\n"
                + "      to HandbrakeCLI.\n"
                + "  delete_original - Whether to delete the original file upon\n"
                + "      successful conversion.  Values: true|false . Default: true";
        return out;
    }
    
    private void warn(String str) {
        Logger.getLogger(HandbrakeWatcher.class.getName()).log(Level.WARNING, str);
    }
}
