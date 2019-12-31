package top.binggo.repositoryprocessor.analysis.core.helper;

import lombok.experimental.UtilityClass;

/**
 * @author binggo
 */
@UtilityClass
public class FolderHelper {

    private static boolean isWindows() {
        String property = System.getProperty("os.name");
        return property.toLowerCase().contains("windows");
    }

    public static String getBaseDir() {
        return isWindows() ? "D:\\codeTipSourceJar" : System.getProperty("user.home")+"/file/codeTip-data";
    }

    public static String getSpliter() {
        return isWindows() ? "\\" : "/";
    }




}

