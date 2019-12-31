package top.binggo.repositoryprocessor.analysis.core.helper;

import com.google.common.collect.Sets;

import java.io.File;
import java.io.FileFilter;
import java.util.Set;

/**
 * @author binggo
 */
public class AnalysisFileFilter implements FileFilter {
    public static final FileFilter INSTANCE = new AnalysisFileFilter();
    private static final FileFilter FILE_FILTER = newOrFileFilter(new DirFileFilter(), new SourceFileFilter());

    private AnalysisFileFilter() {

    }

    private static FileFilter newOrFileFilter(FileFilter... fileFilters) {
        return pathname -> {
            for (FileFilter fileFilter : fileFilters) {
                if (fileFilter.accept(pathname)) {
                    return true;
                }
            }
            return false;
        };
    }

    @Override
    public boolean accept(File pathname) {
        return FILE_FILTER.accept(pathname);
    }

    private static class DirFileFilter implements FileFilter {
        private static final Set<String> PROHIBITED_DIRECTORY = Sets.newHashSet("bean", "pojo", "vo", "entry");

        @Override
        public boolean accept(File file) {
            return file.isDirectory() && !PROHIBITED_DIRECTORY.contains(file.getName()) && !file.isHidden() && file.canRead();
        }
    }

    private static class SourceFileFilter implements FileFilter {

        private static final Set<String> PROHIBITED_DIRECTORY =
//                Sets.newHashSet("Pojo", "Exception", "Error");
                Sets.newHashSet();
        @Override
        public boolean accept(File pathname) {
            return pathname.canRead() && pathname.isFile() && (pathname.getName().endsWith(".java") || pathname.getName().endsWith(".class"))
                    && filterClass(pathname);
        }

        private boolean filterClass(File pathname) {
            String className = pathname.getName().substring(0, pathname.getName().lastIndexOf("."));
            for (String s : PROHIBITED_DIRECTORY) {
                if (className.endsWith(s)) {
                    return false;
                }
            }
            return true;
        }
    }

}
