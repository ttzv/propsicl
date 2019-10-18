package ttzv.propsicl;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Properties;

/**
 * Class Propsicl - short for Properties Init-Config-Load
 * Multipurpose class used for tasks related to properties
 * To use this you should create your own class responsible for properties that extends Propsicl.
 * Then override method defaultPropsVals() if you need default properties in your project.
 * You can create multiple different properties for different classes and modules in your program, properties are named after class inheriting Propsicl with "_def" or "_main" added.
 *
 *  */
public abstract class Propsicl {

    /**
     * field with default properties, used only if main properties object is empty/
     */
    private Properties defaultProps;

    /**
     * field with properties used during runtime, can store and load from this
     */
    private Properties props;

    /**
     * default path for saving properties
     */
    private Path propsPath;
    private String defPropsName;
    private String mainPropsName;
    private boolean modifiable;
    private String saveDir;

    /**
     * Method used for checking if files with properties exists
     * THIS DOES NOT CREATE ANYTHING, ONLY CHECKS
     * @return boolean array where first index represents default props and second represents main props
     * for example, if array[0]==true then defProps.properties exists in given path, same with main props file
     */
    private boolean[] checkFilesExisting() {
        boolean[] res = new boolean[2];

        Path resolvedPath;
        resolvedPath = propsPath.resolve(defPropsName);
        res[0] = Files.exists(resolvedPath);

        resolvedPath = propsPath.resolve(mainPropsName);
        res[1] = Files.exists(resolvedPath);

        return res;
    }

    private void createPropsDir() throws IOException{
        if(!Files.exists(propsPath)){
            Files.createDirectory(propsPath);
        }
    }

    private void createPropsFiles() throws IOException{

        boolean[] res = checkFilesExisting();

        Path resolvedPath;
        resolvedPath = propsPath.resolve(defPropsName);
        if(!res[0]){
            Files.createFile(resolvedPath);
        }
        resolvedPath = propsPath.resolve(mainPropsName);
        if(!res[1]){
            Files.createFile(resolvedPath);
        }

    }

    /**
     * Method used for defining default properties. Override this method at the start of application when object extending propsicl is created but before <i>init()</i> method.
     * To define property use:
     * <pre>
     * <code>
     * defPropSet( K, V );
     * </code>
     * K, V - string values/objects
     * </pre>
     */
    public abstract void defaultPropsVals();/*{
        ///insert all PUTS here

            defaultProps.put(PDef.BackupDir, "test1");
            defaultProps.put(PDef.SteamIdDir, "test2");

            }*/

    public void defPropSet(String key, String val){
        if(!modifiable) {
            defaultProps.put(key, val);
        } else {
            System.err.println("Cannot modify default properties after create() method was called, modify defaults properly by overriding defaultPropsVals() method of Propsicl");
        }
    }

    public void setProperty(String key, String val){
        if(modifiable) {
            props.put(key, val);
        } else {
            System.err.println("properties object not initialized, use init() method before making any changes");
        }
    }

    private void loadProperties(Properties properties, Path path) throws IOException{
        FileInputStream fis = new FileInputStream(path.toFile());
        properties.load(fis);
        fis.close();
    }

    /**
     * Save without comment
     * @param properties properties object to save
     * @param path location to save in
     * @throws IOException
     */
    private void saveProperties(Properties properties, Path path) throws IOException{
        FileOutputStream fos = new FileOutputStream(path.toFile());
        properties.store(fos, this.getClass().toString());
        fos.close();
    }

    private void saveProperties(Properties properties, Path path, String comment) throws IOException{
        FileOutputStream fos = new FileOutputStream(path.toFile());
        properties.store(fos, comment);
        fos.close();
    }


    /**
     * Loads defaults from default.properties file and main props from main.properties file, initializes main properties object with defaults list and populates it with values read from main.properties.
     */
    private void create(){
        //load from default props file and store in default object
        try {
            loadProperties(defaultProps, propsPath.resolve(defPropsName));
        } catch (IOException io){
            io.printStackTrace();
        }

        //load from main props and store in main object
        this.props = new Properties(defaultProps);
        try{
            loadProperties(props, propsPath.resolve(mainPropsName));
        } catch (IOException io){
            io.printStackTrace();
        }

        System.out.println(getClass().getSimpleName() + ": Loading properties from: " + propsPath.toAbsolutePath());
        System.out.println(getClass().getSimpleName() + ": Default properties loaded: " + defaultProps.keySet().size());
        System.out.println(getClass().getSimpleName() + ": Properties loaded: " + props.keySet().size());

        modifiable = true;
    }



    /**
     * Retrieve property stored under given key in currently loaded properties, depending on whether main properties was found, otherwise returned property comes from default predefined properties.
     * If key is not found empty string is returned
     * @param key String value of key identificator, for ease of use apply static fields of Pdef class here
     * @return String value of property stored under given key or empty string if key was not found
     */
    public String retrieveProp(String key){
        String val = props.getProperty(key);

        if(val == null){
            return "";
        } else {
            return val;
        }


    }

    /**
     * Saves any modification of properties in main properties file
     * @throws IOException
     */
    public void saveFile() throws IOException {

        Calendar calendar = Calendar.getInstance(); //get date for comment
        saveProperties(props, propsPath.resolve(this.mainPropsName), "Date of saving: " + calendar.getTime().toString());
    }

    private void saveDefaultProps() throws IOException {

        Calendar calendar = Calendar.getInstance(); //get date for comment
        saveProperties(defaultProps, propsPath.resolve(this.defPropsName), "Date of saving: " + calendar.getTime().toString());

    }

    /**
     * Method encapsulating creation of directories, files, initialization of properties etc, use at the start of application or module/class(constructor preferably).
     * @param saveDir - set to null if properties should be saved in app root dir, set to any other path, and it will save under Appdata/local
     * @throws IOException
     */
    public void init(String saveDir) throws IOException {
        //false until create() method is called to prevent changing default properties during runtime
        modifiable = false;

        defaultProps = new Properties();

        if(saveDir == null || saveDir.isEmpty()) {
            propsPath = Paths.get("cfg");
        } else {
            propsPath = Paths.get(System.getProperty("user.home"))
                    .resolve("AppData")
                    .resolve("Local")
                    .resolve(saveDir)
                    .resolve("cfg");
        }

        defPropsName = this.getClass().getSimpleName() + "_def.properties";
        mainPropsName = this.getClass().getSimpleName() + "_main.properties";

        //Create directory tree
        this.createPropsDir();
        //Create .properties files
        this.createPropsFiles();
        //put something in defaults and save it
        this.defaultPropsVals();
        this.saveDefaultProps();
        //load from filesystem and create objects
        this.create();
    }

    public Path getPropsPath() {
        return propsPath;
    }
}
